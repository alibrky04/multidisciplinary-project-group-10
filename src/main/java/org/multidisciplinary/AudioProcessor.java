package org.multidisciplinary;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for reading and processing WAV audio files.
 * It reads raw audio data from a WAV file using the Java Sound API,
 * and converts the byte data into a float array normalized between [-1, 1].
 */
public final class AudioProcessor {

    private AudioProcessor() {}

    /**
     * Reads a PCM signed WAV file and returns normalized amplitude samples.
     *
     * @param file The .wav file to read.
     * @return A double[] array containing amplitudes in the range [-1, 1].
     * @throws IOException If an I/O error occurs.
     * @throws UnsupportedAudioFileException If the audio format is not PCM signed.
     */
    public static double[] readWavSamples(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file)) {
            final AudioFormat format = audioStream.getFormat();

            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                throw new UnsupportedAudioFileException("Only PCM signed WAV files are supported.");
            }

            final int bytesPerSample = format.getSampleSizeInBits() / 8;
            final long numFrames = audioStream.getFrameLength();
            final int totalSamples = (int) numFrames;
            final int frameSize = format.getFrameSize();
            final byte[] buffer = new byte[(int) (numFrames * frameSize)];
            final int bytesRead = audioStream.read(buffer);

            if (bytesRead != buffer.length) {
                throw new IOException("Incomplete WAV file read.");
            }

            final double[] samples = new double[totalSamples];

            for (int i = 0, s = 0; i < buffer.length; i += frameSize, s++) {
                int sample = 0;
                // We only take the first channel for mono representation
                for (int b = 0; b < bytesPerSample; b++) {
                    sample |= (buffer[i + b] & 0xFF) << (8 * b);
                }
                if (bytesPerSample == 2 && sample >= 32768) {
                    sample -= 65536;
                }
                samples[s] = sample / Math.pow(2, format.getSampleSizeInBits() - 1);
            }

            return samples;
        }
    }
}