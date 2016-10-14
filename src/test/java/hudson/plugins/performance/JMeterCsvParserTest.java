package hudson.plugins.performance;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static hudson.plugins.performance.JMeterCsvParser.*;
import static org.junit.Assert.*;

public class JMeterCsvParserTest {
  private static final Boolean SKIP_FIRST_LINE = true;
  private static final String TEST_FILE_PATTERN = "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes";
  private static final String NO_GLOB = null;
  private File reportFile;

  @Before
  public void beforeMethod() throws Exception {
    reportFile = new File(getClass().getResource("/JMeterCsvResults.csv").toURI());
  }

  @Test
  public void canParseCsvFile() throws Exception {
    final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB, TEST_FILE_PATTERN, DEFAULT_DELIMITER, SKIP_FIRST_LINE);
    parseAndVerifyResult(parser);
  }

  @Test
  public void canParseCsvFileWhenSkipFirstLineIsNotSpecifiedAndFirstLineHasHeader() throws Exception {
    final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB);
    parseAndVerifyResult(parser);
  }

  private void parseAndVerifyResult(JMeterCsvParser parser) throws Exception {
    final PerformanceReport result = parser.parse(reportFile);
    // Verify results.
    assertNotNull(result);
    assertEquals("The source file contains three samples. These should all have been added to the performance report.", 3, result.size());
  }
}