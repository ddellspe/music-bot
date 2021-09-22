package net.ddellspe.music.bot.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MusicAudioTrackSchedulerTest {
  private List<AudioTrack> queue;
  private AudioPlayer mockPlayer;
  private MusicAudioTrackScheduler scheduler;

  @BeforeEach
  public void before() {
    queue = Collections.synchronizedList(new LinkedList<>());
    mockPlayer = Mockito.mock(AudioPlayer.class);
    scheduler = new MusicAudioTrackScheduler(mockPlayer, queue);
  }

  @Test
  public void testNoQueueConstructor() {
    MusicAudioTrackScheduler otherScheduler = new MusicAudioTrackScheduler(mockPlayer);

    assertEquals(0, otherScheduler.getQueue().size());
    assertEquals(mockPlayer, otherScheduler.getPlayer());
  }

  @Test
  public void testWhenQueueEmptyNoPlayAndReturnFalse() {
    assertFalse(scheduler.skip());
  }

  @Test
  public void testWhenQueueHasTrackReturnsTrue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    when(mockPlayer.startTrack(mockTrack, false)).thenReturn(true);

    assertTrue(scheduler.skip());
    verify(mockPlayer, times(1)).startTrack(mockTrack, false);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testWhenQueueHasTrackReturnsTrueStartTrackFails() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    when(mockPlayer.startTrack(mockTrack, false)).thenReturn(false);

    assertFalse(scheduler.skip());
    verify(mockPlayer, times(1)).startTrack(mockTrack, false);
    assertEquals(1, scheduler.getQueue().size());
  }

  @Test
  public void testPlayWithNoForce() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockTrack2 = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    when(mockPlayer.startTrack(mockTrack2, true)).thenReturn(false);

    assertFalse(scheduler.play(mockTrack2));
    verify(mockPlayer, times(1)).startTrack(mockTrack2, true);
    assertEquals(2, scheduler.getQueue().size());
  }

  @Test
  public void testStopClearsQueueWhenFull() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);

    scheduler.stop();
    verify(mockPlayer, times(1)).stopTrack();
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testStopClearsQueueWhenEmpty() {
    scheduler.stop();

    verify(mockPlayer, times(1)).stopTrack();
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testEndReasonMayStartNext() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    when(mockPlayer.startTrack(mockTrack, false)).thenReturn(true);

    scheduler.onTrackEnd(mockPlayer, mockTrack, AudioTrackEndReason.FINISHED);

    verify(mockPlayer, times(1)).startTrack(mockTrack, false);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testEndReasonMayNotStartNext() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    when(mockPlayer.startTrack(mockTrack, false)).thenReturn(true);

    scheduler.onTrackEnd(mockPlayer, mockTrack, AudioTrackEndReason.STOPPED);

    verify(mockPlayer, times(0)).startTrack(mockTrack, false);
    assertEquals(1, scheduler.getQueue().size());
  }
}
