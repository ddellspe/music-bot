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
import discord4j.rest.util.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  public void testPlayWithNoExistingQueueAndNoClient() {
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    when(mockPlayer.startTrack(mockTrack, true)).thenReturn(true);

    assertTrue(scheduler.play(mockTrack));
    verify(mockPlayer, times(1)).startTrack(mockTrack, true);
    assertEquals(0, scheduler.getQueue().size());
  }

  @Test
  public void testPlayWithNoExistingQueueAndClientPresent() {
    Snowflake chatChannel = Snowflake.of("111111");
    AudioTrack mockTrack = Mockito.mock(AudioTrack.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockPlayer.startTrack(mockTrack, true)).thenReturn(true);
    when(mockManager.getChatChannel()).thenReturn(chatChannel);
    when(mockClient.getChannelById(chatChannel)).thenReturn(Mono.just(mockChannel));
    when(mockTrack.getInfo()).thenReturn(mockAudioTrackInfo);
    when(mockChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());

    scheduler.setClient(mockClient);
    assertTrue(scheduler.play(mockTrack));

    verify(mockPlayer, times(1)).startTrack(mockTrack, true);
    assertEquals(0, scheduler.getQueue().size());
    verify(mockChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.MEDIUM_SEA_GREEN, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Now Playing", embedSpec.asRequest().title().get());
    assertEquals(3, embedSpec.asRequest().fields().get().size());
    assertEquals("Track Title", embedSpec.asRequest().fields().get().get(0).name());
    assertEquals("Title", embedSpec.asRequest().fields().get().get(0).value());
    assertEquals("Track Artist", embedSpec.asRequest().fields().get().get(1).name());
    assertEquals("Author", embedSpec.asRequest().fields().get().get(1).value());
    assertEquals("Duration", embedSpec.asRequest().fields().get().get(2).name());
    assertEquals("30 sec.", embedSpec.asRequest().fields().get().get(2).value());
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
