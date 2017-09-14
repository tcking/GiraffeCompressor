package com.github.tcking.giraffecompressor.ffmpeg;

import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.tcking.giraffecompressor.GiraffeCompressor;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tc on 5/21/17.
 */

public class FFMPEGVideoCompressor2 extends GiraffeCompressor {
    RuntimeException error;


    @Override
    protected void compress() throws IOException {

        final String cmd = String.format("-i %s -vcodec libx264 -b:v %s %s"
        ,inputFile.getAbsoluteFile()
        ,bitRate
        ,outputFile.getAbsoluteFile());

        final CountDownLatch countDownLatch = new CountDownLatch(1);


        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd.split(" "), new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    if (DEBUG) {
                        Log.d(TAG, "exec command:ffmpeg "+cmd);
                    }

                }


                @Override
                public void onProgress(String message) {
                    if (DEBUG) {
                        Log.d(TAG, message);
                    }
                }

                @Override
                public void onFailure(String message) {
                    countDownLatch.countDown();
                    error = new RuntimeException(message);
                    if (DEBUG) {
                        Log.d(TAG, "command failure :"+message);
                    }
                }

                @Override
                public void onSuccess(String message) {
                    countDownLatch.countDown();
                    if (DEBUG) {
                        Log.d(TAG, "command success :"+cmd);
                    }
                }

                @Override
                public void onFinish() {
                    if (DEBUG) {
                        Log.d(TAG, "command failure finish");
                    }
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
        try {
            countDownLatch.await();
            if (error != null) {
                throw error;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"==========compress over===========");
    }

    @Override
    protected void doOnUnsubscribe() {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        ffmpeg.killRunningProcesses();
    }
}
