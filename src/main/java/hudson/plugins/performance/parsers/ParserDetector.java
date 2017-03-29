package hudson.plugins.performance.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Auto-detect parser for file
 */
public class ParserDetector {

    public static enum ParserType {
        IAGO, JMETER_CSV, JMETER, JMETER_SUMMARIZER, JUNIT, TAURUS, WRK
    }

    public static ParserType detect(File reportFile) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        String line = reader.readLine();

        return null;
    }
}
