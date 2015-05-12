package com.woowanggood;

import com.sun.management.OperatingSystemMXBean;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by SophiesMac on 15. 5. 10..
 */
//todo ThreadHandler에서 ResourceMonitor를 호출.
public class ResourceMonitor {
    private final String ADDRESS_LOADBALANCER = "localhost";
    private final int PORT_LOADBALANCER = 2000;//todo 같은 서버소켓으로 보내도 되나?

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

    /** scheduler */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ResourceMonitor() throws IOException {
        this.socket = new Socket(ADDRESS_LOADBALANCER, PORT_LOADBALANCER);
        this.dosWithServer = new DataOutputStream(socket.getOutputStream());

        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize  = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
    }

    public void updateAndReport() {
        final Runnable beeper = new Runnable() {
            public void run() {
                updateInfo();
                try {
                    reportToLoadbalancerPeriodically();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("beep");
            }
        };

        final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleAtFixedRate(beeper, 10, 1, SECONDS);

        //todo 1초가 아니라 1회로 하려면?
        scheduler.schedule(new Runnable() {
            public void run() { beeperHandle.cancel(true); }
        }, 1, SECONDS);
    }

    public void run(){
        updateAndReport();
    }

    //@Scheduled
    public void updateInfo(){
        System.out.println("update this.CPUUsage, this.memoryUsage");
        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
    }

    //@Scheduled
    public void reportToLoadbalancerPeriodically() throws IOException {
        dosWithServer.writeUTF("hello from Resourse Monitor");//outputstream타고 socket타고 나감.
    }
}

//not yet: Computer A의 전원 꺼졌을때, B에서 재개하기 (latency 2초 목표?)
