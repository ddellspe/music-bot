package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class PlayCommandTest {
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
    PlayCommand cmd = new PlayCommand();
    assertEquals("play", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    PlayCommand cmd = new PlayCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testInvalidQuerySendsMessageOfException() {
    EmbedCreateSpec expectedSpec =
        EmbedCreateSpec.builder().color(Color.RED).title("Invalid command: '!play'").build();

    when(mockMessage.getContent()).thenReturn("!play");
    when(mockChatChannel.createMessage(expectedSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockChatChannel, times(1)).createMessage(expectedSpec);
  }

  @Test
  public void testNotInVoiceChannel() {
    EmbedCreateSpec expectedSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("You must be in a voice channel to use the music bot.")
            .build();

    // Quick null the voice channel output
    when(mockMessage.getContent()).thenReturn("!play song");
    when(mockEvent.getMember()).thenReturn(Optional.empty());
    when(mockChatChannel.createMessage(expectedSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockChatChannel, times(1)).createMessage(expectedSpec);
  }

  @Test
  public void testInVoiceChannelNotStartedAndDoesNotStart() {
    EmbedCreateSpec expectedSpec =
        EmbedCreateSpec.builder()
            .color(Color.ORANGE)
            .title("Bot could not be started, check permissions of bot and voice channels.")
            .build();
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockState = Mockito.mock(VoiceState.class);

    // Quick null the voice channel output
    when(mockMessage.getContent()).thenReturn("!play song");
    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(Mono.just(mockState));
    when(mockState.getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));
    when(mockChatChannel.createMessage(expectedSpec))
        .thenReturn(MessageCreateMono.of(mockChatChannel).withEmbeds(expectedSpec));
    // Required for internal createMessage operation
    when(mockChatChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockManager.isStarted()).thenReturn(false, false);
    when(mockManager.start(mockClient, VOICE_CHANNEL_ID, CHAT_CHANNEL_ID)).thenReturn(false);

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(mockChatChannel, times(1)).createMessage(expectedSpec);
    verify(mockManager, times(1)).start(mockClient, VOICE_CHANNEL_ID, CHAT_CHANNEL_ID);
  }

  @Test
  public void testInVoiceChannelStartedAndQueues() {
    Member mockMember = Mockito.mock(Member.class);
    VoiceState mockState = Mockito.mock(VoiceState.class);
    MusicAudioManager.PLAYER_MANAGER = Mockito.mock(AudioPlayerManager.class);

    // Quick null the voice channel output
    when(mockMessage.getContent()).thenReturn("!play song");
    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getVoiceState()).thenReturn(Mono.just(mockState));
    when(mockState.getChannelId()).thenReturn(Optional.of(VOICE_CHANNEL_ID));
    when(mockManager.isStarted()).thenReturn(true, true);

    PlayCommand cmd = new PlayCommand();
    cmd.handle(mockEvent).block();
    verify(MusicAudioManager.PLAYER_MANAGER, times(1))
        .loadItemOrdered(eq(mockManager), eq("song"), any(MusicAudioLoadResultHandler.class));
  }
}
