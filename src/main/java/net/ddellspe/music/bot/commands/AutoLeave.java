package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.voice.VoiceConnection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.utils.VoiceUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * While technically not a command, this is written similarly to support the behavior as a more of
 * an event-driven command. This works off of voice state triggers, specifically for move and leave
 * events to ensure that when there is no non-bot users in the channel, the bot leaves.
 */
@Component
public class AutoLeave implements VoiceStateTrigger {

  @Override
  public boolean isCorrectEventType(VoiceStateUpdateEvent event) {
    return !event.getCurrent().getUserId().equals(event.getClient().getSelfId())
        && (event.isMoveEvent() || event.isLeaveEvent());
  }

  @Override
  public Snowflake getFilterChannel(VoiceStateUpdateEvent event) {
    return getCurrentVoiceChannel(event);
  }

  @Override
  public Mono<Void> handle(VoiceStateUpdateEvent event) {
    Snowflake guildId = event.getCurrent().getGuildId();
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    Snowflake voiceChannelId = getCurrentVoiceChannel(event);

    return event
        .getClient()
        .getVoiceConnectionRegistry()
        .getVoiceConnection(guildId)
        .filterWhen(___ -> VoiceUtils.botChannelHasNoPeople(event.getClient(), voiceChannelId))
        .doOnNext(___ -> manager.stop(event.getClient()))
        .flatMap(VoiceConnection::disconnect);
  }
}
