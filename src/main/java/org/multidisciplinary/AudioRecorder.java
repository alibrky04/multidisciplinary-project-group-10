package org.multidisciplinary;

import javax.sound.sampled.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorder {
    public interface Listener {
        void onSamples(double[] samples);
    }

    private final Listener listener;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private AudioFormat audioFormat;

    public AudioRecorder(Listener listener) {
        this.listener = listener;
    }

    public void startRecording(Mixer.Info mixerInfo, AudioFormat format) {
        this.audioFormat = format;
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            isRecording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[targetDataLine.getBufferSize() / 5];
                while (isRecording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        double[] samples = convertBytesToDoubles(buffer, bytesRead, audioFormat);
                        if (listener != null) {
                            listener.onSamples(samples);
                        }
                    }
                }
            });
            recordingThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private double[] convertBytesToDoubles(byte[] buffer, int bytesRead, AudioFormat format) {
        int bytesPerSample = format.getFrameSize();
        int numSamples = bytesRead / bytesPerSample;
        double[] samples = new double[numSamples];
        boolean bigEndian = format.isBigEndian();

        if (format.getSampleSizeInBits() == 16) {
            for (int i = 0; i < numSamples; i++) {
                int byteOffset = i * bytesPerSample;
                int sampleValue;
                if (bigEndian) {
                    sampleValue = ((buffer[byteOffset] << 8) | (buffer[byteOffset + 1] & 0xFF));
                } else {
                    sampleValue = ((buffer[byteOffset + 1] << 8) | (buffer[byteOffset] & 0xFF));
                }
                samples[i] = sampleValue / 32768.0;
            }
        } else if (format.getSampleSizeInBits() == 8) {
            boolean isSigned = format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
            for (int i = 0; i < numSamples; i++) {
                int sampleValue = buffer[i * bytesPerSample] & 0xFF;
                if (isSigned) {
                    if (sampleValue > 127) sampleValue -= 256;
                    samples[i] = sampleValue / 128.0;
                } else {
                    samples[i] = (sampleValue - 128) / 128.0;
                }
            }
        } else {
            return new double[0];
        }
        return samples;
    }
}