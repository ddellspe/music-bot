package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PlayCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
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
}
