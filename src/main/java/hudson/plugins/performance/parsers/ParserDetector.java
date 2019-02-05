package hudson.plugins.performance.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.google.common.annotations.VisibleForTesting;

/**
 * Auto-detect parser for file
 */
public class ParserDetector {

    private ParserDetector() {
        super();
    }
    /**
     * Detect report file type using file content.
     * @return report file type.
     */
    public static String detect(String reportPath) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new FileReader(new File(reportPath)))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("File " + reportPath + " is empty");
            }

            if (line.startsWith("<?xml")) {
                return detectXMLFileType(reportPath);
            } else if (isIagoFileType(line)) {
                return IagoParser.class.getSimpleName();
            } else if (isWRKFileType(line)) {
                return WrkSummarizerParser.class.getSimpleName();
            } else if (isJMeterCSVFileType(line)) {
                return JMeterCsvParser.class.getSimpleName();
            } else if (isJMeterSummarizerFileType(line, reader)) {
                return JmeterSummarizerParser.class.getSimpleName();
            } else if (isLoadRunnerFileType(line)) {
                return LoadRunnerParser.class.getSimpleName();
            } else {
                try {
                    return detectXMLFileType(reportPath);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Can not detect file type: " + reportPath, ex);
                }
            }
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
    private static boolean isJMeterCSVFileType(String header) {
        String line = header.toLowerCase();
        return (line.contains("timestamp") && line.contains("elapsed") &&
                (line.contains("url") || line.contains("label")));
    }

    /**
     * Detect JMeterSummarizer report type.
     * Read file, until It find a string "jmeter.reporters.Summariser: Generate Summary Results"
     */
    private static boolean isJMeterSummarizerFileType(String firstLine, final BufferedReader reader) throws IOException {
        String pattern = "Summariser: Generate Summary Results";
        String line = firstLine;
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
     * Detect LoadRunner MDB file using MS Access magic pattern.
     * http://www.garykessler.net/library/file_sigs.html
     */
    private static boolean isLoadRunnerFileType(String line) {
        String pattern = new String(new char[]{0x00, 0x01, 0x00, 0x00})+"Standard Jet DB";

        return line.length() > pattern.length() &&
            pattern.equals(line.substring(0, pattern.length()));
    }

    /**
     * Detect XML report type using the search of the first opening xml-tag.
     *  <testResults> - JMETER;
     *  <testsuite> - JUNIT;
     *  <FinalStatus> - TAURUS.
     */
    private static String detectXMLFileType(String reportPath) throws IOException {
        try (InputStream in = new FileInputStream(reportPath)) {
            return detectXMLFileType(in);
        } catch (Exception ex) {
            throw new IllegalStateException("XML parsing error: ", ex);
        }
    }

    @VisibleForTesting
    protected static String detectXMLFileType(final InputStream in) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String name = startElement.getName().getLocalPart();

                switch (name) {
                case "testResults":
                    return JMeterParser.class.getSimpleName();
                case "testsuite":
                case "testsuites":
                    return JUnitParser.class.getSimpleName();
                case "FinalStatus":
                    return TaurusParser.class.getSimpleName();
                default:
                    throw new IllegalArgumentException("Unknown xml file format");
                }
            }
        }
        throw new IllegalStateException("XML parsing error: no start element");
    }
}
