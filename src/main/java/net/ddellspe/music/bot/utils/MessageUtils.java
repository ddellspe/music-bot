package net.ddellspe.music.bot.utils;

/** Utilities for messages */
public class MessageUtils {
  /**
   * Converts a duration (in milliseconds) to a string in the format hh:mm:ss.
   *
   * @param duration time duration (in milliseconds)
   * @return String representation of the duration in milliseconds
   */
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
