package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.VoiceConnection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AutoLeave implements VoiceStateTrigger {

  @Override
  public boolean isCorrectEventType(VoiceStateUpdateEvent event) {
    return event.isMoveEvent() || event.isLeaveEvent();
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getVoiceChannel();
  }

  @Override
  public Mono<Void> handle(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    Snowflake voiceChannelId = manager.getVoiceChannel();

    final Mono<Boolean> nonBotChannelCountIsZero =
        event
            .getClient()
            .getChannelById(voiceChannelId)
            .cast(VoiceChannel.class)
            .flatMapMany(VoiceChannel::getVoiceStates)
            .flatMap(VoiceState::getMember)
            .filter(member -> !member.isBot())
            .count()
            .map(count -> count == 0);
    return event
        .getClient()
        .getVoiceConnectionRegistry()
        .getVoiceConnection(guildId)
        .filterWhen(___ -> nonBotChannelCountIsZero)
        .doOnNext(___ -> manager.stop())
        .flatMap(VoiceConnection::disconnect);
  }
}
