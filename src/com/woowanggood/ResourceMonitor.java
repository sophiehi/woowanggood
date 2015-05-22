package com.woowanggood;

import com.sun.management.OperatingSystemMXBean;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

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

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by SophiesMac on 15. 5. 10..
 */
public class ResourceMonitor {
    //TESTMAIN
    public static void main(String args[]) throws IOException, SigarException, InterruptedException {
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

    /** network */
    private double networkBandwidthUsage;
    private long availableNetworkBandwith;

    /** system */
    private double systemCPUpercent ;
    private double systemPMpercent ;

    /** num of clients or sessions */
    private int numOfThreads;
    private int numOfSessions;

    /** socket & outputStream */
    private Socket socket;
    private DataOutputStream dosWithServer;

    /** scheduler */
    public ResourceMonitor() throws IOException, InterruptedException, SigarException {
        new NetworkMonitor(new Sigar());

        this.socket = new Socket(REMOTE_HOST, REMOTE_PORT);
        this.dosWithServer = new DataOutputStream(socket.getOutputStream());

        this.networkBandwidthUsage = -1.0;
        this.availableNetworkBandwith = -1;

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
        String tmp = "A";
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
            // if(netint.getDisplayName().contains("en0")){
            if (check1 && check2){
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
                try {updateInfo();} catch (SigarException e) { e.printStackTrace();}
                try {
                    reportToLoadBalancerPeriodically();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        final ScheduledFuture<?> reportHandle =
                scheduler.scheduleAtFixedRate(beeper, 0, 1, SECONDS);
    }

    public void updateInfo() throws SigarException {
        System.out.println("update()");
        this.processCPUpercent = osBean.getProcessCpuLoad();
        this.availableVMsize = osBean.getCommittedVirtualMemorySize();
        this.systemCPUpercent = osBean.getSystemCpuLoad();
        this.systemPMpercent  = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();

        //todo change name, Usage -> TransmittedBytes
        this.networkBandwidthUsage = NetworkMonitor.getMetric()[1];
        //todo change name, avalable -> max ?
        this.availableNetworkBandwith = -1;
        //TODO TODO 금요일에 두대로 iperf로 재보기 : performance, MTU ??
        //https://iperf.fr/

        //ref: http://sourceforge.net/projects/ibm/files/


        System.out.println("result: "
                + this.localHost_externalAddress + ":" +this.localPort +" "
                + this.processCPUpercent + ", " + this.availableVMsize + ", "
                + this.networkBandwidthUsage + ", " + this.availableNetworkBandwith+ ", "
                + this.systemCPUpercent + ", " + this.systemPMpercent);
    }

    public void reportToLoadBalancerPeriodically() throws IOException {
        MonitoredInfo monitoredInfo = new MonitoredInfo(this.localHost_externalAddress, this.localPort,
                                        this.processCPUpercent, this.availableVMsize,
                                        this.networkBandwidthUsage, this.availableNetworkBandwith,
                                        this.systemCPUpercent, this.systemPMpercent);

        dosWithServer.writeUTF(monitoredInfo.toString()); // output stream -> socket으로 전송.
        // dosWithServer.flush();
    }
}

// not yet : Computer A의 전원 꺼졌을 때, B에서 재개하기 (latency 2초 목표?)