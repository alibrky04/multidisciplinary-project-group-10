package org.multidisciplinary;

import org.math.plot.Plot2DPanel;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.Line;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Vector;

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

    private JComboBox<Mixer.Info> inputDeviceComboBox;
    private JButton startLiveButton;
    private JButton stopLiveButton;
    private JLabel statusLabel; // fileNameLabel'ı durum için de kullanabiliriz veya yeni ekleyebiliriz
    private volatile boolean isRecording = false; // Kayıt durumunu belirten bayrak (volatile önemli!)
    
    private TargetDataLine targetDataLine;        // Aktif ses giriş hattı
    private Thread recordingThread;             // Ses okuma iş parçacığı
    private AudioFormat audioFormat;              // Kullanılacak ses formatı

    private Plot2DPanel waveformPlot;
    private Plot2DPanel frequencyPlot;

    // Waveform için kayan pencere verisi
    private final int WAVEFORM_WINDOW_SIZE = 4096 * 16; // Gösterilecek örnek sayısı geriye dönük ses uzunluğu ile ilişkili
    private LinkedList<Double> waveformWindowData = new LinkedList<>();

    // FFT Analiz penceresi (Örnek boyut, tampon boyutundan farklı olabilir)
    private final int FFT_WINDOW_SIZE = 2048;
    private double[] fftBuffer = new double[FFT_WINDOW_SIZE];
    private int fftBufferIndex = 0;

    /**
     * Constructor for initializing the AppWindow.
     * Sets up the layout, buttons, and plots.
     *
     * @param title The title of the window.
     */
    public AppWindow(final String title) {
        super(title);// JFrame'in constructor'ını çağırır, pencere başlığını ayarlar.

        audioPlayer = new AudioPlayer();// Ses çalmak için kullanılacak nesneyi oluşturur.

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Pencere Boyutunu ve Konumunu Ayarlama
        final int window_width = 1000;
        final int window_height = 800;
        setSize(window_width, window_height);
        setLocationRelativeTo(null);

        // 3. Grafik Panellerini Oluşturma
        waveformPlot = createPlot("Time (samples)", "Amplitude");
        waveformPlot.setFixedBounds(0, 0, WAVEFORM_WINDOW_SIZE);
        waveformPlot.setFixedBounds(1, -1.0, 1.0); // Örnek: 16-bit için -1 ile 1 arası normalleştirilmiş

        frequencyPlot = createPlot("Frequency (Hz)", "Magnitude");
        frequencyPlot.setFixedBounds(0, 0, 300); // Eksen 0 = X ekseni

        // Genlik/Frekans eksenlerini sabitlemek iyi olabilir


        // 4. Etiket ve Düğmeleri Oluşturma
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

        // --- YENİ CANLI KONTROL ELEMANLARI ---
        inputDeviceComboBox = new JComboBox<>();
        populateInputDevices(); // ComboBox'ı dolduran metodu çağır

        startLiveButton = new JButton("Record");//isimler değişebilir.
        stopLiveButton = new JButton("Stop");
        stopLiveButton.setEnabled(false);

        startLiveButton.addActionListener(e -> startRecording(waveformPlot, frequencyPlot));
        stopLiveButton.addActionListener(e -> stopRecording());

        // 5. Düğmeleri Gruplayan Panel
        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(startLiveButton);
        buttonPanel.add(stopLiveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(playButton);


        // 6. Üst Paneli Oluşturma (Etiket ve Düğme Paneli)
        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(fileNameLabel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 7. Bölünmüş Paneli (Split Pane) Oluşturma
        // İki grafik panelini dikey olarak ayıran ve boyutları ayarlanabilen bir bölme oluşturur.
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(waveformPlot), new JScrollPane(frequencyPlot));

        // 8. Ana Bileşenleri Pencereye Ekleme
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // 9. Pencere Boyutu Değiştiğinde Bölücüyü Ayarlama
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

    private void populateInputDevices() {
        Vector<Mixer.Info> mixers = new Vector<>();
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            // TargetDataLine destekleyen bir hat var mı kontrol et
            Line.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, getDesiredAudioFormat()); // Formatı kontrol et
            if (mixer.isLineSupported(targetLineInfo)) {
                mixers.add(mixerInfo);
            }
        }
        // Modeli oluştur ve ComboBox'a ata
        inputDeviceComboBox.setModel(new DefaultComboBoxModel<>(mixers));

        // ComboBox için bir renderer ekleyerek sadece cihaz adını göstermek daha kullanıcı dostu olur
        inputDeviceComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Mixer.Info) {
                    setText(((Mixer.Info) value).getName());
                }
                return this;
            }
        });
    }
    /**
     * Canlı kayıt için tercih edilen ses formatını döndürür.
     * @return AudioFormat nesnesi.
     */
    private AudioFormat getDesiredAudioFormat() {
        // Bu formatı ihtiyaçlarınıza göre ayarlayın
        float sampleRate = 44100.0F;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = false; // Yaygın olan little-endian
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    private void startRecording(Plot2DPanel wavePlot, Plot2DPanel freqPlot) {
        Mixer.Info selectedMixerInfo = (Mixer.Info) inputDeviceComboBox.getSelectedItem();
        if (selectedMixerInfo == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir giriş aygıtı seçin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        audioFormat = getDesiredAudioFormat(); // Formatı al
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        try {
            Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
            targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            // Tampon boyutunu ses formatına göre ayarlayabiliriz.
            // Örn: 1024 byte / (16 bit/örnek = 2 byte/örnek) = 512 örnek/tampon
            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize() / 10; // Örnek: 1/10 saniyelik tampon
            targetDataLine.open(audioFormat, bufferSize);

        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this, "Seçilen aygıt kullanılamıyor: " + e.getMessage(), "Hat Unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        isRecording = true;
        recordingThread = new Thread(this::recordAndProcessAudio); // Metot referansı ile thread oluştur
        recordingThread.setName("Audio Recording Thread");
        recordingThread.start();

        targetDataLine.start(); // Ses akışını başlat

        // GUI Durumunu Güncelle
        startLiveButton.setEnabled(false);
        stopLiveButton.setEnabled(true);
        inputDeviceComboBox.setEnabled(false);
        // İsteğe bağlı: Dosya yükleme/çalmayı devre dışı bırak
        // loadButton.setEnabled(false);
        // playButton.setEnabled(false);
        statusLabel.setText("Recording from: " + selectedMixerInfo.getName());
        waveformWindowData.clear(); // Önceki veriyi temizle
        fftBufferIndex = 0; // FFT tamponunu sıfırla
    }

    /**
     * Canlı ses kaydını durdurur.
     */
    private void stopRecording() {
        if (targetDataLine != null) {
            isRecording = false; // Thread'e durma sinyali
            targetDataLine.stop();
            targetDataLine.close();
            targetDataLine = null; // Kaynağı serbest bırak
        }

        // Thread'in bitmesini beklemek isteyebilirsiniz (GUI'yi kilitleyebilir!)
        // try {
        //     if (recordingThread != null) {
        //         recordingThread.join(1000); // 1 saniye bekle
        //     }
        // } catch (InterruptedException e) {
        //     Thread.currentThread().interrupt(); // Kesintiyi tekrar işaretle
        //     System.err.println("Recording thread interruption");
        // }
        recordingThread = null;

        // GUI Durumunu Güncelle
        startLiveButton.setEnabled(true);
        stopLiveButton.setEnabled(false);
        inputDeviceComboBox.setEnabled(true);
        // İsteğe bağlı: Dosya yükleme/çalmayı etkinleştir
        // loadButton.setEnabled(true);
        // playButton.setEnabled(true);
        statusLabel.setText("Ready. Select input or load file.");
    }

    private void recordAndProcessAudio() {
        byte[] buffer = new byte[targetDataLine.getBufferSize() / 5]; // Daha küçük okuma tamponu
        double[] samplesForWaveform;
        double[] samplesForFFT;

        while (isRecording) {
            int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                // 1. Byte'ları double'a çevir (waveform için)
                samplesForWaveform = convertBytesToDoubles(buffer, bytesRead, audioFormat);

                // 2. Waveform penceresini güncelle (EDT'de)
                updateLiveWaveform(waveformPlot, samplesForWaveform);

                // 3. FFT tamponunu doldur
                samplesForFFT = samplesForWaveform; // Veya aynı dönüşümü kullan
                int samplesToAdd = samplesForFFT.length;
                int spaceLeft = FFT_WINDOW_SIZE - fftBufferIndex;

                if (samplesToAdd >= spaceLeft) {
                    // Tampon doldu veya taştı
                    System.arraycopy(samplesForFFT, 0, fftBuffer, fftBufferIndex, spaceLeft);
                    // FFT HESAPLA
                    processFFT(frequencyPlot, fftBuffer.clone()); // Klonuyla çalışmak daha güvenli olabilir
                    // Tamponu kaydır (eski verinin bir kısmını atıp yeniyi ekle - overlapping)
                    // Veya sıfırdan başla: fftBufferIndex = 0;
                    // Basitlik için sıfırlayalım:
                    int remainingSamples = samplesToAdd - spaceLeft;
                    System.arraycopy(samplesForFFT, spaceLeft, fftBuffer, 0, remainingSamples);
                    fftBufferIndex = remainingSamples;
                } else {
                    // Tampona ekle
                    System.arraycopy(samplesForFFT, 0, fftBuffer, fftBufferIndex, samplesToAdd);
                    fftBufferIndex += samplesToAdd;
                }

            } else if (bytesRead == -1) {
                // Hat kapandı veya hata oluştu
                break; // Döngüden çık
            }
        }
        System.out.println("Recording thread finished.");
    }

    /**
     * Byte dizisini double dizisine çevirir (16-bit PCM varsayımı).
     * @param buffer Okunan byte tamponu.
     * @param bytesRead Tampondaki geçerli byte sayısı.
     * @param format Ses formatı.
     * @return Normalleştirilmiş double örnekleri (-1.0 ile 1.0 arası).
     */
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
                // 16-bit signed değeri (-32768 to 32767) -1.0 to 1.0 arasına normalleştir
                samples[i] = sampleValue / 32768.0;
            }
        } else if (format.getSampleSizeInBits() == 8) {
             // 8-bit için (signed veya unsigned olmasına göre değişir)
             boolean isSigned = format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
             for (int i = 0; i < numSamples; i++) {
                 int sampleValue = buffer[i * bytesPerSample] & 0xFF; // Önce unsigned oku
                 if (isSigned) {
                     if (sampleValue > 127) sampleValue -= 256; // Signed'a çevir (-128 to 127)
                     samples[i] = sampleValue / 128.0; // Normalleştir
                 } else {
                     samples[i] = (sampleValue - 128) / 128.0; // Unsigned'ı (-1 to 1) arasına getir
                 }
             }
        } else {
            // Diğer formatlar desteklenmiyor (şimdilik)
            System.err.println("Unsupported sample size: " + format.getSampleSizeInBits());
            return new double[0]; // Boş dizi döndür
        }
        return samples;
    }

    private void updateLiveWaveform(Plot2DPanel plot, double[] newSamples) {
        // Yeni örnekleri pencereye ekle
        for (double sample : newSamples) {
            waveformWindowData.addLast(sample);
        }
        // Pencere boyutunu aşan eski örnekleri kaldır
        while (waveformWindowData.size() > WAVEFORM_WINDOW_SIZE) {
            waveformWindowData.removeFirst();
        }

        // Mevcut pencere verisini kopyala (ConcurrentModificationException önlemek için)
        final double[] plotData = waveformWindowData.stream().mapToDouble(d -> d).toArray();

        // GUI Güncellemesi (EDT üzerinde yapılmalı!)
        SwingUtilities.invokeLater(() -> {
             plot.removeAllPlots(); // Önceki çizimi temizle
             if (plotData.length > 0) {
                 double[] timeIndices = new double[plotData.length];
                 for(int i=0; i<plotData.length; i++) timeIndices[i] = i; // Basit indeksleme
                 plot.addLinePlot("Live Waveform", timeIndices, plotData);

                // --- SINIRLARI YENİDEN UYGULA ---
                plot.setFixedBounds(0, 0, WAVEFORM_WINDOW_SIZE); // X Ekseni
                plot.setFixedBounds(1, -1.0, 1.0); 
             }
        });
    }

    private void processFFT(Plot2DPanel plot, double[] fftData) {
        // Pencereleme fonksiyonu uygulamak (örn. Hanning) FFT sonuçlarını iyileştirebilir
        // applyWindow(fftData); // Opsiyonel

        // FFT Hesapla (Mevcut FFT sınıfınızı kullanın)
        final double[] magnitudes = FFT.computeMagnitude(fftData); // Mevcut FFT sınıfınızı varsayıyoruz
        final double[] frequencies = FFT.computeFrequencies(magnitudes.length, (int) audioFormat.getSampleRate());

        // Spektrumu güncelle (EDT'de)
        updateLiveSpectrum(plot, frequencies, magnitudes);
    }

    private void updateLiveSpectrum(Plot2DPanel plot, double[] frequencies, double[] magnitudes) {
        // Verileri kopyala (thread güvenliği için iyi bir pratik)
        final double[] freqData = frequencies.clone();
        final double[] magData = magnitudes.clone();

        // GUI Güncellemesi (EDT üzerinde yapılmalı!)
        SwingUtilities.invokeLater(() -> {
            plot.removeAllPlots(); // Önceki çizimi temizle
            if (freqData.length == magData.length && freqData.length > 0) {
                plot.addLinePlot("Live Spectrum", freqData, magData);
            }
            plot.setFixedBounds(0, 0, 3000);
        });
    }

}