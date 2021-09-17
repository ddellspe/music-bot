package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class MusicBotCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");

  @Test
  public void testGetCurrentVoiceChannelConnectionNotNull() {
    VoiceStateUpdateEvent mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);
    Snowflake channelId = Snowflake.of("111111");
    Mono<Snowflake> channelIdMono = Mono.just(channelId);

    when(mockEvent.getCurrent()).thenReturn(mockVoiceState);
    when(mockVoiceState.getGuildId()).thenReturn(GUILD_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockConnection.getChannelId()).thenReturn(channelIdMono);

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertEquals(channelId, actualChannelId);
    StepVerifier.create(channelIdMono).expectNext(channelId).verifyComplete();
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
  }

  @Test
  public void testGetCurrentVoiceChannelConnectionNull() {
    VoiceStateUpdateEvent mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);

    when(mockEvent.getCurrent()).thenReturn(mockVoiceState);
    when(mockVoiceState.getGuildId()).thenReturn(GUILD_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.empty());

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertNull(actualChannelId);
  }

  @Test
  public void testGetCurrentVoiceChannelConnectionNotNullNoChannelId() {
    VoiceStateUpdateEvent mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    VoiceConnection mockConnection = Mockito.mock(VoiceConnection.class);
    Mono<VoiceConnection> connection = Mono.just(mockConnection);

    when(mockEvent.getCurrent()).thenReturn(mockVoiceState);
    when(mockVoiceState.getGuildId()).thenReturn(GUILD_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(connection);
    when(mockConnection.getChannelId()).thenReturn(Mono.empty());

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertNull(actualChannelId);
    StepVerifier.create(connection).expectNext(mockConnection).verifyComplete();
  }

  @Test
  public void testGetCurrentVoiceChannelMemberNotPresent() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);

    when(mockEvent.getMember()).thenReturn(Optional.empty());

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertNull(actualChannelId);
  }

  @Test
  public void testGetCurrentVoiceChannelMemberPresentWithEmptyVoiceState() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Member mockMember = Mockito.mock(Member.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(Mono.empty());

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertNull(actualChannelId);
  }

  @Test
  public void testGetCurrentVoiceChannelMemberPresentWithEmptyVoiceStateChannelId() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<VoiceState> voiceState = Mono.just(mockVoiceState);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(voiceState);
    when(mockVoiceState.getChannelId()).thenReturn(Optional.empty());

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertNull(actualChannelId);
    StepVerifier.create(voiceState).expectNext(mockVoiceState).verifyComplete();
  }

  @Test
  public void testGetCurrentVoiceChannelMemberPresentWithValidVoiceState() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockVoiceState = Mockito.mock(VoiceState.class);
    Mono<VoiceState> voiceState = Mono.just(mockVoiceState);
    Snowflake channelId = Snowflake.of("111111");

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(voiceState);
    when(mockVoiceState.getChannelId()).thenReturn(Optional.of(channelId));

    MusicBotCommand cmd = new MusicBotCommand() {};
    Snowflake actualChannelId = cmd.getCurrentVoiceChannel(mockEvent);

    assertEquals(channelId, actualChannelId);
    StepVerifier.create(voiceState).expectNext(mockVoiceState).verifyComplete();
  }
}
