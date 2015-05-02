package com.woowanggood;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by SophiesMac on 15. 5. 1..
 */
public class VideoStreamer {
    public final int TS_PACKET_SIZE_BYTES = 188;
    private final int MAX_FRAME_SIZE_BYTES = 200000;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    //InputStream in;
    BufferedInputStream fis; //video file pointer
    int frameNumber; //frame number for next frame
    int numOfTotalFrames;
    public static LinkedList<Integer> keyFrameIndexTable = new LinkedList<Integer>();

    public VideoStreamer() throws Exception { this("movie_new.ts"); }

    public VideoStreamer(String filename) throws Exception {
        frameNumber = 0;

        // find path for a file, if in same package
        fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
        fis.markSupported();

        int sizeInBytes = (int) new File(filename).length();
        numOfTotalFrames = sizeInBytes / TS_PACKET_SIZE_BYTES ;
        System.out.println("size: "+sizeInBytes+" Bytes , "+numOfTotalFrames +" frames");
        generateKeyFrameIndexTable();
        System.out.println("numOFKeyFrames: " + keyFrameIndexTable.size());
    }

    private boolean isKeyFrame(byte[] buf, int numOfPackets, int currFrameNumber ){
        //COPY 1ST PACKET의 20번째(19th) 바이트.
        /**
         TS Packet packet starts as below
         +-----------+--------------------+---------------------------------------------------
         | TS Header | PES Header         | ES (=PES Payloas) ....
         | (4 Bytes  | (6 Bytes)          | (178 Bytes)
         +-----------+--------------------+---------------------------------------------------
         ex.
         47 40 31 31   07 00 FF FF   FF FF FF FF   00 00 01 E0   00 00 80 "C0"   0A 35 B5 39
         * */
        byte [] bufCopy = Arrays.copyOfRange(buf, 19, 20);

        String hexString = bytesToHexString(bufCopy);
        System.out.println(hexString);
        if(hexString.equals("C0")) {
            System.out.println("it's key frame!");
            return true;
        }
        return false;
    }

    private void generateKeyFrameIndexTable() throws Exception {
        int numOfPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];

        for( int i = 1 ; i <= numOfTotalFrames ; i++ ) { //todo original
            //for( int i = 1 ; i <= 30 ; i++ ) {
            numOfPackets = getNextFrame(buf) / TS_PACKET_SIZE_BYTES ;

            if(isKeyFrame(buf, numOfPackets, i)){
                keyFrameIndexTable.add(i);
                System.out.println("i: "+ i);
            }
        }
    }

    public int findNearestKeyFrameNumber(int currFrameNumber){
        return currFrameNumber;
    }

    public static void main(String[] args) throws Exception {
        VideoStreamer vs = new VideoStreamer();

        /*
        for( int i=0 ; i<30 ; i++ ) {
            currFrameNumber++;
            //numPackets = vs.getNextFrameTest();
            numBytes = vs.getNextFrame(buf);
            //System.out.println("num of packets: " + numPackets + "\n");
            System.out.println("size of frames: " + numBytes);
            System.out.println("num of packets: " + numBytes/188);

            printOneFrame_BytesToHex(buf, numBytes, currFrameNumber);
        }
        */
    }

    public static void printOneFrame_BytesToHex(byte[] buf, int numOfPackets, int currFrameNumber ){
        for(int j=0; j < numOfPackets ; j++){
            byte [] bufCopy = Arrays.copyOfRange(buf, j*188, (j+1)*188);

            /** check if buf is a key-frame */
            // todo (now not working)
            //byte [] oneByte = Arrays.copyOfRange(bufCopy, 5, 6);//copy 5th Byte
            //boolean isKeyFrame;
            //if ((oneByte[1] & 1) == 1) isKeyFrame = true ;
            //else isKeyFrame = false ;
            //if (isKeyFrame){
            //    keyFrameIndexTable.add(currFrameNumber);
            //}
            /** */

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
        /** check if it's Key frame */
        //if (hexChars[38] == 'C' && hexChars[39] == '0')

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

        int result = fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        if(result == -1) return true; //error, false ?

        payloadUnitStartIndicator = (aPacket[1] >>> 6) & 0x01;

        switch (payloadUnitStartIndicator) {
            case 1: return true;
            case 0: return false;
            default: System.out.println("ERROR: in isStartingPacket()!");
                return false;
        }
    }

    private int howManyPacketsForNextFrame_WithFisMovingForward() throws IOException {
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();

        /** 2. Count after second packet(following packets), if exists. */
        while(true) {
            fis.mark(200);/** 2-1. MARKED. mark = maybe next starting point(?) */
            numOfPackets++;

            if (isStartingPacket()) {
                numOfPackets--;
                fis.reset();/** 2-2. Rewind by 1 packet. Jumping back to MARKED above. */
                break;
            }
        }

        return numOfPackets;
    }


    private int howManyPacketsForNextFrame() throws IOException {
        fis.mark(MAX_FRAME_SIZE_BYTES);
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();

        /** 2. Count after second packet(following packets), if exists. */
        while(true) {
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
}