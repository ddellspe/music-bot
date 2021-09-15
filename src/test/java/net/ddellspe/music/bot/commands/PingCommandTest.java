package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class PingCommandTest {
  private static Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager manager;

  @BeforeEach
  public void before() {
    manager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, manager);
  }

  @Test
  public void testGetName() {
    PingCommand cmd = new PingCommand();
    assertEquals("ping", cmd.getName());
  }

  @Test
  public void testGetFilterChannelReturnsProperValue() {
    Snowflake chatChannel = Snowflake.of("111111");
    when(manager.getChatChannel()).thenReturn(Snowflake.of("111111"));
    PingCommand cmd = new PingCommand();
    assertEquals(chatChannel, cmd.getFilterChannel(GUILD_ID));
  }

  @Test
  public void testHandlerForEvent() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    MessageChannel mockMessageChannel = Mockito.mock(MessageChannel.class);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    Mono<MessageChannel> channel = Mono.just(mockMessageChannel);
    StepVerifier.create(channel).expectNext(mockMessageChannel).verifyComplete();
    when(mockMessage.getChannel()).thenReturn(channel);
    when(mockMessageChannel.createMessage("Pong"))
        .thenReturn(Mono.just(Mockito.mock(Message.class)));

    PingCommand cmd = new PingCommand();
    cmd.handle(mockEvent).block();

    verify(mockMessageChannel, times(1)).createMessage(anyString());
  }
}
