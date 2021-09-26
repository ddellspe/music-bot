package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

/**
 * Base interface for commands that are set up to support a command name, and filter channel and
 * handle a MessageCreateEvent (a message in a channel).
 */
public interface MessageResponseCommand extends MusicBotCommand {
  String getName();

  Snowflake getFilterChannel(Snowflake guildId);

  Mono<Void> handle(MessageCreateEvent event);
}
