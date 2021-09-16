package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    final String message;
    if (manager.isStarted()) {
      manager.getScheduler().stop();
      message = "Silencing music bot";
    } else {
      message = "Music bot is not running, currently.";
    }

    return event
        .getMessage()
        .getChannel()
        .flatMap(channel -> channel.createMessage(message))
        .then();
  }
}
