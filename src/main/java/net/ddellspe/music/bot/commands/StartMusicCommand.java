package net.ddellspe.music.bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StartMusicCommand implements MessageResponseCommand {
  @Override
  public String getName() {
    return "start";
  }

  @Override
  public Snowflake getFilterChannel(Snowflake guildId) {
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    return manager.getChatChannel();
  }

  @Override
  public Mono<Void> handle(MessageCreateEvent event) {
    // This will be guaranteed to be present since we're limiting to Join and Move events
    Snowflake guildId = event.getGuildId().get();
    MusicAudioManager manager = MusicAudioManager.of(guildId);
    final Snowflake voiceChannelId = getCurrentVoiceChannel(event);

    if (voiceChannelId == null) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createEmbed(
                      spec ->
                          spec.setColor(Color.RED)
                              .setTitle("You must be in a voice channel to start the music bot.")))
          .then();
    }

    final Mono<Boolean> nonBotChannelCountIsGreaterThanZero =
        event
            .getClient()
            .getChannelById(voiceChannelId)
            .cast(VoiceChannel.class)
            .flatMapMany(VoiceChannel::getVoiceStates)
            .flatMap(VoiceState::getMember)
            .filter(member -> !member.isBot())
            .count()
            .map(count -> count > 0);
    return event
        .getMessage()
        .getChannel()
        .filter(___ -> !manager.isStarted())
        .doOnNext(___ -> manager.start())
        .flatMap(message -> event.getClient().getChannelById(voiceChannelId))
        .cast(VoiceChannel.class)
        .filterWhen(___ -> nonBotChannelCountIsGreaterThanZero)
        .filter(___ -> manager.isStarted())
        .flatMap(
            channel ->
                channel.join(spec -> spec.setSelfDeaf(true).setProvider(manager.getProvider())))
        .onErrorResume(
            e -> {
              manager.stop();
              event
                  .getMessage()
                  .getChannel()
                  .flatMap(
                      channel ->
                          channel.createEmbed(
                              spec ->
                                  spec.setColor(Color.RED)
                                      .setTitle(
                                          "Unable to start the manager, make sure I have "
                                              + "permissions in the voice channel that you "
                                              + "are in.")))
                  .subscribe();
              return event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId);
            })
        .filterWhen(VoiceConnection::isConnected)
        .flatMap(VoiceConnection::getChannelId)
        .flatMap(channelId -> event.getClient().getChannelById(channelId))
        .cast(VoiceChannel.class)
        .zipWith(event.getMessage().getChannel())
        .flatMap(
            objects -> {
              return objects
                  .getT2()
                  .createEmbed(
                      spec ->
                          spec.setColor(Color.MEDIUM_SEA_GREEN)
                              .setTitle("Music Bot Started")
                              .addField("Joined Channel", objects.getT1().getName(), true));
            })
        .then();
  }
}
