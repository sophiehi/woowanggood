package com.woowanggood;

import java.io.*;
import java.net.*;

/**
 * Created by swchoi06 on 4/4/15.
 */
public class ThreadHandler {
    public static void main(String argv[]) throws Exception {
        final int remotePort = Integer.valueOf(argv[1]); // argv[1] == port number

        while (true) {
            try {
                Socket socket = new Socket(SocketHandler.host, remotePort);
                new EventHandler(socket).start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
