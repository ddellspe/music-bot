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
import net.ddellspe.music.bot.model.GuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MusicAudioManagerTest {
  AudioPlayerManager mockAudioManager;

  @BeforeEach
  public void before() {
    mockAudioManager = Mockito.mock(AudioPlayerManager.class);
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
  public void testStart() {
    Snowflake guildId = Snowflake.of("884566541402779718");
    AudioPlayer mockAudioPlayer = Mockito.mock(AudioPlayer.class);
    MusicAudioTrackScheduler mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);
    manager.start(mockClient);

    assertTrue(manager.isStarted());
  }

  @Test
  public void testStop() {
    Snowflake guildId = Snowflake.of("884566541402779718");
    AudioPlayer mockAudioPlayer = Mockito.mock(AudioPlayer.class);
    MusicAudioTrackScheduler mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);
    when(mockAudioManager.createPlayer()).thenReturn(mockAudioPlayer);

    MusicAudioManager manager = MusicAudioManager.of(guildId);
    manager.setScheduler(mockScheduler);
    manager.start(mockClient);
    manager.stop();

    assertFalse(manager.isStarted());
    verify(mockScheduler, times(1)).setClient(mockClient);
    verify(mockScheduler, times(1)).stop();
  }
}
