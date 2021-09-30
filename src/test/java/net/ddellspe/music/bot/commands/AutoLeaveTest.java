package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AutoLeaveTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private static final Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
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
  public void testIsCorrectEventTypeOnJoinEvent() {
    when(mockEvent.isMoveEvent()).thenReturn(false);
    when(mockEvent.isLeaveEvent()).thenReturn(false);
    when(mockEvent.isJoinEvent()).thenReturn(true);

    AutoLeave tgr = new AutoLeave();
    assertFalse(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testIsCorrectEventTypeOnMoveEvent() {
    when(mockEvent.isMoveEvent()).thenReturn(true);
    when(mockEvent.isLeaveEvent()).thenReturn(false);
    when(mockEvent.isJoinEvent()).thenReturn(false);

    AutoLeave tgr = new AutoLeave();
    assertTrue(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testIsCorrectEventTypeOnLeaveEvent() {
    when(mockEvent.isMoveEvent()).thenReturn(false);
    when(mockEvent.isLeaveEvent()).thenReturn(true);
    when(mockEvent.isJoinEvent()).thenReturn(false);

    AutoLeave tgr = new AutoLeave();
    assertTrue(tgr.isCorrectEventType(mockEvent));
  }

  @Test
  public void testGetFilterChannelGoesThroughGetCurrentVoiceChannel() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));

    AutoLeave tgr = new AutoLeave();
    assertEquals(VOICE_CHANNEL_ID, tgr.getFilterChannel(mockEvent));
  }

  @Test
  public void testWhenChannelHasNoNonBotUsersManagerIsStopped() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Member mockMember = Mockito.mock(Member.class);

    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));
    when(mockVoiceState.getMember()).thenReturn(Mono.just(mockMember));
    when(mockMember.isBot()).thenReturn(true);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockVoiceConnection.disconnect()).thenReturn(Mono.empty());

    AutoLeave tgr = new AutoLeave();
    tgr.handle(mockEvent).block();

    verify(mockManager, times(1)).stop(mockClient);
    verify(mockVoiceConnection, times(1)).disconnect();
  }

  @Test
  public void testWhenChannelHasANonBotUsersManagerIsStopped() {
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Member mockMember = Mockito.mock(Member.class);

    when(mockVoiceChannel.getVoiceStates()).thenReturn(Flux.just(mockVoiceState));
    when(mockVoiceState.getMember()).thenReturn(Mono.just(mockMember));
    when(mockMember.isBot()).thenReturn(false);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockVoiceConnection.disconnect()).thenReturn(Mono.empty());

    AutoLeave tgr = new AutoLeave();
    tgr.handle(mockEvent).block();

    verify(mockManager, times(0)).stop(mockClient);
    verify(mockVoiceConnection, times(0)).disconnect();
  }
}
