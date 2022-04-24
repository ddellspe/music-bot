package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.utils.MessageUtils;

/** AudioLoadResultHandler for the Music Bot Classes. */
public class MusicAudioLoadResultHandler implements AudioLoadResultHandler {
  private final MessageCreateEvent event;
  private final String query;
  private final boolean forcePlay;
  private final boolean requeueCurrent;

  public MusicAudioLoadResultHandler(MessageCreateEvent event, String query) {
    this(event, query, false, false);
  }

  public MusicAudioLoadResultHandler(MessageCreateEvent event, String query, boolean forcePlay) {
    this(event, query, forcePlay, false);
  }

  public MusicAudioLoadResultHandler(
      MessageCreateEvent event, String query, boolean forcePlay, boolean requeueCurrent) {
    this.event = event;
    this.query = query;
    this.forcePlay = forcePlay;
    this.requeueCurrent = requeueCurrent;
  }

  public MessageCreateEvent getEvent() {
    return event;
  }

  public String getQuery() {
    return query;
  }

  public boolean isForcePlay() {
    return forcePlay;
  }

  public boolean shouldRequeueCurrent() {
    return requeueCurrent;
  }

  @Override
  public void trackLoaded(AudioTrack audioTrack) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    if (!manager.getScheduler().play(audioTrack, forcePlay, requeueCurrent)) {
      event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.MEDIUM_SEA_GREEN)
                          .title("Added track to queue")
                          .addField("Track Title", audioTrack.getInfo().title, false)
                          .addField("Track Artist", audioTrack.getInfo().author, false)
                          .addField(
                              "Duration",
                              MessageUtils.getDurationAsMinSecond(audioTrack.getInfo().length),
                              false)
                          .build()))
          .subscribe();
    }
  }

  @Override
  public void playlistLoaded(AudioPlaylist audioPlaylist) {
    MusicAudioTrackScheduler scheduler =
        MusicAudioManager.of(event.getGuildId().get()).getScheduler();
    int count = 0;

    for (AudioTrack audioTrack : audioPlaylist.getTracks()) {
      final boolean playing;
      if (forcePlay) {
        if (count == 0) {
          playing = scheduler.play(audioTrack, true, requeueCurrent);
        } else {
          playing = scheduler.addToQueueAtPosition(audioTrack, count - 1);
        }
      } else {
        playing = scheduler.play(audioTrack);
      }
      if (!playing) {
        event
            .getMessage()
            .getChannel()
            .flatMap(
                channel ->
                    channel.createMessage(
                        EmbedCreateSpec.builder()
                            .color(Color.MEDIUM_SEA_GREEN)
                            .title("Added track to queue")
                            .addField("Track Title", audioTrack.getInfo().title, false)
                            .addField("Track Artist", audioTrack.getInfo().author, false)
                            .addField(
                                "Duration",
                                MessageUtils.getDurationAsMinSecond(audioTrack.getInfo().length),
                                false)
                            .build()))
            .subscribe();
      }
      count++;
    }
  }

  @Override
  public void noMatches() {
    event
        .getMessage()
        .getChannel()
        .flatMap(
            channel ->
                channel.createMessage(
                    EmbedCreateSpec.builder()
                        .color(Color.RED)
                        .title("Could not find track")
                        .addField("Query", query, false)
                        .footer(
                            "This bot does not support searching for a song on "
                                + "YouTube via keyword, you must provide a video id "
                                + "or video link.",
                            null)
                        .build()))
        .subscribe();
  }

  @Override
  public void loadFailed(FriendlyException e) {
    event
        .getMessage()
        .getChannel()
        .flatMap(
            channel ->
                channel.createMessage(
                    EmbedCreateSpec.builder()
                        .color(Color.RED)
                        .title("Error loading the track")
                        .addField("Error Message", e.getMessage(), false)
                        .build()))
        .subscribe();
  }
}
