//package com.woowanggood;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 47 40 31 35   07 00 FF FF   FF FF FF FF   00 00 01 E0   00 00 80 C0   0A 35 B5 37
 47 40 31 3D   07 00 FF FF   FF FF FF FF   00 00 01 E0   00 00 80 C0   0A 35 B5 37
 47 40 31 31   07 00 FF FF   FF FF FF FF   00 00 01 E0   00 00 80 "C0"   0A 35 B5 39

 C0 =12 (IDR_flag = key frame =i frame)
 80 = 8 (normal frame)
 "00 00 01" NAL unit header - http://egloos.zum.com/yajino/v/782492
 47 40 31 3D   07 00 FF FF   FF FF// FF FF   "00 00 01" E0   00 00 80 "80"   05 25 B5 39
 http://electronicimaging.spiedigitallibrary.org/article.aspx?articleid=1100857%20


 47 40 31 3C   07 00 FF FF   FF FF FF FF   00 00 01 E0   00 00 80 80   05 25 B5 37
 47 40 35 35   07 00 FF FF   FF FF FF FF   00 00 01 BD   03 08 80 80   05 25 B5 33
 47 40 00 11   00 00 B0 11   00 00 C1 00   00 00 00 E0   1F 00 01 E0   30 25 71 07
 47 40 30 11   00 02 B0 1C   00 01 C1 00   00 E0 31 F0   00 1B E0 31   F0 00 81 E0

 * */

public class VideoStreamer {
    public final int TS_PACKET_SIZE_BYTES = 188;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    //InputStream in;
    BufferedInputStream fis; //video file pointer
    int frameNumber; //frame number for next frame
    private LinkedList<Integer> keyFrameIndexTable = new LinkedList<Integer>();

    public VideoStreamer(String filename) throws Exception {
        frameNumber = 0;

        // find path for a file, if in same package
        fis = new BufferedInputStream(getClass().getResourceAsStream("movie.ts"));
        // fis = new BufferedInputStream(new FileInputStream( "/Users/swchoi06/IdeaProjects/woowanggood/movie.ts"));
        fis.markSupported();
        //int numOfPackets = howManyPacketsForNextFrame();
        //frameNumber++;
        //byte[] aPacket = new byte[TS_PACKET_SIZE_BYTES];
        //fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
    }

    private void genKeyFrameTable(){

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
            System.out.println("num of packets: " + numBytes/188);
            printBytesInHex(buf, numBytes);
        }
    }

    public static void printBytesInHex(byte[] buf, int numBytes){
        for(int j=0; j<numBytes/188; j++){
            byte [] bufCopy = Arrays.copyOfRange(buf, j*188, (j+1)*188);
            //1. 이어서 출력
            //System.out.println(bytesToHexString(bufCopy)+"\n");
            //2. 나눠서 출력
            char[] hexChars = bytesToHexArr(bufCopy);
            for(int i = 0; i< hexChars.length; i++ ){
                if (i%2==0) System.out.print(" ");
                if (i%8==0) System.out.print("  ");
                if (i%48==0) System.out.println();
                if (i % (188*2) ==0 ) System.out.println("");
                System.out.print(hexChars[i]);
            }
        }
    }

    public static char[] bytesToHexArr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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

        System.out.println("\n\n"+frameNumber+", " +numOfPackets+", "+ sizeOfNextFrame);
        System.out.printf("Frame size :  %d \n", sizeOfNextFrame);
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );

        // Should be removed (only for debug).
        //if (sizeOfNextFrame != sizeOfNextFrameCheck){ System.out.println("wrong: size different");
        //}else { System.out.println("correct: size same"); }

        return sizeOfNextFrame;
    }

    public int getNextSevenPacket(byte[] frame) throws Exception {
        int numOfPackets = 7;
        frameNumber++;
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
