package com.woowanggood;

/**
 * Created by swchoi06 on 4/4/15.
 */
/* usage : java Server [RTSP listening port] */

import java.io.*;
import java.net.*;

public class ThreadHandler {
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

                //new thread for a client
                new EventHandler(socket).start();
                break;
            }
            catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
        }
    }
}