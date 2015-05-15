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
        int RTSPPort = 3000, RTPPort = 4000;

        try {
            RTSPPort = 3000;
            serverSocket = new ServerSocket(RTSPPort);
        }
        catch (BindException e) {
            e.printStackTrace();
            RTSPPort = 3001;
            serverSocket = new ServerSocket(RTSPPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("Server started! port : " + String.valueOf(RTSPPort));
        }

        /*new Thread() {
            @Override
            public void run() {
                try {
                    ResourceMonitor resourceMonitor = new ResourceMonitor();
                    resourceMonitor.startReporting();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();*/

        while (true) {
            try {
                socket = serverSocket.accept();

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                String clientIPAddr = dataInputStream.readUTF();
                System.out.println("clientIPAddr : " + clientIPAddr);

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