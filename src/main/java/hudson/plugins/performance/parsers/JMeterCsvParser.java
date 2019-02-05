package hudson.plugins.performance.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;

public class JMeterCsvParser extends AbstractParser {

    public char delimiter;
    public int timestampIdx = -1;
    public int elapsedIdx = -1;
    public int responseCodeIdx = -1;
    public int successIdx = -1;
    public int urlIdx = -1;
    public int bytesIdx = -1;
    public int sentBytesIdx = -1;


    public JMeterCsvParser(String glob, String percentiles) {
        this(glob, percentiles, PerformanceReport.INCLUDE_ALL);
    }
    
    @DataBoundConstructor
    public JMeterCsvParser(String glob, String percentiles, String filterRegex) {
        super(glob, percentiles, filterRegex);
    }

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JMeterCSV";
        }
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.csv";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        clearDateFormat();

        final PerformanceReport report = createPerformanceReport();
        report.setExcludeResponseTime(excludeResponseTime);
        report.setReportFileName(reportFile.getName());

        String[] header = null;
        try (FileReader fr = new FileReader(reportFile);
                BufferedReader reader = new BufferedReader(fr)){
            String line = reader.readLine();
            if (line != null) {
                header = readCSVHeader(line);
            }
        } 

        try (Reader fileReader = new FileReader(reportFile)){
            parseCSV(fileReader, header, report);
        }

        return report;
    }

    protected void parseCSV(Reader in, String[] header, PerformanceReport report) throws IOException {
        CSVFormat csvFormat = CSVFormat.newFormat(delimiter).withHeader(header).withQuote('"').withSkipHeaderRecord();
        Iterable<CSVRecord> records = csvFormat.parse(in);
        for (CSVRecord record : records) {
            final HttpSample sample = getSample(record);
            report.addSample(sample);
        }
    }

    protected String[] readCSVHeader(String line) {
        this.delimiter = lookingForDelimiter(line);
        final String[] header = line.split(String.valueOf(delimiter));
        for (int i = 0; i < header.length; i++) {
            String field = header[i];
            if ("timestamp".equalsIgnoreCase(field)) {
                timestampIdx = i;
            } else if ("elapsed".equalsIgnoreCase(field)) {
                elapsedIdx = i;
            } else if ("responseCode".equalsIgnoreCase(field)) {
                responseCodeIdx = i;
            } else if ("success".equalsIgnoreCase(field)) {
                successIdx = i;
            } else if ("bytes".equalsIgnoreCase(field)) {
                bytesIdx = i;
            } else if ("sentBytes".equalsIgnoreCase(field)) {
                sentBytesIdx = i;
            } else if ("URL".equalsIgnoreCase(field) && urlIdx < 0) {
                urlIdx = i;
            } else if ("label".equalsIgnoreCase(field) && urlIdx < 0) {
                urlIdx = i;
            }
        }

        if (timestampIdx < 0 || elapsedIdx < 0 || responseCodeIdx < 0
                || successIdx < 0 || urlIdx < 0 || bytesIdx < 0
                // || sentBytesIdx < 0 // sentBytes was introduced in 3.1
                ) {
            throw new IllegalStateException("Missing required column");
        }

        return header;
    }

    protected static char lookingForDelimiter(String line) {
        for (char ch : line.toCharArray()) {
            if (!Character.isLetter(ch)) {
                return ch;
            }
        }
        throw new IllegalStateException("Cannot find delimiter in header " + line);
    }

    /**
     * Parses a single HttpSample instance from a single CSV Record.
     *
     * @param record csv record from report file (cannot be null).
     * @return An sample instance (never null).
     */
    private HttpSample getSample(CSVRecord record) {
        final HttpSample sample = new HttpSample();
        sample.setDate(parseTimestamp(record.get(timestampIdx)));
        sample.setDuration(Long.valueOf(record.get(elapsedIdx)));
        sample.setHttpCode(record.get(responseCodeIdx));
        sample.setSuccessful(Boolean.valueOf(record.get(successIdx)));
        long bytes = Long.parseLong(record.get(bytesIdx));
        if(sentBytesIdx != -1) {
            bytes += Long.parseLong(record.get(sentBytesIdx));
        }
        sample.setSizeInKb(Double.valueOf(bytes) / 1024d);
        sample.setUri(record.get(urlIdx));
        return sample;
    }
}
