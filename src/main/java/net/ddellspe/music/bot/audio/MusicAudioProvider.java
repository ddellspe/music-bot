package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;
import java.nio.ByteBuffer;

public class MusicAudioProvider extends AudioProvider {
  private final AudioPlayer player;
  private final MutableAudioFrame frame;

  /**
   * This is just the provider for the player, if we want to add additional ways to provide audio
   * based on lookups, this is where we would do it.
   *
   * @param player the AudioPlayer for playback
   */
  public MusicAudioProvider(final AudioPlayer player) {
    super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));

    frame = new MutableAudioFrame();
    frame.setBuffer(getBuffer());
    this.player = player;
  }

  public AudioPlayer getPlayer() {
    return player;
  }

  public MutableAudioFrame getFrame() {
    return frame;
  }

  @Override
  public boolean provide() {
    final boolean didProvide = player.provide(frame);

    if (didProvide) {
      getBuffer().flip();
    }

    return didProvide;
  }
}
