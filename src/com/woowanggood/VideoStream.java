package com.woowanggood;

/**
 * Created by swchoi06 on 4/4/15.
 */

import java.io.*;

public class VideoStream {
    private FileInputStream fis; //video file

    public VideoStream(String filename) {
        try {
            fis = new FileInputStream(filename);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns the next frame as an array of byte and the size of the frame
    public int getNextFrame(byte[] frame) {
        int length = 0;
        byte[] frameLength = new byte[5];

        try {
            //read current frame length
            fis.read(frameLength, 0, 5);

            //transform frameLength to int
            length = Integer.parseInt(new String(frameLength));

            return fis.read(frame, 0, length);
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
