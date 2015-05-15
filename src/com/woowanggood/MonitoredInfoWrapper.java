package com.woowanggood;

/**
 * Created by KangGyu on 2015-05-15.
 */
public class MonitoredInfoWrapper {
    public MonitoredInfo monitoredInfo;

    public MonitoredInfoWrapper(String myIP, int myPort,
                                double processCPUpercent, long availableVMsize,
                                double networkBandwidthUsage, long availableNetworkBandwith,
                                double systemCPUpercent, double systemPMpercent,
                                int numOfThreads, int numOfSessions) {
        this.monitoredInfo = new MonitoredInfo(myIP, myPort, processCPUpercent, availableVMsize,
                networkBandwidthUsage, availableNetworkBandwith, systemCPUpercent, systemPMpercent,
                numOfThreads, numOfSessions);
    }
}
