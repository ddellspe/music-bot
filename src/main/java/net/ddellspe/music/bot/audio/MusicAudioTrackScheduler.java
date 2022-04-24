package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.ddellspe.music.bot.utils.MessageUtils;

public class MusicAudioTrackScheduler extends AudioEventAdapter {
  private final List<AudioTrack> queue;
  private final AudioPlayer player;
  private final MusicAudioManager manager;
  private GatewayDiscordClient client;
  private boolean currentlyPlaying;
  private AudioTrack currentTrack;

  /**
   * The scheduler for the music bot. This scheduler contains the queue of tracks as well as the
   * player and manager, all of these combine to successively provide the points to make the
   * required calls into the chat channel with the appropriate information about playback.
   *
   * <p>This constructor should ONLY be used for testing directly.
   *
   * @param player The Audio Player
   * @param manager The Audio Manager
   * @param queue The Queue of tracks
   */
  public MusicAudioTrackScheduler(
      final AudioPlayer player, final MusicAudioManager manager, final List<AudioTrack> queue) {
    this.player = player;
    this.manager = manager;
    this.queue = queue;
  }

  /**
   * The scheduler for the music bot. This scheduler contains the queue of tracks as well as the
   * player and manager, all of these combine to successively provide the points to make the
   * required calls into the chat channel with the appropriate information about playback.
   *
   * @param player The Audio Player
   * @param manager The Audio Manager
   */
  public MusicAudioTrackScheduler(final AudioPlayer player, final MusicAudioManager manager) {
    this(player, manager, Collections.synchronizedList(new LinkedList<>()));
  }

  public List<AudioTrack> getQueue() {
    return queue;
  }

  public AudioPlayer getPlayer() {
    return player;
  }

  public GatewayDiscordClient getClient() {
    return client;
  }

  public void setClient(GatewayDiscordClient client) {
    this.client = client;
  }

  public MusicAudioManager getManager() {
    return manager;
  }

  /**
   * Play the specified audio track, if something is already playing, the track is added to the
   * queue for later playing when the current track stops or is completed.
   *
   * @param track The AudioTrack to play
   * @return true if the track requested has been set to play
   */
  public boolean play(final AudioTrack track) {
    return play(track, false, false);
  }

  /**
   * Play the specified audio track, boolean options to decide to force the playing of this track.
   *
   * @param track The AudioTrack to play
   * @param force whether to force the playing of the track or not
   * @return true if the track requested has been set to play
   */
  public boolean play(final AudioTrack track, final boolean force) {
    return play(track, force, false);
  }

  /**
   * Play the specified audio track, boolean options to decide to force the playing and if the
   * current playing track should be re-added to the queue.
   *
   * @param track The AudioTrack to play
   * @param force whether to force the playing of the track or not
   * @param requeueCurrent if there's a currently playing track, whether to re-queue it (at the
   *     start of the queue) or to simply skip it
   * @return true if the track requested has been set to play
   */
  public boolean play(final AudioTrack track, final boolean force, final boolean requeueCurrent) {
    if (currentlyPlaying && force && requeueCurrent) {
      AudioTrack updatedTrack = currentTrack.makeClone();
      if (updatedTrack.isSeekable()) {
        updatedTrack.setPosition(currentTrack.getPosition());
      }
      queue.add(0, updatedTrack);
    }
    final boolean playing = player.startTrack(track, !force);
    if (!playing) {
      queue.add(track);
    }
    return playing;
  }

  public boolean skip() {
    return !queue.isEmpty() && play(queue.remove(0), true, false);
  }

  public void stop() {
    if (!queue.isEmpty()) {
      queue.clear();
    }
    player.stopTrack();
  }

  public boolean isCurrentlyPlaying() {
    return currentlyPlaying && currentTrack != null;
  }

  @Override
  public void onPlayerPause(AudioPlayer player) {
    currentlyPlaying = false;
  }

  @Override
  public void onPlayerResume(AudioPlayer player) {
    currentlyPlaying = true;
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    currentlyPlaying = true;
    currentTrack = track;
    // client should never be null, it's set when the manager that owns the scheduler starts
    if (client != null) {
      Snowflake chatChannel = manager.getChatChannel();
      client
          .getChannelById(chatChannel)
          .cast(MessageChannel.class)
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.MEDIUM_SEA_GREEN)
                          .title("Now Playing")
                          .addField("Track Title", track.getInfo().title, false)
                          .addField("Track Artist", track.getInfo().author, false)
                          .addField(
                              "Duration",
                              MessageUtils.getDurationAsMinSecond(track.getInfo().length),
                              false)
                          .build()))
          .subscribe();
    }
  }

  @Override
  public void onTrackEnd(
      final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
    currentlyPlaying = false;
    currentTrack = null;
    switch (endReason) {
      case FINISHED:
      case LOAD_FAILED:
        skip();
        break;
    }
  }
}
