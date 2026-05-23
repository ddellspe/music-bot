package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.audio.MusicAudioTrackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ListCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;
  private MusicAudioTrackScheduler mockScheduler;
  private com.sedmelluq.discord.lavaplayer.player.AudioPlayer mockPlayer;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    mockPlayer = Mockito.mock(com.sedmelluq.discord.lavaplayer.player.AudioPlayer.class);
    when(mockManager.getScheduler()).thenReturn(mockScheduler);
    when(mockScheduler.getPlayer()).thenReturn(mockPlayer);
    when(mockPlayer.getPlayingTrack()).thenReturn(null);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    ListCommand cmd = new ListCommand();
    assertEquals("list", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    ListCommand cmd = new ListCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testGetPrefix() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockManager.getPrefix()).thenReturn("!");
    ListCommand cmd = new ListCommand();
    assertEquals("!list", cmd.getPrefix(mockEvent));
  }

  @Test
  public void testWhenMessageDoesNotMatchPrefixReturnsEmpty() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);

    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockMessage.getContent()).thenReturn("!listabc");

    ListCommand cmd = new ListCommand();
    Mono<Void> result = cmd.handle(mockEvent);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  public void testWhenManagerNotStartedEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder().color(Color.DARK_GOLDENROD).title("Bot not started").build();

    when(mockManager.isStarted()).thenReturn(false);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testWhenQueueIsEmptyEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder().color(Color.DARK_GOLDENROD).title("Playlist is empty").build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(new ArrayList<>());
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testWhenQueueHasItemsDefaultLimitUsed() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 5 of 7 tracks)");
    for (int i = 0; i < 5; i++) {
      expectedBuilder.addField(
          (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 1:05 | [Video Link](test)",
          false);
    }
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenQueueHasItemsCustomLimitUsed() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 3 of 7 tracks)");
    for (int i = 0; i < 3; i++) {
      expectedBuilder.addField(
          (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 1:05 | [Video Link](test)",
          false);
    }
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list 3");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenQueueHasItemsCustomLimitExceedsQueueSize() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 2 of 2 tracks)");
    for (int i = 0; i < 2; i++) {
      expectedBuilder.addField(
          (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 1:05 | [Video Link](test)",
          false);
    }
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list 10");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenQueueHasItemsInvalidLimitUsed() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 5 of 7 tracks)");
    for (int i = 0; i < 5; i++) {
      expectedBuilder.addField(
          (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 1:05 | [Video Link](test)",
          false);
    }
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list abc");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenQueueHasItemsNonPositiveLimitUsed() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec.Builder expectedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 5 of 7 tracks)");
    for (int i = 0; i < 5; i++) {
      expectedBuilder.addField(
          (i + 1) + ". Title " + (i + 1),
          "Artist: Author " + (i + 1) + " | Duration: 1:05 | [Video Link](test)",
          false);
    }
    EmbedCreateSpec expectedEmbed = expectedBuilder.build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list -10");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenPlayingTrackIsPresentAndQueueIsEmpty() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    AudioTrack mockTrack = mock(AudioTrack.class);
    AudioTrackInfo info =
        new AudioTrackInfo("Playing Title", "Playing Author", 30000L, "identifier", true, "test");
    when(mockTrack.getInfo()).thenReturn(info);
    when(mockPlayer.getPlayingTrack()).thenReturn(mockTrack);

    EmbedCreateSpec expectedEmbed =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 1 of 1 tracks)")
            .addField(
                "1. Playing Title (Currently Playing)",
                "Artist: Playing Author | Duration: 30 sec. | [Video Link](test)",
                false)
            .build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(new ArrayList<>());
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }

  @Test
  public void testWhenPlayingTrackIsPresentAndQueueHasItems() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    AudioTrack mockPlayingTrack = mock(AudioTrack.class);
    AudioTrackInfo playingInfo =
        new AudioTrackInfo("Playing Title", "Playing Author", 30000L, "identifier", true, "test");
    when(mockPlayingTrack.getInfo()).thenReturn(playingInfo);
    when(mockPlayer.getPlayingTrack()).thenReturn(mockPlayingTrack);

    List<AudioTrack> queue = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      AudioTrack mockTrack = mock(AudioTrack.class);
      AudioTrackInfo info =
          new AudioTrackInfo(
              "Title " + (i + 1), "Author " + (i + 1), 65000L, "identifier", true, "test");
      when(mockTrack.getInfo()).thenReturn(info);
      queue.add(mockTrack);
    }

    EmbedCreateSpec expectedEmbed =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Upcoming Playlist (Next 3 of 3 tracks)")
            .addField(
                "1. Playing Title (Currently Playing)",
                "Artist: Playing Author | Duration: 30 sec. | [Video Link](test)",
                false)
            .addField("2. Title 1", "Artist: Author 1 | Duration: 1:05 | [Video Link](test)", false)
            .addField("3. Title 2", "Artist: Author 2 | Duration: 1:05 | [Video Link](test)", false)
            .build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockScheduler.getQueue()).thenReturn(queue);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!list");
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(expectedEmbed))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(expectedEmbed));
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    ListCommand cmd = new ListCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(expectedEmbed);
  }
}
