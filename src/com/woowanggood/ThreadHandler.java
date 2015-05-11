package com.woowanggood;

import java.io.*;
import java.net.*;

/**
 * Created by swchoi06 on 4/4/15.
 */
public class ThreadHandler {
    public static void main(String argv[]) throws Exception {
        ServerSocket serverSocket = null;
        Socket socket = null;

        //user IPv4
        System.setProperty("java.net.preferIPv4Stack" , "true");

        //get RTSP socket port from the command line
        int RTSPPort = 3000;
        int RTPPort = 4000;

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

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                String clientIPAddr = dataInputStream.readUTF();

                //new thread for a client
                new EventHandler(socket, RTPPort++, clientIPAddr).start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
