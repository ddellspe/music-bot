package net.ddellspe.music.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.utils.MessageUtils;

public class MusicAudioLoadResultHandler implements AudioLoadResultHandler {
  private final MessageCreateEvent event;
  private final String query;

  public MusicAudioLoadResultHandler(MessageCreateEvent event, String query) {
    this.event = event;
    this.query = query;
  }

  @Override
  public void trackLoaded(AudioTrack audioTrack) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    manager.getScheduler().play(audioTrack);
    event
        .getMessage()
        .getChannel()
        .flatMap(
            channel ->
                channel.createEmbed(
                    spec ->
                        spec.setColor(Color.MEDIUM_SEA_GREEN)
                            .setTitle("Added Track to queue")
                            .addField("Track Title", audioTrack.getInfo().title, false)
                            .addField("Track Artist", audioTrack.getInfo().author, false)
                            .addField(
                                "Duration",
                                MessageUtils.getDurationAsMinSecond(audioTrack.getInfo().length),
                                false)))
        .subscribe();
  }

  @Override
  public void playlistLoaded(AudioPlaylist audioPlaylist) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    for (AudioTrack audioTrack : audioPlaylist.getTracks()) {
      manager.getScheduler().play(audioTrack);
      event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createEmbed(
                      spec ->
                          spec.setColor(Color.MEDIUM_SEA_GREEN)
                              .setTitle("Added Track to queue")
                              .addField("Track Title", audioTrack.getInfo().title, false)
                              .addField("Track Artist", audioTrack.getInfo().author, false)
                              .addField(
                                  "Duration",
                                  MessageUtils.getDurationAsMinSecond(audioTrack.getInfo().length),
                                  false)))
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
                channel.createEmbed(
                    spec ->
                        spec.setColor(Color.RED)
                            .setTitle("Could not find track")
                            .addField("Query", query, false)
                            .setFooter(
                                "This bot does not support searching for a song on "
                                    + "YouTube via keyword, you must provide a video id "
                                    + "or video link.",
                                null)))
        .subscribe();
  }

  @Override
  public void loadFailed(FriendlyException e) {
    event
        .getMessage()
        .getChannel()
        .flatMap(
            channel ->
                channel.createEmbed(
                    spec ->
                        spec.setColor(Color.RED)
                            .setTitle("Error loading the track")
                            .addField("Error Message", e.getMessage(), false)))
        .subscribe();
  }
}
