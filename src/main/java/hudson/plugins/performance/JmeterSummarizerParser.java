package hudson.plugins.performance;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Parses JMeter Summarized results
 *
 * @author Agoley
 */
public class JmeterSummarizerParser extends AbstractParser {

  public final String logDateFormat;

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JmeterSummarizer";
    }
  }

  @DataBoundConstructor
  public JmeterSummarizerParser(String glob, String logDateFormat) {
    super(glob);

    if (logDateFormat == null || logDateFormat.length() == 0) {
      this.logDateFormat = getDefaultDatePattern();
    } else {
      this.logDateFormat = logDateFormat;
    }
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.log";
  }

  public String getDefaultDatePattern() {
    return "yyyy/mm/dd HH:mm:ss";
  }

  @Override
  PerformanceReport parse(File reportFile) throws Exception {
    final PerformanceReport report = new PerformanceReport();
    report.setReportFileName(reportFile.getName());
    report.setReportFileName(reportFile.getName());

    Scanner s = null;
    try {
      s = new Scanner(reportFile);
      String key;
      String line;
      SimpleDateFormat dateFormat = new SimpleDateFormat(logDateFormat);

      while (s.hasNextLine()) {
        line = s.nextLine().replaceAll("=", " ");
        if (line.contains("+") && line.contains("jmeter.reporters.Summariser:")) {
          Scanner scanner = null;
          try {
            scanner = new Scanner(line);
            final Pattern delimiter = scanner.delimiter();
            scanner.useDelimiter("INFO"); // as jmeter logs INFO mode
            final HttpSample sample = new HttpSample();
            final String dateString = scanner.next();
            sample.setDate(dateFormat.parse(dateString));
            scanner.findInLine("jmeter.reporters.Summariser:");
            scanner.useDelimiter("\\+");
            key = scanner.next().trim();
            scanner.useDelimiter(delimiter);
            scanner.next();
            sample.setSummarizerSamples(scanner.nextLong()); // set SamplesCount
            scanner.findInLine("Avg:"); // set response time
            sample.setDuration(scanner.nextLong());
            sample.setSuccessful(true);
            scanner.findInLine("Min:"); // set MIN
            sample.setSummarizerMin(scanner.nextLong());
            scanner.findInLine("Max:"); // set MAX
            sample.setSummarizerMax(scanner.nextLong());
            scanner.findInLine("Err:"); // set errors count
            sample.setSummarizerErrors(scanner.nextInt());
            // sample.setSummarizerErrors(
            // Float.valueOf(scanner.next().replaceAll("[()%]","")));
            sample.setUri(key);
            report.addSample(sample);
          } finally {
            if (scanner != null) scanner.close();
          }
        }
      }

      return report;
    } finally {
      if (s != null) s.close();
    }
  }
}