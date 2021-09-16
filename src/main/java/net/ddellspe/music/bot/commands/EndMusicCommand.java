package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.voice.VoiceConnection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EndMusicCommand implements MessageResponseCommand {
  @Override
  public String getName() {
    return "end";
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

    return event
        .getMessage()
        .getChannel()
        .filter(___ -> manager.isStarted())
        .flatMap(channel -> channel.createMessage("Stopping music bot"))
        .doOnNext(___ -> manager.stop())
        .flatMap(___ -> event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId))
        .filter(___ -> !manager.isStarted())
        .flatMap(VoiceConnection::disconnect);
  }
}
