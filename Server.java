/* Server
usag: java Server [RTSP listening port]
---------------------- */


import java.io.*;
import java.net.*;
import java.util.*;

public class Server  {

  //RTP variables:
  //----------------
  DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
  DatagramPacket senddp; //UDP packet containing the video frames

  InetAddress ClientIPAddr; //Client IP address
  int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

  //Video variables:
  //----------------
  int imagenb = 0; //image nb of the image currently transmitted
  VideoStream video; //VideoStream object used to access video frames
  static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
  static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
  static int VIDEO_LENGTH = 500; //length of the video in frames

  Timer timer; //timer used to send the images at the video frame rate
  byte[] buf; //buffer used to store the images to send to the client 

  //RTSP variables
  //----------------
  //rtsp states
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  //rtsp message types
  final static int SETUP = 3;
  final static int PLAY = 4;
  final static int PAUSE = 5;
  final static int TEARDOWN = 6;
  final static int OPTIONS = 7;

  static int state; //RTSP Server state == INIT or READY or PLAY
  Socket RTSPsocket; //socket used to send/receive RTSP messages
  //input and output stream filters
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String VideoFileName; //video file requested from the client
  static int RTSP_ID = 123456; //ID of the RTSP session
  int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

  final static String CRLF = "\r\n";

  //--------------------------------
  //Constructor
  //--------------------------------
  public Server(){
    //allocate memory for the sending buffer
    buf = new byte[15000]; 
  }

  //------------------------------------
  //main
  //------------------------------------
  public static void main(String argv[]) throws Exception
  {

    System.out.println("Server started");
    //create a Server object
    Server theServer = new Server();

    //get RTSP socket port from the command line
    int RTSPport = Integer.parseInt(argv[0]);

    //Initiate TCP connection with the client for the RTSP session
    ServerSocket listenSocket = new ServerSocket(RTSPport);
    theServer.RTSPsocket = listenSocket.accept();
    listenSocket.close();

    //Get Client IP address
    theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();

    //Initiate RTSPstate
    state = INIT;

    //Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theServer.RTSPsocket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.RTSPsocket.getOutputStream()) );

    //Wait for the SETUP message from the client
    int request_type;
    boolean done = false;
    while(!done)
    {
      request_type = theServer.parse_RTSP_request(); //blocking
      if(request_type != -1){
        System.out.printf("Request_type = %d\n", request_type);
      }

      if (request_type == OPTIONS){
        // Send response
        theServer.send_RTSP_response(request_type);
      }
      else if (request_type == SETUP)
      {
        done = true;

        //update RTSP state
        state = READY;
        System.out.println("New RTSP state: READY");

        //Send response
        theServer.send_RTSP_response(request_type);

        //init the VideoStream object:
        theServer.video = new VideoStream(VideoFileName);

        //init RTP socket
        theServer.RTPsocket = new DatagramSocket();
      }
    }

    //loop to handle RTSP requests
    while(true)
    {
      //parse the request
      request_type = theServer.parse_RTSP_request(); //blocking

      if ((request_type == PLAY) && (state == READY))
      {
        //update state
        state = PLAYING;
        System.out.println("New RTSP state: PLAYING");
      }
      else if ((request_type == PAUSE) && (state == PLAYING))
      {
        //update state
        state = READY;
        System.out.println("New RTSP state: READY");
      }
      else if (request_type == TEARDOWN)
      {
        //close sockets
        theServer.RTSPsocket.close();
        theServer.RTPsocket.close();

        System.exit(0);
      }
    }
  }


  //------------------------
  // Send RTP Packet
  //------------------------
  public void sendRTPPacket() {
    //if the current image nb is less than the length of the video
    if (imagenb < VIDEO_LENGTH)
    {
      //update current imagenb
      imagenb++;

      try {
        //get next frame to send from the video, as well as its size
        int frame_length = video.getnextframe(buf);

        //Builds an RTPpacket object containing the frame
        RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, buf, frame_length);

        //get to total length of the full rtp packet to send
        int packet_length = rtp_packet.getlength();

        //retrieve the packet bitstream and store it in an array of bytes
        byte[] packet_bits = new byte[packet_length];
        rtp_packet.getpacket(packet_bits);

        //send the packet as a DatagramPacket over the UDP socket 
        senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
        RTPsocket.send(senddp);

        //System.out.println("Send frame #"+imagenb);
        //print the header bitstream
        rtp_packet.printheader();

        System.out.println("Send frame #" + imagenb);
      }
      catch(Exception ex)
      {
        System.out.println("Exception caught: "+ex);
        System.exit(0);
      }
    }
  }

  //------------------------------------
  //Parse RTSP Request
  //------------------------------------
  private int parse_RTSP_request()
  {
    int request_type = -1;
    try{
      //parse request line and extract the request_type:
      String RequestLine = RTSPBufferedReader.readLine();
      if(RequestLine == null || RequestLine.equals("")){
        return -1;
      }

      StringTokenizer tokens = new StringTokenizer(RequestLine);
      String request_type_string = tokens.nextToken();
      System.out.println(request_type_string);

      //convert to request_type structure:
      if ((new String(request_type_string)).compareTo("SETUP") == 0)
        request_type = SETUP;
      else if ((new String(request_type_string)).compareTo("PLAY") == 0)
        request_type = PLAY;
      else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
        request_type = PAUSE;
      else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
        request_type = TEARDOWN;
      else if ((new String(request_type_string)).compareTo("OPTIONS") == 0)
        request_type = OPTIONS;

      if (request_type == SETUP)
      {
        //extract VideoFileName from RequestLine
        VideoFileName = tokens.nextToken();
      }

      //parse the SeqNumLine and extract CSeq field
      String SeqNumLine = RTSPBufferedReader.readLine();
      System.out.println(SeqNumLine);
      tokens = new StringTokenizer(SeqNumLine);
      tokens.nextToken();
      RTSPSeqNb = Integer.parseInt(tokens.nextToken());

      //get LastLine
      String LastLine = RTSPBufferedReader.readLine();
      System.out.println(LastLine);

      if (request_type == SETUP)
      {
        //extract RTP_dest_port from LastLine
        tokens = new StringTokenizer(LastLine);
        for (int i=0; i<3; i++)
          tokens.nextToken(); //skip unused stuff
        RTP_dest_port = Integer.parseInt(tokens.nextToken());
      }
      //else LastLine will be the SessionId line ... do not check for now.
    }
    catch(Exception ex)
    {
      System.out.println("Exception caught: "+ex);
      System.exit(0);
    }
    return(request_type);
  }

  //------------------------------------
  //Send RTSP Response
  //------------------------------------
  private void send_RTSP_response(int request_type)
  {
    try{
      if(request_type == OPTIONS){
        RTSPBufferedWriter.write("RTSP/1.0 200 OK");
        RTSPBufferedWriter.write("CSeq: 1");
        RTSPBufferedWriter.write("Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
        RTSPBufferedWriter.flush();
        System.out.println("RTSP Server - Sent response to Client.");
      }else{
        RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
        RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
        RTSPBufferedWriter.write("Session: "+RTSP_ID+CRLF);
        RTSPBufferedWriter.flush();
        System.out.println("RTSP Server - Sent response to Client.");
      }
    }
    catch(Exception ex)
    {
      System.out.println("Exception caught: "+ex);
      System.exit(0);
    }
  }
}
