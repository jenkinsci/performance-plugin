package hudson.plugins.performance;

import hudson.Extension;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class JMeterCsvParser extends AbstractParser {

  public static final String DEFAULT_DELIMITER = ",";
  public static final String DEFAULT_CSV_FORMAT = "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,Latency";
  public static final String COMMAS_NOT_INSIDE_QUOTES = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
  private static final boolean DONT_SKIP_FIRST_LINE = false;
  public final boolean skipFirstLine;
  public final String delimiter;
  public int timestampIdx = -1;
  public int elapsedIdx = -1;
  public int responseCodeIdx = -1;
  public int successIdx = -1;
  public int urlIdx = -1;
  public final String pattern;

  @DataBoundConstructor
  public JMeterCsvParser(String glob, String pattern, String delimiter, Boolean skipFirstLine) throws Exception {
    super(glob);
    this.skipFirstLine = skipFirstLine;
    this.delimiter = delimiter;
    this.pattern = pattern;
    String[] fields = pattern.split(delimiter);
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      if ("timestamp".equalsIgnoreCase(field)) {
        timestampIdx = i;
      } else if ("elapsed".equalsIgnoreCase(field)) {
        elapsedIdx = i;
      } else if ("responseCode".equalsIgnoreCase(field)) {
        responseCodeIdx = i;
      } else if ("success".equalsIgnoreCase(field)) {
        successIdx = i;
      } else if ("URL".equalsIgnoreCase(field) && urlIdx < 0) {
        urlIdx = i;
      } else if ("label".equalsIgnoreCase(field) && urlIdx < 0) {
        urlIdx = i;
      }
    }
    if (timestampIdx < 0 || elapsedIdx < 0 || responseCodeIdx < 0
        || successIdx < 0 || urlIdx < 0) {
      throw new Exception("Missing required column");
    }
  }

  public JMeterCsvParser(String glob) throws Exception {
    this(glob, DEFAULT_CSV_FORMAT, DEFAULT_DELIMITER, DONT_SKIP_FIRST_LINE);
  }

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JMeterCSV";
    }

    public FormValidation doCheckDelimiter(@QueryParameter String delimiter) {
      if (delimiter == null || delimiter.isEmpty()) {
        return FormValidation.error(Messages
            .CsvParser_validation_delimiterEmpty());
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckPattern(@QueryParameter String pattern) {
      if (pattern == null || pattern.isEmpty()) {
        FormValidation.error(Messages.CsvParser_validation_patternEmpty());
      }
      Set<String> missing = new HashSet<String>();
      validatePresent(missing, pattern, "timestamp");
      validatePresent(missing, pattern, "elapsed");
      validatePresent(missing, pattern, "responseCode");
      validatePresent(missing, pattern, "success");
      validatePresent(missing, pattern, "URL");
      if (missing.isEmpty()) {
        return FormValidation.ok();
      } else {
        StringBuilder builder = new StringBuilder();
        for (String field : missing) {
          builder.append(field + ", ");
        }
        builder.setLength(builder.length() - 2);
        return FormValidation.error(Messages
            .CsvParser_validation_MissingFields() + ": " + builder.toString());
      }
    }

    private void validatePresent(Set<String> missing, String pattern, String string) {
      if (!pattern.toLowerCase().contains(string.toLowerCase())) {
        missing.add(string);
      }
    }
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.csv";
  }

  // This may be unnecessary. I tried many things getting the pattern to show up
  // correctly in the UI and this was one of them.
  public String getDefaultPattern() {
    return "timestamp,elapsed,responseCode,threadName,success,failureMessage,grpThreads,allThreads,URL,Latency,SampleCount,ErrorCount";
  }

  @Override
  PerformanceReport parse(File reportFile) throws Exception {
    final PerformanceReport report = new PerformanceReport();
    report.setReportFileName(reportFile.getName());

    final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    try {
      String line = reader.readLine();
      if (line != null && (skipFirstLine || isFirstLineHeaderLine(line))) {
        // skip the header line
        line = reader.readLine();
      }
      while (line != null) {
        final HttpSample sample = getSample(line);
        if (sample != null) {
          try {
            report.addSample(sample);
          } catch (SAXException e) {
            throw new RuntimeException("Error parsing file '" + reportFile + "': Unable to add sample for line " + line, e);
          }
        }
        line = reader.readLine();
      }

      return report;
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * If the first CSV value is a date then it definitely is not a header line.
   * @param line First line of a CSV file.
   * @return false if the first csv value is a date, else true.
     */
  private boolean isFirstLineHeaderLine(String line) {
    try {
      final String[] values = line.split(COMMAS_NOT_INSIDE_QUOTES);
      new Date(Long.valueOf(values[0]));
    }catch (Exception e){
      return true;
    }
    return false;
  }

  /**
   * Parses a single HttpSample instance from a single CSV line.
   *
   * @param line file line with the provided pattern (cannot be null).
   * @return An sample instance (never null).
   */
  private HttpSample getSample(String line) {
    final HttpSample sample = new HttpSample();
    final String[] values = line.split(COMMAS_NOT_INSIDE_QUOTES);
    sample.setDate(parseTimestamp(values[timestampIdx]));
    sample.setDuration(Long.valueOf(values[elapsedIdx]));
    sample.setHttpCode(values[responseCodeIdx]);
    sample.setSuccessful(Boolean.valueOf(values[successIdx]));
    sample.setUri(values[urlIdx]);
    return sample;
  }

  protected final static String[] DATE_FORMATS = new String[]{
    "yyyy/MM/dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.SSS"
  };


  private Date parseTimestamp(String timestamp) {
    Date result = null;

    for (String format : DATE_FORMATS) {
      try {
        result = new SimpleDateFormat(format).parse(timestamp);
      } catch (ParseException ex) {
        // ok
      }

      if (result != null) {
        break;
      }
    }

    if (result == null) {
      try {
        result = new Date(Long.valueOf(timestamp));
      } catch (NumberFormatException ex) {
        throw new RuntimeException("Cannot parse timestamp: " + timestamp +
            ". Please, use one of supported formats: " + Arrays.toString(DATE_FORMATS), ex);
      }
    }

    return result;
  }

}
