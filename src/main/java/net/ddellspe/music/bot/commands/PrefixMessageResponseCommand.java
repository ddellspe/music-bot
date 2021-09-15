package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import reactor.core.publisher.Mono;

public interface PrefixMessageResponseCommand {
  String getName();

  Snowflake getFilterChannel(Snowflake guildId);

  Mono<Void> handle(MessageCreateEvent event);

  default String getMessageAfterPrefix(MessageCreateEvent event) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    String prefix = manager.getPrefix() + getName() + ' ';
    return event.getMessage().getContent().split(prefix)[1];
  }
}
