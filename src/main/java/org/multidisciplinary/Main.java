package org.multidisciplinary;

/**
 * Entry point of the multidisciplinary project application.
 * Initializes and displays the main application window.
 */
public class Main {
    public static void main(String[] args) {
        AppWindow appWindow = new AppWindow("Phonocardiogram Visualizer");
        appWindow.showWindow();
    }
}
