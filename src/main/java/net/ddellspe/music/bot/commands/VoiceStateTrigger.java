package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import reactor.core.publisher.Mono;

public interface VoiceStateTrigger {
  boolean isCorrectEventType(VoiceStateUpdateEvent event);

  Snowflake getFilterChannel(Snowflake guildId);

  Mono<Void> handle(VoiceStateUpdateEvent event);
}
