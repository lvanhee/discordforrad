package discordforrad.discordmanagement.audio;


import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;



import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
//"C:/Users/loisv/Desktop/Code/my code/discordforrad/lib/libmpg123-0.dll"
//"C:/Users/loisv/Desktop/Code/my code/discordforrad/lib/connector.dll"

public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
    	boolean res = this.audioPlayer.provide(this.frame); 
    	//return true;
    	AudioFrame af = audioPlayer.provide();
    	
        String fileLocation = "C:\\Users\\loisv\\Desktop\\TMP\\test2.mp3";
        SeekableInputStream sis = new LocalSeekableInputStream(new File(fileLocation));
        AudioTrackInfo ati = new AudioTrackInfo("a", "b", 204000, "c", false,  fileLocation);
        Mp3AudioTrack m = new Mp3AudioTrack(ati, sis);
    	audioPlayer.playTrack(m);
    	
    	AudioFrame af2 = audioPlayer.provide();
        return res;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
    	ByteBuffer res = this.buffer.flip(); 
        return res;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}