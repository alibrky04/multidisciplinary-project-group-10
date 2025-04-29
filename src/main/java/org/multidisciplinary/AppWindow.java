package org.multidisciplinary;

import org.math.plot.Plot2DPanel;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Main application window.
 * This class builds the graphical user interface (GUI) using Swing.
 * It allows the user to load a WAV file, then displays:
 * - The waveform (amplitude vs time)
 * - The frequency spectrum (amplitude vs frequency) using FFT.
 * It connects everything together: user interaction, audio loading, and plotting.
 */
public class AppWindow extends JFrame {

    private final AudioPlayer audioPlayer;

    /**
     * Constructor for initializing the AppWindow.
     * Sets up the layout, buttons, and plots.
     *
     * @param title The title of the window.
     */
    public AppWindow(final String title) {
        super(title);

        audioPlayer = new AudioPlayer();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        final int window_width = 1000;
        final int window_height = 800;
        setSize(window_width, window_height);
        setLocationRelativeTo(null);

        final Plot2DPanel waveformPlot = createPlot("Time (samples)", "Amplitude");
        final Plot2DPanel frequencyPlot = createPlot("Frequency (Hz)", "Magnitude");

        final JLabel fileNameLabel = new JLabel("No file loaded.");
        fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        final JButton loadButton = new JButton("Load WAV File");
        loadButton.addActionListener(e -> chooseFile()
                .flatMap(this::readSamples)
                .ifPresent(samples -> {
                    renderWaveform(waveformPlot, samples);
                    renderFrequencySpectrum(frequencyPlot, samples);
                    playAudio();
                }));

        final JButton playButton = new JButton("Play WAV File");
        playButton.addActionListener(e -> playAudio());

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(loadButton);
        buttonPanel.add(playButton);

        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(fileNameLabel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(waveformPlot), new JScrollPane(frequencyPlot));

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                final int topPanelHeight = topPanel.getHeight();
                splitPane.setDividerLocation((getHeight() - topPanelHeight) / 2);
            }
        });
    }

    /**
     * Creates a Plot2DPanel with the specified axis labels.
     *
     * @param xLabel The label for the x-axis.
     * @param yLabel The label for the y-axis.
     * @return A new Plot2DPanel instance.
     */
    private Plot2DPanel createPlot(final String xLabel, final String yLabel) {
        final Plot2DPanel plot = new Plot2DPanel();
        plot.setAxisLabels(xLabel, yLabel);
        plot.addLegend("SOUTH");
        return plot;
    }

    /**
     * Opens a file chooser dialog and allows the user to select a WAV file.
     *
     * @return An Optional containing the selected file, if a file was chosen.
     */
    private Optional<File> chooseFile() {
        final JFileChooser chooser = new JFileChooser();
        final int result = chooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? Optional.of(chooser.getSelectedFile()) : Optional.empty();
    }

    /**
     * Reads the samples from the selected WAV file.
     *
     * @param file The file to read.
     * @return An Optional containing the audio samples, or empty if an error occurred.
     */
    private Optional<double[]> readSamples(final File file) {
        try {
            updateFileNameLabel(file.getName());
            audioPlayer.loadAudio(file);

            return Optional.of(AudioProcessor.readWavSamples(file));
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            JOptionPane.showMessageDialog(this, "Error loading WAV file: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Updates the file name label in the GUI with the name of the loaded WAV file.
     *
     * @param fileName The name of the file to display.
     */
    private void updateFileNameLabel(final String fileName) {
        final JLabel fileNameLabel = (JLabel) ((JPanel) getContentPane().getComponent(0)).getComponent(0);
        fileNameLabel.setText("Loaded: " + fileName);
    }

    /**
     * Renders the waveform plot with the given audio samples.
     *
     * @param plot    The Plot2DPanel to render the waveform.
     * @param samples The audio samples to plot.
     */
    private void renderWaveform(final Plot2DPanel plot, final double[] samples) {
        plot.removeAllPlots();
        plot.addLinePlot("Waveform", samples);
    }

    /**
     * Renders the frequency spectrum plot with the given audio samples.
     *
     * @param plot    The Plot2DPanel to render the frequency spectrum.
     * @param samples The audio samples to analyze and plot.
     */
    private void renderFrequencySpectrum(final Plot2DPanel plot, final double[] samples) {
        final double[] magnitudes = FFT.computeMagnitude(samples);
        final int SAMPLE_RATE = 44100;
        final double[] frequencies = FFT.computeFrequencies(magnitudes.length, SAMPLE_RATE);

        plot.removeAllPlots();
        plot.addLinePlot("Frequency Spectrum", frequencies, magnitudes);
    }

    /**
     * Starts playing the loaded audio file through the AudioPlayer.
     * If the audio is already playing, it shows an alert.
     */
    private void playAudio() {
        if (!audioPlayer.isPlaying()) {
            audioPlayer.play();
        } else {
            JOptionPane.showMessageDialog(this, "Audio is already playing.");
        }
    }

    /**
     * Makes the main application window visible.
     * This method is invoked from the Event Dispatch Thread (EDT).
     */
    public void showWindow() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}