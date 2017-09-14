package com.github.tcking.giraffecompressor.ffmpeg;

import com.github.tcking.giraffecompressor.GiraffeCompressor;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.io.IOException;

/**
 * Created by tc on 5/21/17.
 */

public class FFMPEGVideoCompressor extends GiraffeCompressor {

    @Override
    protected void compress() throws IOException {
        String cmd = String.format("ffmpeg -i %s -vcodec libx264 -b:v %s %s"
        ,inputFile.getAbsoluteFile()
        ,bitRate
        ,outputFile.getAbsoluteFile());
        UtilityAdapter.FFmpegRun("", cmd);
        System.out.println("==========compress over===========");
    }
}
