package com.woowanggood;

/**
 * Created by SophiesMac on 15. 5. 10..
 */
public class ResourceMonitor {
    private double CPUUsage;
    private double memoryUsage;

    private final String ADDRESS_LOADBALANCER = "0.0.0.0";
    private final String PORT_LOADBALANCER = "0000";

    public ResourceMonitor(){
        this(0.0, 0.0);
    }

    public ResourceMonitor(double CPUUsage, double memoryUsage){
        this.CPUUsage = CPUUsage;
        this.memoryUsage = memoryUsage;

        //todo Q. JVM ? or OS ? >> I think OS, not JVM.
        //https://support.hyperic.com/display/SIGAR/Home //시스템 정보 .
        //해당 프로세스? oo 스레드? xx
        //http://stackoverflow.com/questions/74674/how-to-do-i-check-cpu-and-memory-usage-in-java
        //http://helloworld.naver.com/helloworld/textyle/184615
    }


    //@Scheduled
    public void updateInfo(){
        //update this.CPUUsage, this.memoryUsage
    }

    //@Scheduled
    public void report(){
        //send this.CPUUsage, this.memoryUsage to LoadBalancer C
    }

}



//not yet: Computer A의 전원 꺼졌을때, B에서 재개하기 (latency 2초 목표?)

