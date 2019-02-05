package hudson.plugins.performance.parsers;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;

/**
 * Parses JMeter Summarized results
 *
 * @author Agoley
 */
public class JmeterSummarizerParser extends AbstractParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JmeterSummarizer";
        }
    }


    public JmeterSummarizerParser(String glob, String percentiles) {
        super(glob, percentiles, PerformanceReport.INCLUDE_ALL);
    }
    
    @DataBoundConstructor
    public JmeterSummarizerParser(String glob, String percentiles, String filterRegex) {
        super(glob, percentiles, filterRegex);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.log";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        clearDateFormat();

        final PerformanceReport report = createPerformanceReport();
        report.setExcludeResponseTime(excludeResponseTime);
        report.setReportFileName(reportFile.getName());

        try (Scanner fileScanner = new Scanner(reportFile)){
            String line;
            String lastEqualsLine = null;
            while (fileScanner.hasNextLine()) {
                line = fileScanner.nextLine();
                if (line.contains("=") && line.contains("Summariser:")) {
                    lastEqualsLine = line;
                }
            }

            long reportSamples = Long.MIN_VALUE;
            long reportAvg = Long.MIN_VALUE;
            long reportMin = Long.MAX_VALUE;
            long reportMax = Long.MIN_VALUE;
            String reportErrorPercent = "";
            if (lastEqualsLine != null) {
                try (Scanner lineScanner = new Scanner(lastEqualsLine)) {
                    final Pattern delimiter = lineScanner.delimiter();
                    lineScanner.useDelimiter("INFO"); // as jmeter logs INFO mode
                    final HttpSample sample = new HttpSample();
                    final String dateString = lineScanner.next();
                    sample.setDate(parseTimestamp(dateString));
                    lineScanner.findInLine("Summariser:");
                    lineScanner.useDelimiter("\\=");
                    String key = lineScanner.next().trim();
                    lineScanner.useDelimiter(delimiter);
                    lineScanner.next();
                    reportSamples = lineScanner.nextLong();
                    sample.setSummarizerSamples(reportSamples); // set SamplesCount
                    sample.setSummarizer(true);
                    lineScanner.findInLine("Avg:"); // set response time
                    sample.setDuration(lineScanner.nextLong());
                    reportAvg = sample.getDuration();
                    sample.setSuccessful(true);

                    lineScanner.findInLine("Min:"); // set MIN
                    long sampleMin = lineScanner.nextLong();
                    sample.setSummarizerMin(sampleMin);
                    reportMin = Math.min(reportMin, sampleMin);

                    lineScanner.findInLine("Max:"); // set MAX
                    long sampleMax = lineScanner.nextLong();
                    sample.setSummarizerMax(sampleMax);
                    reportMax = Math.max(reportMax, sampleMax);

                    lineScanner.findInLine("Err:"); // set errors count
                    lineScanner.findInLine("\\("); // set errors count
                    lineScanner.useDelimiter("%");
                    reportErrorPercent = lineScanner.next();
                    sample.setSummarizerErrors(Float.parseFloat(reportErrorPercent));
                    // sample.setSummarizerErrors(
                    // Float.valueOf(scanner.next().replaceAll("[()%]","")));
                    sample.setUri(key);
                    report.addSample(sample);
                }
            }

            report.setSummarizerSize(reportSamples);
            report.setSummarizerAvg(reportAvg);
            report.setSummarizerMin(reportMin);
            report.setSummarizerMax(reportMax);
            report.setSummarizerErrors(reportErrorPercent);

            return report;
        }
    }
}