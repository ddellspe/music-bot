package net.ddellspe.music.bot.audio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.JacksonResources;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.ddellspe.music.bot.model.GuildConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class MusicAudioManager {

  public static AudioPlayerManager PLAYER_MANAGER;

  static {
    PLAYER_MANAGER = new DefaultAudioPlayerManager();
    // This is an optimization strategy that Discord4J can utilize to minimize allocations
    PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
    AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
    AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
  }

  /** This is necessary for testing */
  public static void clearManagers() {
    MANAGERS.clear();
  }

  private static final Map<Snowflake, MusicAudioManager> MANAGERS = new ConcurrentHashMap<>();

  public static MusicAudioManager of(final Snowflake id) {
    return MANAGERS.computeIfAbsent(id, ignored -> new MusicAudioManager(id));
  }

  public static void set(final Snowflake id, MusicAudioManager manager) {
    MANAGERS.put(id, manager);
  }

  private final AudioPlayer player;
  private MusicAudioTrackScheduler scheduler;
  private final MusicAudioProvider provider;
  private final GuildConfiguration configuration;
  private final AtomicBoolean started;

  private MusicAudioManager(Snowflake guildId) {
    player = PLAYER_MANAGER.createPlayer();
    scheduler = new MusicAudioTrackScheduler(player, this);
    provider = new MusicAudioProvider(player);

    player.addListener(scheduler);
    JacksonResources d4jMapper = JacksonResources.create();
    PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();

    Map<Snowflake, GuildConfiguration> configurationMap = new HashMap<>();
    try {
      Resource config = matcher.getResource("configs/music_config.json");
      List<GuildConfiguration> guildConfigurations =
          d4jMapper
              .getObjectMapper()
              .readValue(config.getInputStream(), new TypeReference<List<GuildConfiguration>>() {});
      for (GuildConfiguration cfg : guildConfigurations) {
        configurationMap.put(cfg.getGuildId(), cfg);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    configuration = configurationMap.get(guildId);
    started = new AtomicBoolean(false);
  }

  public AudioPlayer getPlayer() {
    return player;
  }

  public void setScheduler(MusicAudioTrackScheduler scheduler) {
    this.scheduler = scheduler;
  }

  public MusicAudioTrackScheduler getScheduler() {
    return scheduler;
  }

  public MusicAudioProvider getProvider() {
    return provider;
  }

  public void start(GatewayDiscordClient client) {
    scheduler.setClient(client);
    started.set(true);
  }

  public void stop() {
    scheduler.stop();
    started.set(false);
  }

  public boolean isStarted() {
    return started.get();
  }

  public GuildConfiguration getConfiguration() {
    return configuration;
  }

  public Snowflake getChatChannel() {
    return configuration.getChatChanelId();
  }

  public String getPrefix() {
    return configuration.getCommandPrefix();
  }
}
