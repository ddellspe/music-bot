package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.audio.MusicAudioTrackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class SilenceMusicCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    SilenceMusicCommand cmd = new SilenceMusicCommand();
    assertEquals("silence", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    SilenceMusicCommand cmd = new SilenceMusicCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testWhenManagerNotStartedEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder().color(Color.RED).title("Music bot is not running").build();

    when(mockManager.isStarted()).thenReturn(false);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    SilenceMusicCommand cmd = new SilenceMusicCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }

  @Test
  public void testWhenManagerStartedEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    MusicAudioTrackScheduler mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    EmbedCreateSpec embedSpec =
        EmbedCreateSpec.builder()
            .color(Color.MEDIUM_SEA_GREEN)
            .title("Music has been silenced")
            .build();

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getScheduler()).thenReturn(mockScheduler);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage(embedSpec))
        .thenReturn(MessageCreateMono.of(mockMessageChannel).withEmbeds(embedSpec));
    // Necessary for embed create spec
    when(mockMessageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.empty());

    SilenceMusicCommand cmd = new SilenceMusicCommand();
    cmd.handle(mockEvent).block();

    verify(mockScheduler, times(1)).stop();
    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel, times(1)).createMessage(embedSpec);
  }
}
