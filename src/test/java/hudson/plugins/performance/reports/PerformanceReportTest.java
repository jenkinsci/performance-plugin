package hudson.plugins.performance.reports;

import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.util.StreamTaskListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PerformanceReportTest {
    public static final String DEFAULT_PERCENTILES = "0,50,90,95,100";

    private PerformanceReport performanceReport;

    @BeforeEach
    void setUp() throws Exception {
        PerformanceBuildAction buildAction = Mockito.mock(PerformanceBuildAction.class);
        performanceReport = new PerformanceReport(DEFAULT_PERCENTILES);
        performanceReport.setBuildAction(buildAction);
    }

    @Test
    void testAddSample() throws Exception {
        PrintStream printStream = Mockito.mock(PrintStream.class);
        Mockito.when(
                performanceReport.getBuildAction().getHudsonConsoleWriter())
                .thenReturn(printStream);
        printStream
                .println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");

        HttpSample sample1 = new HttpSample();
        sample1.setDate(new Date());
        performanceReport.addSample(sample1);

        sample1.setUri("invalidCharacter/");
        performanceReport.addSample(sample1);
        UriReport uriReport = performanceReport.getUriReportMap().get(
                "invalidCharacter_");
        assertNotNull(uriReport);

        String uri = "uri";
        sample1.setUri(uri);
        performanceReport.addSample(sample1);
        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        uriReport = uriReportMap.get(uri);
        assertNotNull(uriReport);
        List<Long> durations = uriReport.getDurations();
        assertEquals(1, durations.size());
        assertEquals(sample1.getUri(), uriReport.getUri());
    }

    @Test
    void testAddTaurusSample() throws Exception {
        PrintStream printStream = Mockito.mock(PrintStream.class);
        Mockito.when(
                performanceReport.getBuildAction().getHudsonConsoleWriter())
                .thenReturn(printStream);
        printStream
                .println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");

        TaurusFinalStats sample = new TaurusFinalStats();
        performanceReport.addSample(sample, true);
        assertEquals(0, performanceReport.countErrors());
    }

    @Test
    void testPerformanceReport() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResults.jtl").toURI()));
        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(2, uriReportMap.size());
        String loginUri = "Home";
        UriReport firstUriReport = uriReportMap.get(loginUri);
        assertEquals(loginUri, firstUriReport.getUri());
        assertEquals(14720, firstUriReport.getDurations().get(0).longValue());
        assertEquals(1296846792004L, firstUriReport.getStart().getTime());
        assertFalse(firstUriReport.isFailed());
        String logoutUri = "Workgroup";
        UriReport secondUriReport = uriReportMap.get(logoutUri);
        assertEquals(logoutUri, secondUriReport.getUri());
        assertEquals(278, secondUriReport.getDurations().get(0).longValue());
        assertEquals(1296846969096L + 58L, secondUriReport.getEnd().getTime());
        assertFalse(secondUriReport.isFailed());
    }

    private PerformanceReport parseOneJMeter(File f) throws IOException {
        return new JMeterParser("", DEFAULT_PERCENTILES).parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out, StandardCharsets.UTF_8)).iterator().next();
    }

    private PerformanceReport parseOneJUnit(File f) throws IOException {
        return new JUnitParser("", PerformanceReportTest.DEFAULT_PERCENTILES).parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out, StandardCharsets.UTF_8)).iterator().next();
    }

    @Test
    void testPerformanceNonHTTPSamplesMultiThread() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsMultiThread.jtl").toURI()));

        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(1, uriReportMap.size());

        String uri = "WebService(SOAP) Request";
        UriReport report = uriReportMap.get(uri);
        assertNotNull(report);

        int[] expectedDurations = {894, 1508, 1384, 1581, 996};
        for (int i = 0; i < expectedDurations.length; i++) {
            final Long duration = report.getDurations().get(i);
            assertEquals(expectedDurations[i], duration.intValue());
        }
    }

    @Test
    void testPerformanceReportJUnit() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJUnit(new File(getClass().getResource("/TEST-JUnitResults.xml").toURI()));
        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(5, uriReportMap.size());
        String firstUri = "hudson.plugins.performance.UriReportTest.testGetMin";
        UriReport firstUriReport = uriReportMap.get(firstUri);
        assertEquals(firstUri, firstUriReport.getUri());
        assertEquals(31, firstUriReport.getDurations().get(0).longValue());
        assertEquals(0L, firstUriReport.getStart().getTime());
        assertFalse(firstUriReport.isFailed());
        String lastUri = "hudson.plugins.performance.UriReportTest.testGetMax";
        UriReport secondUriReport = uriReportMap.get(lastUri);
        assertEquals(lastUri, secondUriReport.getUri());
        assertEquals(26, secondUriReport.getDurations().get(0).longValue());
        assertEquals(0L, secondUriReport.getStart().getTime());
        assertTrue(secondUriReport.isFailed());
    }

    @Test
    void testIssue5571() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJUnit(new File(getClass().getResource("/jUnitIssue5571.xml").toURI()));

        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(1, uriReportMap.size());
        String uri = "junit.framework.JUnit4TestCaseFacade.unknown";
        UriReport report = uriReportMap.get(uri);
        assertEquals(uri, report.getUri());
        assertEquals(890, report.getDurations().get(0).longValue());
        assertEquals(50, report.getDurations().get(1).longValue());
        assertEquals(0L, report.getStart().getTime());
        assertFalse(report.isFailed());
        assertEquals(33, report.getMedian());
    }

    @Test
    void testPerformanceReportMultiLevel() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsMultiLevel.jtl").toURI()));
        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(2, uriReportMap.size());
        UriReport report = uriReportMap.get("Home");
        assertNotNull(report);
    }

    @Test
    void testGetUriListOrdered() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsRandomUri.jtl").toURI()));
        List<UriReport> uriReports = performanceReport.getUriListOrdered();
        assertEquals("Ant", uriReports.get(0).getUri());
    }

    @Test
    void testCanGetCorrect90LineValue() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsTenSamples.jtl").toURI()));
        assertEquals(9L, performanceReport.get90Line());
    }

    @Test
    void testCanGetCorrect95LineValue() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsTenSamples.jtl").toURI()));
        assertEquals(9L, performanceReport.get95Line());
    }

    @Test
    void testCanGetCorrect90LineValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(2L, performanceReport.get90Line());
    }

    @Test
    void testCanGetCorrect95LineValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(2L, performanceReport.get95Line());
    }

    @Test
    void testCanGetCorrectMaxValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(3L, performanceReport.getMax());
    }

    @Test
    void testCanGetCorrectMinValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(1L, performanceReport.getMin());
    }

    @Test
    void testCanGetZeroPercentileDurationForEmptySampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/emptyfile.jtl").toURI()));
        assertEquals(0L, performanceReport.getMin());
    }

    @Test
    void testCanGetZeroPercentileDurationFromOneSampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsOneSample.jtl").toURI()));
        assertEquals(2L, performanceReport.getDurationAt(0));
    }

    @Test
    void testCanGetOneHundredPercentileDurationFromOneSampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsOneSample.jtl").toURI()));
        assertEquals(2L, performanceReport.getDurationAt(100));
    }

    @Test
    void testCannotGetDurationForNegativePercentile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertThrows(IllegalArgumentException.class, () ->
            performanceReport.getDurationAt(-1));
    }

    @Test
    void testCannotGetDurationForMoreThan100Percentile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertThrows(IllegalArgumentException.class, () ->
            performanceReport.getDurationAt(101));
    }

    @Test
    void testCompare() {
        PerformanceReport report = new PerformanceReport(DEFAULT_PERCENTILES);
        report.setReportFileName("aaaaaa");
        PerformanceReport report1 = new PerformanceReport(DEFAULT_PERCENTILES);
        report1.setReportFileName("bbbbb");
        assertEquals(-1, report.compareTo(report1));
        assertEquals(1, report1.compareTo(report));
        assertEquals(0, report1.compareTo(report1));
    }

    @Test
    void testExcludeResponseTimeOfErroredSamples() throws Exception {
        PerformanceReport report = new PerformanceReport(DEFAULT_PERCENTILES);
        report.setExcludeResponseTime(false);

        HttpSample sample1 = new HttpSample();
        sample1.setUri("");
        sample1.setDate(new Date());
        sample1.setDuration(100);
        sample1.setSuccessful(true);
        report.addSample(sample1);

        HttpSample sample2 = new HttpSample();
        sample2.setUri("");
        sample2.setDate(new Date());
        sample2.setDuration(100);
        sample2.setSuccessful(false);
        report.addSample(sample2);

        assertEquals(100, report.getAverage());
        assertEquals(100, report.getUriReportMap().get("").getAverage());


        report = new PerformanceReport(DEFAULT_PERCENTILES);
        report.setExcludeResponseTime(true);
        report.addSample(sample1);
        report.addSample(sample2);

        HttpSample sample3 = new HttpSample();
        sample3.setUri("");
        sample3.setDate(new Date());
        sample3.setDuration(300);
        sample3.setSuccessful(true);
        report.addSample(sample3);

        HttpSample sample4 = new HttpSample();
        sample4.setUri("");
        sample4.setDate(new Date());
        sample4.setDuration(100000);
        sample4.setSuccessful(false);
        report.addSample(sample4);

        assertEquals(100, report.getAverage());
        assertEquals(100, report.getUriReportMap().get("").getAverage());

        report = new PerformanceReport(DEFAULT_PERCENTILES);
        report.setExcludeResponseTime(true);
        report.addSample(sample2);
        report.addSample(sample4);
        assertEquals(0, report.getAverage());
    }

    @Test
    void testDivisionByZero() throws Exception {
        PerformanceReport report = new PerformanceReport(DEFAULT_PERCENTILES);
        report.setExcludeResponseTime(false);

        TaurusFinalStats stats = new TaurusFinalStats();
        stats.setLabel("aaaa");
        stats.setFail(5);
        stats.setSucc(5);
        stats.setTestDuration(10d);

        report.addSample(stats, true);
        assertEquals(10, report.getThroughput().longValue());
    }
}
