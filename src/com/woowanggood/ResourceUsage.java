package com.woowanggood;

import com.google.gson.Gson;

public class ResourceUsage {
    /**
     * process
     */
    private double processCPUPercent;
    private long availableVMSize;

    /**
     * network
     */
    private double networkBandwidthUsage;
    private long availableNetworkBandwith;

    /**
     * system
     */
    private double systemCPUPercent;
    private double systemPMPercent;

    /**
     * num of clients or sessions
     */
    private int numOfThreads;
    private int numOfSessions;

    /**
     * physical info
     */
    private String IP;
    private int port;

    public ResourceUsage(String IP, int port,
                         double processCPUPercent, long availableVMSize,
                         double networkBandwidthUsage, long availableNetworkBandwith,
                         double systemCPUPercent, double systemPMPercent) {
        this(IP, port, processCPUPercent, availableVMSize,
                networkBandwidthUsage, availableNetworkBandwith, systemCPUPercent, systemPMPercent,
                -1, -1);
    }

    public ResourceUsage(String IP, int port,
                         double processCPUPercent, long availableVMSize,
                         double networkBandwidthUsage, long availableNetworkBandwith,
                         double systemCPUPercent, double systemPMPercent,
                         int numOfThreads, int numOfSessions) {
        this.IP = IP;
        this.port = port;
        this.networkBandwidthUsage = networkBandwidthUsage;
        this.availableNetworkBandwith = availableNetworkBandwith;
        this.processCPUPercent = processCPUPercent;
        this.availableVMSize = availableVMSize;
        this.systemCPUPercent = systemCPUPercent;
        this.systemPMPercent = systemPMPercent;
        this.numOfThreads = numOfThreads;
        this.numOfSessions = numOfSessions;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(new ResourceUsage(IP, port, processCPUPercent, availableVMSize, networkBandwidthUsage, availableNetworkBandwith,
                systemCPUPercent, systemPMPercent, numOfThreads, numOfSessions));
    }

    public double getProcessCPUPercent() {
        return processCPUPercent;
    }

    public long getAvailableVMSize() {
        return availableVMSize;
    }

    public double getNetworkBandwidthUsage() {
        return networkBandwidthUsage;
    }

    public long getAvailableNetworkBandwith() {
        return availableNetworkBandwith;
    }

    public double getSystemCPUPercent() {
        return systemCPUPercent;
    }

    public double getSystemPMPercent() {
        return systemPMPercent;
    }

    public void setNumOfThreads(int numOfThreads) {
        this.numOfThreads = numOfThreads;
    }

    public void setNumOfSessions(int numOfSessions) {
        this.numOfSessions = numOfSessions;
    }

    public void incNumOfSessions() {
        this.numOfSessions++;
    }

    public void decNumOfSessions() {
        this.numOfSessions--;
    }

    public int getNumOfThreads() {
        return numOfThreads;
    }

    public int getNumOfSessions() {
        return numOfSessions;
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return port;
    }
}
