package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * While technically not a command, this is written similarly to support the behavior as a more of
 * an event-driven command. This works off of voice state triggers, specifically for leave events
 * when it's the actual bot that leaves, this is set up to deal with when someone disconnects the
 * bot from a channel manually, it takes care of cleaning up the manager connection.
 */
@Component
public class SelfBoot implements VoiceStateTrigger {

  @Override
  public boolean isCorrectEventType(VoiceStateUpdateEvent event) {
    return event.getCurrent().getUserId().equals(event.getClient().getSelfId())
        && event.isLeaveEvent();
  }

  @Override
  public boolean isCorrectChannel(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    VoiceConnection connection =
        event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
    if (connection == null) {
      return false;
    }
    Snowflake channelId = connection.getChannelId().block();
    return VoiceStateTrigger.super.isCorrectChannel(event) || channelId == null;
  }

  @Override
  public Snowflake getFilterChannel(VoiceStateUpdateEvent event) {
    return getCurrentVoiceChannel(event);
  }

  @Override
  public Mono<Void> handle(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    MusicAudioManager manager = MusicAudioManager.of(guildId);

    if (manager.isStarted()) {
      manager.stop(event.getClient());
      return event
          .getClient()
          .getChannelById(manager.getChatChannel())
          .cast(MessageChannel.class)
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.RED)
                          .title("Bot has been disconnected from the voice channel.")
                          .build()))
          .then();
    } else {
      return Mono.empty().then();
    }
  }
}
