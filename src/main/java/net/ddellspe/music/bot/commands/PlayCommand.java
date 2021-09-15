package net.ddellspe.music.bot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PlayCommand implements PrefixMessageResponseCommand {

  @Override
  public String getName() {
    return "play";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);

    final String query = getMessageAfterPrefix(event);
    final String messageForChannel;
    if (manager.isStarted()) {
      messageForChannel = null;
      MusicAudioManager.PLAYER_MANAGER.loadItemOrdered(
          manager,
          query,
          new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack audioTrack) {
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
                                          getDurationAsMinSecond(audioTrack.getInfo().length),
                                          false)))
                  .subscribe();
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {}

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
            public void loadFailed(FriendlyException e) {}
          });
    } else {
      messageForChannel =
          "Music Manager is not started, please type the command, "
              + manager.getPrefix()
              + "start to start the Music Manager.";
    }
    return event
        .getMessage()
        .getChannel()
        .filter(___ -> messageForChannel != null)
        .flatMap(channel -> channel.createMessage(messageForChannel))
        .then();
  }

  private String getDurationAsMinSecond(long duration) {
    long seconds = duration / 1000;
    long hours = seconds / 3600;
    seconds -= hours * 3600;
    long minutes = seconds / 60;
    seconds -= minutes * 60;
    return hours > 0
        ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
        : (minutes > 0
            ? String.format("%02d:%02d", minutes, seconds)
            : String.format("%02d sec.", seconds));
  }
}
