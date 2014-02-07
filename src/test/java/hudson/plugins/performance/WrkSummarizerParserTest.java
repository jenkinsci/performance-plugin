package hudson.plugins.performance;

import static org.junit.Assert.*;

import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

public class WrkSummarizerParserTest {

  private WrkSummarizerParser parser;
  private PrintStream logger;
  private TaskListener listener;

  @Before
  public void before() {
    parser = new WrkSummarizerParser(null);
    logger = System.out;
    listener = new StreamTaskListener((java.io.OutputStream) System.out);
  }

  @Test
  public void testParseResultsWithMilliSecondResponseTimes() {
    List<File> files = new ArrayList<File>(1);
    files.add(new File(getClass().getResource("/WrkResultsQuick.wrk").getFile()));

    try {
      Collection<PerformanceReport> reports = parser.parse(null, files,
          listener);
      assertFalse(reports.isEmpty());
      for (PerformanceReport report : reports) {
        // should not have average time >= 1s
        assertTrue(report.getAverage() < 1000);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testParseResultsWithSecondResponseTimes() {
    List<File> files = new ArrayList<File>(1);
    files.add(new File(getClass().getResource("/WrkResultsLong.wrk").getFile()));

    try {
      Collection<PerformanceReport> reports = parser.parse(null, files, listener);
      assertFalse(reports.isEmpty());
      for (PerformanceReport report : reports) {
        // should have average time >= 1s
        assertTrue(report.getAverage() >= 1000);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testParseWithLatencyDistributionBuckets() {
    List<File> files = new ArrayList<File>(1);
    files.add(new File(getClass().getResource("/WrkResultsWithLatencyFlag.wrk") .getFile()));

    try {
      Collection<PerformanceReport> reports = parser.parse(null, files, listener);
      assertFalse(reports.isEmpty());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
  
  @Test
  public void testParseWithErrors() {
    List<File> files = new ArrayList<File>(1);
    files.add(new File(getClass().getResource("/WrkResultsWithErrors.wrk") .getFile()));

    try {
      Collection<PerformanceReport> reports = parser.parse(null, files, listener);
      assertFalse(reports.isEmpty());
      
      // NOTE: uncomment once this is intentionally supported. Currently some
      //       refactoring is needed with summarized reports.
      // for(PerformanceReport report: reports) {
      //   assertTrue(report.countErrors() > 0);
      // }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testParseTimeMeasurements() {
    // milliseconds
    assertEquals(5, parser.getTime("5ms", logger, WrkSummarizerParser.TimeUnit.MILLISECOND));
    assertEquals(5000, parser.getTime("5s", logger, WrkSummarizerParser.TimeUnit.MILLISECOND));
    assertEquals(5000 * 60, parser.getTime("5m", logger, WrkSummarizerParser.TimeUnit.MILLISECOND));
    assertEquals(1000 * 60 * 60, parser.getTime("1h", logger, WrkSummarizerParser.TimeUnit.MILLISECOND));

    // seconds
    assertEquals(1, parser.getTime("1005ms", logger, WrkSummarizerParser.TimeUnit.SECOND));
    assertEquals(5, parser.getTime("5s", logger, WrkSummarizerParser.TimeUnit.SECOND));
    assertEquals(5 * 60, parser.getTime("5m", logger, WrkSummarizerParser.TimeUnit.SECOND));
    assertEquals(60 * 60, parser.getTime("1h", logger, WrkSummarizerParser.TimeUnit.SECOND));

    // minute
    assertEquals(0, parser.getTime("5ms", logger, WrkSummarizerParser.TimeUnit.MINUTE));
    assertEquals((int) Math.floor(5 / 60.0), parser.getTime("5s", logger, WrkSummarizerParser.TimeUnit.MINUTE));
    assertEquals((int) Math.floor((5 * 60) / 60.0), parser.getTime("5m", logger, WrkSummarizerParser.TimeUnit.MINUTE));
    assertEquals((int) Math.floor((60 * 60) / 60.0), parser.getTime("1h", logger, WrkSummarizerParser.TimeUnit.MINUTE));

    // hour
    assertEquals(0, parser.getTime("5ms", logger, WrkSummarizerParser.TimeUnit.HOUR));
    assertEquals((int) Math.floor(5 / (60.0 * 60.0)), parser.getTime("5s", logger, WrkSummarizerParser.TimeUnit.HOUR));
    assertEquals((int) Math.floor((5 * 60) / (60.0 * 60.0)), parser.getTime("5m", logger, WrkSummarizerParser.TimeUnit.HOUR));
    assertEquals((int) Math.floor((60 * 60) / (60.0 * 60.0)), parser.getTime("1h", logger, WrkSummarizerParser.TimeUnit.HOUR));
  }
}
