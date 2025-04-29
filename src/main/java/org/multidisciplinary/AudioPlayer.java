package org.multidisciplinary;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * This class handles the loading and playback of audio files in WAV format.
 * It uses the Java Sound API's Clip to play the audio file.
 */
public class AudioPlayer {

    private Clip audioClip;

    /**
     * Loads the given WAV file into the AudioPlayer for playback.
     *
     * @param file The WAV file to load.
     */
    public void loadAudio(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        final AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        final AudioFormat format = audioStream.getFormat();
        final DataLine.Info info = new DataLine.Info(Clip.class, format);

        audioClip = (Clip) AudioSystem.getLine(info);
        audioClip.open(audioStream);
    }

    /**
     * Starts playing the audio clip from the beginning.
     */
    public void play() {
        if (audioClip != null) {
            audioClip.setFramePosition(0);
            audioClip.start();
        }
    }

    /**
     * Stops the playback of the audio clip.
     */
    public void stop() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }
    }

    /**
     * Pauses the playback of the audio clip.
     * Note: The current implementation does not support true pausing; the audio will stop and start again from the beginning.
     */
    public void pause() {
        stop();
    }

    /**
     * Checks if the audio is currently playing.
     *
     * @return true if the audio is playing, false otherwise.
     */
    public boolean isPlaying() {
        return audioClip != null && audioClip.isRunning();
    }
}