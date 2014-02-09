package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.SAXException;

public class JMeterCsvParser extends PerformanceReportParser {

  public final boolean skipFirstLine;
  public final String delimiter;
  public int timestampIdx = -1;
  public int elapsedIdx = -1;
  public int responseCodeIdx = -1;
  public int successIdx = -1;
  public int urlIdx = -1;
  public final String pattern;

  @DataBoundConstructor
  public JMeterCsvParser(String glob, String pattern, String delimiter,
      Boolean skipFirstLine) throws Exception {
    super(glob);
    this.skipFirstLine = skipFirstLine;
    this.delimiter = delimiter;
    this.pattern = pattern;
    String[] fields = pattern.split(delimiter);
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      if ("timestamp".equals(field)) {
        timestampIdx = i;
      } else if ("elapsed".equals(field)) {
        elapsedIdx = i;
      } else if ("responseCode".equals(field)) {
        responseCodeIdx = i;
      } else if ("success".equals(field)) {
        successIdx = i;
      } else if ("URL".equals(field)) {
        urlIdx = i;
      }
    }
    if (timestampIdx < 0 || elapsedIdx < 0 || responseCodeIdx < 0
        || successIdx < 0 || urlIdx < 0) {
      throw new Exception("Missing required column");
    }
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

    private void validatePresent(Set<String> missing, String pattern,
        String string) {
      if (!pattern.contains(string)) {
        missing.add(string);
      }
    }
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.csv";
  }

  // This may be unneccesary. I tried many things getting the pattern to show up
  // correctly in the UI and this was one of them.
  public String getDefaultPattern() {
    return "timestamp,elapsed,responseCode,threadName,success,failureMessage,grpThreads,allThreads,URL,Latency,SampleCount,ErrorCount";
  }

  @Override
  public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
      Collection<File> reports, TaskListener listener) throws IOException {
    List<PerformanceReport> result = new ArrayList<PerformanceReport>();

    PrintStream logger = listener.getLogger();
    for (File f : reports) {
      final PerformanceReport r = new PerformanceReport();
      r.setReportFileName(f.getName());
      logger.println("Performance: Parsing JMeter report file " + f.getName());
      BufferedReader reader = new BufferedReader(new FileReader(f));
      try {
        String line = reader.readLine();
        if (line != null && skipFirstLine) {
          logger.println("Performance: Skipping first line");
          line = reader.readLine();
        }
        while (line != null) {
          HttpSample sample = getSample(line);
          if (sample != null) {
            try {
              r.addSample(sample);
            } catch (SAXException e) {
              throw new RuntimeException("Unnable to add sample for line "
                  + line, e);
            }
          }
          line = reader.readLine();
        }
      } finally {
        if (reader != null)
          reader.close();
      }
      result.add(r);
    }
    return result;
  }

  /**
   * @param line
   *          file line with the provided pattern
   * @return
   */
  private HttpSample getSample(String line) {
    HttpSample sample = new HttpSample();
    final String commasNotInsideQuotes = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    String[] values = line.split(commasNotInsideQuotes);
    sample.setDate(new Date(Long.valueOf(values[timestampIdx])));
    sample.setDuration(Long.valueOf(values[elapsedIdx]));
    sample.setHttpCode(values[responseCodeIdx]);
    sample.setSuccessful(Boolean.valueOf(values[successIdx]));
    sample.setUri(values[urlIdx]);
    return sample;
  }

}
