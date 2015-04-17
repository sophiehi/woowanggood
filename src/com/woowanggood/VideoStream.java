package com.woowanggood;

import java.io.*;

public class VideoStream {
    private final int TS_PACKET_SIZE_BYTES = 188;
    InputStream in;
    BufferedInputStream fis; //video file pointer
    int frameNumber; //frame number for next frame

    public VideoStream(String filename) throws Exception{
        frameNumber = 0;

        //find path for a file, if in same package
        fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
        fis.markSupported();
    }

    public static void main(String[] args) throws Exception {
        VideoStream vs = new VideoStream("movie_new.ts");
        int n ;
        for(int i = 0 ; i < 30; i++) {
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
            case 1: return true;
            case 0: return false;
            default: System.out.println("ERROR: in isStartingPacket()!");
                    return false;
        }
    }

    private int howManyPacketsForNextFrame() throws IOException {
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();

        /** 2. Count after second packet(following packets), if exists. */
        while(true) {
            fis.mark( 200 );/** 2-1. MARKED. mark = maybe next starting point(?) */
            numOfPackets++;

            if(isStartingPacket()) {
                numOfPackets--;
                fis.reset();/** 2-2. Rewind by 1 packet. Jumping back to MARKED above. */
                break;
            }
        }

        return numOfPackets;
    }

    public int getNextFrameTest() throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;
        return numOfPackets;//for debug, return different thing.
    }

    public int getNextFrame(byte[] frame) throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;

        /** Write the next frame to "frame[]" as an array of byte. */
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );
        System.out.println(frame.toString());

        // Should be removed (only for debug).
        if (sizeOfNextFrame != sizeOfNextFrameCheck){
            System.out.println("wrong: size different");
        }else {
            System.out.println("correct: size same");
        }

        return sizeOfNextFrame;
    }
}