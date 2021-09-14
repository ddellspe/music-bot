package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PingCommand implements SuperUserMessageResponseCommand {
  @Override
  public String getName() {
    return "ping";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    return null;
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Pong")).then();
  }
}
