package com.woowanggood;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by SophiesMac on 15. 5. 12..
 */
public class LoadBalancer {//todo merge with SocketHandler
    public static final String host = "localhost";
    public static final int port = 7171;

    public static void main(String args[]) {
        //todo SocketHandler에서 different port소켓 하나 열기
        String resourceReport;

        try {System.out.println("Starting serverSocket " + host + ":" + port );
            ServerSocket serverSocket = new ServerSocket(port);
            //todo change blocking socket to a new thread
            Socket socket = serverSocket.accept();

            while(true){
                DataInputStream is = new DataInputStream(socket.getInputStream());
                resourceReport = is.readUTF();
                System.out.println(resourceReport);
                //resourceReport json example as below:
                /* {"192.168.0.127:51926":
                    {{"processCPUpercent":"6.46434166426508E-4"},{"availableVMsize":"6170157056"},
                    {"systemCPUpercent":"0.07711442786069651"},{"systemPMpercent":"0.0"}}} */
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
