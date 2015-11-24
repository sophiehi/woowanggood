package com.woowanggood;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

public class VideoStreamer {
    public int cnt = 0;

    private static final int TS_PACKET_SIZE_BYTES = 188;
    private static final int MAX_FRAME_SIZE_BYTES = 200000;

    private RandomAccessFile fis;
    private int nextFrameIndex;
    private int numOfTotalFrames;

    private static LinkedList<Integer> iFrames = new LinkedList<Integer>();
    private static LinkedList<PCR_to_iFrame> PCR_to_iFrames = new LinkedList<PCR_to_iFrame>();

    public VideoStreamer() throws Exception {
        this("movie.ts");
    }

    public VideoStreamer(String filename) throws Exception {
        this(filename, true);
    }

    public VideoStreamer(String filename, boolean needKeyTable) throws Exception {
        this(filename, needKeyTable, 2);
    }

    public VideoStreamer(String filename, boolean needKeyTable, int keyTableType) throws Exception {
        // int keyTableType = { 1: iframeIndex list, 2: (PCR, iframeIndex) list, 3: tree, 4: opt augmented red-black tree }
        nextFrameIndex = -1;
        fis = new RandomAccessFile(filename, "r");//여기서는 그냥 bufferedInputStream이 나은가?
        this.numOfTotalFrames = (int) fis.length() / TS_PACKET_SIZE_BYTES;

        if (needKeyTable) {
            switch (keyTableType) {
                case 1:
                    generateKeyFrameIndexTable();
                    // System.out.println("\n\nnum of key frames total: " + iFrames.size());
                    break;
                case 2:
                    generateKeyFrameIndexTable_list();
                    // System.out.println("\nnumOfKeyFrames: " + PCR_to_iFrames.size());
                    for (int i = 0; i < PCR_to_iFrames.size(); i++) {
                        // System.out.println("PCR("+i+"): " + PCR_to_iFrames.get(i).getPCR() + ", iframeIndex: " + PCR_to_iFrames.get(i).getiFrameIndex());
                    }
                    break;
                default:
                    break;
            }

            fis.seek(0);
        }
    }

    private double parsePCRFromOneFrame(byte[] buf) {
        double PCR = -1.0;

        /** parse PCR */
        int adaptationFieldExistFlag = (buf[3] & 0x20) >> 5;
        if (adaptationFieldExistFlag == 1) {
            int PCRExistFlag = (buf[5] & 0x10) >> 4;
            if (PCRExistFlag == 1) {
                long PCRBase =
                        ((buf[6] & 0xFFL) << 25)
                                | ((buf[7] & 0xFFL) << 17)
                                | ((buf[8] & 0xFFL) << 9)
                                | ((buf[9] & 0xFFL) << 1)
                                | ((buf[10] & 0xFFL) >> 7);

                long PCRExt = ((buf[10] & 0x01L) << 8)
                        | (buf[11] & 0xFFL);

                PCR = ((double) PCRBase / 90000.0f) + ((double) PCRExt / 27000000.0f);
                //System.out.println("PCR :" + PCR + " ");
                return PCR;
            }
        }
        return PCR;
    }

