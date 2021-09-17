package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.voice.VoiceConnection;
import org.springframework.lang.Nullable;

public interface MusicBotCommand {

  @Nullable
  default Snowflake getCurrentVoiceChannel(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    VoiceConnection connection =
        event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
    if (connection != null) {
      return connection.getChannelId().block();
    }
    return null;
  }

  @Nullable
  default Snowflake getCurrentVoiceChannel(MessageCreateEvent event) {
    if (event.getMember().isPresent()) {
      VoiceState state = event.getMember().get().getVoiceState().block();
      if (state != null && state.getChannelId().isPresent()) {
        return state.getChannelId().get();
      }
    }
    return null;
  }
}
