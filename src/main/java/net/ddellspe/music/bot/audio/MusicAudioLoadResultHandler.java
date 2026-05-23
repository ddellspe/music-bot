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
  private final boolean loadFullPlaylist;

  public MusicAudioLoadResultHandler(MessageCreateEvent event, String query) {
    this(event, query, false, false, false);
  }

  public MusicAudioLoadResultHandler(MessageCreateEvent event, String query, boolean forcePlay) {
    this(event, query, forcePlay, false, false);
  }

  public MusicAudioLoadResultHandler(
      MessageCreateEvent event, String query, boolean forcePlay, boolean requeueCurrent) {
    this(event, query, forcePlay, requeueCurrent, false);
  }

  public MusicAudioLoadResultHandler(
      MessageCreateEvent event,
      String query,
      boolean forcePlay,
      boolean requeueCurrent,
      boolean loadFullPlaylist) {
    this.event = event;
    this.query = query;
    this.forcePlay = forcePlay;
    this.requeueCurrent = requeueCurrent;
    this.loadFullPlaylist = loadFullPlaylist;
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

  public boolean isLoadFullPlaylist() {
    return loadFullPlaylist;
  }

  @Override
  public void trackLoaded(AudioTrack audioTrack) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    if (!manager.getScheduler().play(audioTrack, forcePlay, requeueCurrent)) {
      final int queuePosition = manager.getScheduler().getQueue().size();
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
                          .addField("Queue Position", String.valueOf(queuePosition), false)
                          .build()))
          .subscribe();
    }
  }

  @Override
  public void playlistLoaded(AudioPlaylist audioPlaylist) {
    if (audioPlaylist.isSearchResult()) {
      AudioTrack track = audioPlaylist.getSelectedTrack();
      if (track == null && !audioPlaylist.getTracks().isEmpty()) {
        track = audioPlaylist.getTracks().get(0);
      }
      if (track != null) {
        trackLoaded(track);
      }
      return;
    }

    if (!loadFullPlaylist) {
      AudioTrack initialTrack = audioPlaylist.getSelectedTrack();
      if (initialTrack == null && !audioPlaylist.getTracks().isEmpty()) {
        initialTrack = audioPlaylist.getTracks().get(0);
      }
      final AudioTrack track = initialTrack;
      if (track != null) {
        MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
        final boolean playing = manager.getScheduler().play(track, forcePlay, requeueCurrent);
        final String prefix = manager.getPrefix();
        final String hintCommand = forcePlay ? "forceplayall" : "playall";
        if (!playing) {
          final int queuePosition = manager.getScheduler().getQueue().size();
          event
              .getMessage()
              .getChannel()
              .flatMap(
                  channel ->
                      channel.createMessage(
                          EmbedCreateSpec.builder()
                              .color(Color.MEDIUM_SEA_GREEN)
                              .title("Added track to queue")
                              .addField("Track Title", track.getInfo().title, false)
                              .addField("Track Artist", track.getInfo().author, false)
                              .addField(
                                  "Duration",
                                  MessageUtils.getDurationAsMinSecond(track.getInfo().length),
                                  false)
                              .addField("Queue Position", String.valueOf(queuePosition), false)
                              .footer(
                                  String.format(
                                      "Note: To add the entire playlist, use '%s%s <url>' instead.",
                                      prefix, hintCommand),
                                  null)
                              .build()))
              .subscribe();
        } else {
          event
              .getMessage()
              .getChannel()
              .flatMap(
                  channel ->
                      channel.createMessage(
                          EmbedCreateSpec.builder()
                              .color(Color.MEDIUM_SEA_GREEN)
                              .title("Playing track from playlist")
                              .description(
                                  String.format(
                                      "Now playing targeted track: **%s**.\n*Note: To add the entire playlist, use '%s%s <url>' instead.*",
                                      track.getInfo().title, prefix, hintCommand))
                              .build()))
              .subscribe();
        }
      }
      return;
    }

    MusicAudioTrackScheduler scheduler =
        MusicAudioManager.of(event.getGuildId().get()).getScheduler();
    int count = 0;
    int addedCount = 0;

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
        addedCount++;
      }
      count++;
    }

    if (addedCount > 0) {
      final int totalTracks = audioPlaylist.getTracks().size();
      final int queueSize = scheduler.getQueue().size();
      EmbedCreateSpec.Builder embedBuilder =
          EmbedCreateSpec.builder()
              .color(Color.MEDIUM_SEA_GREEN)
              .title("Added playlist to queue")
              .description("**Playlist:** [" + audioPlaylist.getName() + "](" + query + ")");

      if (!forcePlay) {
        final String positionText;
        if (addedCount > 1) {
          positionText = (queueSize - addedCount + 1) + " - " + queueSize;
        } else {
          positionText = String.valueOf(queueSize);
        }
        embedBuilder.addField("Queue Position", positionText, false);
      }

      final int previewLimit = Math.min(5, totalTracks);
      final int startPosition = forcePlay ? 1 : (queueSize - addedCount + 1);
      for (int i = 0; i < previewLimit; i++) {
        AudioTrack track = audioPlaylist.getTracks().get(i);
        embedBuilder.addField(
            (startPosition + i) + ". " + track.getInfo().title,
            "Artist: "
                + track.getInfo().author
                + " | Duration: "
                + MessageUtils.getDurationAsMinSecond(track.getInfo().length)
                + " | [Video Link]("
                + track.getInfo().uri
                + ")",
            false);
      }

      if (totalTracks > 5) {
        embedBuilder.footer(
            "...and " + (totalTracks - 5) + " more tracks (total of " + totalTracks + ")", null);
      } else {
        embedBuilder.footer("(total of " + totalTracks + ")", null);
      }

      event
          .getMessage()
          .getChannel()
          .flatMap(channel -> channel.createMessage(embedBuilder.build()))
          .subscribe();
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
