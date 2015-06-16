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
    public static final String host = "192.168.0.102"; // proxy server ip
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
            int selected = 0;
            final double CPU_LIMIT = 0.7;
            final long VM_LIMIT = 100 * 1024 * 1024;

            /*System.out.println("CPU : " + formA.monitoredInfo.getProcessCPUpercent() + "/" + formB.monitoredInfo.getProcessCPUpercent());
            System.out.println("VM : " + formA.monitoredInfo.getAvailableVMsize() + "/" + formB.monitoredInfo.getAvailableVMsize());*/

            if (formA.monitoredInfo.getProcessCPUpercent() <= CPU_LIMIT && formB.monitoredInfo.getProcessCPUpercent() <= CPU_LIMIT) {
                if (formA.monitoredInfo.getAvailableVMsize() >= VM_LIMIT && formB.monitoredInfo.getAvailableVMsize() >= VM_LIMIT) {
                    selected = formA.monitoredInfo.getNetworkBandwidthUsage() < formB.monitoredInfo.getNetworkBandwidthUsage() ? 0 : 1;
                    System.out.println(selected == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A)");
                }
                else if (formA.monitoredInfo.getAvailableVMsize() < VM_LIMIT && formB.monitoredInfo.getAvailableVMsize() >= VM_LIMIT) {
                    selected = 1;
                    System.out.println("Server B is selected(A exceeds limit of available VM size)");
                }
                else if (formB.monitoredInfo.getAvailableVMsize() < VM_LIMIT && formA.monitoredInfo.getAvailableVMsize() >= VM_LIMIT) {
                    selected = 0;
                    System.out.println("Server A is selected(B exceeds limit of available VM size)");
                }
                else {
                    selected = formA.monitoredInfo.getNetworkBandwidthUsage() < formB.monitoredInfo.getNetworkBandwidthUsage() ? 0 : 1;
                    System.out.println(selected == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A");
                }
            }
            else if (formA.monitoredInfo.getProcessCPUpercent() > CPU_LIMIT && formB.monitoredInfo.getProcessCPUpercent() <= CPU_LIMIT) {
                selected = 1;
                System.out.println("Server B is selected(A exceeds limit of process CPU usage)");
            }
            else if (formB.monitoredInfo.getProcessCPUpercent() > CPU_LIMIT && formA.monitoredInfo.getProcessCPUpercent() >= CPU_LIMIT) {
                selected = 0;
                System.out.println("Server A is selected(B exceeds limit of process CPU usage)");
            }
            else {
                selected = formA.monitoredInfo.getNetworkBandwidthUsage() < formB.monitoredInfo.getNetworkBandwidthUsage() ? 0 : 1;
                System.out.println(selected == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A)");
            }

            return selected;
        }
    }
}