package net.ddellspe.music.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class PrefixMessageResponseCommandTest {
  private static final Snowflake GUILD_ID = Snowflake.of("123456");
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockManager = Mockito.mock(MusicAudioManager.class);
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testGetPrefixReturnsProperPrefix() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));

    PrefixMessageResponseCommand cmd = new TestPrefixMessageResponseCommand();
    assertEquals("!test ", cmd.getPrefix(mockEvent));

    verify(mockEvent, times(1)).getGuildId();
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testGetMessageAfterPrefixReturnsProperPrefix() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!test blah");

    PrefixMessageResponseCommand cmd = new TestPrefixMessageResponseCommand();
    assertEquals("blah", cmd.getMessageAfterPrefix(mockEvent));

    verify(mockEvent, times(1)).getGuildId();
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testGetMessageAfterPrefixThrowsExceptionWhenDataNotPrefixingContent() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!test");

    PrefixMessageResponseCommand cmd = new TestPrefixMessageResponseCommand();
    IndexOutOfBoundsException thrown =
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> {
              cmd.getMessageAfterPrefix(mockEvent);
            });
    assertEquals("Index 1 out of bounds for length 1", thrown.getMessage());

    verify(mockEvent, times(1)).getGuildId();
    verify(mockManager, times(1)).getPrefix();
  }

  @Test
  public void testGetMessageAfterPrefixThrowsExceptionWhenDataNoDataAfterPrefix() {
    MessageCreateEvent mockEvent = Mockito.mock(MessageCreateEvent.class);
    Message mockMessage = Mockito.mock(Message.class);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getContent()).thenReturn("!test ");

    PrefixMessageResponseCommand cmd = new TestPrefixMessageResponseCommand();
    IndexOutOfBoundsException thrown =
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> {
              cmd.getMessageAfterPrefix(mockEvent);
            });
    assertEquals("Index 1 out of bounds for length 0", thrown.getMessage());

    verify(mockEvent, times(1)).getGuildId();
    verify(mockManager, times(1)).getPrefix();
  }

  class TestPrefixMessageResponseCommand implements PrefixMessageResponseCommand {
    @Override
    public String getName() {
      return "test";
    }

    // Not needed for our tests
    @Override
    public Snowflake getFilterChannel(Snowflake guildId) {
      return null;
    }

    // Not needed for our tests
    @Override
    public Mono<Void> handle(MessageCreateEvent event) {
      return null;
    }
  }
}
