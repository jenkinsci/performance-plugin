package hudson.plugins.performance.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-detect parser for file
 */
public class ParserDetector {

    public enum ParserType {
        IAGO, JMETER_CSV, JMETER, JMETER_SUMMARIZER, JUNIT, TAURUS, WRK
    }

    /**
     * Detect report file type using file content.
     * @return report file type.
     */
    public static ParserType detect(File reportFile) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        String line = reader.readLine();
        if (line == null) {
            throw new IllegalArgumentException("File " + reportFile.getName() + " is empty");
        }

        if (line.startsWith("<?xml")) {
            return detectXMLFileType(reader);
        } else if (isIagoFileType(line)) {
            return ParserType.IAGO;
        } else if (isWRKFileType(line)) {
            return ParserType.WRK;
        } else if (isJMeterCSVFileType(line)) {
            return ParserType.JMETER_CSV;
        } else if (isJMeterSummarizerFileType(line, reader)) {
            return ParserType.JMETER_SUMMARIZER;
        } else {
            throw new IllegalArgumentException("Can not detect file type: " + reportFile.getName());
        }
    }

    /**
     * Detect Iago report type using pattern "INF \[.*\] stats:.*
     */
    private static boolean isIagoFileType(String line) {
        String patternString = "INF \\[.*\\] stats:.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }

    /**
     * Detect WRK report type using pattern "Running .*s test @.*"
     */
    private static boolean isWRKFileType(String line) {
        String patternString = "Running .*s test @.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }

    /**
     * Detect JMeterCSV report type using the search of main columns in CSV header,
     *  such as timestamp, elapsed and url/label
     */
    private static boolean isJMeterCSVFileType(String line) {
        line = line.toLowerCase();
        return (line.contains("timestamp") && line.contains("elapsed") &&
                (line.contains("url") || line.contains("label")));
    }

    /**
     * Detect JMeterSummarizer report type.
     * Read file, until It find a string "jmeter.reporters.Summariser: Generate Summary Results"
     */
    private static boolean isJMeterSummarizerFileType(String line, final BufferedReader reader) throws IOException {
        String pattern = "jmeter.reporters.Summariser: Generate Summary Results";
        if (line.contains(pattern)) {
            return true;
        }

        line = reader.readLine();
        while (line != null) {
            if (line.contains(pattern)) {
                return true;
            }
            line = reader.readLine();
        }
        return false;
    }

    /**
     * Detect XML report type using the search of the first opening xml-tag.
     *  <testResults> - JMETER;
     *  <testsuite> - JUNIT;
     *  <FinalStatus> - TAURUS.
     */
    private static ParserType detectXMLFileType(final BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IllegalArgumentException("File contains only xml header");
        }

        if (line.contains("<testResults")) {
            return ParserType.JMETER;
        } else if (line.contains("<testsuite")) {
            return ParserType.JUNIT;
        } else if (line.contains("<FinalStatus>")) {
            return ParserType.TAURUS;
        } else {
            throw new IllegalArgumentException("Unknown xml file format");
        }
    }
}
