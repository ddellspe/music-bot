package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
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

  public boolean play(final AudioTrack track) {
    return play(track, false);
  }

  public boolean play(final AudioTrack track, final boolean force) {
    final boolean playing = player.startTrack(track, !force);

    if (!playing) {
      queue.add(track);
    } else {
      // client should never be null in these cases, since the manager has to be started to play a
      // track
      if (client != null) {
        Snowflake chatChannel = manager.getChatChannel();
        client
            .getChannelById(chatChannel)
            .cast(MessageChannel.class)
            .flatMap(
                channel ->
                    channel.createEmbed(
                        spec ->
                            spec.setColor(Color.MEDIUM_SEA_GREEN)
                                .setTitle("Now Playing")
                                .addField("Track Title", track.getInfo().title, false)
                                .addField("Track Artist", track.getInfo().author, false)
                                .addField(
                                    "Duration",
                                    MessageUtils.getDurationAsMinSecond(track.getInfo().length),
                                    false)))
            .subscribe();
      }
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
