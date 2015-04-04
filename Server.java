/* Server
usag: java Server [RTSP listening port]
---------------------- */


import java.io.*;
import java.net.*;
import java.util.*;

public class Server{
  //------------------------------------
  //main
  //------------------------------------
  public static void main(String argv[]) throws Exception
  {
    System.out.println("Server started");

    ServerSocket serverSocket = null;
    Socket socket = null;

    //get RTSP socket port from the command line
    int RTSPport = Integer.parseInt(argv[0]);

    try {
      serverSocket = new ServerSocket(RTSPport);
    } catch (IOException e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        socket = serverSocket.accept();
      } catch (IOException e) {
        System.out.println("I/O error: " + e);
      }
      // new threa for a client
      new RTSPThread(socket).start();
    }
  }
}
