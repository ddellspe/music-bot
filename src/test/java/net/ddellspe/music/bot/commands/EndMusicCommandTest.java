package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class EndMusicCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    EndMusicCommand cmd = new EndMusicCommand();
    assertEquals("end", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    EndMusicCommand cmd = new EndMusicCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testStoppingWhenManagerIsStarted() {
    when(mockManager.isStarted()).thenReturn(true, false);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Stopping music bot"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockConnection.disconnect()).thenReturn(Mono.empty().then());

    EndMusicCommand cmd = new EndMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage("Stopping music bot");
    verify(mockManager, times(1)).stop();
    verify(mockConnection, times(1)).disconnect();
    verify(mockManager, times(2)).isStarted();
  }

  @Test
  public void testStoppingWhenManagerIsStartedButFailsToStop() {
    when(mockManager.isStarted()).thenReturn(true, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Stopping music bot"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockConnection.disconnect()).thenReturn(Mono.empty().then());

    EndMusicCommand cmd = new EndMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage("Stopping music bot");
    verify(mockManager, times(1)).stop();
    verify(mockConnection, times(0)).disconnect();
    verify(mockManager, times(2)).isStarted();
  }

  @Test
  public void testStoppingWhenManagerIsNotStarted() {
    when(mockManager.isStarted()).thenReturn(false);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);

    EndMusicCommand cmd = new EndMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockManager, times(1)).isStarted();
    verify(mockMessageChannel, times(0)).createMessage("Stopping music bot");
    verify(mockManager, times(0)).stop();
  }
}
