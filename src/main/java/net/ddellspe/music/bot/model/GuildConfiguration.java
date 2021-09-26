package net.ddellspe.music.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import discord4j.common.util.Snowflake;

public class GuildConfiguration {
  @JsonProperty("guildId")
  private String guildId;

  @JsonProperty("chatChannelId")
  private String chatChannelId;

  @JsonProperty("prefix")
  private String prefix;

  public Snowflake getGuildId() {
    return Snowflake.of(guildId);
  }

  public Snowflake getChatChanelId() {
    return chatChannelId == null ? null : Snowflake.of(chatChannelId);
  }

  public String getCommandPrefix() {
    return prefix;
  }
}
