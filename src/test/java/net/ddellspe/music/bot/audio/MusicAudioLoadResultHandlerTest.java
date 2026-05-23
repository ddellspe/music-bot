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
    when(mockManager.getPrefix()).thenReturn("!");
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
            .addField("Queue Position", "1", false)
            .build();

    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(false);
    when(mockScheduler.getQueue()).thenReturn(List.of(mockAudioTrack));
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

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, true);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack);
  }

  @Test
  public void testPlayListLoadedPlayingForcePlayNoRequeue() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockAudioTrack2 = Mockito.mock(AudioTrack.class);
    AudioTrackInfo info1 =
        new AudioTrackInfo("Title 1", "Author 1", 30000L, "identifier", true, "test");
    AudioTrackInfo info2 =
        new AudioTrackInfo("Title 2", "Author 2", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(info1);
    when(mockAudioTrack2.getInfo()).thenReturn(info2);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getName()).thenReturn("PlaylistName");
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack, mockAudioTrack2));

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added playlist to queue")
            .description("**Playlist:** [PlaylistName]()")
            .addField(
                "1. Title 1", "Artist: Author 1 | Duration: 30 sec. | [Video Link](test)", false)
            .addField(
                "2. Title 2", "Artist: Author 2 | Duration: 30 sec. | [Video Link](test)", false)
            .footer("(total of 2)", null)
            .build();

    when(mockScheduler.play(mockAudioTrack, true, false)).thenReturn(true);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.addToQueueAtPosition(mockAudioTrack2, 0)).thenReturn(false);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, true, false, true);
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
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getName()).thenReturn("PlaylistName");
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added playlist to queue")
            .description("**Playlist:** [PlaylistName]()")
            .addField("Queue Position", "1", false)
            .addField("1. Title", "Artist: Author | Duration: 30 sec. | [Video Link](test)", false)
            .footer("(total of 1)", null)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack)).thenReturn(false);
    when(mockScheduler.getQueue()).thenReturn(List.of(mockAudioTrack));
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, true);
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

  @Test
  public void testPlayListLoadedMoreThanFiveTracks() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    List<AudioTrack> tracks = new java.util.ArrayList<>();
    for (int i = 0; i < 7; i++) {
      AudioTrack track = Mockito.mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 30000L, "identifier", true, "test");
      when(track.getInfo()).thenReturn(info);
      when(mockScheduler.play(track)).thenReturn(false);
      tracks.add(track);
    }

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getName()).thenReturn("PlaylistName");
    when(mockPlaylist.getTracks()).thenReturn(tracks);

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added playlist to queue")
            .description("**Playlist:** [PlaylistName]()")
            .addField("Queue Position", "1 - 7", false);
    for (int i = 0; i < 5; i++) {
      expectedBuilder.addField(
          "" + (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 30 sec. | [Video Link](test)",
          false);
    }
    expectedBuilder.footer("...and 2 more tracks (total of 7)", null);
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.getQueue()).thenReturn(tracks);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(expectedEmbed));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, true);
    handler.playlistLoaded(mockPlaylist);

    for (AudioTrack track : tracks) {
      verify(mockScheduler, times(1)).play(track);
    }
    verify(mockChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testPlayListLoadedSingleTrackOnlyNotPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(mockAudioTrack);

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .addField("Queue Position", "1", false)
            .footer("Note: To add the entire playlist, use '!playall <url>' instead.", null)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(false);
    when(mockScheduler.getQueue()).thenReturn(List.of(mockAudioTrack));
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testPlayListLoadedSingleTrackOnlyPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(mockAudioTrack);

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Playing track from playlist")
            .description(
                "Now playing targeted track: **Title**.\n*Note: To add the entire playlist, use '!playall <url>' instead.*")
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(true);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testPlayListLoadedSearchResult() {
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);

    when(mockPlaylist.isSearchResult()).thenReturn(true);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(mockAudioTrack);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(true);

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
  }

  @Test
  public void testConstructorsAndGetters() {
    MusicAudioLoadResultHandler handler3 = new MusicAudioLoadResultHandler(mockEvent, query, true);
    assertEquals(mockEvent, handler3.getEvent());
    assertEquals(query, handler3.getQuery());
    assertEquals(true, handler3.isForcePlay());
    assertEquals(false, handler3.shouldRequeueCurrent());
    assertEquals(false, handler3.isLoadFullPlaylist());

    MusicAudioLoadResultHandler handler4 =
        new MusicAudioLoadResultHandler(mockEvent, query, true, true);
    assertEquals(mockEvent, handler4.getEvent());
    assertEquals(query, handler4.getQuery());
    assertEquals(true, handler4.isForcePlay());
    assertEquals(true, handler4.shouldRequeueCurrent());
    assertEquals(false, handler4.isLoadFullPlaylist());
  }

  @Test
  public void testPlayListLoadedSearchResultNullSelectedTrackNotEmpty() {
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);

    when(mockPlaylist.isSearchResult()).thenReturn(true);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(null);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(true);

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
  }

  @Test
  public void testPlayListLoadedSearchResultNullSelectedTrackEmpty() {
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);

    when(mockPlaylist.isSearchResult()).thenReturn(true);
    when(mockPlaylist.getTracks()).thenReturn(List.of());
    when(mockPlaylist.getSelectedTrack()).thenReturn(null);

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(0)).play(any(), any(Boolean.class), any(Boolean.class));
  }

  @Test
  public void testPlayListLoadedSingleTrackOnlyNullSelectedTrackNotEmpty() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(null);

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .addField("Queue Position", "1", false)
            .footer("Note: To add the entire playlist, use '!playall <url>' instead.", null)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack, false, false)).thenReturn(false);
    when(mockScheduler.getQueue()).thenReturn(List.of(mockAudioTrack));
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, false, false);
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testPlayListLoadedSingleTrackOnlyNullSelectedTrackEmpty() {
    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getTracks()).thenReturn(List.of());
    when(mockPlaylist.getSelectedTrack()).thenReturn(null);

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, false, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(0)).play(any(), any(Boolean.class), any(Boolean.class));
  }

  @Test
  public void testPlayListLoadedPlayingForcePlayFirstTrackNotPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrack mockAudioTrack2 = Mockito.mock(AudioTrack.class);
    AudioTrackInfo info1 =
        new AudioTrackInfo("Title 1", "Author 1", 30000L, "identifier", true, "test");
    AudioTrackInfo info2 =
        new AudioTrackInfo("Title 2", "Author 2", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(info1);
    when(mockAudioTrack2.getInfo()).thenReturn(info2);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getName()).thenReturn("PlaylistName");
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack, mockAudioTrack2));

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added playlist to queue")
            .description("**Playlist:** [PlaylistName]()")
            .addField(
                "1. Title 1", "Artist: Author 1 | Duration: 30 sec. | [Video Link](test)", false)
            .addField(
                "2. Title 2", "Artist: Author 2 | Duration: 30 sec. | [Video Link](test)", false)
            .footer("(total of 2)", null)
            .build();

    when(mockScheduler.play(mockAudioTrack, true, false)).thenReturn(false);
    when(mockScheduler.addToQueueAtPosition(mockAudioTrack2, 0)).thenReturn(true);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, true, false, true);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, true, false);
    verify(mockScheduler, times(1)).addToQueueAtPosition(mockAudioTrack2, 0);
  }

  @Test
  public void testPlayListLoadedSingleTrackOnlyForcePlayNotPlaying() {
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockChannel = Mockito.mock(MessageChannel.class);
    AudioTrack mockAudioTrack = Mockito.mock(AudioTrack.class);
    AudioTrackInfo mockAudioTrackInfo =
        new AudioTrackInfo("Title", "Author", 30000L, "identifier", true, "test");
    when(mockAudioTrack.getInfo()).thenReturn(mockAudioTrackInfo);

    AudioPlaylist mockPlaylist = Mockito.mock(AudioPlaylist.class);
    when(mockPlaylist.getTracks()).thenReturn(List.of(mockAudioTrack));
    when(mockPlaylist.getSelectedTrack()).thenReturn(mockAudioTrack);

    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Added track to queue")
            .addField("Track Title", "Title", false)
            .addField("Track Artist", "Author", false)
            .addField("Duration", "30 sec.", false)
            .addField("Queue Position", "1", false)
            .footer("Note: To add the entire playlist, use '!forceplayall <url>' instead.", null)
            .build();

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockScheduler.play(mockAudioTrack, true, false)).thenReturn(false);
    when(mockScheduler.getQueue()).thenReturn(List.of(mockAudioTrack));
    when(mockMessage.getChannel()).thenReturn(Mono.just(mockChannel));
    when(mockChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockChannel).withEmbeds(embedSpec));
    when(mockChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    MusicAudioLoadResultHandler handler =
        new MusicAudioLoadResultHandler(mockEvent, query, true, false, false);
    handler.playlistLoaded(mockPlaylist);

    verify(mockScheduler, times(1)).play(mockAudioTrack, true, false);
    verify(mockChannel, times(1)).createMessage(embedSpec);
  }
}
