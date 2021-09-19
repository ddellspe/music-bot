package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class VoiceStateTriggerTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private static final Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
  private VoiceStateUpdateEvent mockEvent;
  private VoiceConnectionRegistry mockRegistry;

  @BeforeEach
  public void before() {
    mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
  }

  @Test
  public void testWhenConnectionNullFalse() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.empty());
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenConnectionNotNullChannelIdNotPresentOldIsNotPresent() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.empty());
    when(mockEvent.getOld()).thenReturn(Optional.empty());

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenConnectionNotNullChannelIdPresentChannelIdNotExpectedValueOldIsNotPresent() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.of(Snowflake.of("888888")));
    when(mockEvent.getOld()).thenReturn(Optional.empty());

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenConnectionNotNullChannelIdPresentChannelIdExpectedValueTrue() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertTrue(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenCurrentChannelNullOldPresentButNotChannel() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceState mockOldState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.empty());
    when(mockEvent.getOld()).thenReturn(Optional.of(mockOldState));
    when(mockOldState.getChannelId()).thenReturn(Optional.empty());

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenCurrentChannelNullOldPresentWithUnequalId() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceState mockOldState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.empty());
    when(mockEvent.getOld()).thenReturn(Optional.of(mockOldState));
    when(mockOldState.getChannelId()).thenReturn(Optional.of(Snowflake.of("888888")));

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertFalse(tgr.isCorrectChannel(mockEvent));
  }

  @Test
  public void testWhenCurrentChannelNullOldPresentWithEqualId() {
    VoiceState mockCurrentState = Mockito.mock(VoiceState.class);
    VoiceState mockOldState = Mockito.mock(VoiceState.class);
    VoiceConnection mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockVoiceConnection);

    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockEvent.getCurrent()).thenReturn(mockCurrentState);
    when(mockCurrentState.getGuildId()).thenReturn(GUILD_ID);
    when(mockVoiceConnection.getChannelId()).thenReturn(Mono.just(VOICE_CHANNEL_ID));
    when(mockCurrentState.getChannelId()).thenReturn(Optional.empty());
    when(mockEvent.getOld()).thenReturn(Optional.of(mockOldState));
    when(mockOldState.getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));

    VoiceStateTrigger tgr = new MockVoiceStateTrigger();
    assertTrue(tgr.isCorrectChannel(mockEvent));
  }

  // Just creating the class to be used, none of these methods will be tested or implemented
  class MockVoiceStateTrigger implements VoiceStateTrigger {

    @Override
    public boolean isCorrectEventType(VoiceStateUpdateEvent event) {
      return false;
    }

    @Override
    public Snowflake getFilterChannel(Snowflake guildId, VoiceStateUpdateEvent event) {
      return null;
    }

    @Override
    public Mono<Void> handle(VoiceStateUpdateEvent event) {
      return null;
    }
  }
}
