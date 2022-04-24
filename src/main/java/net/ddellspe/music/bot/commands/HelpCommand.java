package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This is a command to end the manager and pull the bot from the voice channel. This command is not
 * really needed unless someone wants to prevent the bot from making noise when people remain in the
 * channel.
 */
@Component
public class HelpCommand implements MessageResponseCommand {
  @Override
  public String getName() {
    return "help";
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
        .flatMap(
            channel ->
                channel.createMessage(
                    EmbedCreateSpec.builder()
                        .color(Color.BLUE)
                        .title("Music Bot Command Help")
                        .addField(manager.getPrefix() + "help", "display this message", false)
                        .addField(
                            manager.getPrefix() + "silence",
                            "Stops the music and clears the queue",
                            false)
                        .addField(
                            manager.getPrefix() + "end",
                            "Stops the music, clears the queue, and bot will leave voice channel",
                            false)
                        .addField(
                            manager.getPrefix() + "skip",
                            "Skips to the next track in the queue (if none present, no change)",
                            false)
                        .addField(
                            manager.getPrefix() + "play <url>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "or adds to queue if a song is actively playing.",
                            false)
                        .addField(
                            manager.getPrefix() + "fplay <url>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "if a song is playing, or songs are in the queue, "
                                + "the provided song url will take precedence",
                            false)
                        .addField(
                            manager.getPrefix() + "interrupt <url>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "if a song is playing, or songs are in the queue, "
                                + "the provided song url will take precedence, "
                                + "and current song will be added to back to the queue",
                            false)
                        .addField(
                            manager.getPrefix() + "play <YouTube playlist URL>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "all items in playlist automatically added to queue",
                            false)
                        .addField(
                            manager.getPrefix() + "fplay <YouTube playlist URL>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "if a song is playing, or songs are in the queue, "
                                + "the provided playlist url will take precedence, "
                                + "all items in playlist automatically added to beginning queue",
                            false)
                        .addField(
                            manager.getPrefix() + "interrupt <YouTube playlist URL>",
                            "Starts the music bot and plays the song at the provided url, "
                                + "if a song is playing, or songs are in the queue, "
                                + "the provided playlist url will take precedence, "
                                + "and current song will be added to back to the queue"
                                + "all items in playlist automatically added to beginning queue",
                            false)
                        .build()))
        .then();
  }
}