    private boolean isH264iFrame(byte[] buf, int numOfPackets) {
        int offset = 4; /** start offset */ // TS header = 4 bytes
        int endOffset = 4; /** end offset */ // checking 4 bytes each.
        byte[] bufTemp;
        double PCR = -1.0;

        /** parse PCR */
        // PCR = parsePCRFromOneFrame(buf, numOfPackets);
        // same as above
        int adaptationFieldExistFlag = (buf[3] & 0x20) >> 5;
        if (adaptationFieldExistFlag == 1) {
            int PCRExistFlag = (buf[5] & 0x10) >> 4;
            if (PCRExistFlag == 1) {
                long PCRBase = ((buf[6] & 0xFFL) << 25)
                        | ((buf[7] & 0xFFL) << 17)
                        | ((buf[8] & 0xFFL) << 9)
                        | ((buf[9] & 0xFFL) << 1)
                        | ((buf[10] & 0xFFL) >> 7);

                long PCRExt = ((buf[10] & 0x01L) << 8)
                        | (buf[11] & 0xFFL);

                PCR = ((double) PCRBase / 90000.0f) + ((double) PCRExt / 27000000.0f);
                // System.out.println("PCR :" + PCR + " ");
            }
        }

        for (int i = 0; offset + i + endOffset < (TS_PACKET_SIZE_BYTES * numOfPackets); i++) {
            /* if Nal Unit Start Prefix 0x 00 00 01 (0x 00 00 00 01 excluded for now ) */
            if (buf[offset + i] == 0 && buf[offset + i + 1] == 0 && buf[offset + i + 2] == 1) {
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
                int forbiddenZeroBit = buf[offset + i + 3] & 0x80;
                int NRI = (buf[offset + i + 3] & 0x60) >> 5; // NRI = nalRefIdc = 11
                int nalType = buf[offset + i + 3] & 0x1F;

                if (forbiddenZeroBit == 0 & NRI == 3 & nalType == 5) {
                    bufTemp = Arrays.copyOfRange(buf, offset, offset + i + 4);
                    // bufTemp = Arrays.copyOfRange(buf, offset + i, offset + i + 4); //todo original
                    // System.out.println(nalType + " it's key frame! " + bytesToHexString(bufTemp));
                    return true;
                }
            }
        }
        return false;
    }

    private int findBiggestPreviousPCRIndex(double thePCR) {
        // thePCR 보다 작은 PCR 중에 가장 큰 PCR 을 찾기
        for (int i = 0; i < PCR_to_iFrames.size(); i++) {
            double PCR = PCR_to_iFrames.get(i).getPCR();
            if (thePCR < PCR)
                return (i - 1);
        }
        return -1;
    }

    public void seek(double playPositionInseconds) throws IOException {
        moveFisForRandomAccess(playPositionInseconds);
        System.out.println("seek: " + playPositionInseconds + ": " + fis.getFilePointer() / TS_PACKET_SIZE_BYTES);
    }

    public void moveFisForRandomAccess(double playPosInSec) throws IOException {
        int PCRIndex = findBiggestPreviousPCRIndex(playPosInSec);
        int iFrameIndex = PCR_to_iFrames.get(PCRIndex).getiFrameIndex();
        fis.seek(TS_PACKET_SIZE_BYTES * iFrameIndex);
    }

    private void generateKeyFrameIndexTable_list() throws Exception {
        // 앞에서부터 iFrame을 찾은 뒤, 바로 연달아오는 PCR과 묶어서 table에 삽입. (즉 PCR과, 그 PCR바로 직전의 iFrame 묶기.)

        double lastPCR;
        int iFrameIndex = -1;
        int numOfPackets;

        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for (int i = 0; i < numOfTotalFrames; i++) {
            numOfPackets = getNextFrame() / TS_PACKET_SIZE_BYTES;

            if (isH264iFrame(buf, numOfPackets)) {
                iFrameIndex = i;
            }
            double currPCR = parsePCRFromOneFrame(buf);

            if (currPCR >= 0.0 && iFrameIndex >= 0) {
                lastPCR = currPCR;
                PCR_to_iFrames.addLast(new PCR_to_iFrame(lastPCR, iFrameIndex));
                iFrameIndex = -1;
            }
        }
    }

