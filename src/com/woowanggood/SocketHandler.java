package com.woowanggood;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketHandler {
    public static final String hostA = "192.168.0.107";
    public static final String hostB = "192.168.0.106";
    public static final int localPort = 2000;
    public static final int remotePort = 3000;

    public static void main(String[] args) {
        try {
            // Print a start-up message
            System.out.println("Starting proxy server!");

            ServerSocket proxyServerSocket = null;
            try {
                // Create a ServerSocket to listen for connections with clients
                proxyServerSocket = new ServerSocket(localPort);
                new Thread() {
                    @Override
                    public void run() {
                        new LoadBalancer().start();
                    }
                }.start();
            } catch (IOException e) {
                System.out.println("proxyServerSocket open error");
                e.printStackTrace();
            }

            while (true) {
                Socket socket = proxyServerSocket.accept();
                int serverNumber = LoadBalancer.ResourceMonitorThread.selectServer();
                new ProxyServerThread(socket, serverNumber == 0 ? hostA : hostB).start();
            }
        } catch (Exception e) {
            System.out.println("runProxyServer part error");
            e.printStackTrace();
        }
    }

    // A asynchronous thread to read the client's requests and pass them to the server.
    static class ProxyServerThread extends Thread {
        private Socket clientSocket;
        private Socket serverSocket;
        private String remoteHost;

        private DataInputStream disWithClient, disWithServer;
        private DataOutputStream dosWithClient, dosWithServer;

        public ProxyServerThread(Socket clientSocket, String remoteHost) {
            this.clientSocket = clientSocket;
            this.remoteHost = remoteHost;
        }

        @Override
        public void run() {
            final byte[] request = new byte[4096]; // 4MB
            byte[] reply = new byte[4096]; // 4MB

            try {
                disWithClient = new DataInputStream(clientSocket.getInputStream());
                dosWithClient = new DataOutputStream(clientSocket.getOutputStream());
            } catch (Exception e) {
                System.out.println("open stream with client error\n" + e.getMessage());
            }

            try {
                serverSocket = new Socket(remoteHost, remotePort);
                disWithServer = new DataInputStream(serverSocket.getInputStream());
                dosWithServer = new DataOutputStream(serverSocket.getOutputStream());
                dosWithServer.writeUTF(clientSocket.getInetAddress().getHostAddress());
            } catch (Exception e) {
                System.out.println("connect server socket\n" + e.getMessage());
            }

            new Thread() {
                public void run() {
                    try {
                        int bytes;
                        while ((bytes = disWithClient.read(request)) != -1) {
                            dosWithServer.write(request, 0, bytes);
                            dosWithServer.flush();
                        }
                    } catch (IOException e) {
                        System.out.println("Client-To-Server write error\n" + e.getMessage());
                    }

                    try {
                        dosWithServer.close();
                    } catch (IOException e) {
                        System.out.println("dosWithServer close error\n" + e.getMessage());
                    }
                }
            }.start();

            // Read the server's responses and pass them back to the client.
            try {
                int bytes;
                while ((bytes = disWithServer.read(reply)) != -1) {
                    dosWithClient.write(reply, 0, bytes);
                    dosWithClient.flush();
                }
            } catch (IOException e) {
                System.out.println("Server-To-Client write error\n" + e.getMessage());
            } finally {
                try {
                    if (serverSocket != null)
                        serverSocket.close();
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (Exception e) {
                    System.out.println("Sockets close\n" + e.getMessage());
                }
            }

            try {
                dosWithClient.close();
            } catch (Exception e) {
                System.out.println("dosWithClient close error\n" + e.getMessage());
            }
        }
    }
}
