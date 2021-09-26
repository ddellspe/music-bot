package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import reactor.core.publisher.Mono;

/**
 * This is a command that supports triggering with arguments, this supports a prefix (command) plus
 * argument(s) and those arguments are then able to be processed further by the handle command.
 */
public interface PrefixMessageResponseCommand extends MusicBotCommand {
  String getName();

  Snowflake getFilterChannel(Snowflake guildId);

  Mono<Void> handle(MessageCreateEvent event);

  default String getPrefix(MessageCreateEvent event) {
    return MusicAudioManager.of(event.getGuildId().get()).getPrefix() + getName() + " ";
  }

  default String getMessageAfterPrefix(MessageCreateEvent event) {
    return event.getMessage().getContent().split(getPrefix(event))[1];
  }
}
