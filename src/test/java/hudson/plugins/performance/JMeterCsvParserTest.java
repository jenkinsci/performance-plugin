package hudson.plugins.performance;

import org.junit.Test;

import java.io.File;

import static hudson.plugins.performance.JMeterCsvParser.*;
import static org.junit.Assert.*;

public class JMeterCsvParserTest {
  private static final Boolean SKIP_FIRST_LINE = true;
  private static final String TEST_FILE_PATTERN = "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes";
  private static final String NO_GLOB = null;

  @Test
  public void canParseCsvFile() throws Exception {
    final File reportFile = new File(getClass().getResource("/JMeterCsvResults.csv").toURI());

    final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB, TEST_FILE_PATTERN, DEFAULT_DELIMITER, SKIP_FIRST_LINE);
    final PerformanceReport result = parser.parse(reportFile);

    // Verify results.
    assertNotNull(result);
    assertEquals("The source file contains three samples. These should all have been added to the performance report.",
        3, result.size());
  }
}