    private void generateKeyFrameIndexTable() throws Exception {
        // fake
        int numOfPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];
        for (int i = 0; i < numOfTotalFrames; i++) {
            numOfPackets = getNextFrame() / TS_PACKET_SIZE_BYTES;

            if (isH264iFrame(buf, numOfPackets)) {
                // http://stackoverflow.com/questions/1957427/detect-mpeg4-h264-i-frame-idr-in-rtp-stream
                iFrames.add(i);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        /** with KeyFrameIndexTable */
        // VideoStreamer vs = new VideoStreamer("movie.ts", true);

        /** test Random Access */
        // vs.seek(9.11);
        // vs.seek(107.0);

        /** without KeyFrameIndexTable */
        VideoStreamer videoStreamer = new VideoStreamer("movie.ts", false);
        int numBytes, numPackets;
        byte[] buf = new byte[MAX_FRAME_SIZE_BYTES];

        for (int i = 0; i < videoStreamer.numOfTotalFrames; i++) {
            // numPackets = vs.getNextFrameTest();
            numBytes = videoStreamer.getNextFrame();
            numPackets = numBytes / TS_PACKET_SIZE_BYTES;
            // System.out.println("size of frames: " + numBytes);
            System.out.println("num of packets: " + numPackets);

            videoStreamer.printOneFrame_BytesToHex_noSpaces(buf);
        }
        //System.out.println("ASDfasdfa : " + vs.cnt);
        //System.out.println("ASDfasdfa : " + vs.numOfTotalFrames);
    }

    private void printOneFrame_BytesToHex_noSpaces(byte[] buf) throws IOException {
        if (this.isStartingPacket(buf)) {
            cnt++;
        }
    }

    private boolean isVideoStream(int streamId) {
        if (224 <= streamId && streamId <= 239)
            return true;
        else
            return false;
    }

    private boolean isStartingPacket() throws IOException {
        int payloadUnitStartIndicator;
        byte[] aPacket = new byte[TS_PACKET_SIZE_BYTES];

        int result = fis.read(aPacket, 0, TS_PACKET_SIZE_BYTES);
        if (result == -1)
            return true;

        payloadUnitStartIndicator = (aPacket[1] >>> 6) & 0x01;

        switch (payloadUnitStartIndicator) {
            case 1:
                return true;
            case 0:
                return false;
            default:
                System.out.println("ERROR: in isStartingPacket()!");
                return false;
        }
    }

    public boolean isStartingPacket(byte[] buf) throws IOException {
        int payloadUnitStartIndicator; // TS Header 10th Bit.
        int streamId = -1; // PES Header 4th Byte (index 0 = 1st)
        payloadUnitStartIndicator = (buf[1] >>> 6) & 0x01;

        int offset = 0;   /** start offset */ //TS header = 4 bytes
        int endOffset = 4;   /** end offset */   //checking 4 bytes each.

        for (int i = 0; offset + i + endOffset < TS_PACKET_SIZE_BYTES; i++) {
            /* if Nal Unit Start Prefix 0x 00 00 01 (0x 00 00 00 01 excluded for now) */
            if (buf[offset + i] == 0 && buf[offset + i + 1] == 0 && buf[offset + i + 2] == 1) {
                streamId = buf[offset + i + 3] & 0xFF;

                if (payloadUnitStartIndicator == 1 && isVideoStream(streamId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int howManyPacketsForNextFrame() throws IOException {
        long curFis = fis.getFilePointer(); // mark fis curr position.
        int numOfPackets = 0;

        /** 1. Count first packet(starting packet). */
        numOfPackets++;
        isStartingPacket();

        /** 2. Count after second packet(following packets), if exists. */
        while (true) {
            numOfPackets++;

            if (isStartingPacket()) {
                numOfPackets--;
                break;
            }
        }

        fis.seek(curFis); // roll back.
        return numOfPackets;
    }

    public int getNextFrame() throws Exception {
        int numOfPackets = howManyPacketsForNextFrame();
        nextFrameIndex++;

        /** Write the next frame to "frame[]" as an array of byte. */
        return numOfPackets * TS_PACKET_SIZE_BYTES;
    }

    public int getNextSevenPacket() throws Exception {
        int numOfPackets = 7;
        nextFrameIndex++;

        /** Write the next frame to "frame[]" as an array of byte. */
        return numOfPackets * TS_PACKET_SIZE_BYTES;
    }
}

class PCR_to_iFrame {
    private double PCR;
    private int iFrameIndex;

    public PCR_to_iFrame(double PCR, int iFrameIndex) {
        this.PCR = PCR;
        this.iFrameIndex = iFrameIndex;
    }

    public double getPCR() {
        return PCR;
    }

    public int getiFrameIndex() {
        return iFrameIndex;
    }
}
