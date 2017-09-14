package com.github.tcking.giraffecompressor.ffmpeg;

import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.tcking.giraffecompressor.GiraffeCompressor;

import java.io.IOException;

/**
 * Created by tc on 5/21/17.
 */

public class FFMPEGVideoCompressor2 extends GiraffeCompressor {


    @Override
    protected void compress() throws IOException {

        final String cmd = String.format("-i %s -vcodec libx264 -b:v %s %s"
                , inputFile.getAbsoluteFile()
                , bitRate
                , outputFile.getAbsoluteFile());

        new FFmpegExecutor(context).exec(cmd);


        Log.d(TAG, "==========compress over===========");
    }


    @Override
    protected void doOnUnsubscribe() {
        new FFmpegExecutor(context).killRunningProcesses();
    }
}
