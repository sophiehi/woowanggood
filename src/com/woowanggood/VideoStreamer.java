package com.woowanggood;

import java.io.*;

public class VideoStreamer {
    public final int TS_PACKET_SIZE_BYTES = 188;

    //InputStream in;
    BufferedInputStream fis; //video file pointer
    int frameNumber; //frame number for next frame

    public VideoStreamer(String filename) throws Exception {
        frameNumber = 0;

        // find path for a file, if in same package
        fis = new BufferedInputStream(getClass().getResourceAsStream("movie.ts"));

        //fis = new BufferedInputStream(new FileInputStream( "/Users/swchoi06/IdeaProjects/woowanggood/movie.ts"));
        fis.markSupported();
        //int numOfPackets = howManyPacketsForNextFrame();
        //frameNumber++;
        //byte[] aPacket = new byte[TS_PACKET_SIZE_BYTES];
        //fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
    }
    public static void main(String[] args) throws Exception {
        int numPackets = 0; int numBytes = 0;
        byte[] buf = new byte[200000];
        VideoStreamer vs = new VideoStreamer("movie_new.ts");

        for( int i=0 ; i<30 ; i++ ) {
            //numPackets = vs.getNextFrameTest();
            numBytes = vs.getNextFrame(buf);
            //System.out.println("num of packets: " + numPackets + "\n");
            System.out.println("size of frames: " + numBytes);
            System.out.println("num of packets: " + numBytes/188 + "\n");
        }
    }

    public void printOneFrame() {

    }

    private boolean isStartingPacket() throws IOException {
        int payloadUnitStartIndicator;
        byte [] aPacket = new byte[ TS_PACKET_SIZE_BYTES ];

        fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        payloadUnitStartIndicator = (aPacket[1] >>> 6) & 0x01;

        switch (payloadUnitStartIndicator) {
            case 1: return true;
            case 0: return false;
            default: System.out.println("ERROR: in isStartingPacket()!");
                    return false;
        }
    }

    private int howManyPacketsForNextFrame() throws IOException {
        fis.mark(200000);
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();

        /** 2. Count after second packet(following packets), if exists. */

        while (true) {
            fis.mark( 200 );/** 2-1. MARKED. mark = maybe next starting point(?) */
            numOfPackets++;

            if (isStartingPacket()) {
                numOfPackets--;
                break;
            }
        }

        fis.reset();
        return numOfPackets;
    }

    public int getNextFrameTest() throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;
        System.out.println(frameNumber+", "+ numOfPackets);
        return numOfPackets;//for debug, return different thing.
    }

    public int getNextFrame(byte[] frame) throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        frameNumber++;

        /** Write the next frame to "frame[]" as an array of byte. */
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame);
        System.out.println(frame.toString());

        /** Write the next frame to "frame[]" as an array of byte. */
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;
        System.out.printf("Frame size :  %d \n", sizeOfNextFrame);
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );
        // Should be removed (only for debug).
        //if (sizeOfNextFrame != sizeOfNextFrameCheck){ System.out.println("wrong: size different");
        //} else { System.out.println("correct: size same"); }

        return sizeOfNextFrame;
    }

    static public boolean isStartingPacket(byte [] tsPacket) throws IOException{
        int payloadUnitStartIndicator;
        payloadUnitStartIndicator = (tsPacket[1] >>> 6) & 0x01;

        switch (payloadUnitStartIndicator) {
            case 1: return true;
            case 0: return false;
            default: System.out.println("ERROR: in isStartingPacket()!");
                return false;
        }
    }

}
