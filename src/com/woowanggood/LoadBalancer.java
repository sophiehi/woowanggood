package com.woowanggood;

import com.google.gson.Gson;
import com.woowanggood.form.ResourceUsageWrapper;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class LoadBalancer {
    public static final String host = "192.168.0.102"; // proxy server ip
    public static final int port = 7171; // for monitoring resource

    public void start() {
        try {
            System.out.println("Starting load balancer in " + host + ":" + port);
            ServerSocket listener = new ServerSocket(port);

            while (true) {
                Socket socket = listener.accept();
                new ResourceMonitorThread(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ResourceMonitorThread extends Thread {
        private static ResourceUsageWrapper informationA;
        private static ResourceUsageWrapper informationB;

        private Socket socket;
        private String report;

        public ResourceMonitorThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Gson gson = new Gson();

            try {
                while (true) {
                    DataInputStream is = new DataInputStream(socket.getInputStream());
                    report = is.readUTF();
                    informationA = gson.fromJson(report, ResourceUsageWrapper.class);

                    report = is.readUTF();
                    informationB = gson.fromJson(report, ResourceUsageWrapper.class);

                    /* {"192.168.0.127:51926":
                        [{"networkBandwithUsage":"0.12"},{"availableNetworkBandwith":"1256"},
                        {"processCPUpercent":"6.46434166426508E-4"},{"availableVMsize":"6170157056"},
                        {"systemCPUpercent":"0.07711442786069651"},{"systemPMpercent":"0.0"}]} */
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static int selectServer() {
            int serverNumber = 0;
            final double CPU_LIMIT = 0.7;
            final long VM_LIMIT = 100 * 1024 * 1024;

            if (informationA.resourceUsage.getProcessCPUPercent() <= CPU_LIMIT && informationB.resourceUsage.getProcessCPUPercent() <= CPU_LIMIT) {
                if (informationA.resourceUsage.getAvailableVMSize() >= VM_LIMIT && informationB.resourceUsage.getAvailableVMSize() >= VM_LIMIT) {
                    serverNumber = informationA.resourceUsage.getNetworkBandwidthUsage() < informationB.resourceUsage.getNetworkBandwidthUsage() ? 0 : 1;
                    System.out.println(serverNumber == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A)");
                } else if (informationA.resourceUsage.getAvailableVMSize() < VM_LIMIT && informationB.resourceUsage.getAvailableVMSize() >= VM_LIMIT) {
                    serverNumber = 1;
                    System.out.println("Server B is selected(A exceeds limit of available VM size)");
                } else if (informationB.resourceUsage.getAvailableVMSize() < VM_LIMIT && informationA.resourceUsage.getAvailableVMSize() >= VM_LIMIT) {
                    serverNumber = 0;
                    System.out.println("Server A is selected(B exceeds limit of available VM size)");
                } else {
                    serverNumber = informationA.resourceUsage.getNetworkBandwidthUsage() < informationB.resourceUsage.getNetworkBandwidthUsage() ? 0 : 1;
                    System.out.println(serverNumber == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A");
                }
            } else if (informationA.resourceUsage.getProcessCPUPercent() > CPU_LIMIT && informationB.resourceUsage.getProcessCPUPercent() <= CPU_LIMIT) {
                serverNumber = 1;
                System.out.println("Server B is selected(A exceeds limit of process CPU usage)");
            } else if (informationB.resourceUsage.getProcessCPUPercent() > CPU_LIMIT && informationA.resourceUsage.getProcessCPUPercent() >= CPU_LIMIT) {
                serverNumber = 0;
                System.out.println("Server A is selected(B exceeds limit of process CPU usage)");
            } else {
                serverNumber = informationA.resourceUsage.getNetworkBandwidthUsage() < informationB.resourceUsage.getNetworkBandwidthUsage() ? 0 : 1;
                System.out.println(serverNumber == 0 ? "Server A is selected(Network bandwidth usage : A < B)" : "Server B is selected(Network bandwidth usage : B < A)");
            }

            return serverNumber;
        }
    }
}