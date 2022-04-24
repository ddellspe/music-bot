package net.ddellspe.music.bot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import net.ddellspe.music.bot.audio.MusicAudioLoadResultHandler;
import net.ddellspe.music.bot.audio.MusicAudioManager;
import org.springframework.stereotype.Component;

@Component
public class ForcePlayCommand extends PlayCommand {
  @Override
  public String getName() {
    return "fplay";
  }

  @Override
  void playTrack(MusicAudioManager manager, String query, MessageCreateEvent event) {
    MusicAudioManager.PLAYER_MANAGER.loadItemOrdered(
        manager, query, new MusicAudioLoadResultHandler(event, query, true));
  }
}
