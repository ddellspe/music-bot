package net.ddellspe.music.bot.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import java.util.Collection;
import java.util.List;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import net.ddellspe.music.bot.commands.SuperUserMessageResponseCommand;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Listener for full message commands that require a specific role (in this case, DJ) */
public class SuperUserMessageResponseCommandListener {
  private final Collection<SuperUserMessageResponseCommand> commands;

  public SuperUserMessageResponseCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(SuperUserMessageResponseCommand.class).values();
  }

  public Mono<Void> handle(MessageCreateEvent event) {

    final List<Role> roles = event.getMember().get().getRoles().collectList().block();

    MusicAudioManager manager = MusicAudioManager.of(event.getGuildId().get());

    return Flux.fromIterable(commands)
        .filter(___ -> roles.stream().anyMatch(role -> "DJ".equals(role.getName())))
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
