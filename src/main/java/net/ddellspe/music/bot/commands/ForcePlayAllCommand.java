package net.ddellspe.music.bot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;

/**
 * Command that allows force playing a full playlist immediately, placing subsequent tracks at the
 * front of the queue.
 */
@Component
public class ForcePlayAllCommand extends PlayCommand {
  @Override
  public String getName() {
    return "fplayall";
  }

  @Override
  void playTrack(MusicAudioManager manager, String query, MessageCreateEvent event) {
    if (query.contains("music.youtube")) {
      query = query.replace("music.youtube", "youtube");
    }
    MusicAudioManager.PLAYER_MANAGER.loadItemOrdered(
        manager, query, new MusicAudioLoadResultHandler(event, query, true, false, true));
  }
}
