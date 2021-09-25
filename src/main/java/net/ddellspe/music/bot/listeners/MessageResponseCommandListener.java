package net.ddellspe.music.bot.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import java.util.Collection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.commands.MessageResponseCommand;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Listener for full message commands that don't require a specific role */
public class MessageResponseCommandListener {
  private final Collection<MessageResponseCommand> commands;

  public MessageResponseCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(MessageResponseCommand.class).values();
  }

  public Mono<Void> handle(MessageCreateEvent event) {
    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());
    return Flux.fromIterable(commands)
        .filter(
            command ->
                ((command.getFilterChannel(event.getGuildId().get()) != null
                        && event
                            .getMessage()
                            .getChannelId()
                            .equals(command.getFilterChannel(event.getGuildId().get())))
                    || command.getFilterChannel(event.getGuildId().get()) == null))
        .filter(
            command ->
                (manager.getPrefix() + command.getName())
                    .equals(event.getMessage().getContent().toLowerCase()))
        // Only one command will respond to the command, so limit the scope once we find it
        .next()
        .flatMap(command -> command.handle(event));
  }
}
