package com.woowanggood;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by KangGyu on 2015-05-01.
 */
public class SocketHandler {
    public static final String host = "proxy_server_address";
    private static final int localPort = 3000;
    public static final int remotePort = 4000;

    public static void main(String[] args) {
        try {
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remotePort + " on port " + localPort);
            // And start running the server
            runServer();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runServer() {
        ServerSocket proxyServerSocket = null;

        final byte[] request = new byte[1024]; // 1MB
        byte[] reply = new byte[4096]; // 4MB

        try {
            // Create a ServerSocket to listen for connections with clients
            proxyServerSocket = new ServerSocket(localPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            Socket clientSocket = null, serverSocket = null;

            try {
                // Wait for a connection on the local port
                clientSocket = proxyServerSocket.accept();
                serverSocket = new Socket(host, remotePort);

                DataOutputStream dosWithClient = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream disWithClient = new DataInputStream(clientSocket.getInputStream());

                DataOutputStream dosWithServer = new DataOutputStream(serverSocket.getOutputStream());
                DataInputStream disWithServer = new DataInputStream(serverSocket.getInputStream());

                // A thread to read the client's requests and pass them to the server.
                // A separate thread for asynchronous.
                new Thread() {
                    public void run() {
                        int bytes;

                        try {
                            while ((bytes = disWithClient.read(request)) != -1) {
                                dosWithServer.write(request, 0, bytes);
                                dosWithServer.flush();
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {
                            try {
                                // The client closed the connection to us, so close out connection to the server(Computer A, Computer B).
                                dosWithServer.close();
                                disWithClient.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
                // Start the client-to-server request thread running

                // Read the server's responses
                // and pass them back to the client.
                new Thread() {
                    public void run() {
                        int bytes;

                        try {
                            while ((bytes = disWithServer.read(reply)) != -1) {
                                dosWithClient.write(reply, 0, bytes);
                                dosWithClient.flush();
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {
                            try {
                                // The server(Computer A, Computer B) closed its connection to us, so we close our connection to our client.
                                dosWithClient.close();
                                disWithServer.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (serverSocket != null)
                        serverSocket.close();
                    if (clientSocket != null)
                        clientSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
