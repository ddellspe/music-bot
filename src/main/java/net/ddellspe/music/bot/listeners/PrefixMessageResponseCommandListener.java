package net.ddellspe.music.bot.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import java.util.Collection;
import java.util.List;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.commands.PrefixMessageResponseCommand;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Listener for dealing with commands with a prefix (so more data is available in the command) */
public class PrefixMessageResponseCommandListener {
  private final Collection<PrefixMessageResponseCommand> commands;

  public PrefixMessageResponseCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(PrefixMessageResponseCommand.class).values();
  }

  public Mono<Void> handle(MessageCreateEvent event) {

    final List<Role> roles = event.getMember().get().getRoles().collectList().block();

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
                    .equals(
                        event
                            .getMessage()
                            .getContent()
                            .substring(
                                0,
                                (manager.getPrefix().length() + command.getName().length() + 1))))
        // Only one command will respond to the command, so limit the scope once we find it
        .next()
        .flatMap(command -> command.handle(event));
  }
}
