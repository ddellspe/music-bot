package net.ddellspe.music.bot.listeners;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Map;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.commands.MessageResponseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

public class MessageResponseCommandListenerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private final Snowflake CHAT_CHANNEL_ID = Snowflake.of("111111");
  private ApplicationContext mockApplicationContext;
  private MessageResponseCommand mockCommand;
  private MessageCreateEvent mockEvent;
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockCommand = Mockito.mock(MessageResponseCommand.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testFilterChannelNull() {
    Message mockMessage = Mockito.mock(Message.class);
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(null);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockCommand.getName()).thenReturn("name");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("blah");

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testFilterChannelNotEqualToMessageChannel() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(Snowflake.of("135790"));
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockManager, times(0)).getPrefix();
  }

  @Test
  public void testFilterChannelEqualToMessageChannelCommandPrefixWrong() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn(">blah");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testFilterChannelEqualToMessageChannelCommandNameWrong() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn("!blab");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testSuccessfulHandleCall() {
    Message mockMessage = Mockito.mock(Message.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn("!blah");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testSuccessfulHandleCallUpperCase() {
    Message mockMessage = Mockito.mock(Message.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn("!BLAH");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockManager, times(1)).getPrefix();
  }
}
