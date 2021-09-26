package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

/**
 * This is a command in response to a chat message that requires a specific role to be present to
 * operate correctly, currently this is handled at the listener level, but it could be configurable
 * in the future.
 */
public interface SuperUserMessageResponseCommand extends MusicBotCommand {
  String getName();

  Snowflake getFilterChannel(Snowflake guildId);

  Mono<Void> handle(MessageCreateEvent event);
}
