package net.ddellspe.music.bot.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Mono;

public class VoiceUtils {
  public static Mono<Long> getVoiceChannelPersonCount(
      GatewayDiscordClient client, Snowflake voiceChannelId) {
    return client
        .getChannelById(voiceChannelId)
        .cast(VoiceChannel.class)
        .flatMapMany(VoiceChannel::getVoiceStates)
        .flatMap(VoiceState::getMember)
        .filter(member -> !member.isBot())
        .count();
  }

  public static Mono<Boolean> botChannelHasPeople(
      GatewayDiscordClient client, Snowflake voiceChannelId) {
    return getVoiceChannelPersonCount(client, voiceChannelId).map(count -> count > 0);
  }

  public static Mono<Boolean> botChannelHasNoPeople(
      GatewayDiscordClient client, Snowflake voiceChannelId) {
    return getVoiceChannelPersonCount(client, voiceChannelId).map(count -> count == 0);
  }
}
