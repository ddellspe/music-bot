package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StartMusicCommand implements MessageResponseCommand {
  @Override
  public String getName() {
    return "start";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    // This will be guaranteed to be present since we're limiting to Join and Move events
    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    Snowflake voiceChannelId = manager.getVoiceChannel();

    final Mono<Boolean> nonBotChannelCountIsGreaterThanZero =
        event
            .getClient()
            .getChannelById(voiceChannelId)
            .cast(VoiceChannel.class)
            .flatMapMany(VoiceChannel::getVoiceStates)
            .flatMap(VoiceState::getMember)
            .filter(member -> !member.isBot())
            .count()
            .map(count -> count > 0);
    return event
        .getMessage()
        .getChannel()
        .filter(___ -> !manager.isStarted())
        .flatMap(channel -> channel.createMessage("Starting Music Manager"))
        .doOnNext(___ -> manager.start())
        .flatMap(channel -> event.getClient().getChannelById(voiceChannelId))
        .cast(VoiceChannel.class)
        .filterWhen(___ -> nonBotChannelCountIsGreaterThanZero)
        .filter(___ -> manager.isStarted())
        .flatMap(
            channel ->
                channel.join(spec -> spec.setSelfDeaf(true).setProvider(manager.getProvider())))
        .then();
  }
}
