package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnectionRegistry;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class EndMusicCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;
  private MessageCreateEvent mockEvent;
  private VoiceConnection mockVoiceConnection;
  private GatewayDiscordClient mockClient;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockClient = Mockito.mock(GatewayDiscordClient.class);
    VoiceConnectionRegistry mockRegistry = Mockito.mock(VoiceConnectionRegistry.class);
    mockVoiceConnection = Mockito.mock(VoiceConnection.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getVoiceConnectionRegistry()).thenReturn(mockRegistry);
    when(mockRegistry.getVoiceConnection(GUILD_ID)).thenReturn(Mono.just(mockVoiceConnection));
  }

  @Test
  public void testGetName() {
    EndMusicCommand cmd = new EndMusicCommand();
    assertEquals("end", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    EndMusicCommand cmd = new EndMusicCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testStoppingWhenManagerIsStarted() {
    when(mockManager.isStarted()).thenReturn(true);
    when(mockVoiceConnection.disconnect()).thenReturn(Mono.empty());

    EndMusicCommand cmd = new EndMusicCommand();
    cmd.handle(mockEvent).block();

    verify(mockManager, times(1)).stop(mockClient);
    verify(mockManager, times(1)).isStarted();
  }

  @Test
  public void testStoppingWhenManagerIsNotStarted() {
    when(mockManager.isStarted()).thenReturn(false);

    EndMusicCommand cmd = new EndMusicCommand();
    cmd.handle(mockEvent).block();

    verify(mockManager, times(1)).isStarted();
    verify(mockManager, times(0)).stop(mockClient);
  }
}
