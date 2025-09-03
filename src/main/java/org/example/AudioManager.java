package org.example;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class AudioManager
{
    private Clip backgroundClip;
    private boolean isPlaying = false;

    public void startBackgroundMusic(String filePath)
    {
        stopBackgroundMusic();

        try
        {
            File audioFile = new File(filePath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioIn);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
            isPlaying = true;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
        {
            System.err.println("Error playing background music: " + e.getMessage());
        }
    }

    public void stopBackgroundMusic()
    {
        if (backgroundClip != null && isPlaying)
        {
            backgroundClip.stop();
            backgroundClip.close();
            isPlaying = false;
        }
    }

    public void setVolume(float volume) {
        if (backgroundClip != null && backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }
}
