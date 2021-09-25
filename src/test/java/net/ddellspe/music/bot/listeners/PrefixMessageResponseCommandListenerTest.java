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
import net.ddellspe.music.bot.commands.PrefixMessageResponseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

public class PrefixMessageResponseCommandListenerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private final Snowflake CHAT_CHANNEL_ID = Snowflake.of("111111");
  private ApplicationContext mockApplicationContext;
  private PrefixMessageResponseCommand mockCommand;
  private MessageCreateEvent mockEvent;
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockCommand = Mockito.mock(PrefixMessageResponseCommand.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    when(mockApplicationContext.getBeansOfType(PrefixMessageResponseCommand.class))
        .thenReturn(Map.of("PrefixMessageResponseCommand", mockCommand));
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testFilterChannelNullAndMessageDoesNotMatch() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(null);
    when(mockCommand.getPrefix(mockEvent)).thenReturn("!blah ");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!blab ");

    PrefixMessageResponseCommandListener listener =
        new PrefixMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(1)).getPrefix(mockEvent);
  }

  @Test
  public void testFilterChannelDoesNotMatchFilterChannel() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(Snowflake.of("222222"));

    PrefixMessageResponseCommandListener listener =
        new PrefixMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(0)).getPrefix(mockEvent);
  }

  @Test
  public void testFilterChannelEqualsFilterChannelNoCommandMatch() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getPrefix(mockEvent)).thenReturn("!blah ");
    when(mockMessage.getContent()).thenReturn("!blab ");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);

    PrefixMessageResponseCommandListener listener =
        new PrefixMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(1)).getPrefix(mockEvent);
  }

  @Test
  public void testFilterChannelEqualsContentStartsWithPasses() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getPrefix(mockEvent)).thenReturn("!blah ");
    when(mockMessage.getContent()).thenReturn("!blah things");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    PrefixMessageResponseCommandListener listener =
        new PrefixMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockCommand, times(1)).getPrefix(mockEvent);
  }

  @Test
  public void testFilterChannelEqualsContentStartsWithPassesWithUppercase() {
    Message mockMessage = Mockito.mock(Message.class);

    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getPrefix(mockEvent)).thenReturn("!blah ");
    when(mockMessage.getContent()).thenReturn("!BLAH things");
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    PrefixMessageResponseCommandListener listener =
        new PrefixMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockCommand, times(1)).getPrefix(mockEvent);
  }
}
