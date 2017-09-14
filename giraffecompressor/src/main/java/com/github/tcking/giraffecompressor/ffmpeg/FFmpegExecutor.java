package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.tcking.giraffecompressor.GiraffeCompressor;

import java.util.concurrent.CountDownLatch;

/**
 * Created by TangChao on 2017/9/14.
 */

public class FFmpegExecutor {

    RuntimeException error;

    public FFmpegExecutor(Context context) {
        this.context = context;
    }

    private Context context;

    public boolean exec(final String cmd) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd.split(" "), new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "exec command:ffmpeg "+cmd);
                    }
                }


                @Override
                public void onProgress(String message) {
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, message);
                    }
                }

                @Override
                public void onFailure(String message) {
                    countDownLatch.countDown();
                    error = new RuntimeException(message);
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "command failure :"+message);
                    }
                }

                @Override
                public void onSuccess(String message) {
                    countDownLatch.countDown();
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "command success :"+cmd);
                    }
                }

                @Override
                public void onFinish() {
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "command failure finish");
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
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void killRunningProcesses() {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        ffmpeg.killRunningProcesses();
    }
}
