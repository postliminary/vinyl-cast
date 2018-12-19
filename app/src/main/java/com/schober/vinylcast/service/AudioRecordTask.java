package com.schober.vinylcast.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Runnable that handles saving raw PCM audio data to an input stream.
 */

public class AudioRecordTask implements Runnable {
    private static final String TAG = "AudioRecorderRunnable";

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int AUDIO_SAMPLE_RATE = 48000;
    private static final int AUDIO_CHANNEL_COUNT = 2;
    private static final int AUDIO_BIT_DEPTH = 16;

    private static final int MIN_RAW_BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
    );

    private AudioRecord audioRecord;

    private PipedOutputStream rawAudioOutputStream;
    private PipedInputStream rawAudioInputStream;

    private PipedOutputStream musicDetectOutputStream;
    private PipedInputStream musicDetectInputStream;

    public AudioRecordTask() {
        Log.d(TAG, "AudioRecordTask - BufferSize: " + MIN_RAW_BUFFER_SIZE);

        this.audioRecord = new AudioRecord(
                AUDIO_SOURCE,
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                MIN_RAW_BUFFER_SIZE);

        try {
            Log.d(TAG, "device: " + audioRecord.getPreferredDevice());
            Log.d(TAG, "productName: " + audioRecord.getPreferredDevice().getProductName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get InputStream that provides raw audio output. Must be called before starting Runnable.
     * @return rawAudioInputStream
     */
    public InputStream getRawAudioInputStream() {
        try {
            this.rawAudioInputStream = new PipedInputStream(MIN_RAW_BUFFER_SIZE);
            this.rawAudioOutputStream = new PipedOutputStream(rawAudioInputStream);

            this.musicDetectInputStream = new PipedInputStream(MIN_RAW_BUFFER_SIZE);
            this.musicDetectOutputStream = new PipedOutputStream(musicDetectInputStream);

            return this.rawAudioInputStream;
        } catch (IOException e) {
            Log.e(TAG, "Exception creating output stream", e);
            return null;
        }
    }

    public int getSampleRate() {
        return AUDIO_SAMPLE_RATE;
    }

    public int getChannelCount() {
        return AUDIO_CHANNEL_COUNT;
    }

    @Override
    public void run() {
        Log.d(TAG, "starting...");

        audioRecord.startRecording();

        byte[] buffer = new byte[MIN_RAW_BUFFER_SIZE];
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int bufferReadResult = audioRecord.read(buffer, 0, buffer.length);
                if (bufferReadResult > 0) {
                    rawAudioOutputStream.write(buffer, 0, bufferReadResult);
                    rawAudioOutputStream.flush();
                    musicDetectOutputStream.write(buffer, 0, bufferReadResult);
                    musicDetectOutputStream.flush();
                }
            } catch (InterruptedIOException e) {
                Log.d(TAG, "interrupted");
                break;
            } catch (IOException e) {
                Log.e(TAG, "Exception writing audio output", e);
                break;
            }
        }

        Log.d(TAG, "stopping...");
        audioRecord.stop();
        audioRecord.release();
        try {
            rawAudioOutputStream.close();
            musicDetectOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception closing streams", e);
        }
    }
}
