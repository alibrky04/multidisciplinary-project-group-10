package org.multidisciplinary;

/**
 * Fast Fourier Transform (FFT) utility class.
 * The FFT algorithm transforms a time-domain audio signal (waveform)
 * into a frequency-domain signal (spectrum).
 * This is useful when you want to analyze which frequencies are present in the sound.
 * Important:
 * - If you only want to display the waveform, you don't need FFT.
 * - If you want to display the frequency spectrum, you must use FFT.
 */
public final class FFT {

    private FFT() {}

    /**
     * Computes the magnitude spectrum of a time-domain signal using the FFT algorithm.
     * The input is zero-padded to the next power of two for optimal FFT performance.
     *
     * @param samples the input time-domain signal (an array of real values)
     * @return an array representing the magnitude of each frequency bin in the signal
     */
    public static double[] computeMagnitude(final double[] samples) {
        final int paddedLength = nextPowerOfTwo(samples.length);
        final Complex[] complexSamples = convertToComplex(samples, paddedLength);
        final Complex[] fftResult = fft(complexSamples);

        final double[] magnitudes = new double[paddedLength / 2];
        for (int i = 0; i < paddedLength / 2; i++) {
            magnitudes[i] = fftResult[i].abs();
        }
        return magnitudes;
    }

    /**
     * Computes the actual frequency values corresponding to each FFT bin.
     *
     * @param length the number of bins (i.e., the length of the magnitude array)
     * @param sampleRate the sampling rate of the original time-domain signal
     * @return an array of frequencies (in Hz) corresponding to each bin
     */
    public static double[] computeFrequencies(final int length, final int sampleRate) {
        final double binSize = (double) sampleRate / (length * 2);
        final double[] frequencies = new double[length];
        for (int i = 0; i < length; i++) {
            frequencies[i] = i * binSize;
        }
        return frequencies;
    }

    /**
     * Converts a real-valued signal into a complex array and pads it with zeros to the specified length.
     *
     * @param samples the real-valued time-domain input signal
     * @param paddedLength the desired length after zero-padding (typically a power of two)
     * @return a complex-valued array representing the input signal
     */
    private static Complex[] convertToComplex(final double[] samples, final int paddedLength) {
        final Complex[] result = new Complex[paddedLength];
        for (int i = 0; i < samples.length; i++) {
            result[i] = new Complex(samples[i], 0);
        }
        for (int i = samples.length; i < paddedLength; i++) {
            result[i] = new Complex(0, 0);
        }
        return result;
    }

    /**
     * Calculates the smallest power of two that is greater than or equal to a given number.
     * This is used to determine the optimal length for FFT computation.
     *
     * @param n the input number
     * @return the next power of two greater than or equal to {@code n}
     */
    private static int nextPowerOfTwo(final int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }

    /**
     * Performs the Cooley-Tukey FFT algorithm recursively.
     * This function splits the signal into even and odd parts and combines their FFTs.
     *
     * @param x the complex-valued input signal (length must be a power of two)
     * @return the FFT-transformed complex array
     */
    private static Complex[] fft(final Complex[] x) {
        final int n = x.length;
        if (n == 1) return new Complex[] { x[0] };

        final Complex[] even = new Complex[n / 2];
        final Complex[] odd = new Complex[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }

        final Complex[] q = fft(even);
        final Complex[] r = fft(odd);
        final Complex[] y = new Complex[n];

        for (int k = 0; k < n / 2; k++) {
            final double kth = -2 * Math.PI * k / n;
            final Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + n / 2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }
}