package hudson.plugins.performance.parsers;

import java.io.IOException;

public class ParserFactory {

    public static PerformanceReportParser getParser(String filePath) throws IOException {
        final String parserName = ParserDetector.detect(filePath);

        if (parserName.equals(JMeterParser.class.getSimpleName())) {
            return new JMeterParser(filePath);
        } else if (parserName.equals(JMeterCsvParser.class.getSimpleName())) {
            return new JMeterCsvParser(filePath);
        } else if (parserName.equals(JUnitParser.class.getSimpleName())) {
            return new JUnitParser(filePath);
        } else if (parserName.equals(TaurusParser.class.getSimpleName())) {
            return new TaurusParser(filePath);
        } else if (parserName.equals(WrkSummarizerParser.class.getSimpleName())) {
            return new WrkSummarizerParser(filePath);
        } else if (parserName.equals(JmeterSummarizerParser.class.getSimpleName())) {
            return new JmeterSummarizerParser(filePath);
        } else if (parserName.equals(IagoParser.class.getSimpleName())) {
            return new IagoParser(filePath);
        } else {
            throw new IllegalArgumentException("Unknown parser type: " + parserName);
        }
    }

}
