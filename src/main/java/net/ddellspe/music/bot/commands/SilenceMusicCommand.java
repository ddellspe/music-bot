package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Command which silences all actively playing music and clears the queue of further tracks. */
@Component
public class SilenceMusicCommand implements MessageResponseCommand {
  @Override
  public String getName() {
    return "silence";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    // This will be guaranteed to be present since we're limiting to Join and Move events
    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);

    if (manager.isStarted()) {
      manager.getScheduler().stop();
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createEmbed(
                      spec ->
                          spec.setColor(Color.MEDIUM_SEA_GREEN)
                              .setTitle("Music has been silenced")))
          .then();
    } else {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createEmbed(
                      spec -> spec.setColor(Color.RED).setTitle("Music bot is not running")))
          .then();
    }
  }
}
