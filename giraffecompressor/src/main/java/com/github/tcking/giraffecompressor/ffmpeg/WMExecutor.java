package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.github.tcking.giraffecompressor.GiraffeCompressor;

import java.util.concurrent.CountDownLatch;

/**
 * Created by TangChao on 2017/9/14.
 */

public class WMExecutor implements FFMPEGCmdExecutor {
    private RuntimeException error;
    private static final String TAG = "WMExecutor";

    public boolean exec(final String cmd) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        FFmpeg ffmpeg = FFmpeg.getInstance(GiraffeCompressor.getContext());
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd.split(" "), new ExecuteBinaryResponseHandler() {


                @Override
                public void onStart() {
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "exec command:ffmpeg " + cmd);
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
                        Log.d(GiraffeCompressor.TAG, "command failure :" + message);
                    }
                }

                @Override
                public void onSuccess(String message) {
                    countDownLatch.countDown();
                    if (GiraffeCompressor.DEBUG) {
                        Log.d(GiraffeCompressor.TAG, "command success :" + cmd);
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

    @Override
    public boolean killRunningProcesses(String tag) {
        FFmpeg ffmpeg = FFmpeg.getInstance(GiraffeCompressor.getContext());
        return ffmpeg.killRunningProcesses();
    }

    @Override
    public void init(Context context) {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.i(TAG, "FFmpeg init start");
                }

                @Override
                public void onFailure() {
                    Log.i(TAG, "FFmpeg init failure");
                }

                @Override
                public void onSuccess() {
                    Log.i(TAG, "FFmpeg init success");
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            e.printStackTrace();

        }

    }
}
