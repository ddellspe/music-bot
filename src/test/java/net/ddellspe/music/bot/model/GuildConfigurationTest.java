package net.ddellspe.music.bot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import discord4j.common.JacksonResources;
import discord4j.common.util.Snowflake;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class GuildConfigurationTest {
  @Test
  public void testGuildConfigurationReadFromFileWorks() throws IOException {
    JacksonResources d4jMapper = JacksonResources.create();
    PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
    Resource config = matcher.getResource("configs/music_config.json");
    List<GuildConfiguration> configs =
        d4jMapper
            .getObjectMapper()
            .readValue(config.getInputStream(), new TypeReference<List<GuildConfiguration>>() {});

    assertEquals(2, configs.size());
    GuildConfiguration guildConfig = configs.get(0);
    assertEquals(Snowflake.of("884566541402779718"), guildConfig.getGuildId());
    assertEquals(Snowflake.of("884566626165477437"), guildConfig.getChatChanelId());
    assertEquals(">", guildConfig.getCommandPrefix());
    guildConfig = configs.get(1);
    assertEquals(Snowflake.of("123456789"), guildConfig.getGuildId());
    assertEquals(null, guildConfig.getChatChanelId());
    assertEquals("!", guildConfig.getCommandPrefix());
  }
}
