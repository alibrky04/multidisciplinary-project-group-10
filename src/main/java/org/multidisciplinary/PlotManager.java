package org.multidisciplinary;

import org.math.plot.Plot2DPanel;
import javax.swing.*;

public class PlotManager {

    public static void updateWaveformPlot(Plot2DPanel plot, double[] data, int windowSize) {
        SwingUtilities.invokeLater(() -> {
            plot.removeAllPlots();
            if (data.length > 0) {
                double[] timeIndices = new double[data.length];
                for (int i = 0; i < data.length; i++) timeIndices[i] = i;
                plot.addLinePlot("Live Waveform", timeIndices, data);
                plot.setFixedBounds(0, 0, windowSize);
                plot.setFixedBounds(1, -1.0, 1.0);
            }
        });
    }

    public static void updateSpectrumPlot(Plot2DPanel plot, double[] frequencies, double[] magnitudes) {
        SwingUtilities.invokeLater(() -> {
            plot.removeAllPlots();
            if (frequencies.length == magnitudes.length && frequencies.length > 0) {
                plot.addLinePlot("Live Spectrum", frequencies, magnitudes);
            }
            plot.setFixedBounds(0, 0, 3000);
        });
    }

    public static void updateStaticWaveformPlot(Plot2DPanel plot, double[] samples) {
        plot.removeAllPlots();
        plot.addLinePlot("Waveform", samples);
    }

    public static void updateStaticSpectrumPlot(Plot2DPanel plot, double[] frequencies, double[] magnitudes) {
        plot.removeAllPlots();
        plot.addLinePlot("Frequency Spectrum", frequencies, magnitudes);
        plot.setFixedBounds(0, 0, 3000);
    }
}