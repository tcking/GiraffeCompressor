package com.github.tcking.giraffecompressor.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

/**
 * Created by TangChao on 2017/5/22.
 */

@TargetApi(18)
public class JellyMediaCodecVideoCompressor extends BaseMediaCodecVideoCompressor{

    private static final int TIMEOUT_USEC = 100;
    private static final boolean VERBOSE = true;

    int trackIndex = -1;

    boolean isMuxerStarted = false;


    long lastSampleTime = 0;

    long encoderPresentationTimeUs = 0;


    private void setupEncoder() {
        try {
            //2.初始化输出文件格式
            outputVideoMediaFormat = initOutputVideoMediaFormat(trackInfo);
            encoder = MediaCodec.createEncoderByType(trackInfo.getVideoMediaFormat().getString(MediaFormat.KEY_MIME));
            encoder.configure(outputVideoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            encoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setupMuxer() {

        try {
            muxer = createMuxer();
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }
    }



    private void feedDataToEncoder() {

        lastSampleTime = 0;

        MediaCodec decoder = null;

        mediaExtractor.selectTrack(trackInfo.getVideoIndex());

        try {
            decoder = MediaCodec.createDecoderByType(trackInfo.getVideoMediaFormat().getString(MediaFormat.KEY_MIME));
            outputSurface = new OutputSurface();

            decoder.configure(trackInfo.getVideoMediaFormat(), outputSurface.getSurface(), null, 0);
            decoder.start();

            resampleVideo(mediaExtractor, decoder);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (outputSurface != null) {
                outputSurface.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            if (mediaExtractor != null) {
                mediaExtractor.release();
                mediaExtractor = null;
            }
        }
    }



    private void releaseOutputResources() {

        if (inputSurface != null) {
            inputSurface.release();
        }

        if (encoder != null) {
            encoder.stop();
            encoder.release();
        }

        if (muxer != null) {
            muxer.stop();
            muxer.release();
            muxer = null;
        }
    }


    private void resampleVideo(MediaExtractor extractor, MediaCodec decoder) {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int outputCount = 0;

        boolean outputDoneNextTimeWeCheck = false;

        boolean outputDone = false;
        boolean inputDone = false;
        boolean decoderDone = false;

        while (!outputDone) {
            if (VERBOSE)
                Log.d(TAG, "edit loop");
            // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    // Copy a chunk of input to the decoder. The first chunk should have
                    // the BUFFER_FLAG_CODEC_CONFIG flag set.
                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    inputBuf.clear();

                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (sampleSize < 0) {
                        inputDone = true;
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        Log.d(TAG, "InputBuffer ADVANCING");
                        decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }

                    inputChunk++;
                } else {
                    if (VERBOSE)
                        Log.d(TAG, "input buffer not available");
                }
            }

            // Assume output is available. Loop until both assumptions are false.
            boolean decoderOutputAvailable = !decoderDone;
            boolean encoderOutputAvailable = true;
            while (decoderOutputAvailable || encoderOutputAvailable) {
                // Start by draining any pending output from the encoder. It's important to
                // do this before we try to stuff any more data in.
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE)
                        Log.d(TAG, "no output from encoder available");
                    encoderOutputAvailable = false;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    if (VERBOSE)
                        Log.d(TAG, "encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                    MediaFormat newFormat = encoder.getOutputFormat();

                    trackIndex = muxer.addTrack(newFormat);
                    muxer.start();
                    isMuxerStarted = true;
                    if (VERBOSE)
                        Log.d(TAG, "encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    // fail( "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus );
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        // fail( "encoderOutputBuffer " + encoderStatus + " was null" );
                    }
                    // Write the data to the output "file".
                    if (info.size != 0) {
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);
                        outputCount++;

                        muxer.writeSampleData(trackIndex, encodedData, info);

                        if (VERBOSE)
                            Log.d(TAG, "encoder output " + info.size + " bytes");
                    }
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    encoder.releaseOutputBuffer(encoderStatus, false);
                }

                if (outputDoneNextTimeWeCheck) {
                    outputDone = true;
                }

                if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // Continue attempts to drain output.
                    continue;
                }
                // Encoder is drained, check to see if we've got a new frame of output from
                // the decoder. (The output is going to a Surface, rather than a ByteBuffer,
                // but we still get information through BufferInfo.)
                if (!decoderDone) {
                    int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE)
                            Log.d(TAG, "no output from decoder available");
                        decoderOutputAvailable = false;
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // decoderOutputBuffers = decoder.getOutputBuffers();
                        if (VERBOSE)
                            Log.d(TAG, "decoder output buffers changed (we don't care)");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // expected before first buffer of data
                        MediaFormat newFormat = decoder.getOutputFormat();
                        if (VERBOSE)
                            Log.d(TAG, "decoder output format changed: " + newFormat);
                    } else if (decoderStatus < 0) {
                        // fail( "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus );
                    } else { // decoderStatus >= 0
                        if (VERBOSE)
                            Log.d(TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                        // The ByteBuffers are null references, but we still get a nonzero
                        // size for the decoded data.
                        boolean doRender = (info.size != 0);
                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture. The API doesn't
                        // guarantee that the texture will be available before the call
                        // returns, so we need to wait for the onFrameAvailable callback to
                        // fire. If we don't wait, we risk rendering from the previous frame.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender) {
                            // This waits for the image and renders it after it arrives.
                            if (VERBOSE)
                                Log.d(TAG, "awaiting frame");
                            outputSurface.awaitNewImage();
                            outputSurface.drawImage();
                            // Send it to the encoder.

                            long nSecs = info.presentationTimeUs * 1000;

                            Log.d("this", "Setting presentation time " + nSecs / (1000 * 1000));
                            nSecs = Math.max(0, nSecs);

                            encoderPresentationTimeUs += (nSecs - lastSampleTime);

                            lastSampleTime = nSecs;

                            inputSurface.setPresentationTime(encoderPresentationTimeUs);
                            if (VERBOSE)
                                Log.d(TAG, "swapBuffers");
                            inputSurface.swapBuffers();
                        }
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            // encoder.signalEndOfInputStream();
                            outputDoneNextTimeWeCheck = true;
                        }
                    }
                }
            }
        }
        if (inputChunk != outputCount) {

        }
    }



    /**
     * Creates a muxer to write the encoded frames.
     * <p>
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    protected MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(outputFile.getAbsolutePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }



    @Override
    protected void compress() throws IOException {
        initTrackInfo();
        setupEncoder();
        setupMuxer();
        feedDataToEncoder();
        encoder.signalEndOfInputStream();
        releaseOutputResources();

    }
}
