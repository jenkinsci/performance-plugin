package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class LocustParser extends AbstractParser {
    enum ReportColumns {
        Type(0), Name(1), Requests(2), Failures(3), Median(4),
        Average(5), Min(6), Max(7), AvgContentSize(8), Rps(9);

        int column;

        ReportColumns(final int order) {
            this.column = order;
        }

        int getColumn() {
            return column;
        }
    }

    public LocustParser(String glob, String percentiles) {
        super(glob, percentiles, PerformanceReport.INCLUDE_ALL);
    }

    @DataBoundConstructor
    public LocustParser(String glob, String percentiles, String filterRegex) {
        super(glob, percentiles, filterRegex);
    }

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "Locust";
        }
    }

    @Override
    PerformanceReport parse(final File reportFile) throws Exception {
        PerformanceReport report = createPerformanceReport();
        report.setReportFileName(reportFile.getName());
        report.setExcludeResponseTime(excludeResponseTime);
        report.setShowTrendGraphs(showTrendGraphs);
        List<CSVRecord> reportData = getCsvData(reportFile);
        final Date now = new Date();

        for (CSVRecord record : reportData) {
            String name = record.get(ReportColumns.Name.getColumn());
            long average = Double.valueOf(record.get(ReportColumns.Average.getColumn())).longValue();
            long min = Double.valueOf(record.get(ReportColumns.Min.getColumn())).longValue();
            long max = Double.valueOf(record.get(ReportColumns.Max.getColumn())).longValue();
            long failures = Double.valueOf(record.get(ReportColumns.Failures.getColumn())).longValue();
            long success = Double.valueOf(record.get(ReportColumns.Requests.getColumn())).longValue();
            long errors = Double.valueOf(failures / success).longValue();
            long avgContentSize = Double.valueOf(record.get(ReportColumns.AvgContentSize.getColumn())).longValue();

            if (name.equals("Aggregated")) {
                report.setSummarizerSize(reportData.size() - 1);
                report.setSummarizerAvg(average);
                report.setSummarizerMin(min);
                report.setSummarizerMax(max);
                report.setSummarizerErrors(Float.toString(errors));
            } else {
                HttpSample sample = new HttpSample();
                sample.setSuccessful(failures == 0);
                sample.setSummarizer(true);
                sample.setUri(name);
                sample.setSummarizerMax(max);
                sample.setSummarizerMin(min);
                sample.setDuration(average);
                sample.setSummarizerSamples(success);
                sample.setSummarizerErrors(errors);
                sample.setSizeInKb(avgContentSize * success);
                sample.setDate(now);
                report.addSample(sample);
            }
        }

        return report;
    }

    List<CSVRecord> getCsvData(final File reportFile) {
        List<CSVRecord> records = null;
        try (Reader reader = new BufferedReader(new FileReader(reportFile));
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().build())) {
            records = csvParser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*_stats.csv";
    }
}
