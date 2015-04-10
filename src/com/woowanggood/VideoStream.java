package com.woowanggood;

import java.io.*;

public class VideoStream {
    private final int TS_PACKET_SIZE_BYTES = 188;
    //FileInputStream vs. BufferedInputStream vs. RandomAccessFile
    //FileInputStream fis; //video file pointer
    InputStream in;
    BufferedInputStream fis;
    int frameNumber; //current frame number

    public VideoStream(String filename) throws Exception{
        frameNumber = 0;

        //in = new FileInputStream(filename);//path for ??
        in = getClass().getResourceAsStream(filename);//path for same package
        fis = new BufferedInputStream(in);
        fis.markSupported();
    }

    public static void main(String[] args) throws Exception {
        VideoStream vs = new VideoStream("movie_new.ts");
        int n ;
        for(int i = 0 ; i < 10; i++) {
            n = vs.getNextFrameTest();
            System.out.println("\nnum of packets: " + n + "\n");
        }
    }

    private boolean isStartingPacket() throws IOException {
        int payloadUnitStartIndicator;
        byte [] aPacket = new byte[ TS_PACKET_SIZE_BYTES ];
        fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        payloadUnitStartIndicator = (aPacket[1] >>> 6) & 0x01;

        switch(payloadUnitStartIndicator){
            case 1:
                System.out.print("1 ");
                return true;
            case 0:
                System.out.print("0 ");
                return false;
            default:
                System.out.println("undefined err !");
                return false;//err
        }
    }

    private int howManyPacketsForNextFrame() throws IOException {
        int numOfPackets = 0;
        int indicator = 0;

        //count first packet
        numOfPackets++;
        isStartingPacket();

        //count after second packet
        while(true) {
            fis.mark( 200 );// todo // mark = 다음에 시작할 곳인듯?
            numOfPackets++;
            if(isStartingPacket()) indicator = 1;

            if (indicator == 1){
                numOfPackets--;
                fis.reset();//todo rewind by 1 packet. marked just before
                break;
            }
        }

        return numOfPackets;
    }

    public int getNextFrameTest() throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;

        // Write the next frame to "frame[]" as an array of byte.
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;

        return numOfPackets;//for debug
    }

    public int getNextFrame(byte[] frame) throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;

        // Write the next frame to "frame[]" as an array of byte.
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );

        // Should be removed (only for debug).
        if (sizeOfNextFrame != sizeOfNextFrameCheck){
            System.out.println("wrong: size different");
        }else {
            System.out.println("correct: size same");
        }

        return sizeOfNextFrame;
    }
}
