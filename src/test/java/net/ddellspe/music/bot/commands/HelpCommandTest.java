package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class HelpCommandTest {
  private static Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    HelpCommand cmd = new HelpCommand();
    assertEquals("help", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    HelpCommand cmd = new HelpCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testPongResponseIsReturned() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    MusicAudioManager mockManager = Mockito.mock(MusicAudioManager.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.BLUE)
            .title("Music Bot Command Help")
            .addField("!help", "display this message", false)
            .addField("!silence", "Stops the music and clears the queue", false)
            .addField(
                "!end",
                "Stops the music, clears the queue, and bot will leave voice channel",
                false)
            .addField(
                "!skip", "Skips to the next track in the queue (if none present, no change)", false)
            .addField(
                "!play <url>",
                "Starts the music bot and plays the song at the provided url, "
                    + "or adds to queue if a song is actively playing.",
                false)
            .addField(
                "!fplay <url>",
                "Starts the music bot and plays the song at the provided url, "
                    + "if a song is playing, or songs are in the queue, "
                    + "the provided song url will take precedence",
                false)
            .addField(
                "!interrupt <url>",
                "Starts the music bot and plays the song at the provided url, "
                    + "if a song is playing, or songs are in the queue, "
                    + "the provided song url will take precedence, "
                    + "and current song will be added to back to the queue",
                false)
            .addField(
                "!play <YouTube playlist URL>",
                "Starts the music bot and plays the song at the provided url, "
                    + "all items in playlist automatically added to queue",
                false)
            .addField(
                "!fplay <YouTube playlist URL>",
                "Starts the music bot and plays the song at the provided url, "
                    + "if a song is playing, or songs are in the queue, "
                    + "the provided playlist url will take precedence, "
                    + "all items in playlist automatically added to beginning queue",
                false)
            .addField(
                "!interrupt <YouTube playlist URL>",
                "Starts the music bot and plays the song at the provided url, "
                    + "if a song is playing, or songs are in the queue, "
                    + "the provided playlist url will take precedence, "
                    + "and current song will be added to back to the queue"
                    + "all items in playlist automatically added to beginning queue",
                false)
            .build();

    MusicAudioManager.set(GUILD_ID, mockManager);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockManager.getPrefix()).thenReturn("!");

    HelpCommand cmd = new HelpCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
    verify(mockManager, times(10)).getPrefix();
  }
}
