package com.woowanggood;

import java.util.Arrays;

public class RTPPacket {
    private final int HEADER_SIZE = 12; // size of the RTP header
    private final int BYTE_SIZE = 8; // 1 byte = 8 bit

    // fields that compose the RTP header
    private int version;
    private int padding;
    private int extension;
    private int cc;
    private int marker;
    private int payloadType;
    private int sequenceNumber;
    private int timestamp;
    private int Ssrc;

    private byte[] header; // bit stream of the RTP header
    private int payloadSize; // size of the RTP payload
    private byte[] payload; // bit stream of the RTP payload

    // constructor of an RTPPacket object from header fields and payload bit stream
    public RTPPacket(int PType, int seqNumber, int time, byte[] data, int dataLength) {
        // fill by default header fields:
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        Ssrc = 0;

        // fill changing header fields
        sequenceNumber = seqNumber;
        System.out.println(sequenceNumber);
        timestamp = time;
        payloadType = PType;

        // build the header bit stream
        header = new byte[HEADER_SIZE];

        // version = 2 bit, padding = 1 bit, extension = 1 bit, cc = 4 bit
        header[0] = (byte) ((version << 6) | (padding << 5) | (extension << 4) | cc);
        // marker = 1 bit, payloadType = 7 bit
        header[1] = (byte) ((marker << 7) | payloadType);
        // sequenceNumber = 16 bit, top 8 bits are stored at header[2]
        header[2] = (byte) (sequenceNumber >> BYTE_SIZE);
        // followed 8 bits are stored at header[3]
        header[3] = (byte) (sequenceNumber);

        // timestamp = 32 bit and tokenize by 8 bit
        for (int i = 0; i < 4; i++)
            header[7 - i] = (byte) (timestamp >> (BYTE_SIZE * i));
        // Ssrc = 32 bit and tokenize by 8 bit
        for (int i = 0; i < 4; i++)
            header[11 - i] = (byte) (Ssrc >> (BYTE_SIZE * i));

        // fill the payload bit stream
        payloadSize = dataLength;
        payload = data;
    }

    // constructor of an RTPPacket object from the packet bit stream
    public RTPPacket(byte[] packet, int packet_size) {
        // fill default fields
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        Ssrc = 0;

        // check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE) {
            // get the header bit stream
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            // get the payload bit stream
            payloadSize = packet_size - HEADER_SIZE;
            payload = new byte[payloadSize];
            for (int i = HEADER_SIZE; i < packet_size; i++)
                payload[i - HEADER_SIZE] = packet[i];

            // interpret the changing fields of the header:
            payloadType = header[1] & 127;
            sequenceNumber = byteToUnsignedInt(header[3]) + 256 * byteToUnsignedInt(header[2]);
            timestamp = byteToUnsignedInt(header[7]) + 256 * byteToUnsignedInt(header[6]) + 65536 * byteToUnsignedInt(header[5]) + 16777216 * byteToUnsignedInt(header[4]);
        }
    }

    // getPayload: return the payload bit stream of the RTPPacket and its size
    public byte[] getPayload() {
        return payload;
    }

    // getPayloadSize: return the length of the payload
    public int getPayloadSize() {
        return payloadSize;
    }

    // getPacketSize: return the total length of the RTP packet
    public int getPacketSize() {
        return payloadSize + HEADER_SIZE;
    }

    // getPacket: returns the packet bit stream and its length
    public byte[] getPacket() {
        byte[] packet = new byte[HEADER_SIZE + payloadSize];

        // construct the packet = header + payload
        for (int i = 0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i = 0; i < payloadSize; i++)
            packet[i + HEADER_SIZE] = payload[i];

        // return total size of the packet
        return packet;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getPayloadType() {
        return payloadType;
    }

    // print headers
    public void printRTPHeader() {
        System.out.println(Arrays.toString(payload));
    }

    private int byteToUnsignedInt(byte b) {
        if (b >= 0)
            return b;
        else
            return 256 + b;
    }
}
