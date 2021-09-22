package net.ddellspe.music.bot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MessageUtilsTest {

  @Test
  public void testDurationUnderTenSeconds() {
    assertEquals("5 sec.", MessageUtils.getDurationAsMinSecond(5_000));
  }

  @Test
  public void testDurationUnderOneMinute() {
    assertEquals("15 sec.", MessageUtils.getDurationAsMinSecond(15_000));
  }

  @Test
  public void testDurationUnderTenMinutes() {
    assertEquals("5:15", MessageUtils.getDurationAsMinSecond(315_000));
  }

  @Test
  public void testDurationUnderOneHour() {
    assertEquals("45:30", MessageUtils.getDurationAsMinSecond(2_730_000));
  }

  @Test
  public void testDurationUnderTenHours() {
    assertEquals("7:33:13", MessageUtils.getDurationAsMinSecond(27_193_000));
  }

  @Test
  public void testDurationOverTenHours() {
    assertEquals("12:27:11", MessageUtils.getDurationAsMinSecond(44_831_000));
  }
}
