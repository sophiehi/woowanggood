package com.woowanggood;

import com.sun.management.OperatingSystemMXBean;
//import com.woowanggood.MonitoredInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;
/**
 * Created by SophiesMac on 15. 5. 10..
 */
public class ResourceMonitor {

    public static void main(String args[]) throws IOException {
        //todo ThreadHandler에서 ResourceMonitor를 호출.
        ResourceMonitor rm = new ResourceMonitor();
        rm.startReporting();
    }

    private final String REMOTE_HOST = "localhost";
    private final int REMOTE_PORT = 7171;
    private String localHost_externalAddress;
    private int localPort;

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
    public ResourceMonitor() throws IOException {
        this.socket = new Socket(REMOTE_HOST, REMOTE_PORT);
        this.dosWithServer = new DataOutputStream(socket.getOutputStream());

        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize  = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();

        //get localIP
        this.localHost_externalAddress = getIP();

        //get localPort
        this.localPort = socket.getLocalPort();
    }


    public String getIP() throws SocketException {
        String tmp ="A";
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            tmp = findExternalIP(netint);
            if (!tmp.equals("A")) {
                return tmp;
            }
        }
        return "B";
    }

    public String findExternalIP(NetworkInterface netint) throws SocketException {
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            boolean check1 = inetAddress.toString().contains(".");
            boolean check2 = !inetAddress.toString().contains("127.0.0.1");
            //if(netint.getDisplayName().contains("en0")){
            if(check1 && check2){
                return inetAddress.toString().replace("/", "");
            }
        }
        return "A";
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
        System.out.println("result"+this.localHost_externalAddress +": "+this.processCPUpercent +", "+this.availableVMsize +", "+this.systemCPUpercent +", "+this.systemPMpercent);
    }

    public void reportToLoadbalancerPeriodically() throws IOException {
        System.out.println("report()");
        MonitoredInfo mi = new MonitoredInfo(this.processCPUpercent, this.availableVMsize,
                                        this.systemCPUpercent, this.systemPMpercent,
                                        this.localHost_externalAddress, this.localPort);

        dosWithServer.writeUTF(mi.toString());//outputstream타고 socket타고 나감.
        //dosWithServer.flush();
    }
}

//not yet: Computer A의 전원 꺼졌을때, B에서 재개하기 (latency 2초 목표?)
