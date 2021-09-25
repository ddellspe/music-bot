package net.ddellspe.music.bot.listeners;

import static org.mockito.Mockito.atLeast;
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
import net.ddellspe.music.bot.commands.SuperUserMessageResponseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SuperUserMessageResponseCommandListenerTest {
  private final Snowflake GUILD_ID = Snowflake.of("123456");
  private final Snowflake CHAT_CHANNEL_ID = Snowflake.of("111111");
  private ApplicationContext mockApplicationContext;
  private SuperUserMessageResponseCommand mockCommand;
  private MessageCreateEvent mockEvent;
  private MusicAudioManager mockManager;

  @BeforeEach
  public void before() {
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockCommand = Mockito.mock(SuperUserMessageResponseCommand.class);
    mockEvent = Mockito.mock(MessageCreateEvent.class);
    mockManager = Mockito.mock(MusicAudioManager.class);
    when(mockApplicationContext.getBeansOfType(SuperUserMessageResponseCommand.class))
        .thenReturn(Map.of("SuperUserMessageResponseCommand", mockCommand));
    when(mockEvent.getGuildId()).thenReturn(Optional.of(GUILD_ID));
    MusicAudioManager.set(GUILD_ID, mockManager);
  }

  @Test
  public void testNoMember() {
    when(mockEvent.getMember()).thenReturn(Optional.empty());

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(0)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(0)).getPrefix();
  }

  @Test
  public void testNoRolesForMember() {
    Member mockMember = Mockito.mock(Member.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getRoles()).thenReturn(Flux.empty());

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(0)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(0)).getPrefix();
    verify(mockMember, times(1)).getRoles();
  }

  @Test
  public void testRolesNotDJ() {
    Member mockMember = Mockito.mock(Member.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("BLAH");

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(0)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(0)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }

  @Test
  public void testRoleHasDJNullFilterChannelNoCommandMatch() {
    Member mockMember = Mockito.mock(Member.class);
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("DJ");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(null);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockCommand.getName()).thenReturn("blah");
    when(mockMessage.getContent()).thenReturn("!blab");

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, atLeast(1)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(1)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }

  @Test
  public void testFilterChannelDoesNotMatch() {
    Member mockMember = Mockito.mock(Member.class);
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("DJ");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getChannelId()).thenReturn(Snowflake.of("222222"));

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, atLeast(1)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(0)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }

  @Test
  public void testFilterChannelMatchesCommandDoesNotMatch() {
    Member mockMember = Mockito.mock(Member.class);
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("DJ");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockCommand.getName()).thenReturn("blah");
    when(mockMessage.getContent()).thenReturn("!blab");

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, atLeast(1)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(1)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }

  @Test
  public void testCommandMatchesLowercase() {
    Member mockMember = Mockito.mock(Member.class);
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("DJ");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockCommand.getName()).thenReturn("blah");
    when(mockMessage.getContent()).thenReturn("!blah");
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockCommand, atLeast(1)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(1)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }

  @Test
  public void testCommandMatchesUppercase() {
    Member mockMember = Mockito.mock(Member.class);
    Message mockMessage = Mockito.mock(Message.class);
    Role mockRole = Mockito.mock(Role.class);

    when(mockEvent.getMember()).thenReturn(Optional.of(mockMember));
    when(mockEvent.getMessage()).thenReturn(mockMessage);
    when(mockMember.getRoles()).thenReturn(Flux.just(mockRole));
    when(mockRole.getName()).thenReturn("DJ");
    when(mockCommand.getFilterChannel(GUILD_ID)).thenReturn(CHAT_CHANNEL_ID);
    when(mockMessage.getChannelId()).thenReturn(CHAT_CHANNEL_ID);
    when(mockManager.getPrefix()).thenReturn("!");
    when(mockCommand.getName()).thenReturn("blah");
    when(mockMessage.getContent()).thenReturn("!BLAH");
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    SuperUserMessageResponseCommandListener listener =
        new SuperUserMessageResponseCommandListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockCommand, atLeast(1)).getFilterChannel(GUILD_ID);
    verify(mockManager, times(1)).getPrefix();
    verify(mockMember, times(1)).getRoles();
    verify(mockRole, times(1)).getName();
  }
}
