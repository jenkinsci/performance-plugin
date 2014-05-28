package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Parses JMeter Summarized results
 *
 * @author Agoley
 */
public class JmeterSummarizerParser extends PerformanceReportParser {

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
  public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
      Collection<File> reports, TaskListener listener) {

    List<PerformanceReport> result = new ArrayList<PerformanceReport>();
    PrintStream logger = listener.getLogger();

    for (File f : reports) {
      Scanner s = null;
      try {
        final PerformanceReport r = new PerformanceReport();
        r.setReportFileName(f.getName());
        r.setReportFileName(f.getName());

        s = new Scanner(f);
        String key;
        String line;
        SimpleDateFormat dateFormat = new SimpleDateFormat(logDateFormat);

        logger.println("Performance: Parsing JMeterSummarizer report file " + f.getName());
        while (s.hasNextLine()) {
          line = s.nextLine().replaceAll("=", " ");
          if (line.contains("+") && line.contains("jmeter.reporters.Summariser:")) {
            Scanner scanner = null;
            try {
                    scanner = new Scanner(line);
                    Pattern delimiter = scanner.delimiter();
                    scanner.useDelimiter("INFO"); // as jmeter logs INFO mode
                    HttpSample sample = new HttpSample();
                    String dateString = scanner.next();
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
                    r.addSample(sample);
            } finally {
              if (scanner != null) scanner.close();
            }
          }
        }
        result.add(r);
      } catch (FileNotFoundException e) {
        logger.println("File not found" + e.getMessage());
      } catch (SAXException e) {
        logger.println(e.getMessage());
      } catch (ParseException e) {
        logger.println(e.getMessage());
      } finally {
        if (s != null) s.close();
      }
    }
    return result;
  }

}