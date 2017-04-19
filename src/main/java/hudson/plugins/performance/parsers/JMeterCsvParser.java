package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class JMeterCsvParser extends AbstractParser {

    public char delimiter;
    public int timestampIdx = -1;
    public int elapsedIdx = -1;
    public int responseCodeIdx = -1;
    public int successIdx = -1;
    public int urlIdx = -1;


    @DataBoundConstructor
    public JMeterCsvParser(String glob) {
        super(glob);
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
        this.dateFormat = null;
        this.isNumberDateFormat = false;

        final PerformanceReport report = new PerformanceReport();
        report.setReportFileName(reportFile.getName());

        String[] header = null;
        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        try {
            String line = reader.readLine();
            if (line != null) {
                header = readCSVHeader(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        Reader fileReader = new FileReader(reportFile);
        try {
            parseCSV(fileReader, header, report);
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }

        return report;
    }

    protected void parseCSV(Reader in, String[] header, PerformanceReport report) throws IOException {
        CSVFormat csvFormat = CSVFormat.newFormat(delimiter).withHeader(header).withQuote('"').withSkipHeaderRecord();
        Iterable<CSVRecord> records = csvFormat.parse(in);
        for (CSVRecord record : records) {
            final HttpSample sample = getSample(record);
            try {
                report.addSample(sample);
            } catch (SAXException e) {
                throw new RuntimeException("Error parsing file '" + report.getReportFileName() + "': Unable to add sample for CSVRecord " + record, e);

            }
        }
    }

    protected String[] readCSVHeader(String line) throws Exception {
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

        return header;
    }

    protected static char lookingForDelimiter(String line) throws Exception {
        for (char ch : line.toCharArray()) {
            if (!Character.isLetter(ch)) {
                return ch;
            }
        }
        throw new Exception("Cannot find delimiter in header " + line);
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
        sample.setUri(record.get(urlIdx));
        return sample;
    }

    private boolean isNumberDateFormat = false;
    private SimpleDateFormat dateFormat;

    protected final static String[] DATE_FORMATS = new String[]{
            "yyyy/MM/dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.SSS"
    };


    private Date parseTimestamp(String timestamp) {
        if (this.dateFormat == null) {
            initDateFormat(timestamp);
        }

        try {
            return isNumberDateFormat ?
                    new Date(Long.valueOf(timestamp)) :
                    dateFormat.parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse timestamp: " + timestamp +
                    ". Please, use one of supported formats: " + Arrays.toString(DATE_FORMATS), e);
        }
    }

    private void initDateFormat(String timestamp) {
        Date result = null;
        for (String format : DATE_FORMATS) {
            try {
                dateFormat = new SimpleDateFormat(format);
                result = dateFormat.parse(timestamp);
            } catch (ParseException ex) {
                // ok
                dateFormat = null;
            }

            if (result != null) {
                break;
            }
        }

        if (result == null) {
            try {
                Long.valueOf(timestamp);
                isNumberDateFormat = true;
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Cannot parse timestamp: " + timestamp +
                        ". Please, use one of supported formats: " + Arrays.toString(DATE_FORMATS), ex);
            }
        }
    }

}
