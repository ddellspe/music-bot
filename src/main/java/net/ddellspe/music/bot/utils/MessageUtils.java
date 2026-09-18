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

  /**
   * Truncates a string to the specified maximum length. If the string exceeds maxLength, it will be
   * truncated and appended with "..." such that the total length equals maxLength.
   *
   * @param text the text to truncate
   * @param maxLength the maximum allowed length
   * @return the truncated string, or null if text is null
   */
  public static String truncate(String text, int maxLength) {
    if (text == null) {
      return null;
    }
    if (text.length() <= maxLength) {
      return text;
    }
    if (maxLength <= 3) {
      return text.substring(0, maxLength);
    }
    return text.substring(0, maxLength - 3) + "...";
  }
}
