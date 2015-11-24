package com.woowanggood;

import org.hyperic.sigar.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NetworkMonitor {
    private static Map<String, Long> rxCurrentMap = new HashMap<>();
    private static Map<String, List<Long>> rxChangeMap = new HashMap<>();
    private static Map<String, Long> txCurrentMap = new HashMap<>();
    private static Map<String, List<Long>> txChangeMap = new HashMap<>();

    private static Sigar sigar;

    /**
     * @throws InterruptedException
     * @throws SigarException
     */
    public NetworkMonitor(Sigar sigar) throws SigarException, InterruptedException {
        NetworkMonitor.sigar = sigar;

        getMetric();
        System.out.println(networkInfo());

        Thread.sleep(1000);
    }

    public static void main(String[] args) throws SigarException, InterruptedException {
        new NetworkMonitor(new Sigar());
        NetworkMonitor.startMetricTest();
    }

    public static String networkInfo() throws SigarException {
        return sigar.getNetInfo().toString() + "\n" + sigar.getNetInterfaceConfig().toString();
    }

    public static String getDefaultGateway() throws SigarException {
        return sigar.getNetInfo().getDefaultGateway();
    }

    public static void startMetricTest() throws SigarException, InterruptedException {
        while (true) {
            Thread.sleep(1000);
        }
    }

    public static Long[] getMetric() throws SigarException {
        for (String netInterface : sigar.getNetInterfaceList()) {
            NetInterfaceStat netStat = sigar.getNetInterfaceStat(netInterface);
            NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig(netInterface);
            String hwAddr = null;

            if (!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr())) {
                hwAddr = ifConfig.getHwaddr();
            }

            if (hwAddr != null) {
                long rxCurrentTemp = netStat.getRxBytes();
                saveChange(rxCurrentMap, rxChangeMap, hwAddr, rxCurrentTemp, netInterface);
                long txCurrentTemp = netStat.getTxBytes();
                saveChange(txCurrentMap, txChangeMap, hwAddr, txCurrentTemp, netInterface);
            }
        }

        long totalRxDown = getMetricData(rxChangeMap);
        long totalTxUp = getMetricData(txChangeMap);
        for (List<Long> value : rxChangeMap.values())
            value.clear();
        for (List<Long> value : txChangeMap.values())
            value.clear();

        return new Long[]{totalRxDown, totalTxUp};
    }

    private static long getMetricData(Map<String, List<Long>> rxChangeMap) {
        long total = 0;

        for (Map.Entry<String, List<Long>> entry : rxChangeMap.entrySet()) {
            int average = 0;
            for (Long value : entry.getValue()) {
                average += value;
            }
            total += average / entry.getValue().size();
        }

        return total;
    }

    private static void saveChange(Map<String, Long> currentMap,
                                   Map<String, List<Long>> changeMap, String hwAddr, long current,
                                   String netInterface) {
        Long oldCurrent = currentMap.get(netInterface);

        if (oldCurrent != null) {
            List<Long> list = changeMap.get(hwAddr);

            if (list == null) {
                list = new LinkedList<>();
                changeMap.put(hwAddr, list);
            }
            list.add((current - oldCurrent));
        }

        currentMap.put(netInterface, current);
    }
}