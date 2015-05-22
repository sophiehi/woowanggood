package com.woowanggood;

import com.google.gson.Gson;
//good-ref: 대역폭 계산 http://docs.oracle.com/cd/E19159-01/820-4902/abfcr/index.html
//todo good-ref: RTP/H.264 대역폭 https://technet.microsoft.com/ko-kr/library/jj688118(v=ocs.15).aspx
/**
 * Created by SophiesMac on 15. 5. 13..
 */
public class MonitoredInfo {
    /** process */
    private double processCPUpercent;
    private long availableVMsize;

    /** network */
    private double networkBandwidthUsage;
    private long availableNetworkBandwith;

    /** system */
    private double systemCPUpercent;
    private double systemPMpercent;

    /** num of clients or sessions */
    private int numOfThreads;
    private int numOfSessions;

    /** physical info */
    private String myIP;
    private int myPort;

    public MonitoredInfo(String myIP, int myPort,
                         double processCPUpercent, long availableVMsize,
                         double networkBandwidthUsage, long availableNetworkBandwith,
                         double systemCPUpercent, double systemPMpercent) {
        this(myIP, myPort, processCPUpercent, availableVMsize,
                networkBandwidthUsage, availableNetworkBandwith, systemCPUpercent, systemPMpercent,
                -1, -1);
    }

    public MonitoredInfo(String myIP, int myPort,
                         double processCPUpercent, long availableVMsize,
                         double networkBandwidthUsage, long availableNetworkBandwith,
                         double systemCPUpercent, double systemPMpercent,
                         int numOfThreads, int numOfSessions) {
        this.myIP = myIP;
        this.myPort = myPort;
        this.networkBandwidthUsage = networkBandwidthUsage;
        this.availableNetworkBandwith = availableNetworkBandwith;
        this.processCPUpercent = processCPUpercent;
        this.availableVMsize = availableVMsize;
        this.systemCPUpercent = systemCPUpercent;
        this.systemPMpercent = systemPMpercent;
        this.numOfThreads = numOfThreads;
        this.numOfSessions = numOfSessions;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(new MonitoredInfoWrapper(myIP, myPort, processCPUpercent, availableVMsize, networkBandwidthUsage, availableNetworkBandwith,
                systemCPUpercent, systemPMpercent, numOfThreads, numOfSessions));
    }

    public double getProcessCPUpercent() { return processCPUpercent; }

    public long getAvailableVMsize() { return availableVMsize; }

    public double getNetworkBandwidthUsage() { return networkBandwidthUsage; }

    public long getAvailableNetworkBandwith() { return availableNetworkBandwith; }

    public double getSystemCPUpercent() {
        return systemCPUpercent;
    }

    public double getSystemPMpercent() {
        return systemPMpercent;
    }

    public void setNumOfThreads(int numOfThreads) { this.numOfThreads = numOfThreads; } // todo
    public void setNumOfSessions(int numOfSessions) { this.numOfSessions = numOfSessions; }
    public void incNumOfSessions(){
        this.numOfSessions++;
    }
    public void decNumOfSessions(){
        this.numOfSessions--;
    }

    public int getNumOfThreads() {
        return numOfThreads;
    }
    public int getNumOfSessions() {
        return numOfSessions;
    }

    public String getMyIP() {
        return myIP;
    }

    public int getMyPort() {
        return myPort;
    }
}
