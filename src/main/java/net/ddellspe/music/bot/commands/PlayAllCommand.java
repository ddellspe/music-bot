package net.ddellspe.music.bot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;

/** Command that allows playing a full playlist (loading all tracks) at the end of the queue. */
@Component
public class PlayAllCommand extends PlayCommand {
  @Override
  public String getName() {
    return "playall";
  }

  @Override
  void playTrack(MusicAudioManager manager, String query, MessageCreateEvent event) {
    if (query.contains("music.youtube")) {
      query = query.replace("music.youtube", "youtube");
    }
    MusicAudioManager.PLAYER_MANAGER.loadItemOrdered(
        manager, query, new MusicAudioLoadResultHandler(event, query, false, false, true));
  }
}
