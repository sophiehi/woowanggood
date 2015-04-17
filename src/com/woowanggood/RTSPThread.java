package com.woowanggood;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class RTSPThread extends Thread {
    /* RTSP variables: */
    int state; //RTSP Server state == INIT or READY or PLAYING
    int RTSPSeqNumber = 0; //Sequence number of RTSP messages within the session
    String IP_4 = "172.20.10.8";
    String IP_4_client = "172.20.10.1";
    String Port = "3000";
    String RTSPSessionID = "09F6248"; //ID of the RTSP session
    String RTSPRange, RTSPContentTrack;
    //String RTSPContentBase = "rtsp://" + IP_4 + ":" + Port+"/movie.ts/", RTSPContentType = "application/sdp";
    String RTSPContentBase, RTSPContentType = "application/sdp";
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
    String RTPClientPort; // destination port for RTP packets  (given by the RTSP Client)
    String RTPCastType, RTPProfile;

    // Video variables:
    private VideoStream video; //VideoStream object used to access video frames
    private byte[] buf; //buffer used to store the images to send to the client
    //private String videoFileName = "/Users/swchoi06/Downloads/movie.ts"; //video file requested from the client
    private String videoFileName; //video file requested from the client

    private final int TS_PAYLOAD_TYPE = 33; //RTP payload type for TS video
    private final int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    private final int VIDEO_LENGTH = 500; //length of the video in frames
    int imageNumber = 0; //image number of the image currently transmitted

    //input and output stream filters:
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    final String CRLF = "\r\n";

    public RTSPThread(Socket clientSocket) {
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
            // Wait 1 sec to repeat
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

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
                    video = new VideoStream(videoFileName);

                    //init RTP socket
                    RTPSocket = new DatagramSocket(Integer.parseInt(RTPClientPort.substring(0, 5)));
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
                Thread.sleep(1000);
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

                    System.exit(0);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Send RTP Packet
    public void sendRTPPacket() {
        if (imageNumber < VIDEO_LENGTH) {
            //update current image number
            imageNumber++;

            try {
                //get next frame to send from the video, as well as its size

                int frameLength = video.getNextFrame(buf);

                //Builds an RTPpacket object containing the frame
                RTPpacket RTPPacket = new RTPpacket(TS_PAYLOAD_TYPE, imageNumber, imageNumber * FRAME_PERIOD, buf, frameLength);

                //get to total length of the full rtp packet to send
                int packetLength = RTPPacket.getLength();

                //retrieve the packet bit stream and store it in an array of bytes
                byte[] packetBits = new byte[packetLength];
                RTPPacket.getPacket(packetBits);

                //send the packet as a DatagramPacket over the UDP socket
                StringTokenizer tokens = new StringTokenizer(RTPClientPort, "-");
                UDPPacket = new DatagramPacket(packetBits, packetLength, clientIPAddr, Integer.parseInt(tokens.nextToken()));

                RTPSocket.send(UDPPacket);

                //print the header bit stream
                RTPPacket.printHeader();

                System.out.println("Send frame #" + imageNumber);
            }
            catch (Exception e) {
                System.out.println("Exception caught: " + e);
                System.exit(0);
            }
        }
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
                //extract video file name from request sentences
                videoFileName = tokens.nextToken();
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
                    RTPClientPort = tokens.nextToken().substring(12);

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
                            "o=- 1428647490294731 1 IN IP4 "+ IP_4+CRLF+
                            "s=MPEG Transport Stream, streamed by the LIVE555 Media Server"+CRLF+
                            "i=movie.ts"+CRLF+
                            "t=0 0"+CRLF+
                            "a=tool:LIVE555 Streaming Media v2014.10.20"+CRLF+
                            "a=type:broadcast"+CRLF+
                            "a=control:*"+CRLF +
                            "a=range:npt=0-"+CRLF+
                            "a=x-qt-text-nam:MPEG Transport Stream, streamed by the LIVE555 Media Server"+CRLF+
                            "a=x-qt-text-inf:movie.ts"+CRLF+
                            "m=video 0 RTP/AVP 33"+CRLF+
                            "c=IN IP4 0.0.0.0"+CRLF+
                            "b=AS:5000"+CRLF+
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
                            "client_port=" + RTPClientPort + ";server_port=9000-9001" + CRLF);
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
