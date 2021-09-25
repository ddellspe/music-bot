package net.ddellspe.music.bot.listeners;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import java.util.Map;
import net.ddellspe.music.bot.commands.VoiceStateTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

public class VoiceStateTriggerListenerTest {
  private final Snowflake SELF_ID = Snowflake.of("111111");
  private final Snowflake EVENT_ID = Snowflake.of("222222");
  private ApplicationContext mockApplicationContext;
  private VoiceStateTrigger mockCommand;
  private VoiceStateUpdateEvent mockEvent;

  @BeforeEach
  public void before() {
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockCommand = Mockito.mock(VoiceStateTrigger.class);
    mockEvent = Mockito.mock(VoiceStateUpdateEvent.class);
    when(mockApplicationContext.getBeansOfType(VoiceStateTrigger.class))
        .thenReturn(Map.of("VoiceStateTrigger", mockCommand));
  }

  @Test
  public void testCurrentIdEqualToSelfId() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(SELF_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);

    VoiceStateTriggerListener listener = new VoiceStateTriggerListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(0)).isCorrectEventType(mockEvent);
    verify(mockCommand, times(0)).isCorrectChannel(mockEvent);
  }

  @Test
  public void testNotCorrectEventType() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(EVENT_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);
    when(mockCommand.isCorrectEventType(mockEvent)).thenReturn(false);

    VoiceStateTriggerListener listener = new VoiceStateTriggerListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(1)).isCorrectEventType(mockEvent);
    verify(mockCommand, times(0)).isCorrectChannel(mockEvent);
  }

  @Test
  public void testNotCorrectChannel() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(EVENT_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);
    when(mockCommand.isCorrectEventType(mockEvent)).thenReturn(true);
    when(mockCommand.isCorrectChannel(mockEvent)).thenReturn(false);

    VoiceStateTriggerListener listener = new VoiceStateTriggerListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(0)).handle(mockEvent);
    verify(mockCommand, times(1)).isCorrectEventType(mockEvent);
    verify(mockCommand, times(1)).isCorrectChannel(mockEvent);
  }

  @Test
  public void testCallsHandleMethod() {
    VoiceState mockState = Mockito.mock(VoiceState.class);
    GatewayDiscordClient mockClient = Mockito.mock(GatewayDiscordClient.class);

    when(mockEvent.getCurrent()).thenReturn(mockState);
    when(mockState.getUserId()).thenReturn(EVENT_ID);
    when(mockEvent.getClient()).thenReturn(mockClient);
    when(mockClient.getSelfId()).thenReturn(SELF_ID);
    when(mockCommand.isCorrectEventType(mockEvent)).thenReturn(true);
    when(mockCommand.isCorrectChannel(mockEvent)).thenReturn(true);
    when(mockCommand.handle(mockEvent)).thenReturn(Mono.empty());

    VoiceStateTriggerListener listener = new VoiceStateTriggerListener(mockApplicationContext);
    listener.handle(mockEvent).block();

    verify(mockCommand, times(1)).handle(mockEvent);
    verify(mockCommand, times(1)).isCorrectEventType(mockEvent);
    verify(mockCommand, times(1)).isCorrectChannel(mockEvent);
  }
}
