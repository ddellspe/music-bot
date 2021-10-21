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
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.ddellspe.music.bot.model.GuildConfiguration;
import net.ddellspe.music.bot.utils.VoiceUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import reactor.core.publisher.Mono;

/**
 * Audio Manager for Music Bot, this holds the references to the Audio Players as well as the
 * overall configuration and state for each audio manager per guild.
 */
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

  /**
   * Returns the existing audio manager or creates one for the provided guild.
   *
   * @param id the guild id to get the manager for
   * @return the manager either if it exists or if there is configuration for it
   */
  public static MusicAudioManager of(final Snowflake id) {
    return MANAGERS.computeIfAbsent(id, ignored -> new MusicAudioManager(id));
  }

  /**
   * This should only be used for testing to allow you to mock the Manager in tests.
   *
   * @param id the guild id for the manager
   * @param manager the audio manager for the given guild
   */
  public static void set(final Snowflake id, MusicAudioManager manager) {
    MANAGERS.put(id, manager);
  }

  /** non-final for testing purposes, in practice this is final */
  private MusicAudioTrackScheduler scheduler;

  private MusicAudioProvider provider;

  private final Snowflake guildId;
  private final AudioPlayer player;
  private final GuildConfiguration configuration;
  private final AtomicBoolean started;

  private MusicAudioManager(Snowflake guildId) {
    this.guildId = guildId;
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

  public void setProvider(MusicAudioProvider provider) {
    this.provider = provider;
  }

  public MusicAudioTrackScheduler getScheduler() {
    return scheduler;
  }

  public MusicAudioProvider getProvider() {
    return provider;
  }

  public boolean start(
      GatewayDiscordClient client, Snowflake voiceChannelId, Snowflake chatChannelId) {
    scheduler.setClient(client);
    VoiceConnection connection = joinVoiceChannel(client, voiceChannelId).block();
    if (connection == null) {
      client
          .getChannelById(chatChannelId)
          .cast(MessageChannel.class)
          .zipWith(client.getChannelById(voiceChannelId).cast(VoiceChannel.class))
          .flatMap(
              objects ->
                  objects
                      .getT1()
                      .createMessage(
                          EmbedCreateSpec.builder()
                              .color(Color.RED)
                              .title("Issue Joining Channel")
                              .addField(
                                  "Could Not Join Voice Channel", objects.getT2().getName(), true)
                              .build()))
          .block();
      started.set(false);
    } else {
      client
          .getChannelById(chatChannelId)
          .cast(MessageChannel.class)
          .zipWith(client.getChannelById(voiceChannelId).cast(VoiceChannel.class))
          .flatMap(
              objects ->
                  objects
                      .getT1()
                      .createMessage(
                          EmbedCreateSpec.builder()
                              .color(Color.MEDIUM_SEA_GREEN)
                              .title("Music Bot Started")
                              .addField("Joined Channel", objects.getT2().getName(), true)
                              .build()))
          .block();
      started.set(true);
    }
    return started.get();
  }

  /**
   * Stop the Manager (if it's started)
   *
   * @param client
   */
  public void stop(GatewayDiscordClient client) {
    VoiceConnection conn = client.getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
    if (conn != null) {
      conn.disconnect().block();
    }
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

  private Mono<VoiceConnection> joinVoiceChannel(
      GatewayDiscordClient client, Snowflake voiceChannelId) {
    return client
        .getChannelById(voiceChannelId)
        .cast(VoiceChannel.class)
        .filterWhen(___ -> VoiceUtils.botChannelHasPeople(client, voiceChannelId))
        .flatMap(
            channel ->
                channel
                    .join(
                        VoiceChannelJoinSpec.builder()
                            .selfDeaf(true)
                            .provider(provider)
                            .timeout(Duration.of(2000, ChronoUnit.MILLIS))
                            .build())
                    .onErrorResume(e -> Mono.empty()));
  }
}
