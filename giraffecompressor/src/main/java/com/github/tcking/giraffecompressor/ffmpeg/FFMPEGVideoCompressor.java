package com.github.tcking.giraffecompressor.ffmpeg;

import android.util.Log;

import com.github.tcking.giraffecompressor.GiraffeCompressor;

import java.io.IOException;

/**
 * Created by tc on 5/21/17.
 */

public class FFMPEGVideoCompressor extends GiraffeCompressor {


    @Override
    protected void compress() throws IOException {

        final String cmd = String.format("-i %s -vcodec libx264 -b:v %s -y %s"
                , inputFile.getAbsoluteFile()
                , bitRate
                , outputFile.getAbsoluteFile());
        FFMPEGCmdExecutorFactory.create().exec(cmd);
        Log.d("FFMPEGVideoCompressor", "compress completed");
    }


    @Override
    protected void doOnUnsubscribe() {
        FFMPEGCmdExecutorFactory.create().killRunningProcesses("");
    }
}
