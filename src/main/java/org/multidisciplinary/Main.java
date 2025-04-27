package org.multidisciplinary;

import javax.swing.SwingUtilities;

/**
 * Entry point of the multidisciplinary project application.
 * Initializes and displays the main application window.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AppWindow().setVisible(true);
        });
    }
}
