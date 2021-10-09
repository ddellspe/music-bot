package net.ddellspe.music.bot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class VoiceUtilsTest {
  private static Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
  private GatewayDiscordClient mockClient;
  private VoiceChannel mockVoiceChannel;

  @BeforeEach
  public void setup() {
    mockClient = Mockito.mock(GatewayDiscordClient.class);
    mockVoiceChannel = Mockito.mock(VoiceChannel.class);
    when(mockClient.getChannelById(VOICE_CHANNEL_ID)).thenReturn(Mono.just(mockVoiceChannel));
    // this is to appease jacoco for lines of coverage
    new VoiceUtils();
  }

  @Test
  public void testCountEqualToOne() {
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    VoiceState mockVoiceStateTwo = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);
    Member mockMemberTwo = Mockito.mock(Member.class);

    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockVoiceStateTwo.getMember()).thenReturn(Mono.just(mockMemberTwo));
    when(mockMemberOne.isBot()).thenReturn(false);
    when(mockMemberTwo.isBot()).thenReturn(true);
    when(mockVoiceChannel.getVoiceStates())
        .thenReturn(Flux.just(mockVoiceStateOne, mockVoiceStateTwo));

    assertEquals(1L, VoiceUtils.getVoiceChannelPersonCount(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.TRUE, VoiceUtils.botChannelHasPeople(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.FALSE, VoiceUtils.botChannelHasNoPeople(mockClient, VOICE_CHANNEL_ID).block());
  }

  @Test
  public void testCountEqualToTwo() {
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    VoiceState mockVoiceStateTwo = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);
    Member mockMemberTwo = Mockito.mock(Member.class);

    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockVoiceStateTwo.getMember()).thenReturn(Mono.just(mockMemberTwo));
    when(mockMemberOne.isBot()).thenReturn(false);
    when(mockMemberTwo.isBot()).thenReturn(false);
    when(mockVoiceChannel.getVoiceStates())
        .thenReturn(Flux.just(mockVoiceStateOne, mockVoiceStateTwo));

    assertEquals(2L, VoiceUtils.getVoiceChannelPersonCount(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.TRUE, VoiceUtils.botChannelHasPeople(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.FALSE, VoiceUtils.botChannelHasNoPeople(mockClient, VOICE_CHANNEL_ID).block());
  }

  @Test
  public void testCountEqualToZero() {
    VoiceState mockVoiceStateOne = Mockito.mock(VoiceState.class);
    VoiceState mockVoiceStateTwo = Mockito.mock(VoiceState.class);
    Member mockMemberOne = Mockito.mock(Member.class);
    Member mockMemberTwo = Mockito.mock(Member.class);

    when(mockVoiceStateOne.getMember()).thenReturn(Mono.just(mockMemberOne));
    when(mockVoiceStateTwo.getMember()).thenReturn(Mono.just(mockMemberTwo));
    when(mockMemberOne.isBot()).thenReturn(true);
    when(mockMemberTwo.isBot()).thenReturn(true);
    when(mockVoiceChannel.getVoiceStates())
        .thenReturn(Flux.just(mockVoiceStateOne, mockVoiceStateTwo));

    assertEquals(0L, VoiceUtils.getVoiceChannelPersonCount(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.FALSE, VoiceUtils.botChannelHasPeople(mockClient, VOICE_CHANNEL_ID).block());
    assertEquals(
        Boolean.TRUE, VoiceUtils.botChannelHasNoPeople(mockClient, VOICE_CHANNEL_ID).block());
  }
}
