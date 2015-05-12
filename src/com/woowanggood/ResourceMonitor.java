//package com.woowanggood;

import com.sun.management.OperatingSystemMXBean;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;

/**
 * Created by SophiesMac on 15. 5. 10..
 */
//todo ThreadHandler에서 ResourceMonitor를 호출.
public class ResourceMonitor {
    private final String ADDRESS_LOADBALANCER = "localhost";
    private final int PORT_LOADBALANCER = 7171;
    private final String myIp ="A" ;

    private OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();

    /** process */
    private double processCPUpercent ;
    private long availableVMsize ;

    /** system */
    private double systemCPUpercent ;
    private double systemPMpercent ;

    /** num of clients or sessions*/
    private int numOfThreads;
    private int numOfSessions;

    /** socket & outputStream */
    private Socket socket;
    private DataOutputStream dosWithServer;
    private String myIpAddress;

    /** scheduler */
    public ResourceMonitor() throws IOException {
        this.socket = new Socket(ADDRESS_LOADBALANCER, PORT_LOADBALANCER);
        this.dosWithServer = new DataOutputStream(socket.getOutputStream());
        //this.myIpAddress = socket.getInetAddress().getHostAddress(); //TODO

        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize  = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
    }


    public static void main(String args[]) throws IOException {
        ResourceMonitor rm = new ResourceMonitor();
        rm.startReporting();
    }

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public void startReporting() throws IOException {
        final Runnable beeper = new Runnable() {
            public void run() {
                System.out.println("update & report()");
                updateInfo();
                try {
                    reportToLoadbalancerPeriodically();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleAtFixedRate(beeper, 0, 1, SECONDS);

    }

    public void updateInfo(){
        System.out.println("update()");
        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
        System.out.println("result"+this.myIp+": "+this.processCPUpercent +", "+this.availableVMsize +", "+this.systemCPUpercent +", "+this.systemPMpercent);
    }

    public void reportToLoadbalancerPeriodically() throws IOException {
        System.out.println("report()");
        dosWithServer.writeUTF("result"+this.myIp+": "+this.processCPUpercent +", "+this.availableVMsize +", "+this.systemCPUpercent +", "+this.systemPMpercent);//outputstream타고 socket타고 나감.
        //dosWithServer.flush();
    }
}

//not yet: Computer A의 전원 꺼졌을때, B에서 재개하기 (latency 2초 목표?)
