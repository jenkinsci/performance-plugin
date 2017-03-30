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

    public static ParserType detect(File reportFile) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        String line = reader.readLine();
        if (line == null) {
            throw new RuntimeException("File " + reportFile.getName() + " is empty");
        }

        if (line.startsWith("<?xml")) {
            return detectXMLFileType(reader);
        } else if (isIagoFileType(line)) {
            return ParserType.IAGO;
        } else if (isWRKFileType(line)) {
            return ParserType.WRK;
        } else if (isJMeterCSVFileType(line)) {
            return ParserType.JMETER_CSV;
        }

        return null;
    }

    private static boolean isIagoFileType(String line) {
        String patternString = "INF \\[.*\\] stats:.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }

    private static boolean isWRKFileType(String line) {
        String patternString = "Running .*s test @.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }

    private static boolean isJMeterCSVFileType(String line) {
//        JMeterCsvParser.DEFAULT_CSV_FORMAT;
        return false;
    }

    private static ParserType detectXMLFileType(final BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new RuntimeException("File contains only xml header");
        }

        if (line.contains("<testResults")) {
            return ParserType.JMETER;
        } else if (line.contains("<testsuite")) {
            return ParserType.JUNIT;
        } else if (line.contains("<FinalStatus>")) {
            return ParserType.TAURUS;
        } else {
            throw new RuntimeException("Unknown xml file format");
        }
    }
}
