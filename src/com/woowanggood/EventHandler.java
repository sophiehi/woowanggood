package com.woowanggood;

import java.io.*;
import java.net.*;
import java.util.*;

public class EventHandler extends Thread {
    /* RTSP variables: */
    int state; //RTSP Server state == INIT or READY or PLAYING
    int RTSPSeqNumber = 0; //Sequence number of RTSP messages within the session
    String IP_4 = "192.168.0.103";
    String IP_4_client = "172.20.10.1";
    String Port = "3000";
    String RTSPSessionID = "09F6248"; //ID of the RTSP session
    String RTSPRange, RTSPContentTrack;
    //String RTSPContentBase = "rtsp://" + IP_4 + ":" + Port+"/movie.ts/", RTSPContentType = "application/sdp";
    String RTSPContentBase, RTSPContentType = "application/sdp";
    private Socket RTSPSocket; //socket used to send/receive RTSP messages
    int state; //RTSP Server state == INIT or READY or PLAYING

    // RTSP states
    private final int INIT = 0;
    private final int READY = 1;
    private final int PLAYING = 2;

    // RTSP method types
    private final String SETUP = "SETUP";
    private final String PLAY = "PLAY";
    private final String PAUSE = "PAUSE";
    private final String TEARDOWN = "TEARDOWN";
    private final String OPTIONS = "OPTIONS";
    private final String DESCRIBE = "DESCRIBE";
    private String methodType;

    // RTP variables
    private DatagramSocket RTPSocket; // socket to be used to send and receive UDP packets
    private DatagramPacket UDPPacket; // UDP packet containing the video frames
    private InetAddress clientIPAddr; // Client IP address
    String RTPClientPort; // destination port for RTP packets  (given by the RTSP Client)

    // Video variables:
    private VideoStreamer video; //VideoStream object used to access video frames
    private byte[] buf = new byte[200000]; //buffer used to store the images to send to the client
    //private String videoFileName = "/Users/swchoi06/Downloads/movie.ts"; //video file requested from the client
    private String videoFileName = "movie.ts"; //video file requested from the client

    private final int TS_PAYLOAD_TYPE = 33; //RTP payload type for TS video
    private final int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    private final int VIDEO_LENGTH = 5000; //length of the video in frames
    int imageNumber = 0; //image number of the image currently transmitted

    private BufferedWriter RTSPBufferedWriter;
    private RTSPPacket RTSPPacket;

    public EventHandler(Socket clientSocket) {
        this.RTSPSocket = clientSocket;
    }

    public void run() {
        //Initiate RTSP state
        state = INIT;
        System.out.println("New event handler started!");

        //Get Client IP address
        clientIPAddr = RTSPSocket.getInetAddress();

        //Set input and output stream filters:
        try {
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPSocket.getOutputStream()));
            RTSPPacket = new RTSPPacket(RTSPSocket.getInputStream());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        receiveRTSPRequest();
        handleRTSPRequest();
    }

    public void receiveRTSPRequest() {
        // Wait for the message from the client
        boolean isDone = false;

        while (!isDone) {
            // Wait 1 sec to repeat
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Parse RTSP Request
            methodType = RTSPPacket.parseRTSPRequest();
            System.out.println("Current Method Type : " + methodType);

            switch (methodType) {
                case OPTIONS:
                    sendRTSPResponse(methodType);
                    break;
                case DESCRIBE:
                    sendRTSPResponse(methodType);
                    break;
                case SETUP:
                    isDone = true;
                try {
                    //init the VideoStream object:
                    video = new VideoStreamer(videoFileName);

                    sendRTSPResponse(methodType);

                    try {
                        //init the VideoStream object:
                        videoStreamer = new VideoStreamer(RTSPPacket.videoFileName);
                        //init RTP socket
                        //RTPSocket = new DatagramSocket(Integer.parseInt(RTPClientPort.substring(0, 5)));
                        RTPSocket = new DatagramSocket(9001, InetAddress.getByName("172.20.10.8"));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public void handleRTSPRequest() {
        //loop to handle RTSP requests
        while (true) {
            //Wait 1 sec to repeat
            try {
                Thread.sleep(0);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            //parse the request
            methodType = RTSPPacket.parseRTSPRequest();
            System.out.println("Current Request Type :" + methodType);

            if (methodType.equals(PLAY) && state == READY) {
                //update state
                state = PLAYING;
                System.out.println("RTSP server state : PLAYING");

                sendRTSPResponse(methodType);
                sendRTPPacket();
            }
            else if (methodType.equals(PAUSE) && state == PLAYING) {
                //update state
                state = READY;
                System.out.println("RTSP server state : READY");
            }
            else if (methodType.equals(TEARDOWN)) {
                try {
                    //close sockets
                    RTSPSocket.close();
                    RTPSocket.close();

                    //System.exit(0);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Send RTSP Response
    private void sendRTSPResponse(String methodType) {
        try {
            String packet = RTSPPacket.packetizeRTSPResponse(methodType);
            RTSPBufferedWriter.write(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Send RTP Packet
    public void sendRTPPacket() {
        System.out.println("Send RTPPacket Start!");

        //while (imageNumber < VIDEO_LENGTH) {
        while (true) {
            //update current image number
            imageNumber++;

            if (imageNumber % 100 == 0) {
                // Wait 1 sec to repeat
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }

            try {
                //get next frame to send from the video, as well as its size
                System.out.println(String.valueOf(imageNumber) + " ");

                int frameLength = videoStreamer.getNextSevenPacket(buf);
                int i = 0;
                while (frameLength > 0) {
                    int j = 7;
                    if (frameLength < 188 * 7) {
                        j = frameLength / 188;
                    }

                    //Builds an RTPpacket object containing the frame
                    byte[] newArray = Arrays.copyOfRange(buf, i * 188, (i + j) * 188);
                    RTPPacket RTPPacket = new RTPPacket(TS_PAYLOAD_TYPE, imageNumber + i, imageNumber + i, newArray, 188 * j);
                    newArray = null;

                    //get to total length of the full rtp packet to send
                    int packetSize = RTPPacket.getPacketSize();

                    //retrieve the packet bit stream and store it in an array of bytes
                    byte[] packetBits = RTPPacket.getPacket();

                    //send the packet as a DatagramPacket over the UDP socket
                    StringTokenizer tokens = new StringTokenizer(RTPClientPort, "-");
                    UDPPacket = new DatagramPacket(packetBits, packetSize, clientIPAddr, Integer.parseInt(tokens.nextToken()));

                    /* if (RTPSocket == null || UDPPacket == null) {
                        System.out.println("?!");
                    } */
                    RTPSocket.send(UDPPacket);

                    i = i + j;
                    frameLength -= 188 * j;
                }
                System.out.println("Send frame #" + imageNumber);
            }
            catch (Exception e) {
                System.out.println("Exception caught: " + e);
                e.printStackTrace(System.out);
                System.exit(0);
            }
        }
    }
}