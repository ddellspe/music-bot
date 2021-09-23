package net.ddellspe.music.bot.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MusicAudioProviderTest {
  AudioPlayer mockPlayer;

  @BeforeEach
  public void before() {
    mockPlayer = Mockito.mock(AudioPlayer.class);
  }

  @Test
  public void defaultsTest() {
    MusicAudioProvider provider = new MusicAudioProvider(mockPlayer);

    assertEquals(mockPlayer, provider.getPlayer());
    assertEquals(MutableAudioFrame.class, provider.getFrame().getClass());
  }

  @Test
  public void testProviderDidProvide() {
    when(mockPlayer.provide(any())).thenReturn(true);

    MusicAudioProvider provider = new MusicAudioProvider(mockPlayer);

    assertTrue(provider.provide());
  }

  @Test
  public void testProviderDidNotProvide() {
    when(mockPlayer.provide(any())).thenReturn(false);

    MusicAudioProvider provider = new MusicAudioProvider(mockPlayer);

    assertFalse(provider.provide());
  }
}
