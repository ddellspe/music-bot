package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class SelfBootTest {

  private final Snowflake SELF_ID = Snowflake.of("222222");
  private final Snowflake EVENT_ID = Snowflake.of("333333");
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private static final Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
  private static final Snowflake CHAT_CHANNEL_ID = Snowflake.of("111112");
  private VoiceStateUpdateEvent mockEvent;
  private VoiceConnectionRegistry mockRegistry;
  private MusicAudioManager mockManager;
  private VoiceChannel mockVoiceChannel;
  private GatewayDiscordClient mockClient;

  @BeforeEach
  public void before() {
    mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    mockClient = Mockito.mock(GatewayDiscordClient.class);
    mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockVoiceChannel = Mockito.mock(VoiceChannel.class);

    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getCurrent()).thenReturn(mockVoiceState);
    when(mockVoiceState.getGuildId()).thenReturn(GUILD_ID);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockClient.getChannelById(VOICE_CHANNEL_ID)).thenReturn(Mono.just(mockVoiceChannel));
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testIsCorrectEventTypeNotSelf() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(EVENT_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);

    SelfBoot tgr = new SelfBoot();
    assertFalse(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testIsCorrectEventTypeSelfNotLeaveEvent() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(SELF_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);
    when(mockEvent.isLeaveEvent()).thenReturn(false);

    SelfBoot tgr = new SelfBoot();
    assertFalse(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testIsCorrectEventTypeSelfLeaveEvent() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(SELF_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);
    when(mockEvent.isLeaveEvent()).thenReturn(true);

    SelfBoot tgr = new SelfBoot();
    assertTrue(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testIsCorrectChannelVoiceConnectionIsNull() {
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.empty());

    SelfBoot tgr = new SelfBoot();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testIsCorrectChannelVoiceConnectionNotNullChannelIdNotNullIsNotCurrentChannel() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockEvent.getCurrent().getChannelId()).thenReturn(Optional.of(CHAT_CHANNEL_ID));

    SelfBoot tgr = new SelfBoot();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testIsCorrectChannelVoiceConnectionNotNullChannelIdNotNullIsCurrentChannel() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockEvent.getCurrent().getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));

    SelfBoot tgr = new SelfBoot();
    assertTrue(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testIsCorrectChannelVoiceConnectionNotNullChannelIdNull() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.empty());

    SelfBoot tgr = new SelfBoot();
    assertTrue(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testGetFilterChannelGoesThroughGetCurrentVoiceChannel() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));

    SelfBoot tgr = new SelfBoot();
    assertEquals(VOICE_CHANNEL_ID, tgr.getFilterChannel(mockEvent));
  }

  @Test
  public void testHandleManagerNotStarted() {
    when(mockManager.isStarted()).thenReturn(false);

    SelfBoot tgr = new SelfBoot();
    tgr.handle(mockEvent).block();
    verify(mockEvent, times(0)).getClient();
  }

  @Test
  public void testHandleManagerStarted() {
    EmbedCreateSpec expectedSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("Bot has been disconnected from the voice channel.")
            .build();

    MessageChannel mockChatChannel = Mockito.mock(MessageChannel.class);
    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getChatChannel()).thenReturn(CHAT_CHANNEL_ID);
    when(mockClient.getChannelById(CHAT_CHANNEL_ID)).thenReturn(Mono.just(mockChatChannel));
    when(mockChatChannel.createMessage(expectedSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    SelfBoot tgr = new SelfBoot();
    tgr.handle(mockEvent).block();
    verify(mockChatChannel, times(1)).createMessage(expectedSpec);
  }
}
