//package com.woowanggood;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by SophiesMac on 15. 5. 12..
 */
public class LoadBalancer {//SocketHandler
    public static final String host = "localhost";
    public static final int port = 7171;

    private String sthFromRm;

    public static void main(String args[]) {
        String resourceReport;

        try {
            System.out.println("Starting serverSocket " + host + ":" + port );
            ServerSocket serverSocket = new ServerSocket(port);
            Socket socket = serverSocket.accept();

            while(true){
                DataInputStream is = new DataInputStream(socket.getInputStream());
                resourceReport = is.readUTF();//TODO
                System.out.println("rsc report: " + resourceReport);
                //socket.close();//TODO
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    static class rmThread extends Thread {
        private Socket clientSocket;
        private Socket serverSocket;

        private DataInputStream disdisWithServer;

        public rmThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

        }
    }
    */
}
