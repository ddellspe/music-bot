package net.ddellspe.music.bot.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class MusicAudioTrackSchedulerTest {
  private List<AudioTrack> queue;
  private AudioPlayer mockPlayer;
  private MusicAudioTrackScheduler scheduler;
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    queue = Collections.synchronizedList(new LinkedList<>());
    mockPlayer = Mockito.mock(AudioPlayer.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    scheduler = new MusicAudioTrackScheduler(mockPlayer, mockManager, queue);
  }

  @Test
  public void testNoQueueConstructor() {
    MusicAudioTrackScheduler otherScheduler = new MusicAudioTrackScheduler(mockPlayer, mockManager);

    assertEquals(0, otherScheduler.getQueue().size());
    assertEquals(mockPlayer, otherScheduler.getPlayer());
    assertEquals(mockManager, otherScheduler.getManager());
    assertNull(otherScheduler.getClient());
  }

  @Test
  public void testWhenQueueEmptyNoPlayAndReturnFalse() {
    assertFalse(scheduler.skip());
  }

  @Test
  public void testWhenQueueHasTrackReturnsTrue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    // immediately play the song
    when(mockPlayer.startTrack(mockTrack, false)).thenReturn(true);

    assertTrue(scheduler.skip());
    verify(mockPlayer, times(1)).startTrack(mockTrack, false);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testWhenQueueHasTrackReturnsTrueStartTrackFails() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockTrack);
    // doesn't immediately play the song
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
    // doesn't immediately play the song
    when(mockPlayer.startTrack(mockTrack2, true)).thenReturn(false);

    assertFalse(scheduler.play(mockTrack2));
    verify(mockPlayer, times(1)).startTrack(mockTrack2, true);
    assertEquals(2, scheduler.getQueue().size());
    assertEquals(mockTrack, scheduler.getQueue().get(0));
    assertEquals(mockTrack2, scheduler.getQueue().get(1));
  }

  @Test
  public void testPlayWithNoExistingQueueOrCurrentPlaying() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    // immediately play the song
    when(mockPlayer.startTrack(mockTrack, true)).thenReturn(true);

    assertTrue(scheduler.play(mockTrack));
    verify(mockPlayer, times(1)).startTrack(mockTrack, true);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackNoForce() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // doesn't immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, true)).thenReturn(false);

    assertFalse(scheduler.play(mockNewTrack, false));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, true);
    assertEquals(1, scheduler.getQueue().size());
    assertEquals(mockNewTrack, scheduler.getQueue().get(0));
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackNoForceViaStandardPlay() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // doesn't immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, true)).thenReturn(false);

    assertFalse(scheduler.play(mockNewTrack));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, true);
    assertEquals(1, scheduler.getQueue().size());
    assertEquals(mockNewTrack, scheduler.getQueue().get(0));
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackForcePlay() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, false)).thenReturn(true);

    assertTrue(scheduler.play(mockNewTrack, true));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, false);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackForcePlayNoRequeue() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, false)).thenReturn(true);

    assertTrue(scheduler.play(mockNewTrack, true, false));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, false);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackForcePlayRequeue() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockCurrentTrackClone = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, false)).thenReturn(true);
    when(mockCurrentTrack.makeClone()).thenReturn(mockCurrentTrackClone);
    when(mockCurrentTrackClone.isSeekable()).thenReturn(true);
    when(mockCurrentTrack.getPosition()).thenReturn(1000L);

    assertTrue(scheduler.play(mockNewTrack, true, true));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, false);
    verify(mockCurrentTrackClone).setPosition(1000L);
    assertEquals(1, scheduler.getQueue().size());
    assertEquals(mockCurrentTrackClone, scheduler.getQueue().get(0));
  }

  @Test
  public void testPlayWithNoExistingQueueButCurrentTrackForcePlayRequeueTrackNotSeekable() {
    AudioTrack mockCurrentTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockCurrentTrackClone = Mockito.mock(AudioTrack.class);
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // setting currentTrack and currentPlaying = true
    scheduler.onTrackStart(mockPlayer, mockCurrentTrack);
    // immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, false)).thenReturn(true);
    when(mockCurrentTrack.makeClone()).thenReturn(mockCurrentTrackClone);
    when(mockCurrentTrackClone.isSeekable()).thenReturn(false);

    assertTrue(scheduler.play(mockNewTrack, true, true));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, false);
    assertEquals(1, scheduler.getQueue().size());
    assertEquals(mockCurrentTrackClone, scheduler.getQueue().get(0));
  }

  @Test
  public void testPlayWithNoCurrentTrackForcePlayRequeue() {
    AudioTrack mockNewTrack = Mockito.mock(AudioTrack.class);
    // immediately play the song
    when(mockPlayer.startTrack(mockNewTrack, false)).thenReturn(true);

    assertTrue(scheduler.play(mockNewTrack, true, true));
    verify(mockPlayer, times(1)).startTrack(mockNewTrack, false);
    assertEquals(0, scheduler.getQueue().size());
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

  @Test
  public void testCurrentlyPlayingStateWithPauseResumeWithNoTrack() {
    assertFalse(scheduler.isCurrentlyPlaying());
    scheduler.onPlayerResume(mockPlayer);
    assertFalse(scheduler.isCurrentlyPlaying());
    scheduler.onPlayerPause(mockPlayer);
    assertFalse(scheduler.isCurrentlyPlaying());
  }

  @Test
  public void testCurrentlyPlayingStateWithPauseResumeWithTrack() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    scheduler.onTrackStart(mockPlayer, mockTrack);
    assertTrue(scheduler.isCurrentlyPlaying());
    scheduler.onPlayerPause(mockPlayer);
    assertFalse(scheduler.isCurrentlyPlaying());
    scheduler.onPlayerResume(mockPlayer);
    assertTrue(scheduler.isCurrentlyPlaying());
  }

  @Test
  public void testOnTrackStartWithTrack() {
    Snowflake chatChannel = Snowflake.of("111111");
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Now Playing")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .build();
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockManager.getChatChannel()).thenReturn(chatChannel);
    when(mockClient.getChannelById(chatChannel)).thenReturn(Mono.just(mockChannel));
    when(mockTrack.getInfo()).thenReturn(mockAudioTrackInfo);
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    scheduler.setClient(mockClient);
    scheduler.onTrackStart(mockPlayer, mockTrack);
    assertTrue(scheduler.isCurrentlyPlaying());
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testAddQueueAtPositionZeroNoQueue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);

    scheduler.addToQueueAtPosition(mockTrack, 0);
    assertEquals(1, scheduler.getQueue().size());
    assertEquals(mockTrack, scheduler.getQueue().get(0));
  }

  @Test
  public void testAddQueueAtPositionZeroExistingQueue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockExistingTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockExistingTrack);

    scheduler.addToQueueAtPosition(mockTrack, 0);
    assertEquals(2, scheduler.getQueue().size());
    assertEquals(mockTrack, scheduler.getQueue().get(0));
    assertEquals(mockExistingTrack, scheduler.getQueue().get(1));
  }

  @Test
  public void testAddQueueAtPositionOneExistingQueue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockExistingTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockExistingTrack);

    scheduler.addToQueueAtPosition(mockTrack, 1);
    assertEquals(2, scheduler.getQueue().size());
    assertEquals(mockExistingTrack, scheduler.getQueue().get(0));
    assertEquals(mockTrack, scheduler.getQueue().get(1));
  }

  @Test
  public void testAddQueueAtPositionTwoOfExistingQueue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockExistingTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockExistingTrack);
    queue.add(mockExistingTrack);
    queue.add(mockExistingTrack);

    scheduler.addToQueueAtPosition(mockTrack, 2);
    assertEquals(4, scheduler.getQueue().size());
    assertEquals(mockExistingTrack, scheduler.getQueue().get(0));
    assertEquals(mockExistingTrack, scheduler.getQueue().get(1));
    assertEquals(mockTrack, scheduler.getQueue().get(2));
    assertEquals(mockExistingTrack, scheduler.getQueue().get(3));
  }

  @Test
  public void testAddQueueAtPositionBeyondSizeOfExistingQueue() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockExistingTrack = Mockito.mock(AudioTrack.class);
    queue.add(mockExistingTrack);
    queue.add(mockExistingTrack);
    queue.add(mockExistingTrack);

    scheduler.addToQueueAtPosition(mockTrack, 62);
    assertEquals(4, scheduler.getQueue().size());
    assertEquals(mockExistingTrack, scheduler.getQueue().get(0));
    assertEquals(mockExistingTrack, scheduler.getQueue().get(1));
    assertEquals(mockExistingTrack, scheduler.getQueue().get(2));
    assertEquals(mockTrack, scheduler.getQueue().get(3));
  }
}
