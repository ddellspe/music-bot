package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MusicAudioTrackScheduler extends AudioEventAdapter {
  private final List<AudioTrack> queue;
  private final AudioPlayer player;

  public MusicAudioTrackScheduler(final AudioPlayer player, final List<AudioTrack> queue) {
    this.queue = queue;
    this.player = player;
  }

  public MusicAudioTrackScheduler(final AudioPlayer player) {
    this(player, Collections.synchronizedList(new LinkedList<>()));
  }

  public List<AudioTrack> getQueue() {
    return queue;
  }

  public AudioPlayer getPlayer() {
    return player;
  }

  public boolean play(final AudioTrack track) {
    return play(track, false);
  }

  public boolean play(final AudioTrack track, final boolean force) {
    final boolean playing = player.startTrack(track, !force);

    if (!playing) {
      queue.add(track);
    }

    return playing;
  }

  public boolean skip() {
    return !queue.isEmpty() && play(queue.remove(0), true);
  }

  public void stop() {
    if (!queue.isEmpty()) {
      queue.clear();
    }
    player.stopTrack();
  }

  @Override
  public void onTrackEnd(
      final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
    if (endReason.mayStartNext) {
      skip();
    }
  }
}
