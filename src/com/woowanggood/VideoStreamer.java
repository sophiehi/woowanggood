package com.woowanggood;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by SophiesMac on 15. 5. 1..
 */

public class VideoStreamer {
    //TODO RTSP.java switch문 에러 고치기. jdk?
    public int cnt = 0;

    private static final int TS_PACKET_SIZE_BYTES = 188;
    private static final int MAX_FRAME_SIZE_BYTES = 200000;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private RandomAccessFile fis;
    private int nextFrameIndex;         //frame number for next frame
    private int numOfTotalFrames;

    //iframe = keyframe = IDR (in H.264) = xx (in H.265)
    private static LinkedList<Integer>       iFrames        = new LinkedList<Integer>();
    private static LinkedList<PCR_to_iFrame> PCR_to_iFrames = new LinkedList<PCR_to_iFrame>();

    public VideoStreamer() throws Exception {
        this("movie_new.ts");
    }

    public VideoStreamer(String filename) throws Exception {
        this(filename, true);
    }

    public VideoStreamer(String filename, boolean needKeyTable) throws Exception {
        this(filename, needKeyTable, 2);
    }

    public VideoStreamer(String filename, boolean needKeyTable, int keyTableType ) throws Exception {
        //int keyTableType = { 1: iframeIndex list, 2: (pcr, iframeIndex) list, 3:tree, 4: opt augmented red-black tree
        nextFrameIndex = -1;
        fis = new RandomAccessFile(filename, "r");//여기서는 그냥 bufferedInputStream이 나은가?
        this.numOfTotalFrames = (int) fis.length() /TS_PACKET_SIZE_BYTES;

        if(needKeyTable){
            switch(keyTableType){
                case 1:
                    generateKeyFrameIndexTable();
                    ////System.out.println("\n\nnum of key frames total: " + iFrames.size());
                    break;

                case 2:
                    generateKeyFrameIndexTable_list();
                    ////System.out.println("\nnumOfKeyFrames: " + PCR_to_iFrames.size());
                    for (int i = 0; i < PCR_to_iFrames.size(); i++) {
                        ////System.out.println("PCR("+i+"): " + PCR_to_iFrames.get(i).getPcr() + ", iframeIndex: " + PCR_to_iFrames.get(i).getiFrameIndex());
                    }
                    ////System.out.println("\n\nnum of key frames total: " + PCR_to_iFrames.size());
                    break;
                default:
                    break;
            }

            fis.seek(0);
        }
    }

    private double parsePCRFromOneFrame(byte[] buf, int numOfPackets) {/** buf = frame */
        double PCR = -1.0;

        /** parse PCR */
        int adaptationFieldExistFlag = (buf[3] & 0x20) >> 5;
        if (adaptationFieldExistFlag == 1) {
            int PCRExistFlag = (buf[5] & 0x10) >> 4;
            if(PCRExistFlag == 1){
                long pcrBase =
                          ((buf[6] & 0xFFL) << 25)
                        | ((buf[7] & 0xFFL) << 17)
                        | ((buf[8] & 0xFFL) <<  9)
                        | ((buf[9] & 0xFFL) <<  1)
                        | ((buf[10]& 0xFFL) >>  7);

                long pcrExt = ((buf[10] & 0x01L) << 8)
                        | (buf[11] & 0xFFL);

                PCR = ((double)pcrBase / 90000.0f) + ((double)pcrExt/27000000.0f);
                //System.out.println("PCR :" + PCR + " ");
                return PCR;
            }
        }
        return PCR;
    }

