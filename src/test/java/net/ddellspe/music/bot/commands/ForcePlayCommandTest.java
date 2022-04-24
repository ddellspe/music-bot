package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class ForcePlayCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private static final Snowflake CHAT_CHANNEL_ID = Snowflake.of("666666");
  private static final Snowflake VOICE_CHANNEL_ID = Snowflake.of("111111");
  private MusicAudioManager mockManager;
  private MessageCreateEvent mockEvent;
  private Message mockMessage;
  private MessageChannel mockChatChannel;
  private GatewayDiscordClient mockClient;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockMessage = Mockito.mock(Message.class);
    mockChatChannel = Mockito.mock(MessageChannel.class);
    mockClient = Mockito.mock(GatewayDiscordClient.class);

    MusicAudioManager.set(GUILD_ID, mockManager);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChatChannel));
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
  }

  @Test
  public void testGetName() {
    ForcePlayCommand cmd = new ForcePlayCommand();
    assertEquals("fplay", cmd.getName());
  }

  @Test
  public void testInVoiceChannelStartedAndQueues() {
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockState = Mockito.mock(VoiceState.class);
    MusicAudioManager.PLAYER_MANAGER = Mockito.mock(AudioPlayerManager.class);
    ArgumentCaptor<MusicAudioLoadResultHandler> musicAudioLoadResultHandlerArgumentCaptor =
        ArgumentCaptor.forClass(MusicAudioLoadResultHandler.class);

    // Quick null the voice channel output
    when(mockMessage.getContent()).thenReturn("!fplay song");
    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(Mono.just(mockState));
    when(mockState.getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));
    when(mockManager.isStarted()).thenReturn(true, true);

    ForcePlayCommand cmd = new ForcePlayCommand();
    cmd.handle(mockEvent).block();
    verify(MusicAudioManager.PLAYER_MANAGER, times(1))
        .loadItemOrdered(
            eq(mockManager), eq("song"), musicAudioLoadResultHandlerArgumentCaptor.capture());
    MusicAudioLoadResultHandler handler = musicAudioLoadResultHandlerArgumentCaptor.getValue();
    assertTrue(handler.isForcePlay());
    assertFalse(handler.shouldRequeueCurrent());
  }
}
