package com.woowanggood;

import java.io.*;
import java.net.*;

/**
 * Created by swchoi06 on 4/4/15.
 */
public class ThreadHandler {
    public static void main(String argv[]) throws Exception {
        final int remotePort = 3000; // argv[0] == port number
        ServerSocket serverSocket = new ServerSocket(remotePort);

        while (true) {
            try {
                new EventHandler(serverSocket.accept()).start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
