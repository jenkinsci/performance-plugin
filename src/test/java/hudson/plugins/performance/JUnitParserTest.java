package hudson.plugins.performance;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUnitParserTest {

  /**
   * Test parsing a test with a long duration (ie. over 999 seconds) will work.
   * Previously this was causing a NumberFormatException since it puts commas
   * in for over 999 seconds, causing system to complain.
   */
  @Test
  public void testParseDurationLongRunningTest() {
    assertEquals("Test having a time with commas will work.", JUnitParser.parseDuration("34,953.254"), (long) 34953254);
    assertEquals("Test having a time with multiple commas will work.", JUnitParser.parseDuration("1,134,953.254"), (long) 1134953254);
    assertEquals("Test that time with no commas still work.", JUnitParser.parseDuration("999.999"), (long) 999999);
  }

}
