package com.woowanggood;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by SophiesMac on 15. 5. 1..
 */
public class VideoStreamer {
    private static final int TS_PACKET_SIZE_BYTES = 188;
    private static final int MAX_FRAME_SIZE_BYTES = 200000;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private BufferedInputStream fis; //video file pointer
    private int frameNumber; //frame number for next frame
    private int numOfTotalFrames;

    public static LinkedList<Integer> keyFrameIndexTable = new LinkedList<Integer>();

    public VideoStreamer() throws Exception {
        this("movie_new.ts");
    }
    public VideoStreamer(boolean needKeyTable) throws Exception {
        this("movie_new.ts", true);
    }

    public VideoStreamer(String filename) throws Exception {
        this(filename, false);
    }

    public VideoStreamer(String filename, boolean needKeyTable ) throws Exception {
        frameNumber = -1;//frame number start with 0 (zero)

        // find path for a file, if in same package
        fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
        fis.markSupported();
        setFileSize(filename);

        if(needKeyTable){
            generateKeyFrameIndexTable();
            System.out.println("\nnumOfKeyFrames: " + keyFrameIndexTable.size());

            //todo:: bring back fis at the begining point of file( temporal hack for now)
            fis.close();
            fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
            fis.markSupported();
        }
    }

    public boolean isH264iFrame(byte[] buf, int numOfPackets) {
        int offset    = 4;   /** start offset */ //todo 3? 4? //TS header = 4 bytes
        int endOffset = 4;   /** end offset */ //checking 4 bytes each.
        byte[] bufTemp;

        for (int i = 0; offset+i+endOffset < (TS_PACKET_SIZE_BYTES * numOfPackets); i++) {
            //todo if state == 2
            //todo if PID   == VIDEO_STREAM_PID
            //todo if VTYPE == H.264

            /** if Nal Unit Start Prefix 0x 00 00 01 (0x 00 00 00 01 excluded for now ) */
            if (buf[offset+i] == 0 && buf[offset+i+1] == 0 && buf[offset+i+2] == 1) {

                /** nalType = 5 = XY's lower 5 bits in "0x 00 00 01 XY". (so, XY = 65)
                 * (official ref: https://tools.ietf.org/html/rfc3984)
                 * */

                /**
                 fining IDR-flag in NAL unit: in BITs (in ES, if H.264)
                 +--------------------------------------------------+
                 | nal unit start pattern      | F | NRI | nalType  |
                 +--------------------------------------------------+
                 | 00000000 00000000 00000001  | 0 | 11  | 00101    |
                 +--------------------------------------------------+
                 */
                int forbiddenZeroBit =  buf[offset + i + 3] & 0x80;
                int NRI              = (buf[offset + i + 3] & 0x60) >> 5;// nalRefIdc = 11
                int nalType          =  buf[offset + i + 3] & 0x1F;

                if (forbiddenZeroBit == 0 & NRI == 3 & nalType == 5) {
                    bufTemp = Arrays.copyOfRange(buf, offset , offset+i+4);
                    //bufTemp = Arrays.copyOfRange(buf, offset+i, offset+i+4); //todo original
                    System.out.println(nalType+ " it's key frame! " + bytesToHexString(bufTemp));
                    return true;
                }
            }
        }
        return false;
        /*
        int offset = 12; //TS payload offset: starting offset of nalType, from TS packet
        int nalType = buf[offset + 2] == 1 ? (buf[offset + 3] & 0x1f) : (buf[offset + 4] & 0x1f);
        System.out.println("nal type: "+ nalType);
        if (nalType == 5) {
            System.out.println("it's key frame!");
            return true;
        }
        else return false;
        */
    }

    private void setFileSize(String filename){
        int sizeInBytes = (int) new File(filename).length();
        this.numOfTotalFrames = sizeInBytes / TS_PACKET_SIZE_BYTES ;
        System.out.println("size: "+sizeInBytes+" Bytes , "+numOfTotalFrames +" frames");

    }

    private void generateKeyFrameIndexTable() throws Exception {
        int numOfPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for( int i = 0 ; i < numOfTotalFrames ; i++ ) {
            numOfPackets = getNextFrame(buf) / TS_PACKET_SIZE_BYTES;

            if(isH264iFrame(buf, numOfPackets)){//http://stackoverflow.com/questions/1957427/detect-mpeg4-h264-i-frame-idr-in-rtp-stream
                keyFrameIndexTable.add(i);
                System.out.println("i: "+ i+", numOfPackets: " + numOfPackets);
            }
        }
    }

    public int findNearestKeyFrameNumber(int currFrameNumber) {
        return findNearestKeyFrameNumber(currFrameNumber, 0, numOfTotalFrames);
    }

    public int findNearestKeyFrameNumber(int currFrameNumber, int start, int end){
        //todo check if correct
        if(keyFrameIndexTable.contains(currFrameNumber)) return currFrameNumber;
        int i;

        while(true){
            if (end - start < 10)
                return keyFrameIndexTable.get(start);

            i = (start + end) / 2;
            if( currFrameNumber < keyFrameIndexTable.get(i)) {
                end = i;
            } else{
                start = i;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //int counter = 10;
        /** with KeyFrameIndexTable */
        //VideoStreamer vs = new VideoStreamer("movie.ts", true);


        /** without KeyFrameIndexTable */
        VideoStreamer vs = new VideoStreamer("movie.ts", false);
        int numBytes, numPackets;

        byte [] buf = new byte[MAX_FRAME_SIZE_BYTES];

        for( int i=0 ; i< vs.numOfTotalFrames ; i++ ) {
            //numPackets = vs.getNextFrameTest();
            numBytes = vs.getNextFrame(buf);
            numPackets = numBytes / TS_PACKET_SIZE_BYTES;
            //System.out.println("size of frames: " + numBytes);
            System.out.println("num of packets: " + numPackets);

            printOneFrame_BytesToHex_noSpaces(buf, numPackets, i);
        }

        /** print num */
        System.out.println("\n\nnum of key frames total: "+ keyFrameIndexTable.size());
    }

    public static void printOneFrame_BytesToHex_noSpaces(byte[] buf, int numOfPackets, int currFrameNumber ){
        for(int j=0; j < numOfPackets ; j++){
            byte [] bufCopy = Arrays.copyOfRange(buf, j*188, (j+1)*188);

            System.out.println(bytesToHexString(bufCopy)+"\n");
        }
    }

    public static void printOneFrame_BytesToHex(byte[] buf, int numOfPackets, int currFrameNumber ){
        for(int j=0; j < numOfPackets ; j++){
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

        int result = fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        //todo output.write(); //http://examples.javacodegeeks.com/core-java/io/file/4-ways-to-copy-file-in-java/
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