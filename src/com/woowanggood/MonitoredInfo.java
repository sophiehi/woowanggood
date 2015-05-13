package com.woowanggood;

/**
 * Created by SophiesMac on 15. 5. 13..
 */
public class MonitoredInfo {
    /** process */
    private double processCPUpercent ;
    private long availableVMsize ;

    /** system */
    private double systemCPUpercent ;
    private double systemPMpercent ;

    /** num of clients or sessions*/
    private int numOfThreads;
    private int numOfSessions;

    /** physical info */
    private String myIP;
    private int myPort;

    public MonitoredInfo(double processCPUpercent, long availableVMsize,
                         double systemCPUpercent, double systemPMpercent,
                         String myIP, int myPort) {
        this(processCPUpercent, availableVMsize, systemCPUpercent, systemPMpercent, -1, -1, myIP, myPort);
    }


    public MonitoredInfo(double processCPUpercent, long availableVMsize,
                         double systemCPUpercent, double systemPMpercent,
                         int numOfThreads, int numOfSessions, String myIP, int myPort) {
        this.processCPUpercent = processCPUpercent;
        this.availableVMsize = availableVMsize;
        this.systemCPUpercent = systemCPUpercent;
        this.systemPMpercent = systemPMpercent;
        this.numOfThreads = numOfThreads;
        this.numOfSessions = numOfSessions;
        this.myIP = myIP;
        this.myPort = myPort;
    }

    @Override
    public String toString(){
        return "{\""+this.myIP+":"+this.myPort+"\":"
                +"{"
                    +"{\"processCPUpercent\":"+"\""+this.processCPUpercent+"\"}, "
                    +"{\"availableVMsize\":"+"\""+this.availableVMsize+"\"}, "
                    +"{\"systemCPUpercent\":"+"\""+this.systemCPUpercent+"\"}, "
                    +"{\"systemPMpercent\":"+"\""+this.systemPMpercent+"\"}"
                +"}}";
    }

    public double getProcessCPUpercent() {
        return processCPUpercent;
    }

    public long getAvailableVMsize() {
        return availableVMsize;
    }

    public double getSystemCPUpercent() {
        return systemCPUpercent;
    }

    public double getSystemPMpercent() {
        return systemPMpercent;
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
