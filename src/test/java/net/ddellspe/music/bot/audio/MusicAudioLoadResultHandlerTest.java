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
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class MusicAudioLoadResultHandlerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private String query;
  private MessageCreateEvent mockEvent;
  private MusicAudioTrackScheduler mockScheduler;

  @BeforeEach
  public void before() {
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    query = "";
    MusicAudioManager mockManager = Mockito.mock(MusicAudioManager.class);
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
  public void testTrackLoadedPlaying() {
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(true);

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.trackLoaded(mockAudioTrack);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
  }

  @Test
  public void testTrackLoadedNotPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .build();

    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(false);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.trackLoaded(mockAudioTrack);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testPlayListLoadedPlaying() {
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);

    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockScheduler.play(mockAudioTrack)).thenReturn(true);

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack);
  }

  @Test
  public void testPlayListLoadedPlayingForcePlayNoRequeue() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockAudioTrack2 = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .build();

    when(mockScheduler.play(mockAudioTrack, true, false)).thenReturn(true);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.addToQueueAtPosition(mockAudioTrack2, 0)).thenReturn(false);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockAudioTrack2.getInfo()).thenReturn(mockAudioTrackInfo);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack, mockAudioTrack2));

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query, true);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, true, false);
    verify(mockScheduler, times(1)).addToQueueAtPosition(mockAudioTrack2, 0);
  }

  @Test
  public void testPlayListLoadedNotPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack)).thenReturn(false);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack);
  }

  @Test
  public void testNoMatches() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    query = "test query";
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("Could not find track")
            .addField("Query", query, false)
            .footer(
                "This bot does not support searching for a song on YouTube via keyword, "
                    + "you must provide a video id or video link.",
                null)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.noMatches();

    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testLoadFailed() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    FriendlyException exception = new FriendlyException("message", Severity.COMMON, null);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.RED)
            .title("Error loading the track")
            .addField("Error Message", "message", false)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler = new MusicAudioLoadResultHandler(mockEvent, query);
    handler.loadFailed(exception);

    verify(mockChannel, times(1)).createMessage(embedSpec);
  }
}
