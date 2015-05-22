package com.woowanggood;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by SophiesMac on 15. 5. 13..
 */
public class LoadBalancer {
    public static final String host = "localhost";
    public static final int port = 7171; // for monitoring resource

    //TESTMAIN
    public static void main(String args[]) {
        try {System.out.println("Starting serverSocket " + host + ":" + port);
            ServerSocket listener = new ServerSocket(port);
            while (true) {
                Socket socket = listener.accept();
                new ResourceMonitorThread(socket).start();}
        } catch (IOException e) { e.printStackTrace();}
    }

    public void start() {
        try {
            System.out.println("Starting load balancer in " + host + ":" + port);
            ServerSocket listener = new ServerSocket(port);

            while (true) {
                Socket socket = listener.accept();
                new ResourceMonitorThread(socket).start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ResourceMonitorThread extends Thread {
        private Socket socket;
        private String resourceReport;
        private static MonitoredInfoWrapper formA;
        private static MonitoredInfoWrapper formB;

        public ResourceMonitorThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Gson gson = new Gson();

            try {
                while (true) {
                    DataInputStream is = new DataInputStream(socket.getInputStream());
                    resourceReport = is.readUTF();
                    formA = gson.fromJson(resourceReport, MonitoredInfoWrapper.class);
                    // System.out.println("Computer A : " + "(" + formA.monitoredInfo.getMyPort() + ")\n" + resourceReport);

                    resourceReport = is.readUTF();
                    formB = gson.fromJson(resourceReport, MonitoredInfoWrapper.class);
                    // System.out.println("Computer B : " + "(" + formA.monitoredInfo.getMyPort() + ")\n" + resourceReport);

                    // resourceReport json example as below:
                    /* {"192.168.0.127:51926":
                        [{"networkBandwithUsage":"0.12"},{"availableNetworkBandwith":"1256"},
                        {"processCPUpercent":"6.46434166426508E-4"},{"availableVMsize":"6170157056"},
                        {"systemCPUpercent":"0.07711442786069651"},{"systemPMpercent":"0.0"}]} */
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static int selectServer() {
            int selected;

            // if (formA.monitoredInfo.getNetworkBandwidthUsage() <= 0.75 && formB.monitoredInfo.getNetworkBandwidthUsage() <= 0.75) {
                if (formA.monitoredInfo.getNumOfSessions() <= 8 && formB.monitoredInfo.getNumOfSessions() <= 8) {
                    if (formA.monitoredInfo.getNumOfThreads() <= 4 && formB.monitoredInfo.getNumOfThreads() < 4) {
                        if (formA.monitoredInfo.getProcessCPUpercent() <= 0.4 && formB.monitoredInfo.getProcessCPUpercent() <= 0.4) {
                            selected = (formA.monitoredInfo.getAvailableVMsize() > formB.monitoredInfo.getAvailableVMsize()) ? 0 : 1;
                            if (selected == 0)
                                System.out.println("Computer A is selected(Available VM size : A > B)");
                            else
                                System.out.println("Computer B is selected(Available VM size : A < B)");
                        }
                        else if (formA.monitoredInfo.getProcessCPUpercent() <= 0.4) {
                            selected = 0;
                            System.out.println("Computer A is selected(process CPU usage of B exceeds max value)");
                        }
                        else if (formB.monitoredInfo.getProcessCPUpercent() <= 0.4) {
                            selected = 1;
                            System.out.println("Computer B is selected(process CPU usage of A exceeds max value)");
                        }
                        else {
                            selected = 0;
                            System.out.println("Computer A is selected(process CPU usages of both computer exceed max value");
                        }
                    } else if (formA.monitoredInfo.getNumOfThreads() <= 4) {
                        selected = 0;
                        System.out.println("Computer A is selected(The number of threads in B exceeds max value)");
                    }
                    else if (formB.monitoredInfo.getNumOfThreads() <= 4) {
                        selected = 1;
                        System.out.println("Computer B is selected(The number of threads in A exceeds max value)");
                    }
                    else {
                        selected = 0;
                        System.out.println("Computer A is selected(The number of threads in both computer exceeds max value)");
                    }
                } else if (formA.monitoredInfo.getNumOfSessions() <= 8) {
                    selected = 0;
                    System.out.println("Computer A is selected(The number of sessions in B exceeds max value)");
                }
                else if (formB.monitoredInfo.getNumOfSessions() <= 8) {
                    selected = 1;
                    System.out.println("Computer B is selected(The number of sessions in A exceeds max value)");
                }
                else {
                    selected = 0;
                    System.out.println("Computer A is selected(The number of sessions in both computer exceeds max value)");
                }
            /*}
            else if (formA.monitoredInfo.getNetworkBandwidthUsage() > 0.75)
                selected = 1;
            else if (formB.monitoredInfo.getNetworkBandwidthUsage() > 0.75)
                selected = 0;
            else
                selected = 0;*/

            return selected;
        }
    }
}