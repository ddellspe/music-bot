package net.ddellspe.music.bot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.List;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.utils.MessageUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Command that displays the upcoming tracks in the current playlist queue. Allows configuring the
 * limit of tracks to display.
 */
@Component
public class ListCommand implements PrefixMessageResponseCommand {

  @Override
  public String getName() {
    return "list";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public String getPrefix(MessageCreateEvent event) {
    return MusicAudioManager.of(event.getGuildId().get()).getPrefix() + getName();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    String content = event.getMessage().getContent().trim();
    String prefix = getPrefix(event);
    if (!content.equalsIgnoreCase(prefix) && !content.toLowerCase().startsWith(prefix + " ")) {
      return Mono.empty();
    }

    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);

    if (!manager.isStarted()) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.DARK_GOLDENROD)
                          .title("Bot not started")
                          .build()))
          .then();
    }

    int limit = 5;
    if (content.toLowerCase().startsWith(prefix + " ")) {
      try {
        limit = Integer.parseInt(content.substring(prefix.length() + 1).trim());
        if (limit <= 0) {
          limit = 5;
        }
      } catch (NumberFormatException e) {
        limit = 5;
      }
    }

    List<AudioTrack> queue = manager.getScheduler().getQueue();
    if (queue.isEmpty()) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.DARK_GOLDENROD)
                          .title("Playlist is empty")
                          .build()))
          .then();
    }

    final int displayCount = Math.min(limit, queue.size());
    EmbedCreateSpec.Builder embedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next " + displayCount + " of " + queue.size() + " tracks)");

    for (int i = 0; i < displayCount; i++) {
      AudioTrack track = queue.get(i);
      embedBuilder.addField(
          (i + 1) + ". " + track.getInfo().title,
          "Artist: "
              + track.getInfo().author
              + " | Duration: "
              + MessageUtils.getDurationAsMinSecond(track.getInfo().length)
              + " | [Video Link]("
              + track.getInfo().uri
              + ")",
          false);
    }

    return event
        .getMessage()
        .getChannel()
        .flatMap(channel -> channel.createMessage(embedBuilder.build()))
        .then();
  }
}
