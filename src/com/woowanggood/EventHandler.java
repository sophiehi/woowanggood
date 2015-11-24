package com.woowanggood;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class EventHandler extends Thread {
    // RTSP variables
    private int state; // RTSP Server state == INIT or READY or PLAYING
    private int RTSPSeqNumber = 0; // Sequence number of RTSP messages within the session
    private String IP_4 = "127.0.0.1";
    private String clientIP;
    private String RTSPSessionID = "09F6248"; // ID of the RTSP session
    private String RTSPRange, RTSPContentTrack;
    private String RTSPContentBase, RTSPContentType = "application/sdp";
    private int RTPSocketPort;
    private Socket RTSPSocket; // Socket used to send/receive RTSP messages

    // RTSP states
    private final int INIT = 0;
    private final int READY = 1;
    private final int PLAYING = 2;

    // RTSP message types
    private final String SETUP = "SETUP";
    private final String PLAY = "PLAY";
    private final String PAUSE = "PAUSE";
    private final String TEARDOWN = "TEARDOWN";
    private final String OPTIONS = "OPTIONS";
    private final String DESCRIBE = "DESCRIBE";
    private final String NONE = "NONE";

    // RTP variables
    private DatagramSocket RTPSocket; // socket to be used to send and receive UDP packets
    private DatagramPacket UDPPacket; // UDP packet containing the video frames
    private InetAddress clientIPAddr; // client IP address
    private String RTPClientPort; // destination port for RTP packets  (given by the RTSP Client)
    private String RTPCastType, RTPProfile;

    // Video variables
    private VideoStreamer videoStreamer; // VideoStream object used to access video frames
    private byte[] buf = new byte[200000]; // buffer used to store the images to send to the client
    // private String videoFileName = "/Users/swchoi06/Downloads/movie.ts"; // video file requested from the client
    private String videoFileName = "movie.ts"; // video file requested from the client

    private final int TS_PAYLOAD_TYPE = 33; //RTP payload type for TS video
    private final String VIDEO_LENGTH = "507.238"; //length of the video in sec
    private int imageNumber = 0; //image number of the image currently transmitted

    // Input and output stream filters:
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    private final String CRLF = "\r\n";

    private boolean isSendingRTPPacketPaused = false;
    private Thread sendRTPThread;

    public EventHandler(Socket clientSocket, int RTPPort, String clientIP) {
        try {
            this.RTSPSocket = clientSocket;
            this.clientIP = clientIP;
            this.RTPSocketPort = RTPPort;
            this.RTPClientPort = String.valueOf(clientSocket.getLocalPort()) + "-" + String.valueOf(clientSocket.getLocalPort() + 1);
            this.clientIPAddr = InetAddress.getByName(this.clientIP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        // user IPv4
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Initiate RTSP state
        state = INIT;
        System.out.println("New Thread started!");

        // Set input and output stream filters:
        try {
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPSocket.getOutputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Wait for the message from the client
        String requestType;
        boolean isDone = false;

        while (!isDone) {
            // Parse RTSP Request
            requestType = parseRTSPRequest();

            if (!requestType.equals(NONE))
                System.out.println("Current Request Type : " + requestType);

            if (requestType.equals(OPTIONS)) {
                sendRTSPResponse(requestType);
            } else if (requestType.equals(DESCRIBE)) {
                sendRTSPResponse(requestType);
            } else if (requestType.equals(SETUP)) {
                isDone = true;

                // update RTSP state
                state = READY;
                System.out.println("RTSP server state : READY");

                sendRTSPResponse(requestType);

                try {
                    // init the VideoStream object:
                    videoStreamer = new VideoStreamer(videoFileName, true);

                    // init RTP socket
                    RTPSocket = new DatagramSocket(RTPSocketPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // loop to handle RTSP requests
        while (true) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            // parse the request
            requestType = parseRTSPRequest();
            System.out.println("Current Request Type :" + requestType);

            if (requestType.equals(PLAY) && state == READY) {
                // update state
                state = PLAYING;
                System.out.println("RTSP server state : PLAYING");

                sendRTSPResponse(requestType);
                isSendingRTPPacketPaused = false;

                StringTokenizer tokens = new StringTokenizer(RTSPRange, "=");
                tokens.nextToken();
                StringTokenizer range = new StringTokenizer(tokens.nextToken(), "-");

                double min = Double.parseDouble(range.nextToken());
                try {
                    if (min != 0.0)
                        videoStreamer.seek(min);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (sendRTPThread == null) {
                    sendRTPThread = new Thread() {
                        public void run() {
                            try {
                                sendRTPPacket();
                            } catch (Exception e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    };
                    sendRTPThread.start();
                }
            } else if (requestType.equals(PAUSE) && state == PLAYING) {
                state = READY;
                System.out.println("RTSP server state : READY");

                sendRTSPResponse(requestType);
                isSendingRTPPacketPaused = true;
            } else if (requestType.equals(TEARDOWN)) {
                try {
                    sendRTSPResponse(requestType);
                    sendRTPThread.interrupt();

                    if (RTSPSocket != null)
                        RTSPSocket.close();
                    if (RTPSocket != null)
                        RTPSocket.close();

                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Send RTP Packet
    public void sendRTPPacket() {
        System.out.println("Send RTPPacket Start!");

        Thread thread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    ex.printStackTrace();
                }
            }
        };
        thread.start();

        int cnt = 0;
        int numberOfFrame = 0;

        while (true) {
            if (isSendingRTPPacketPaused) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }

            // update current image number
            imageNumber++;

            try {
                // get next frame to send from the video, as well as its size
                System.out.println(String.valueOf(imageNumber) + " ");

                int frameLength = videoStreamer.getNextSevenPacket();
                int i = 0;
                System.out.println("Frame length : " + frameLength);

                while (frameLength > 0) {
                    int j = 7;
                    if (frameLength < 188 * 7) {
                        j = frameLength / 188;
                    }

                    for (int k = 0; k < 7; k++) {
                        byte[] tsPacket = Arrays.copyOfRange(buf, (i + k) * 188, (i + k + 1) * 188);
                        boolean isStartingPacket = videoStreamer.isStartingPacket(tsPacket);
                        if (isStartingPacket) {
                            numberOfFrame++;
                            cnt++;
                        }
                    }

                    // Builds an RTPPacket object containing the frame
                    byte[] newArray = Arrays.copyOfRange(buf, i * 188, (i + j) * 188);
                    RTPPacket RTPPacket = new RTPPacket(TS_PAYLOAD_TYPE, imageNumber + i, numberOfFrame * 30000, newArray, 188 * j);
                    newArray = null;

                    // Get total length of the full rtp packet to send
                    int packetLength = RTPPacket.getPacketSize();

                    // Retrieve the packet bit stream and store it in an array of bytes
                    byte[] packetBits = RTPPacket.getPacket();

                    // Send the packet as a DatagramPacket over the UDP socket
                    StringTokenizer tokens = new StringTokenizer(RTPClientPort, "-");
                    int port = Integer.parseInt(tokens.nextToken());
                    UDPPacket = new DatagramPacket(packetBits, packetLength, clientIPAddr, port);

                    System.out.println("client port in UDP packet : " + port);

                    if (RTPSocket == null || UDPPacket == null) {
                        System.out.println("RTPSocket or UDPPacket is null?");
                    }
                    RTPSocket.send(UDPPacket);

                    i = i + j;
                    frameLength -= 188 * j;

                    if (cnt >= 3) {
                        cnt = cnt - 3;
                        thread.join();
                        thread = new Thread() {
                            public void run() {
                                try {
                                    Thread.sleep(102);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    ex.printStackTrace();
                                }
                            }
                        };
                        thread.start();
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception caught : " + e);
                thread.interrupt();
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.out.println("Exception caught : " + e);
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Parse RTSP Request
    private String parseRTSPRequest() {
        String RTSPRequestType = "NONE";

        try {
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPSocket.getInputStream()));

            // parse request line and extract the RTSP request type
            String requestLine = RTSPBufferedReader.readLine(); // ex) OPTIONS rtsp://192.168.0.6:8554/H264_720p.ts RTSP/1.0
            if (requestLine == null || requestLine.equals("")) {
                return RTSPRequestType;
            }

            StringTokenizer tokens = new StringTokenizer(requestLine);
            RTSPRequestType = tokens.nextToken(); // It is 'OPTIONS' if requestLine is equal to above sentence

            if (RTSPRequestType.equals(SETUP))
                RTSPContentTrack = tokens.nextToken(); // rtsp://192.168.0.6:8554/H264_720p.ts/track1
            else
                RTSPContentBase = tokens.nextToken(); // rtsp://192.168.0.6:8554/H264_720p.ts

            if (RTSPRequestType.equals(SETUP)) {
                System.out.println("videoFileName : " + videoFileName);
            }

            // parse the SeqNumLine and extract CSeq field
            String seqNumberLine = RTSPBufferedReader.readLine(); // CSeq: 2
            tokens = new StringTokenizer(seqNumberLine);
            tokens.nextToken(); // CSeq:

            RTSPSeqNumber = Integer.parseInt(tokens.nextToken()); // 2

            switch (RTSPRequestType) {
                case OPTIONS:
                    break;
                case DESCRIBE:
                    RTSPBufferedReader.readLine();
                    String acceptLine = RTSPBufferedReader.readLine();
                    break;
                case SETUP:
                    RTSPBufferedReader.readLine();
                    String transportLine = RTSPBufferedReader.readLine();

                    // extract RTPClientPort from transportLine
                    tokens = new StringTokenizer(transportLine);
                    tokens.nextToken(); // Transport:
                    tokens = new StringTokenizer(tokens.nextToken(), ";"); // RTP/AVP;unicast;~

                    RTPProfile = tokens.nextToken();
                    RTPCastType = tokens.nextToken();
                    System.out.println(RTPProfile + " " + RTPCastType + " " + RTPClientPort);
                    break;
                case PLAY:
                    RTSPBufferedReader.readLine();
                    String sessionLine = RTSPBufferedReader.readLine();

                    tokens = new StringTokenizer(sessionLine);
                    tokens.nextToken();
                    RTSPSessionID = tokens.nextToken();

                    String rangeLine = RTSPBufferedReader.readLine();
                    String temp = RTSPRange;

                    try {
                        tokens = new StringTokenizer(rangeLine);
                        tokens.nextToken();
                        RTSPRange = tokens.nextToken() + VIDEO_LENGTH;
                    } catch (NoSuchElementException e) {
                        RTSPRange = temp;
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("Exception caught : " + e);
            e.printStackTrace();
        }

        return RTSPRequestType;
    }

    // Send RTSP response
    private void sendRTSPResponse(String requestType) {
        try {
            switch (requestType) {
                case OPTIONS:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE" + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case DESCRIBE:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Content-Base: " + RTSPContentBase + CRLF);
                    RTSPBufferedWriter.write("Content-Type: " + RTSPContentType + CRLF);

                    String content = "v=0" + CRLF +
                            "o=- 1430456491484288 1 IN IP4 " + IP_4 + CRLF +
                            "s=MPEG Transport Stream, streamed by the LIVE555 Media Server" + CRLF +
                            "i=movie.ts" + CRLF +
                            "t=0 0" + CRLF +
                            "a=tool:LIVE555 Streaming Media v2014.10.20" + CRLF +
                            "a=type:broadcast" + CRLF +
                            "a=control:*" + CRLF +
                            "a=range:npt=0-" + VIDEO_LENGTH + CRLF +
                            "a=x-qt-text-nam:MPEG Transport Stream, streamed by the LIVE555 Media Server" + CRLF +
                            "a=x-qt-text-inf:movie.ts" + CRLF +
                            "m=video " + (new StringTokenizer(RTPClientPort, "-").nextToken()) + " RTP/AVP 33" + CRLF +
                            // "m=video 5000 RTP/AVP 33"+CRLF+
                            "c=IN IP4 0.0.0.0" + CRLF +
                            "b=AS:5111" + CRLF +
                            "a=control:track1" + CRLF;

                    RTSPBufferedWriter.write("Content-Length: " + content.length() + CRLF + CRLF);
                    RTSPBufferedWriter.write(content);
                    RTSPBufferedWriter.flush();
                    break;
                case SETUP:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Transport: " + RTPProfile + ";" + RTPCastType + ";" +
                            "destination=" + clientIP + ";source=" + IP_4 + ";" +
                            "client_port=" + RTPClientPort + CRLF);
                    //"client_port=" + RTPClientPort + ";server_port=8888-8889" + CRLF);
                    RTSPBufferedWriter.write("Session: " + RTSPSessionID + ";timeout=" + 10 + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case PLAY:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Range: " + RTSPRange + CRLF);
                    RTSPBufferedWriter.write("Session: " + RTSPSessionID + CRLF);
                    RTSPBufferedWriter.write("RTP-Info: " + "url=" + RTSPContentTrack + ";seq=" + 62848 + ";rtptime=" + "1988051910" + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case PAUSE:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Session: " + RTSPSessionID + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case TEARDOWN:
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
            }

            System.out.println("RTSP server sent response to client!");
        } catch (Exception e) {
            System.out.println("Exception caught: " + e);
            e.printStackTrace();
        }
    }

    public String getCurrentTime() {
        /* This function makes date string like below sentence. */
        // Sat, Apr 04 2015 08:58:18 GMT\r\n

        SimpleDateFormat responseDateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss z", Locale.ENGLISH);
        return responseDateFormat.format(new Date());
    }
}
