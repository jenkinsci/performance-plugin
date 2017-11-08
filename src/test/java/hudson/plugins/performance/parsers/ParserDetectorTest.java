package hudson.plugins.performance.parsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.*;


public class ParserDetectorTest {


    @Test
    public void testFlow() throws Exception {
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertEquals(TaurusParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertEquals(JMeterParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertEquals(JUnitParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertEquals(IagoParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertEquals(WrkSummarizerParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertEquals(JMeterCsvParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertEquals(JmeterSummarizerParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/lr-session.mdb").toURI().getPath();
        assertEquals(LoadRunnerParser.class.getSimpleName(), ParserDetector.detect(filePath));
    }

    @Issue("JENKINS-44317")
    @Test
    public void testIssue44317() throws Exception {
        String filePath = getClass().getResource("/TEST-results.xml").toURI().getPath();
        assertEquals(JUnitParser.class.getSimpleName(), ParserDetector.detect(filePath));
    }

    @Issue("JENKINS-45723")
    @Test
    public void testIssue45723() throws Exception {
        String filePath = getClass().getResource("/TEST-JUnitResults-success-failure-error.xml").toURI().getPath();
        assertEquals(JUnitParser.class.getSimpleName(), ParserDetector.detect(filePath));
    }

    @Issue("JENKINS-47808")
    @Test
    public void testIssue() throws Exception {
        assertEquals(JMeterParser.class.getSimpleName(), ParserDetector.detectXMLFileType(getHugeJMeterInputStream()));
    }


    public static InputStream getHugeJMeterInputStream() {
        return new SequenceInputStream(getPrefixInputStream(), getInfiniteSampleInputStream());
    }

    private static ByteArrayInputStream getPrefixInputStream() {
        return new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testResults version=\"1.2\">".getBytes(
                StandardCharsets.UTF_8));
    }

    private static InputStream getInfiniteSampleInputStream() {
        return repeat("<httpSample t=\"289\" lt=\"289\" ts=\"1509447454646\" s=\"true\" lb=\"test\" rc=\"200\" rm=\"OK\" tn=\"test\" dt=\"text\" by=\"1410\"/>".getBytes(
                StandardCharsets.UTF_8), Integer.MAX_VALUE);
    }

    public static InputStream repeat(final byte[] sample, final int times) {
        return new InputStream() {
            private long pos = 0;
            private final long total = (long)sample.length * times;

            public int read() throws IOException {
                return pos < total ? sample[(int)(pos++ % sample.length)] : -1;
            }
        };
    }
}