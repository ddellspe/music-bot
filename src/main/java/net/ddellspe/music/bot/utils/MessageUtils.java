package net.ddellspe.music.bot.utils;

public class MessageUtils {
  public static String getDurationAsMinSecond(long duration) {
    long seconds = duration / 1000;
    long hours = seconds / 3600;
    seconds -= hours * 3600;
    long minutes = seconds / 60;
    seconds -= minutes * 60;
    return hours > 0
        ? String.format("%d:%02d:%02d", hours, minutes, seconds)
        : (minutes > 0
            ? String.format("%d:%02d", minutes, seconds)
            : String.format("%d sec.", seconds));
  }
}
