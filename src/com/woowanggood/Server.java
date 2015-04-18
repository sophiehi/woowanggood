package com.woowanggood;

/**
 * Created by swchoi06 on 4/4/15.
 */
/* Server
usag: java Server [RTSP listening port]
---------------------- */


import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    public static void main(String argv[]) throws Exception {

        ServerSocket serverSocket = null;
        Socket socket = null;

        //get RTSP socket port from the command line
        int RTSPPort = 3000;

        System.out.println("Server started! port : " + String.valueOf(RTSPPort));

        try {
            serverSocket = new ServerSocket(RTSPPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                socket = serverSocket.accept();

                // new thread for a client
                new RTSPThread(socket).start();
            }
            catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
        }
    }
}
