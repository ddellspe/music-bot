package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SkipCommand implements MessageResponseCommand {

  @Override
  public String getName() {
    return "skip";
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
    if (manager.isStarted()) {
      if (manager.getScheduler().skip()) {
        return Mono.empty();
      } else {
        return event
            .getMessage()
            .getChannel()
            .flatMap(
                channel ->
                    channel.createEmbed(
                        spec ->
                            spec.setColor(Color.DARK_GOLDENROD).setTitle("No track to skip to")))
            .then();
      }
    } else {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createEmbed(
                      spec -> spec.setColor(Color.DARK_GOLDENROD).setTitle("Bot not started")))
          .then();
    }
  }
}
