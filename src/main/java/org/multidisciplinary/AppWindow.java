package org.multidisciplinary;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window.
 * This class builds the graphical user interface (GUI) using Swing.
 * It allows the user to load a WAV file, then displays:
 * - The waveform (amplitude vs time)
 * - The frequency spectrum (amplitude vs frequency) using FFT.
 * It connects everything together: user interaction, audio loading, and plotting.
 */
public class AppWindow extends JFrame {
    public AppWindow() {}
}
