package hudson.plugins.performance.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
            return detectXMLFile(reader);
        } else if (line.startsWith("INF [")) {
            return ParserType.IAGO;
        } else if (line.startsWith("Running ")) {
            return ParserType.WRK;
        } else if (isJMeterCSVHeader(line)) {
            return ParserType.JMETER_CSV;
        }

        return null;
    }

    private static boolean isJMeterCSVHeader(String line) {
        return false;
    }

    private static ParserType detectXMLFile(final BufferedReader reader) throws IOException {
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
