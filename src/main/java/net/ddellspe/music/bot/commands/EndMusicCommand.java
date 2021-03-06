package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This is a command to end the manager and pull the bot from the voice channel. This command is not
 * really needed unless someone wants to prevent the bot from making noise when people remain in the
 * channel.
 */
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
        .getClient()
        .getVoiceConnectionRegistry()
        .getVoiceConnection(guildId)
        .filter(___ -> manager.isStarted())
        .doOnNext(___ -> manager.stop(event.getClient()))
        .then();
  }
}
