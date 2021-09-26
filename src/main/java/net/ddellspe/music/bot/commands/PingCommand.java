package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * A simple ping command which responds to the command in order to indicate that the bot is alive.
 * This command requires the DJ role to operate.
 */
@Component
public class PingCommand implements SuperUserMessageResponseCommand {
  @Override
  public String getName() {
    return "ping";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Pong")).then();
  }
}
