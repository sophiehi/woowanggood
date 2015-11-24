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

public class ResourceMonitor {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final String REMOTE_HOST = "192.168.0.102";
    private final int REMOTE_PORT = 7171;

    private String externalAddress;
    private int localPort;
    private OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();

    /**
     * process
     */
    private double processCPUPercent;
    private long availableVMsize;

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
     * socket & output stream
     */
    private Socket socket;
    private DataOutputStream dosWithServer;

    public ResourceMonitor() throws IOException, InterruptedException, SigarException {
        new NetworkMonitor(new Sigar());

        this.socket = new Socket(REMOTE_HOST, REMOTE_PORT);
        this.dosWithServer = new DataOutputStream(socket.getOutputStream());
        this.networkBandwidthUsage = -1.0;
        this.availableNetworkBandwith = -1;
        this.processCPUPercent = osBean.getProcessCpuLoad();
        this.availableVMsize = osBean.getCommittedVirtualMemorySize();
        this.systemCPUPercent = osBean.getSystemCpuLoad();
        this.systemPMPercent = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
        this.externalAddress = getIP();
        this.localPort = socket.getLocalPort();
    }

    public String getIP() throws SocketException {
        String result = "A";
        Enumeration<NetworkInterface> networkInterfaceEnum = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface networkInterface : Collections.list(networkInterfaceEnum)) {
            result = findExternalIP(networkInterface);
            if (!result.equals("A")) {
                return result;
            }
        }
        return "B";
    }

    public String findExternalIP(NetworkInterface netint) throws SocketException {
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            if (inetAddress.toString().contains(".") && !inetAddress.toString().contains("127.0.0.1")) {
                return inetAddress.toString().replace("/", "");
            }
        }
        return "A";
    }

    public void startReporting() throws IOException {
        final Runnable beeper = new Runnable() {
            public void run() {
                try {
                    updateInformation();
                } catch (SigarException e) {
                    e.printStackTrace();
                }
                try {
                    reportToLoadbalancerPeriodically();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final ScheduledFuture<?> reportSchedule =
                scheduler.scheduleAtFixedRate(beeper, 0, 1, SECONDS);
    }

    public void updateInformation() throws SigarException {
        this.processCPUPercent = osBean.getProcessCpuLoad();
        this.availableVMsize = osBean.getCommittedVirtualMemorySize();
        this.systemCPUPercent = osBean.getSystemCpuLoad();
        this.systemPMPercent = osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
        this.networkBandwidthUsage = NetworkMonitor.getMetric()[1];
        this.availableNetworkBandwith = -1;

        System.out.println("System resource usage : "
                + this.externalAddress + ":" + this.localPort + " "
                + this.processCPUPercent + ", " + this.availableVMsize + ", "
                + this.networkBandwidthUsage + ", " + this.systemCPUPercent);
    }

    public void reportToLoadbalancerPeriodically() throws IOException {
        ResourceUsage resourceUsage = new ResourceUsage(this.externalAddress, this.localPort,
                this.processCPUPercent, this.availableVMsize,
                this.networkBandwidthUsage, this.availableNetworkBandwith,
                this.systemCPUPercent, this.systemPMPercent);

        dosWithServer.writeUTF(resourceUsage.toString());
    }
}