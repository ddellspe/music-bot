package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.audio.MusicAudioProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("You must be in a voice channel to start the music bot.")
            .build();

    // This is for the getCurrentVoiceChannel call
    when(mockEvent.getMember()).thenReturn(Optional.empty());
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testStartingWhenManagerIsStopped() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    String voiceChannelName = "Voice";
    MusicAudioProvider mockProvider = Mockito.mock(MusicAudioProvider.class);
    when(mockManager.isStarted()).thenReturn(false, true);
    when(mockManager.getProvider()).thenReturn(mockProvider);
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Music Bot Started")
            .addField("Joined Channel", voiceChannelName, true)
            .build();
    EmbedCreateSpec failedCreateSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title(
                "Unable to start the manager, make sure I have permissions "
                    + "in the voice channel that you are in.")
            .build();
    VoiceChannelJoinSpec joinSpec =
        VoiceChannelJoinSpec.builder().selfDeaf(true).provider(mockProvider).build();

    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    when(mockMessageChannel.createMessage(failedCreateSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(failedCreateSpec));
    // Necessary for embed create spec
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockVoiceChannel.join(joinSpec)).thenReturn(connection);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockConnection.isConnected()).thenReturn(Mono.just(Boolean.TRUE));
    when(mockConnection.getChannelId()).thenReturn(Mono.just(voiceChannelId));
    when(mockVoiceChannel.getName()).thenReturn(voiceChannelName);

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    StepVerifier.create(voiceChannel).expectNext(mockVoiceChannel).verifyComplete();
    verify(mockManager, times(1)).start(mockClient);
    verify(mockManager, times(2)).isStarted();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testStartingWhenManagerIsStoppedAndStartingErrorsOut() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    MusicAudioProvider mockProvider = Mockito.mock(MusicAudioProvider.class);
    when(mockManager.isStarted()).thenReturn(false, true);
    when(mockManager.getProvider()).thenReturn(mockProvider);
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    EmbedCreateSpec failedCreateSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title(
                "Unable to start the manager, make sure I have permissions "
                    + "in the voice channel that you are in.")
            .build();
    VoiceChannelJoinSpec joinSpec =
        VoiceChannelJoinSpec.builder().selfDeaf(true).provider(mockProvider).build();

    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(failedCreateSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(failedCreateSpec));
    when(mockVoiceChannel.join(joinSpec)).thenThrow(RuntimeException.class);
    // Necessary for embed create spec
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);

    // This wouldn't be called in reality, the onErrorResume will not make it to the next
    // filterWhen, we're just doing this because we can't create the test case properly and don't
    // want the further actions to take place
    when(mockConnection.isConnected()).thenReturn(Mono.just(Boolean.FALSE));

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    StepVerifier.create(voiceChannel).expectNext(mockVoiceChannel).verifyComplete();
    verify(mockManager, times(1)).start(mockClient);
    verify(mockManager, times(1)).stop();
    verify(mockManager, times(2)).isStarted();
    verify(mockMessageChannel, times(1)).createMessage(failedCreateSpec);
  }

  @Test
  public void testStartingWhenManagerIsStarted() {
    Snowflake voiceChannelId = Snowflake.of("111111");
    when(mockManager.isStarted()).thenReturn(true, true);
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    Mono<Channel> voiceChannel = Mono.just(mockVoiceChannel);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockManager, times(0)).start(any());
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
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<Member> member = Mono.just(mockMember);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    initializeGetCurrentVoiceChannel(mockEvent, voiceChannelId);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getChannelById(voiceChannelId)).thenReturn(voiceChannel);
    when(mockVoiceState.getMember()).thenReturn(member);
    when(mockMember.isBot()).thenReturn(true);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);

    StartMusicCommand cmd = new StartMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
    StepVerifier.create(member).expectNext(mockMember).verifyComplete();
    verify(mockManager, times(1)).start(mockClient);
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
