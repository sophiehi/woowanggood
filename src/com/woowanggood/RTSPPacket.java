package com.woowanggood;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Created by KangGyu on 2015-04-18.
 */
public class RTSPPacket {
    /* RTSP variables: */
    int RTSPSeqNumber = 0; //Sequence number of RTSP messages within the session
    String IP_4 = "172.20.10.8";
    String IP_4_client = "172.20.10.1";
    String RTSPSessionID = "09F6248"; //ID of the RTSP session
    String RTSPRange, RTSPContentTrack;
    String RTSPContentBase, RTSPContentType = "application/sdp";
    // String RTSPContentBase = "rtsp://" + IP_4 + ":" + Port+"/movie.ts/", RTSPContentType = "application/sdp";

    // RTSP message types
    private final String SETUP = "SETUP";
    private final String PLAY = "PLAY";
    private final String PAUSE = "PAUSE";
    private final String TEARDOWN = "TEARDOWN";
    private final String OPTIONS = "OPTIONS";
    private final String DESCRIBE = "DESCRIBE";

    // RTP variables:
    String RTPClientPort; // destination port for RTP packets  (given by the RTSP Client)
    String RTPCastType, RTPProfile;

    // Video variables:
    public String videoFileName = "movie.ts"; //video file requested from the client
    // private String videoFileName = "/Users/swchoi06/Downloads/movie.ts"; //video file requested from the client

    // input and output stream filters:
    private BufferedReader RTSPBufferedReader;
    private InputStream is;
    final String CRLF = "\r\n";

    public RTSPPacket(InputStream is) {
        this.is = is;
    }

    public String parseRTSPRequest() {
        String RTSPRequestType = "NONE";

        try {
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(is));

            //parse request line and extract the RTSP request type
            String requestLine = RTSPBufferedReader.readLine(); // ex) OPTIONS rtsp://192.168.0.6:8554/H264_720p.ts RTSP/1.0
            if (requestLine == null || requestLine.equals(""))
                return RTSPRequestType;

            StringTokenizer tokens = new StringTokenizer(requestLine);
            RTSPRequestType = tokens.nextToken(); // It is 'OPTIONS' if requestLine is equal to above sentence

            if (RTSPRequestType.equals(SETUP))
                RTSPContentTrack = tokens.nextToken(); // rtsp://192.168.0.6:8554/H264_720p.ts/track1
            else
                RTSPContentBase = tokens.nextToken(); // rtsp://192.168.0.6:8554/H264_720p.ts

            if (RTSPRequestType.equals(SETUP)) {
                // extract video file name from request sentences
                // videoFileName = tokens.nextToken();
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

    public String packetizeRTSPResponse(String methodType) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            switch (methodType) {
                case OPTIONS :
                    stringBuilder.append("RTSP/1.0 200 OK" + CRLF);
                    stringBuilder.append("CSeq: " + RTSPSeqNumber + CRLF);
                    stringBuilder.append("Date: " + getCurrentTime() + CRLF);
                    stringBuilder.append("Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE" + CRLF);
                    stringBuilder.append(CRLF);
                    break;
                case DESCRIBE :
                    stringBuilder.append("RTSP/1.0 200 OK" + CRLF);
                    stringBuilder.append("CSeq: " + RTSPSeqNumber + CRLF);
                    stringBuilder.append("Date: " + getCurrentTime() + CRLF);
                    stringBuilder.append("Content-Base: " + RTSPContentBase + CRLF);
                    stringBuilder.append("Content-Type: " + RTSPContentType + CRLF);

                    String content = "v=0" + CRLF +
                            "o=- 1428647490294731 1 IN IP4 "+ IP_4 + CRLF +
                            "s=MPEG Transport Stream, streamed by the LIVE555 Media Server" + CRLF +
                            "i=movie.ts" + CRLF +
                            "t=0 0" + CRLF +
                            "a=tool:LIVE555 Streaming Media v2014.10.20" + CRLF +
                            "a=type:broadcast" + CRLF +
                            "a=control:*" + CRLF +
                            "a=range:npt=0-" + CRLF +
                            "a=x-qt-text-nam:MPEG Transport Stream, streamed by the LIVE555 Media Server" + CRLF +
                            "a=x-qt-text-inf:movie.ts" + CRLF +
                            "m=video 0 RTP/AVP 33" + CRLF +
                            "c=IN IP4 0.0.0.0" + CRLF +
                            "b=AS:5000" + CRLF +
                            "a=control:track1" + CRLF;

                    stringBuilder.append("Content-Length: " + content.length() + CRLF + CRLF);
                    stringBuilder.append(content);
                    break;
                case SETUP :
                    stringBuilder.append("RTSP/1.0 200 OK" + CRLF);
                    stringBuilder.append("CSeq: " + RTSPSeqNumber + CRLF);
                    stringBuilder.append("Date: " + getCurrentTime() + CRLF);
                    stringBuilder.append("Transport: " + RTPProfile + ";" + RTPCastType + ";" +
                            "destination=" + IP_4_client + ";source=" + IP_4 + ";" +
                            "client_port=" + RTPClientPort + ";server_port=9000-9001" + CRLF);
                    stringBuilder.append("Session: " + RTSPSessionID + ";timeout=" + 10 + CRLF);
                    stringBuilder.append(CRLF);
                    break;
                case PLAY :
                    stringBuilder.append("RTSP/1.0 200 OK" + CRLF);
                    stringBuilder.append("CSeq: " + RTSPSeqNumber + CRLF);
                    stringBuilder.append("Date: " + getCurrentTime() + CRLF);
                    stringBuilder.append("Range: " + RTSPRange + CRLF);
                    stringBuilder.append("Session: " + RTSPSessionID + CRLF);
                    stringBuilder.append("RTP-Info: " + "url=" + RTSPContentTrack + ";seq=" + 62848 + ";rtptime=" + "1988051910" + CRLF);
                    stringBuilder.append(CRLF);
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

        return stringBuilder.toString();
    }

    public String getCurrentTime() {
        /* This function makes date string like below sentence */
        // Sat, Apr 04 2015 08:58:18 GMT\r\n

        SimpleDateFormat responseDateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss z", Locale.ENGLISH);
        return responseDateFormat.format(new Date());
    }
}
