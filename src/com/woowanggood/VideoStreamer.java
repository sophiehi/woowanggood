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
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private BufferedInputStream fis; //video file pointer //todo local variable?
    private int frameNumber;         //frame number for next frame
    private int numOfTotalFrames;

    //iframe =keyframe = IDR (in H.264) = xx (in H.265)
    public static LinkedList<Integer>       iFrames        = new LinkedList<Integer>();
    public static LinkedList<PCR_to_iFrame> PCR_to_iFrames = new LinkedList<PCR_to_iFrame>();

    public VideoStreamer() throws Exception {
        this("movie_new.ts");
    }
    public VideoStreamer(boolean needKeyTable) throws Exception {
        this("movie_new.ts", true);
    }
    public VideoStreamer(String filename) throws Exception {
        this(filename, false);
    }

    public VideoStreamer(String filename, boolean needKeyTable) throws Exception {
        this(filename, needKeyTable, 2);
    }

    public VideoStreamer(String filename, boolean needKeyTable, int keyTableType ) throws Exception {
        //int keyTableType= 1: iframeIndex list, 2: (pcr, iframeIndex) list, 3:tree, 4: opt augmented red-black tree
        frameNumber = -1;//frame number start with 0 (zero)

        // find path for a file, if in same path with this java code file
        fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
        fis.markSupported();
        setFileSize(filename);

        if(needKeyTable){

            switch(keyTableType){
                case 1:
                    generateKeyFrameIndexTable();
                    System.out.println("\n\nnum of key frames total: " + iFrames.size());
                    break;
                case 2:
                    generateKeyFrameIndexTable_list();
                    System.out.println("\nnumOfKeyFrames: " + PCR_to_iFrames.size());
                    for (int i = 0; i < PCR_to_iFrames.size(); i++) {
                        System.out.println("PCR("+i+"): " + PCR_to_iFrames.get(i).getPcr()
                                + ", iframeIndex: " + PCR_to_iFrames.get(i).getiFrameIndex());
                    }
                    System.out.println("\n\nnum of key frames total: " + PCR_to_iFrames.size());
                    break;
                default:
                    break;
            }

            //todo:: bring back fis at the begining point of file( temporal hack for now)
            fis.close();
            fis = new BufferedInputStream(getClass().getResourceAsStream(filename));
            fis.markSupported();
        }
    }

    public double parsePCRFromOneFrame(byte[] buf, int numOfPackets) {/** buf = frame */
        /** parse PCR */
        int adaptationFieldExistFlag = ( buf[3] & 0x20 ) >> 5;
        if (adaptationFieldExistFlag == 1) {
            int PCRExistFlag = (buf[5] & 0x10) >> 4;
            if(PCRExistFlag == 1){
                long pcrBase =    ((buf[6] & 0xFFL)<<25)
                        | ((buf[7] & 0xFFL)<<17)
                        | ((buf[8] & 0xFFL)<<9)
                        | ((buf[9] & 0xFFL)<<1)
                        | ((buf[10]& 0xFFL)>>7);

                long pcrExt = ((buf[10] & 0x01L) << 8)
                        | (buf[11] & 0xFFL);

                double PCR = ((double)pcrBase / 90000.0f) + ((double)pcrExt/27000000.0f);
                System.out.println("PCR :" + PCR + " ");
                return PCR;
            }
        }
        return -1.0;
    }

    public boolean isH264iFrame(byte[] buf, int numOfPackets) {
        int offset    = 4;   /** start offset */ //TS header = 4 bytes
        int endOffset = 4;   /** end offset */   //checking 4 bytes each.
        byte[] bufTemp;

        /** parse PCR */
        int adaptationFieldExistFlag = ( buf[3] & 0x20 ) >> 5;
        if (adaptationFieldExistFlag == 1) {
            int PCRExistFlag = (buf[5] & 0x10) >> 4;
            if(PCRExistFlag == 1){
                long pcrBase =    ((buf[6] & 0xFFL)<<25)
                                | ((buf[7] & 0xFFL)<<17)
                                | ((buf[8] & 0xFFL)<<9)
                                | ((buf[9] & 0xFFL)<<1)
                                | ((buf[10]& 0xFFL)>>7);

                long pcrExt = ((buf[10] & 0x01L) << 8)
                              | (buf[11] & 0xFFL);

                double PCR = ((double)pcrBase / 90000.0f) + ((double)pcrExt/27000000.0f);
                System.out.println("PCR :" + PCR + " ");
            }
        }

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
                int NRI              = (buf[offset + i + 3] & 0x60) >> 5;// NRI = nalRefIdc = 11
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
    }

    private void setFileSize(String filename){
        int sizeInBytes = (int) new File(filename).length();
        this.numOfTotalFrames = sizeInBytes / TS_PACKET_SIZE_BYTES ;
        System.out.println("size: "+sizeInBytes+" Bytes , "+numOfTotalFrames +" frames");

    }
    private int findNearestPCRIndex(double tmpPCR){
        for(int i=0 ; i < PCR_to_iFrames.size(); i++){
            double currPCR = PCR_to_iFrames.get(i).getPcr();
            if (tmpPCR > currPCR) return (i-1);
        }
        return -1;
    }

    public void moveFisForRandomAccess(double playPositionInseconds){
        //ref: http://www.java2s.com/Code/Python/File/Useseektomovefilepointer.htm
        int PCRIndex = findNearestPCRIndex(playPositionInseconds);
        int iFrameIndex = PCR_to_iFrames.get(PCRIndex).getiFrameIndex();

        //todo URGENT
        //fis.seek(TS_PACKET_SIZE_BYTES * iFrameIndex);
        // http://docs.oracle.com/javase/6/docs/api/java/io/RandomAccessFile.html
    }

    private void generateKeyFrameIndexTable_list() throws Exception {
        //find a IDR from the BACKWARD
        //and bind a PCR after IDR
        double lastPCR = 0.0;

        int numOfPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for( int i = 0 ; i < numOfTotalFrames ; i++ ) {
            numOfPackets = getNextFrame(buf) / TS_PACKET_SIZE_BYTES;

            double currPCR = parsePCRFromOneFrame(buf, numOfPackets);

            if (currPCR > 0.0) { lastPCR = currPCR; }

            if(isH264iFrame(buf, numOfPackets)){
                PCR_to_iFrames.addLast(new PCR_to_iFrame(lastPCR, i));
            }
        }
    }

    private void generateIndexer_tree() {
    }

    private boolean isVideoFrame(){
        //if E0 then true
        return false;
    }



    private void generateKeyFrameIndexTable() throws Exception {
        // fake
        int numOfPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for( int i = 0 ; i < numOfTotalFrames ; i++ ) {
            numOfPackets = getNextFrame(buf) / TS_PACKET_SIZE_BYTES;

            if(isH264iFrame(buf, numOfPackets)){//http://stackoverflow.com/questions/1957427/detect-mpeg4-h264-i-frame-idr-in-rtp-stream
                iFrames.add(i);
                System.out.println("i: "+ i+", numOfPackets: " + numOfPackets);
            }
        }
    }

    //fake
    public int findNearestKeyFrameNumber(int currFrameNumber) {
        return findNearestKeyFrameNumber(currFrameNumber, 0, numOfTotalFrames);
    }
    //fake
    public int findNearestKeyFrameNumber(int currFrameNumber, int start, int end){
        //todo check if correct
        if(iFrames.contains(currFrameNumber)) return currFrameNumber;
        int i;

        while(true){
            if (end - start < 10)
                return iFrames.get(start);

            i = (start + end) / 2;
            if( currFrameNumber < iFrames.get(i)) {
                end = i;
            } else{
                start = i;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //int counter = 10;
        /** with KeyFrameIndexTable */
        VideoStreamer vs = new VideoStreamer("movie.ts", true);


        /** without KeyFrameIndexTable */
        /*
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
        }*/
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
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return hexChars;
    }

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
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

class PCR_to_iFrame {
    private double pcr;
    private int iFrameIndex;

    public PCR_to_iFrame(double pcr, int iFrameIndex){
        this.pcr = pcr;
        this.iFrameIndex = iFrameIndex;
    }
    public double getPcr() {return pcr;}
    public int getiFrameIndex() {return iFrameIndex;}
}