    private boolean isH264iFrame(byte[] buf, int numOfPackets) {
        int offset    = 4;   /** start offset */ //TS header = 4 bytes
        int endOffset = 4;   /** end offset */   //checking 4 bytes each.
        byte[] bufTemp;
        double PCR = -1.0;

        /** parse PCR */
        //PCR = parsePCRFromOneFrame(buf, numOfPackets);
        //same as above
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

                PCR = ((double)pcrBase / 90000.0f) + ((double)pcrExt/27000000.0f);
                ////System.out.println("PCR :" + PCR + " ");
            }
        }

        for (int i = 0; offset+i+endOffset < (TS_PACKET_SIZE_BYTES * numOfPackets); i++) {
            //todo if state == 2
            //todo if PID   == VIDEO_STREAM_PID
            //todo if VTYPE == H.264

            /* if Nal Unit Start Prefix 0x 00 00 01 (0x 00 00 00 01 excluded for now ) */
            if (buf[offset+i] == 0 && buf[offset+i+1] == 0 && buf[offset+i+2] == 1) {
                /* nalType = 5 = XY's lower 5 bits in "0x 00 00 01 XY". (so, XY = 65)
                 * (official ref: https://tools.ietf.org/html/rfc3984)
                 * */

                /*
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
                    //System.out.println(nalType+ " it's key frame! " + bytesToHexString(bufTemp));
                    return true;
                }
            }
        }
        return false;
    }

    private int findBiggestPreviousPCRIndex(double thePCR){
        // find BiggestPreviousPCR from thePCR
        // 즉, thePCR 보다 작은 PCR 중에 가장 큰 PCR 을 찾기

          for(int i=0 ; i < PCR_to_iFrames.size(); i++){
            double PCR = PCR_to_iFrames.get(i).getPcr();
            if (thePCR < PCR) return (i-1);
        }
        return -1;
    }

    public void seek(double playPositionInseconds) throws IOException {
        moveFisForRandomAccess(playPositionInseconds);
        ////System.out.println("seek: " + playPositionInseconds + ": " + fis.getFilePointer() / TS_PACKET_SIZE_BYTES);
    }

    public void moveFisForRandomAccess(double playPosInSec) throws IOException {
        int PCRIndex = findBiggestPreviousPCRIndex(playPosInSec);
        int iFrameIndex = PCR_to_iFrames.get(PCRIndex).getiFrameIndex();
        ////System.out.println("PCRIndex: "+ PCRIndex+", FrameIndex: "+iFrameIndex);

        fis.seek(TS_PACKET_SIZE_BYTES * iFrameIndex);
    }

    private void generateKeyFrameIndexTable_list() throws Exception {
        //앞에서부터 iFrame을 찾은 뒤, 바로 연달아오는 PCR과 묶어서 table에 삽입. (즉 PCR과, 그 PCR바로 직전의 iFrame 묶기.)

        double lastPCR;
        int iFrameIndex = -1;
        int numOfPackets;

        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for( int i = 0 ; i < numOfTotalFrames ; i++ ) {
            numOfPackets = getNextFrame(buf) / TS_PACKET_SIZE_BYTES;

            if(isH264iFrame(buf, numOfPackets)){ iFrameIndex = i;}
            double currPCR = parsePCRFromOneFrame(buf, numOfPackets);

            //TODO change from > to >= 0.0
            if (currPCR >= 0.0 && iFrameIndex >= 0) {
                lastPCR = currPCR;
                PCR_to_iFrames.addLast(new PCR_to_iFrame(lastPCR, iFrameIndex));
                iFrameIndex = -1;
            }
        }
    }

    private void generateIndexer_tree() {}

    //todo
    private boolean isH264 (){
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
                ////System.out.println("i: "+ i+", numOfPackets: " + numOfPackets);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        /** with KeyFrameIndexTable */
        //VideoStreamer vs = new VideoStreamer("movie.ts", true);

        /** test Random Access */
        //vs.seek(9.11);
        //vs.seek(107.0);

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

            vs.printOneFrame_BytesToHex_noSpaces(buf, numPackets);
        }
        ////System.out.println("ASDfasdfa : " + vs.cnt);
        ////System.out.println("ASDfasdfa : " + vs.numOfTotalFrames);

    }
    /*
    private void printOneFrame_BytesToHex_noSpaces(byte[] buf, int numOfPackets){
        for(int j=0; j < numOfPackets ; j++){
            byte [] bufCopy = Arrays.copyOfRange(buf, j*188, (j+1)*188);

            System.out.println(bytesToHexString(bufCopy)+"\n");
        }
    }*/

   private void printOneFrame_BytesToHex_noSpaces(byte[] buf, int numOfPackets) throws IOException {
        if(this.isStartingPacket(buf)){
            cnt++;
        }

        for(int j=0; j < numOfPackets ; j++){
            byte [] bufCopy = Arrays.copyOfRange(buf, j*188, (j+1)*188);

            ////System.out.println(bytesToHexString(bufCopy)+"\n");
        }

    }

    private static void printOneFrame_BytesToHex(byte[] buf, int numOfPackets, int currFrameNumber ){
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
                ////System.out.print(hexChars[i]);
            }
        }
    }

    private static char[] bytesToHexArr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return hexChars;
    }

    private static String bytesToHexString(byte[] bytes) {
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

    private boolean isVideoStream(int streamId) {
        //good-ref: http://en.wikipedia.org/wiki/Packetized_elementary_stream

        if (224<= streamId && streamId <= 239){//E0 ~ EF
            return true;
        }
        else return false;
    }

    //static ?
    public boolean isStartingPacket(byte [] buf) throws IOException{
        int payloadUnitStartIndicator;//TS Header 10th Bit.
        int streamId = -1; //PES Header 4th Byte (index 0 = 1st)
        payloadUnitStartIndicator = (buf[1] >>> 6) & 0x01;
        //todo original
        //int TS_payload_offset; // original
        //streamId = tsPacket[TS_payload_offset + 3] & 0xFF; //original

        int offset    = 0;   /** start offset */ //TS header = 4 bytes
        int endOffset = 4;   /** end offset */   //checking 4 bytes each.

        for (int i = 0; offset+i+endOffset < TS_PACKET_SIZE_BYTES; i++) {
            /* if Nal Unit Start Prefix 0x 00 00 01 (0x 00 00 00 01 excluded for now ) */
            if (buf[offset + i] == 0 && buf[offset + i + 1] == 0 && buf[offset + i + 2] == 1) {
                streamId = buf[offset + i + 3] & 0xFF;

                if(payloadUnitStartIndicator == 1 && isVideoStream(streamId)){
                    return true;
                }
            }
        }

        return false;
    }

    private int howManyPacketsForNextFrame() throws IOException {
        long currFis = fis.getFilePointer();//mark fis curr position.
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();//todo 이안에서 fis안움직이게..변경하기.

        /** 2. Count after second packet(following packets), if exists. */
        while(true) {
            numOfPackets++;

            if (isStartingPacket()) {
                numOfPackets--;
                break;
            }
        }

        fis.seek(currFis);//roll back.
        return numOfPackets;
    }

    public int getNextFrame(byte[] frame) throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        nextFrameIndex++;

        /** Write the next frame to "frame[]" as an array of byte. */
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;

        ////System.out.println("\n\n"+ nextFrameIndex +", " +numOfPackets+", "+ sizeOfNextFrame);
        ////System.out.printf("Frame size :  %d \n", sizeOfNextFrame);
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );

        return sizeOfNextFrame;
    }

    public int getNextSevenPacket(byte[] frame) throws Exception {
        int numOfPackets = 7;
        nextFrameIndex++;

        /** Write the next frame to "frame[]" as an array of byte. */
        int sizeOfNextFrame = numOfPackets * TS_PACKET_SIZE_BYTES;
        ////System.out.printf("Frame size :  %d \n", sizeOfNextFrame);
        int sizeOfNextFrameCheck = fis.read (frame, 0, sizeOfNextFrame );

        return sizeOfNextFrame;
    }
}

class TSframe {

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