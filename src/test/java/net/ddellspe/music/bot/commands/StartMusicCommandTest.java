package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import java.util.Optional;
import java.util.function.Consumer;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class StartMusicCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    StartMusicCommand cmd = new StartMusicCommand();
    assertEquals("start", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    StartMusicCommand cmd = new StartMusicCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testUserNotInVoiceChannel() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

    // This is for the getCurrentVoiceChannel call
    when(mockEvent.getMember()).thenReturn(Optional.empty());
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createEmbed(any(Consumer.class)))
        .thenReturn(Mono.just(mock(Message.class)));

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createEmbed(any());
    verify(mockMessageChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.RED, Color.of(embedSpec.asRequest().color().get()));
    assertEquals(
        "You must be in a voice channel to start the music bot.",
        embedSpec.asRequest().title().get());
  }

  @Test
  public void testStartingWhenManagerIsStopped() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    when(mockManager.isStarted()).thenReturn(false, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);

    // Initializing non-bot count
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));

    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Starting music bot"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));
    when(mockMessageChannel.createEmbed(any(Consumer.class)))
        .thenReturn(Mono.just(mock(Message.class)));
    when(mockVoiceChannel.join(any(Consumer.class))).thenReturn(connection);
    when(mockConnection.isConnected()).thenReturn(Mono.just(Boolean.TRUE));

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage("Starting music bot");
    verify(mockManager, times(1)).start();
    verify(mockManager, times(2)).isStarted();
    // Testing the verification of the join spec would require a LOT of mocking, it's probably not
    // worth it to mock that behavior out just to validate the settings of the spec for code
    // coverage.
  }

  @Test
  public void testStartingWhenManagerIsStoppedVoiceConnectionFails() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    when(mockManager.isStarted()).thenReturn(false, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);

    // Initializing non-bot count
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));

    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Starting music bot"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));
    when(mockVoiceChannel.join(any(Consumer.class))).thenReturn(connection);
    when(mockConnection.isConnected()).thenReturn(Mono.just(Boolean.FALSE));
    when(mockMessageChannel.createEmbed(any(Consumer.class)))
        .thenReturn(Mono.just(mock(Message.class)));

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage("Starting music bot");
    verify(mockManager, times(1)).start();
    verify(mockManager, times(1)).stop();
    verify(mockManager, times(2)).isStarted();
    verify(mockMessageChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.RED, Color.of(embedSpec.asRequest().color().get()));
    assertEquals(
        "Unable to join voice channel, shutting down.", embedSpec.asRequest().title().get());
  }

  @Test
  public void testStartingWhenManagerIsStarted() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    when(mockManager.isStarted()).thenReturn(true, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);

    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(0)).createMessage("Starting music bot");
    verify(mockManager, times(0)).start();
    verify(mockManager, times(1)).isStarted();
  }

  @Test
  public void testStartingWhenManagerIsStoppedOnlyBotUsers() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    when(mockManager.isStarted()).thenReturn(false, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);

    // Initializing non-bot count
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(true);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));

    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Starting music bot"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage("Starting music bot");
    verify(mockManager, times(1)).start();
    verify(mockManager, times(1)).isStarted();
  }

  private void initializeGetCurrentVoiceChannel(
      MessageCreateEvent mockEvent, Snowflake voiceChannelId) {
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<VoiceState> voiceState = Mono.just(mockVoiceState);
    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(voiceState);
    when(mockVoiceState.getChannelId()).thenReturn(Optional.of(voiceChannelId));
  }
}
