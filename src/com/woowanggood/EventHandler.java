package com.woowanggood;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EventHandler extends Thread {
    /* RTSP variables: */
    int state; //RTSP Server state == INIT or READY or PLAYING
    int RTSPSeqNumber = 0; //Sequence number of RTSP messages within the session
    String IP_4 = "dev.wafflestudio.net";
    //String IP_4 = "172.20.7.236";
    //String IP_4 = "192.168.0.41";
    String IP_4_client = "0.0.0.0";
    String RTSPSessionID = "09F6248"; //ID of the RTSP session
    String RTSPRange, RTSPContentTrack;
    String RTSPContentBase, RTSPContentType = "application/sdp";
    int RTPSocketPort = 8554;
    private Socket RTSPSocket; //socket used to send/receive RTSP messages

    // RTSP states
    private final int INIT = 0;
    private final int READY = 1;
    private final int PLAYING = 2;

    // RTSP message types
    private final String SETUP = "SETUP";
    private final String PLAY = "PLAY";
    private final String PAUSE = "PAUSE";
    private final String TEARDOWN = "TEARDOWN";
    private final String GET_PARAMETER = "GET_PARAMETER";
    private final String OPTIONS = "OPTIONS";
    private final String DESCRIBE = "DESCRIBE";

    // RTP variables:
    private DatagramSocket RTPSocket; // socket to be used to send and receive UDP packets
    private DatagramPacket UDPPacket; // UDP packet containing the video frames
    private InetAddress clientIPAddr; // Client IP address
    String RTPClientPort = "3000-3001"; // destination port for RTP packets  (given by the RTSP Client)
    String RTPCastType, RTPProfile;

    // Video variables:
    private VideoStreamer video; //VideoStream object used to access video frames
    private byte[] buf = new byte[200000]; //buffer used to store the images to send to the client
    //private String videoFileName = "/Users/swchoi06/Downloads/movie.ts"; //video file requested from the client
    private String videoFileName = "movie.ts"; //video file requested from the client

    private final int TS_PAYLOAD_TYPE = 33; //RTP payload type for TS video
    private final int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    private final int VIDEO_LENGTH = 5000; //length of the video in frames
    int imageNumber = 0; //image number of the image currently transmitted

    //input and output stream filters:
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    final String CRLF = "\r\n";

    public EventHandler(Socket clientSocket) {
        this.RTSPSocket = clientSocket;
    }

    public void run() {
        //Initiate RTSP state
        state = INIT;
        System.out.println("New Thread started!");

        //Get Client IP address
        clientIPAddr = RTSPSocket.getInetAddress();

        //Set input and output stream filters:
        try {
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPSocket.getOutputStream()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Wait for the message from the client
        String requestType;
        boolean isDone = false;

        while (!isDone){
            // Parse RTSP Request
            requestType = parseRTSPRequest();
            System.out.println("Current Request Type : " + requestType);

            if (requestType.equals(OPTIONS)) {
                sendRTSPResponse(requestType);
            }else if(requestType.equals(DESCRIBE)){
                sendRTSPResponse(requestType);
            }
            else if (requestType.equals(SETUP)) {
                isDone = true;

                //update RTSP state
                state = READY;
                System.out.println("RTSP server state : READY");

                sendRTSPResponse(requestType);

                try {
                    //init the VideoStream object:
                    video = new VideoStreamer(videoFileName);

                    //init RTP socket
                    //RTPSocket = new DatagramSocket(RTPSocketPort , InetAddress.getByName(IP_4));
                    RTPSocket = new DatagramSocket(RTPSocketPort);

                    //RTPSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(IP_4),Integer.parseInt(tokens.nextToken())));
                    //RTPSocket = new DatagramSocket(Integer.parseInt(tokens.nextToken()));
                    // RTPSocket.bind(new InetSocketAddress(InetAddress.getByName(IP_4), Integer.parseInt(tokens.nextToken())));
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //loop to handle RTSP requests
        while (true) {
            // Wait 1 sec to repeat
            try {
                Thread.sleep(0);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            //parse the request
            requestType = parseRTSPRequest();

            System.out.println("Current Request Type :" + requestType);

            if (requestType.equals(PLAY) && state == READY) {
                //update state
                state = PLAYING;
                System.out.println("RTSP server state : PLAYING");

                sendRTSPResponse(requestType);
                sendRTPPacket();
            }
            else if (requestType.equals(PAUSE) && state == PLAYING) {
                //update state
                state = READY;
                System.out.println("RTSP server state : READY");
            }
            else if (requestType.equals(TEARDOWN)) {
                try {
                    //close sockets
                    RTSPSocket.close();
                    RTPSocket.close();

                    //System.exit(0);
                    break;
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Send RTP Packet
    public void sendRTPPacket() {
        System.out.println("Send RTPPacket Start!");
        //    while (imageNumber < VIDEO_LENGTH) {


        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(0);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        t.start();
        int cnt = 0;

        while(true){

            //update current image number
            imageNumber++;

            try {
                //get next frame to send from the video, as well as its size
                System.out.println(String.valueOf(imageNumber) + " ");


                int frameLength = video.getNextSevenPacket(buf);
                int i = 0;
                while(frameLength > 0){
                    int j = 7;
                    if(frameLength < 188*7){
                        j = frameLength/188;
                    }
                    for(int k=0; k<7; k++){
                        byte [] tsPacket = Arrays.copyOfRange(buf, (i+k)*188, (i+k+1)*188);
                        boolean isStartingPcaket = VideoStreamer.isStartingPacket(tsPacket);
                        if(isStartingPcaket){
                            cnt ++;
                        }
                    }

                    //Builds an RTPpacket object containing the frame
                    byte[] newArray = Arrays.copyOfRange(buf, i*188, (i+j)*188);
                    RTPPacket RTPPacket = new RTPPacket(TS_PAYLOAD_TYPE, imageNumber+i, imageNumber+i, newArray, 188*j);
                    newArray = null;

                    //get to total length of the full rtp packet to send
                    int packetLength = RTPPacket.getPacketSize();

                    //retrieve the packet bit stream and store it in an array of bytes
                    byte[] packetBits = new byte[packetLength];
                    packetBits = RTPPacket.getPacket();

                    //send the packet as a DatagramPacket over the UDP socket
                    StringTokenizer tokens = new StringTokenizer(RTPClientPort, "-");
                    UDPPacket = new DatagramPacket(packetBits, packetLength, clientIPAddr, Integer.parseInt(tokens.nextToken()));

                    if(RTPSocket == null || UDPPacket == null){
                        System.out.println("?!");
                    }
                    RTPSocket.send(UDPPacket);

                    //print the header bit stream
                    //RTPPacket.printHeader();

                    i = i+j;
                    frameLength -= 188 * j;
                    if(cnt >= 4){
                        cnt = 0;
                        t.join();
                        t = new Thread() {
                            public void run() {
                                try {
                                    Thread.sleep(49);
                                } catch(InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        };
                        t.start();
                    }
                }
                System.out.println("Send frame #" + imageNumber);
            }
            catch (Exception e) {
                System.out.println("Exception caught: " + e);
                e.printStackTrace(System.out);
                System.exit(0);
            }
        }
        //System.out.println("Send RTPPacket End!");
    }

    //Parse RTSP Request
    private String parseRTSPRequest() {
        String RTSPRequestType = "NONE";

        try {
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPSocket.getInputStream()));

            //parse request line and extract the RTSP request type
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
//                //extract video file name from request sentences
//                videoFileName = tokens.nextToken();
                System.out.println("videoFileName : " + videoFileName);
            }


            //parse the SeqNumLine and extract CSeq field
            String seqNumberLine = RTSPBufferedReader.readLine(); // CSeq: 2
            tokens = new StringTokenizer(seqNumberLine);
            tokens.nextToken(); // CSeq:

            RTSPSeqNumber = Integer.parseInt(tokens.nextToken()); // 2

            switch (RTSPRequestType) {
                case OPTIONS:
                    break;
                case DESCRIBE:
                    RTSPBufferedReader.readLine(); // userAgentLine
                    String acceptLine = RTSPBufferedReader.readLine();

                    break;
                case SETUP:
                    RTSPBufferedReader.readLine(); // userAgentLine
                    String transportLine = RTSPBufferedReader.readLine();

                    //extract RTPClientPort from transportLine
                    tokens = new StringTokenizer(transportLine);
                    tokens.nextToken(); // Transport:
                    tokens = new StringTokenizer(tokens.nextToken(), ";"); // RTP/AVP;unicast;~

                    RTPProfile = tokens.nextToken();
                    RTPCastType = tokens.nextToken();

                    System.out.println(RTPProfile + " " + RTPCastType + " " + RTPClientPort);
                    break;
                case PLAY:
                    RTSPBufferedReader.readLine(); // userAgentLine
                    String sessionLine = RTSPBufferedReader.readLine();

                    tokens = new StringTokenizer(sessionLine);
                    tokens.nextToken();
                    RTSPSessionID = tokens.nextToken();

                    String rangeLine = RTSPBufferedReader.readLine();
                    tokens = new StringTokenizer(rangeLine);
                    tokens.nextToken();
                    RTSPRange = tokens.nextToken();

                    break;
            }

        }
        catch (Exception e) {
            System.out.println("Exception caught: " + e);
            System.exit(0);
        }

        return RTSPRequestType;
    }

    //Send RTSP Response
    private void sendRTSPResponse(String requestType) {
        try {
            switch (requestType) {
                case OPTIONS :
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE" + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case DESCRIBE :
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Content-Base: " + RTSPContentBase + CRLF);
                    RTSPBufferedWriter.write("Content-Type: " + RTSPContentType + CRLF);

                    String content = "v=0"+CRLF+
                            "o=- 1430456491484288 1 IN IP4 "+ IP_4+CRLF+
                            "s=MPEG Transport Stream, streamed by the LIVE555 Media Server"+CRLF+
                            "i=movie.ts"+CRLF+
                            "t=0 0"+CRLF+
                            "a=tool:LIVE555 Streaming Media v2014.10.20"+CRLF+
                            "a=type:broadcast"+CRLF+
                            "a=control:*"+CRLF +
                            "a=range:npt=0-507.238"+CRLF+
                            "a=x-qt-text-nam:MPEG Transport Stream, streamed by the LIVE555 Media Server"+CRLF+
                            "a=x-qt-text-inf:movie.ts"+CRLF+
                            "m=video " + (new StringTokenizer(RTPClientPort, "-").nextToken()) + " RTP/AVP 33"+CRLF+
                            //"m=video 5000 RTP/AVP 33"+CRLF+
                            "c=IN IP4 0.0.0.0"+CRLF+
                            "b=AS:5111"+CRLF+
                            "a=control:track1" + CRLF;

                    RTSPBufferedWriter.write("Content-Length: " + content.length() + CRLF + CRLF);
                    RTSPBufferedWriter.write(content);

                    RTSPBufferedWriter.flush();

                    break;

                case SETUP :
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Transport: " + RTPProfile + ";" + RTPCastType + ";" +
                            "destination="+IP_4_client +";source="+ IP_4 + ";" +
                            "client_port=" + RTPClientPort + ";server_port=8888-8889" + CRLF);
                    RTSPBufferedWriter.write("Session: " + RTSPSessionID + ";timeout=" + 10 + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();
                    break;
                case PLAY :
                    RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
                    RTSPBufferedWriter.write("CSeq: " + RTSPSeqNumber + CRLF);
                    RTSPBufferedWriter.write("Date: " + getCurrentTime() + CRLF);
                    RTSPBufferedWriter.write("Range: " + RTSPRange + CRLF);
                    RTSPBufferedWriter.write("Session: " + RTSPSessionID + CRLF);
                    RTSPBufferedWriter.write("RTP-Info: " + "url=" + RTSPContentTrack + ";seq=" + 62848 + ";rtptime=" + "1988051910" + CRLF);
                    RTSPBufferedWriter.write(CRLF);
                    RTSPBufferedWriter.flush();

                    break;
                case PAUSE :
                    break;
                case TEARDOWN :
                    break;
            }

            System.out.println("RTSP server sent response to client!");
        }
        catch (Exception e) {
            System.out.println("Exception caught: " + e);
            System.exit(0);
        }
    }

    public String getCurrentTime() {
        /* This function makes date string like below sentence */
        // Sat, Apr 04 2015 08:58:18 GMT\r\n

        SimpleDateFormat responseDateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss z", Locale.ENGLISH);
        return responseDateFormat.format(new Date());
    }
}