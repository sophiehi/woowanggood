package com.woowanggood;

import java.io.*;

public class VideoStream {
    private final int TS_PACKET_SIZE_BYTES = 188;
    //FileInputStream vs. BufferedInputStream vs. RandomAccessFile
    FileInputStream fis; //video file pointer
    //BufferedInputStream fis;
    int frameNumber; //current frame number

    public VideoStream(String filename) throws Exception{
        frameNumber = 0;
        fis = new FileInputStream(filename);
    }

    public static void main(String[] args) throws Exception {
        //test Main
    }

    private void rollbackByOnePacket(){
        //todo: move file pointer 1 packet back
        //fis = fis - TS_PACKET_SIZE_BYTES;
    }

    private boolean isStartingPacket() throws IOException {
        int payloadUnitStartIndicator;
        byte [] aPacket = new byte[ TS_PACKET_SIZE_BYTES ];
        fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        payloadUnitStartIndicator = (aPacket[1] >>> 6) & 0x01;

        switch(payloadUnitStartIndicator){
            case 1: return true;
            case 0: return false;
            default: return false;//
        }
    }

    private int howManyPacketsForNextFrame() throws IOException {
        int numOfPackets = 0;

        //count first packet
        numOfPackets++;
        isStartingPacket();

        //count after second packet
        while(!isStartingPacket()) {
            numOfPackets++;
            isStartingPacket();
        }

        rollbackByOnePacket();
        return numOfPackets;
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
