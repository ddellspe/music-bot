package net.ddellspe.music.bot.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class MusicAudioLoadResultHandlerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private String query;
  private MessageCreateEvent mockEvent;
  private MusicAudioManager mockManager;
  private MusicAudioTrackScheduler mockScheduler;

  @BeforeEach
  public void before() {
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    query = "";
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockManager.getScheduler()).thenReturn(mockScheduler);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetters() {
    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);

    assertEquals(mockEvent, handler.getEvent());
    assertEquals(query, handler.getQuery());
  }

  @Test
  public void testTrackLoaded() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.trackLoaded(mockAudioTrack);

    verify(mockScheduler, times(1)).play(mockAudioTrack);
    verify(mockChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.MEDIUM_SEA_GREEN, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Added Track to queue", embedSpec.asRequest().title().get());
    assertEquals(3, embedSpec.asRequest().fields().get().size());
    assertEquals("Track Title", embedSpec.asRequest().fields().get().get(0).name());
    assertEquals("Title", embedSpec.asRequest().fields().get().get(0).value());
    assertEquals("Track Artist", embedSpec.asRequest().fields().get().get(1).name());
    assertEquals("Author", embedSpec.asRequest().fields().get().get(1).value());
    assertEquals("Duration", embedSpec.asRequest().fields().get().get(2).name());
    assertEquals("30 sec.", embedSpec.asRequest().fields().get().get(2).value());
  }

  @Test
  public void testPlayListLoaded() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack);
    verify(mockChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.MEDIUM_SEA_GREEN, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Added Track to queue", embedSpec.asRequest().title().get());
    assertEquals(3, embedSpec.asRequest().fields().get().size());
    assertEquals("Track Title", embedSpec.asRequest().fields().get().get(0).name());
    assertEquals("Title", embedSpec.asRequest().fields().get().get(0).value());
    assertEquals("Track Artist", embedSpec.asRequest().fields().get().get(1).name());
    assertEquals("Author", embedSpec.asRequest().fields().get().get(1).value());
    assertEquals("Duration", embedSpec.asRequest().fields().get().get(2).name());
    assertEquals("30 sec.", embedSpec.asRequest().fields().get().get(2).value());
  }

  @Test
  public void testNoMatches() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());
    query = "test query";

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.noMatches();

    verify(mockChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.RED, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Could not find track", embedSpec.asRequest().title().get());
    assertEquals(1, embedSpec.asRequest().fields().get().size());
    assertEquals("Query", embedSpec.asRequest().fields().get().get(0).name());
    assertEquals(query, embedSpec.asRequest().fields().get().get(0).value());
    assertEquals(
        "This bot does not support searching for a song on YouTube via keyword, you must "
            + "provide a video id or video link.",
        embedSpec.asRequest().footer().get().text());
  }

  @Test
  public void testLoadFailed() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    FriendlyException exception = new FriendlyException("message", Severity.COMMON, null);
    ArgumentCaptor<Consumer<EmbedCreateSpec>> consumerCaptor =
        ArgumentCaptor.forClass(Consumer.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.loadFailed(exception);

    verify(mockChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.RED, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Error loading the track", embedSpec.asRequest().title().get());
    assertEquals(1, embedSpec.asRequest().fields().get().size());
    assertEquals("Error Message", embedSpec.asRequest().fields().get().get(0).name());
    assertEquals(exception.getMessage(), embedSpec.asRequest().fields().get().get(0).value());
  }
}
