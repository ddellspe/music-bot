package net.ddellspe.music.bot.listeners;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import java.util.Collection;
import net.ddellspe.music.bot.commands.VoiceStateTrigger;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Listener for detecting voice state items */
public class VoiceStateTriggerListener {
  private final Collection<VoiceStateTrigger> commands;

  public VoiceStateTriggerListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(VoiceStateTrigger.class).values();
  }

  public Mono<Void> handle(VoiceStateUpdateEvent event) {
    return Flux.fromIterable(commands)
        .filter(___ -> !event.getCurrent().getUserId().equals(event.getClient().getSelfId()))
        .filter(command -> command.isCorrectEventType(event))
        .filter(command -> command.isCorrectChannel(event))
        // Multiple Audio Manager Trigger Commands may respond to a single event, so we can't use
        // next here
        .flatMap(command -> command.handle(event))
        .next();
  }
}
