package discordforrad.discordmanagement.audio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class LocalAudioPlayer {
	
	private static final LocalAudioPlayer player = new LocalAudioPlayer();
	
	public static final void play(File f)
	{
		player.play(f.toString());
	}
	
	 public static void main(String[] args) {
	        final LocalAudioPlayer player = new LocalAudioPlayer ();
	        player.play("C:\\Users\\loisv\\Desktop\\Code\\my code\\databases\\discordforrad\\caches\\mp3\\ENaardvark.mp3");
	        player.play("C:\\Users\\loisv\\Desktop\\Code\\my code\\databases\\discordforrad\\caches\\mp3\\ENabalienation.mp3");
	    }
	 
	    public void play(String filePath) {
	        final File file = new File(filePath);
	 
	        try (final AudioInputStream in = getAudioInputStream(file)) {
	             
	            final AudioFormat outFormat = getOutFormat(in.getFormat());
	            final Info info = new Info(SourceDataLine.class, outFormat);
	 
	            try (final SourceDataLine line =
	                     (SourceDataLine) AudioSystem.getLine(info)) {
	 
	                if (line != null) {
	                    line.open(outFormat);
	                    line.start();
	                    stream(getAudioInputStream(outFormat, in), line);
	                    line.drain();
	                    line.stop();
	                }
	            }
	 
	        } catch (UnsupportedAudioFileException 
	               | LineUnavailableException 
	               | IOException e) {
	            throw new IllegalStateException(e);
	        }
	    }
	 
	    private AudioFormat getOutFormat(AudioFormat inFormat) {
	        final int ch = inFormat.getChannels();

	        final float rate = inFormat.getSampleRate();
	        return new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
	    }
	 
	    private void stream(AudioInputStream in, SourceDataLine line) 
	        throws IOException {
	        final byte[] buffer = new byte[4096];
	        for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
	            line.write(buffer, 0, n);
	        }
	    }

}
