package net.ddellspe.music.bot.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;
import net.ddellspe.music.bot.model.GuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MusicAudioManagerTest {
  private static Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
  private static Snowflake CHAT_CHANNEL_ID = Snowflake.of("666666");
  private AudioPlayerManager mockAudioManager;
  private MusicAudioTrackScheduler mockScheduler;
  private MusicAudioProvider mockProvider;
  private GatewayDiscordClient mockClient;
  private AudioPlayer mockAudioPlayer;

  @BeforeEach
  public void before() {
    mockAudioManager = Mockito.mock(AudioPlayerManager.class);
    mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    mockProvider = Mockito.mock(MusicAudioProvider.class);
    mockClient = Mockito.mock(GatewayDiscordClient.class);
    mockAudioPlayer = Mockito.mock(AudioPlayer.class);
    MusicAudioManager.PLAYER_MANAGER = mockAudioManager;
  }

  @AfterEach
  public void after() {
    MusicAudioManager.clearManagers();
  }

  @Test
  public void testComputeIfAbsentWorks() {
    Snowflake guildId = Snowflake.of("884566541402779718");
    AudioPlayer mockAudioPlayer = Mockito.mock(AudioPlayer.class);
    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);

    verify(mockAudioPlayer, times(1)).addListener(any(MusicAudioTrackScheduler.class));
    assertEquals(mockAudioPlayer, manager.getPlayer());
    assertEquals(mockAudioPlayer, manager.getScheduler().getPlayer());
    assertEquals(mockAudioPlayer, manager.getProvider().getPlayer());
    assertFalse(manager.isStarted());
    assertEquals(GuildConfiguration.class, manager.getConfiguration().getClass());
    assertEquals(">", manager.getPrefix());
    assertEquals(Snowflake.of("884566626165477437"), manager.getChatChannel());
  }

  @Test
  public void testStartWithVoiceChannelAvailableAndNoUsers() {
    String voiceChannelName = "Channel";
    EmbedCreateSpec expectedMessageSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("Issue Joining Channel")
            .addField("Could Not Join Voice Channel", voiceChannelName, true)
            .build();
    Snowflake guildId = Snowflake.of("884566541402779718");
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    MessageChannel mockChatChannel = Mockito.mock(MessageChannel.class);
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);

    when(mockClient.getChannelById(VOICE_CHANNEL_ID)).thenReturn(Mono.just(mockVoiceChannel));
    when(mockClient.getChannelById(CHAT_CHANNEL_ID)).thenReturn(Mono.just(mockChatChannel));
    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockMemberOne.isBot()).thenReturn(true);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceStateOne));
    when(mockVoiceChannel.getName()).thenReturn(voiceChannelName);
    when(mockChatChannel.createMessage(expectedMessageSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedMessageSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);
    ;
    manager.setProvider(mockProvider);

    assertFalse(manager.start(mockClient, VOICE_CHANNEL_ID, CHAT_CHANNEL_ID));
    assertFalse(manager.isStarted());
    verify(mockScheduler, times(1)).setClient(mockClient);
    verify(mockChatChannel, times(1)).createMessage(expectedMessageSpec);
  }

  @Test
  public void testStartWithVoiceChannelAvailableAndUsersAvailable() {
    String voiceChannelName = "Channel";
    ArgumentCaptor<VoiceChannelJoinSpec> joinSpecCaptor =
        ArgumentCaptor.forClass(VoiceChannelJoinSpec.class);
    EmbedCreateSpec expectedMessageSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Music Bot Started")
            .addField("Joined Channel", voiceChannelName, true)
            .build();
    Snowflake guildId = Snowflake.of("884566541402779718");
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    MessageChannel mockChatChannel = Mockito.mock(MessageChannel.class);
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);

    when(mockClient.getChannelById(VOICE_CHANNEL_ID)).thenReturn(Mono.just(mockVoiceChannel));
    when(mockClient.getChannelById(CHAT_CHANNEL_ID)).thenReturn(Mono.just(mockChatChannel));
    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockMemberOne.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceStateOne));
    when(mockVoiceChannel.getName()).thenReturn(voiceChannelName);
    when(mockChatChannel.createMessage(expectedMessageSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedMessageSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockVoiceChannel.join(any(VoiceChannelJoinSpec.class)))
        .thenReturn(Mono.just(Mockito.mock(VoiceConnection.class)));

    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);
    manager.setProvider(mockProvider);

    assertTrue(manager.start(mockClient, VOICE_CHANNEL_ID, CHAT_CHANNEL_ID));
    assertTrue(manager.isStarted());
    verify(mockVoiceChannel).join(joinSpecCaptor.capture());
    verify(mockScheduler, times(1)).setClient(mockClient);
    verify(mockChatChannel, times(1)).createMessage(expectedMessageSpec);
    VoiceChannelJoinSpec joinSpec = joinSpecCaptor.getValue();
    assertTrue(joinSpec.selfDeaf());
    assertEquals(mockProvider, joinSpec.provider());
    assertEquals(Duration.of(2000, ChronoUnit.MILLIS), joinSpec.timeout());
  }

  @Test
  public void testStartWithVoiceChannelAvailableAndUsersAvailableTimesOut() {
    String voiceChannelName = "Channel";
    ArgumentCaptor<VoiceChannelJoinSpec> joinSpecCaptor =
        ArgumentCaptor.forClass(VoiceChannelJoinSpec.class);
    EmbedCreateSpec expectedMessageSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("Issue Joining Channel")
            .addField("Could Not Join Voice Channel", voiceChannelName, true)
            .build();
    Snowflake guildId = Snowflake.of("884566541402779718");
    VoiceChannel mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    MessageChannel mockChatChannel = Mockito.mock(MessageChannel.class);
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);

    when(mockClient.getChannelById(VOICE_CHANNEL_ID)).thenReturn(Mono.just(mockVoiceChannel));
    when(mockClient.getChannelById(CHAT_CHANNEL_ID)).thenReturn(Mono.just(mockChatChannel));
    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockMemberOne.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceStateOne));
    when(mockVoiceChannel.getName()).thenReturn(voiceChannelName);
    when(mockChatChannel.createMessage(expectedMessageSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedMessageSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockVoiceChannel.join(any(VoiceChannelJoinSpec.class)))
        .thenReturn(
            Mono.error(
                new TimeoutException(
                    "Did not observe any item or terminal signal within 1000ms in "
                        + "'flatMap' (and no fallback has been configured)")));

    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);
    manager.setProvider(mockProvider);

    assertFalse(manager.start(mockClient, VOICE_CHANNEL_ID, CHAT_CHANNEL_ID));
    assertFalse(manager.isStarted());
    verify(mockVoiceChannel).join(joinSpecCaptor.capture());
    verify(mockScheduler, times(1)).setClient(mockClient);
    verify(mockChatChannel, times(1)).createMessage(expectedMessageSpec);
    VoiceChannelJoinSpec joinSpec = joinSpecCaptor.getValue();
    assertTrue(joinSpec.selfDeaf());
    assertEquals(mockProvider, joinSpec.provider());
    assertEquals(Duration.of(2000, ChronoUnit.MILLIS), joinSpec.timeout());
  }

  @Test
  public void testStopNotStartedAndConnectionNull() {
    Snowflake guildId = Snowflake.of("884566541402779718");
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(guildId)).thenReturn(Mono.empty());
    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);

    manager.stop(mockClient);

    verify(mockScheduler, times(1)).stop();
    assertFalse(manager.isStarted());
  }

  @Test
  public void testStopWhenStarted() {
    Snowflake guildId = Snowflake.of("884566541402779718");
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(guildId)).thenReturn(Mono.just(mockConnection));
    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);
    when(mockConnection.disconnect()).thenReturn(Mono.empty());

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);

    manager.stop(mockClient);

    verify(mockScheduler, times(1)).stop();
    verify(mockConnection, times(1)).disconnect();
    assertFalse(manager.isStarted());
  }
}
