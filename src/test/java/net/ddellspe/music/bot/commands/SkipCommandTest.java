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
import discord4j.rest.util.Color;
import java.util.Optional;
import java.util.function.Consumer;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.audio.MusicAudioTrackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class SkipCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetName() {
    SkipCommand cmd = new SkipCommand();
    assertEquals("skip", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(mockManager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    SkipCommand cmd = new SkipCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testWhenManagerNotStartedEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

    when(mockManager.isStarted()).thenReturn(false);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.just(mockMessage));

    SkipCommand cmd = new SkipCommand();
    cmd.handle(mockEvent).block();

    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.DARK_GOLDENROD, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("Bot not started", embedSpec.asRequest().title().get());
  }

  @Test
  public void testWhenManagerStartedSkipReturnsFalseEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    MusicAudioTrackScheduler mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getScheduler()).thenReturn(mockScheduler);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.just(mockMessage));
    when(mockScheduler.skip()).thenReturn(false);

    SkipCommand cmd = new SkipCommand();
    cmd.handle(mockEvent).block();

    verify(mockScheduler, times(1)).skip();
    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    verify(mockMessageChannel).createEmbed(consumerCaptor.capture());
    Consumer<EmbedCreateSpec> messageSpecConsumer = consumerCaptor.getValue();
    EmbedCreateSpec embedSpec = new EmbedCreateSpec();
    messageSpecConsumer.accept(embedSpec);
    assertEquals(Color.DARK_GOLDENROD, Color.of(embedSpec.asRequest().color().get()));
    assertEquals("No track to skip to", embedSpec.asRequest().title().get());
  }

  @Test
  public void testWhenManagerStartedSkipReturnsTrueEmbedReturned() {
    MessageCreateEvent mockEvent = mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    MusicAudioTrackScheduler mockScheduler = Mockito.mock(MusicAudioTrackScheduler.class);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);

    when(mockManager.isStarted()).thenReturn(true);
    when(mockManager.getScheduler()).thenReturn(mockScheduler);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createEmbed(any(Consumer.class))).thenReturn(Mono.just(mockMessage));
    when(mockScheduler.skip()).thenReturn(true);

    SkipCommand cmd = new SkipCommand();
    cmd.handle(mockEvent).block();

    verify(mockScheduler, times(1)).skip();
    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
  }
}
