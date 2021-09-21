package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.Optional;
import java.util.function.Consumer;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class PlayCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;
  private MessageCreateEvent mockEvent;
  private Message mockMessage;
  private MessageChannel mockChatChannel;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockMessage = Mockito.mock(Message.class);
    mockChatChannel = Mockito.mock(MessageChannel.class);

    MusicAudioManager.set(GUILD_ID, mockManager);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChatChannel));
  }

  @Test
  public void testGetName() {
    PlayCommand cmd = new PlayCommand();
    assertEquals("play", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    PlayCommand cmd = new PlayCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testInvalidQuerySendsMessageOfException() {
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockMessage.getContent()).thenReturn("!play");
    when(mockChatChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockChatChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.RED, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Invalid command: '!play'", embedSpec.asRequest().title().get());
  }

  @Test
  public void testManagerNotStartedSendsMessageOfStatus() {
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockMessage.getContent()).thenReturn("!play stuff");
    when(mockManager.isStarted()).thenReturn(false);
    when(mockChatChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockChatChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.ORANGE, Color.of(embedSpec.asRequest().color().get()));
    assertEquals(
        "Bot not started, please use the command: '!start' to start the bot",
        embedSpec.asRequest().title().get());
  }

  @Test
  public void testManagerStartedNoMessageSentLoadItemOrderedCalled() {
    AudioPlayerManager mockAudioPlayerManager = Mockito.mock(AudioPlayerManager.class);
    MusicAudioManager.PLAYER_MANAGER = mockAudioPlayerManager;
    ArgumentCaptor<MusicAudioLoadResultHandler> musicAudioLoadResultHandlerCaptor =
        ArgumentCaptor.forClass(MusicAudioLoadResultHandler.class);

    when(mockMessage.getContent()).thenReturn("!play stuff");
    when(mockManager.isStarted()).thenReturn(true);

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockAudioPlayerManager)
        .loadItemOrdered(eq(mockManager), eq("stuff"), musicAudioLoadResultHandlerCaptor.capture());
    MusicAudioLoadResultHandler musicAudioLoadResultHandler =
        musicAudioLoadResultHandlerCaptor.getValue();
    assertEquals(mockEvent, musicAudioLoadResultHandler.getEvent());
    assertEquals("stuff", musicAudioLoadResultHandler.getQuery());
  }
}
