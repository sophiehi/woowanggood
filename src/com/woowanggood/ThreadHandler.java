package com.woowanggood;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadHandler {
    public static void main(String argv[]) throws Exception {
        ServerSocket serverSocket = null;
        Socket socket = null;

        int RTSPPort = 3000, RTPPort = 4000;

        // user IPv4
        System.setProperty("java.net.preferIPv4Stack", "true");

        try {
            RTSPPort = 3000;
            serverSocket = new ServerSocket(RTSPPort);
        } catch (BindException e) {
            e.printStackTrace();
            RTSPPort = 3001;
            serverSocket = new ServerSocket(RTSPPort);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Server started! port : " + String.valueOf(RTSPPort));
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    ResourceMonitor resourceMonitor = new ResourceMonitor();
                    resourceMonitor.startReporting();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        while (true) {
            try {
                socket = serverSocket.accept();

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                String clientIPAddr = dataInputStream.readUTF();
                System.out.println("clientIPAddr : " + clientIPAddr);

                // new thread for a client
                new EventHandler(socket, RTPPort++, clientIPAddr).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
