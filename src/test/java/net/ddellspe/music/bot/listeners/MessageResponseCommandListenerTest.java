package net.ddellspe.music.bot.listeners;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import java.util.Map;
import java.util.Optional;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.commands.MessageResponseCommand;
import net.ddellspe.music.bot.commands.SuperUserMessageResponseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MessageResponseCommandListenerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private final Snowflake CHAT_CHANNEL_ID = Snowflake.of("111111");
  private ApplicationContext mockApplicationContext;
  private MessageCreateEvent mockEvent;
  private Member mockMember;
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    mockMember = Mockito.mock(Member.class);
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testFilterChannelNull() {
    Message mockMessage = Mockito.mock(Message.class);

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
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

  @Test
  public void testSuccessfulHandleCallSuperUserCommand() {
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    MessageResponseCommand mockCommand = Mockito.mock(SuperUserMessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockRole.getName()).thenReturn("DJ");
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
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
    verify(mockRole, times(1)).getName();
    verify(mockManager, times(1)).getPrefix();
    verify(mockCommand, times(1)).handle(mockEvent);
  }

  @Test
  public void testSuccessfulHandleCallNonSuperUserCommand() {
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    MessageResponseCommand mockCommand = Mockito.mock(MessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockRole.getName()).thenReturn("DJ");
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
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
    verify(mockRole, times(0)).getName();
    verify(mockManager, times(1)).getPrefix();
    verify(mockCommand, times(1)).handle(mockEvent);
  }

  @Test
  public void testSuccessfulHandleCallSuperUserCommandHasDJRole() {
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);
    Role mockNonDJRole = Mockito.mock(Role.class);

    MessageResponseCommand mockCommand = Mockito.mock(SuperUserMessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockRole.getName()).thenReturn("DJ");
    when(mockNonDJRole.getName()).thenReturn("Admin");
    when(mockMember.getRoles()).thenReturn(Flux.just(mockNonDJRole, mockRole));
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
    verify(mockRole, times(1)).getName();
    verify(mockNonDJRole, times(1)).getName();
    verify(mockManager, times(1)).getPrefix();
    verify(mockCommand, times(1)).handle(mockEvent);
  }

  @Test
  public void testHandleCallSuperUserCommandNotCorrectRoleName() {
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    MessageResponseCommand mockCommand = Mockito.mock(SuperUserMessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockRole.getName()).thenReturn("Admin");
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn("!blah");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockRole, times(1)).getName();
    verify(mockManager, times(0)).getPrefix();
    verify(mockCommand, times(0)).handle(mockEvent);
  }

  @Test
  public void testHandleCallSuperUserCommandNoRoles() {
    Message mockMessage = Mockito.mock(Message.class);

    MessageResponseCommand mockCommand = Mockito.mock(SuperUserMessageResponseCommand.class);
    when(mockApplicationContext.getBeansOfType(MessageResponseCommand.class))
        .thenReturn(Map.of("MessageResponseCommand", mockCommand));

    when(mockMember.getRoles()).thenReturn(Flux.empty());
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getContent()).thenReturn("!blah");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockCommand.getName()).thenReturn("blah");

    MessageResponseCommandListener listener =
        new MessageResponseCommandListener(mockApplicationContext);

    listener.handle(mockEvent).block();
    verify(mockManager, times(0)).getPrefix();
    verify(mockCommand, times(0)).handle(mockEvent);
  }
}
