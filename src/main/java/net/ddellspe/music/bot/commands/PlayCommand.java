package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Command that allows a user, when the manager is running, to play an audio track via path lookup.
 * This path can be either a YouTube video url or local path.
 */
@Component
public class PlayCommand implements PrefixMessageResponseCommand {

  @Override
  public String getName() {
    return "play";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    final String query;
    try {
      query = getMessageAfterPrefix(event);
    } catch (IndexOutOfBoundsException e) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.RED)
                          .title("Invalid command: '" + event.getMessage().getContent() + "'")
                          .build()))
          .then();
    }
    if (manager.isStarted()) {
      MusicAudioManager.PLAYER_MANAGER.loadItemOrdered(
          manager, query, new MusicAudioLoadResultHandler(event, query));
      return Mono.empty().then();
    } else {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .color(Color.ORANGE)
                          .title(
                              "Bot not started, please use the command: '"
                                  + manager.getPrefix()
                                  + "start' to start the bot")
                          .build()))
          .then();
    }
  }
}
