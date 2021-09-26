package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

/**
 * A base command interface that supports handling based on a VoiceStateUpdateEvent. Much of this
 * stems from the VoiceStateUpdateEvent being related to the same channel that the bot is currently
 * in.
 */
public interface VoiceStateTrigger extends MusicBotCommand {
  boolean isCorrectEventType(VoiceStateUpdateEvent event);

  Snowflake getFilterChannel(VoiceStateUpdateEvent event);

  Mono<Void> handle(VoiceStateUpdateEvent event);

  default boolean isCorrectChannel(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    VoiceConnection connection =
        event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
    if (connection == null) {
      return false;
    }
    Snowflake channelId = connection.getChannelId().block();
    if (event.getCurrent().getChannelId().isPresent()
        && event.getCurrent().getChannelId().get().equals(channelId)) {
      return true;
    }
    return event.getOld().isPresent()
        && event.getOld().get().getChannelId().isPresent()
        && event.getOld().get().getChannelId().get().equals(channelId);
  }
}
