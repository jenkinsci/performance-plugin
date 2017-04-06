package hudson.plugins.performance.reports;

import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.util.StreamTaskListener;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PerformanceReportTest {

    private PerformanceReport performanceReport;

    @Before
    public void setUp() throws Exception {
        PerformanceBuildAction buildAction = EasyMock
                .createMock(PerformanceBuildAction.class);
        performanceReport = new PerformanceReport();
        performanceReport.setBuildAction(buildAction);
    }

    @Test
    public void testAddSample() throws Exception {
        PrintStream printStream = EasyMock.createMock(PrintStream.class);
        EasyMock.expect(
                performanceReport.getBuildAction().getHudsonConsoleWriter())
                .andReturn(printStream);
        printStream
                .println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
        EasyMock.replay(printStream);
        EasyMock.replay(performanceReport.getBuildAction());

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
    public void testAddTaurusSample() throws Exception {
        PrintStream printStream = EasyMock.createMock(PrintStream.class);
        EasyMock.expect(
                performanceReport.getBuildAction().getHudsonConsoleWriter())
                .andReturn(printStream);
        printStream
                .println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
        EasyMock.replay(printStream);
        EasyMock.replay(performanceReport.getBuildAction());

        TaurusFinalStats sample = new TaurusFinalStats();
        performanceReport.addSample(sample, true);
        assertEquals(0, performanceReport.countErrors());
    }

    @Test
    public void testPerformanceReport() throws IOException, URISyntaxException {
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
        return new JMeterParser("").parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out)).iterator().next();
    }

    private PerformanceReport parseOneJUnit(File f) throws IOException {
        return new JUnitParser("").parse(null, Collections.singleton(f),
                new StreamTaskListener(System.out)).iterator().next();
    }

    @Test
    public void testPerformanceNonHTTPSamplesMultiThread() throws IOException, URISyntaxException {
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
    public void testPerformanceReportJUnit() throws IOException, URISyntaxException {
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
    public void testIssue5571() throws IOException, URISyntaxException {
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
    public void testPerformanceReportMultiLevel() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsMultiLevel.jtl").toURI()));
        Map<String, UriReport> uriReportMap = performanceReport
                .getUriReportMap();
        assertEquals(2, uriReportMap.size());
        UriReport report = uriReportMap.get("Home");
        assertNotNull(report);
    }

    @Test
    public void testGetUriListOrdered() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsRandomUri.jtl").toURI()));
        List<UriReport> uriReports = performanceReport.getUriListOrdered();
        assertEquals("Ant", uriReports.get(0).getUri());
    }

    @Test
    public void testCanGetCorrect90LineValue() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsTenSamples.jtl").toURI()));
        assertEquals(9L, performanceReport.get90Line());
    }

    @Test
    public void testCanGetCorrect90LineValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(2L, performanceReport.get90Line());
    }

    @Test
    public void testCanGetCorrectMaxValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(3L, performanceReport.getMax());
    }

    @Test
    public void testCanGetCorrectMinValueWithThreeSamples() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        assertEquals(1L, performanceReport.getMin());
    }

    @Test
    public void testCanGetZeroPercentileDurationForEmptySampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/emptyfile.jtl").toURI()));
        assertEquals(0L, performanceReport.getMin());
    }

    @Test
    public void testCanGetZeroPercentileDurationFromOneSampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsOneSample.jtl").toURI()));
        assertEquals(2L, performanceReport.getDurationAt(0));
    }

    @Test
    public void testCanGetOneHundredPercentileDurationFromOneSampleFile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsOneSample.jtl").toURI()));
        assertEquals(2L, performanceReport.getDurationAt(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotGetDurationForNegativePercentile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        performanceReport.getDurationAt(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotGetDurationForMoreThan100Percentile() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = parseOneJMeter(new File(getClass().getResource("/JMeterResultsThreeSamples.jtl").toURI()));
        performanceReport.getDurationAt(101);
    }

    @Test
    public void testCompare() {
        PerformanceReport report = new PerformanceReport();
        report.setReportFileName("aaaaaa");
        PerformanceReport report1 = new PerformanceReport();
        report1.setReportFileName("bbbbb");
        assertEquals(-1, report.compareTo(report1));
        assertEquals(1, report1.compareTo(report));
        assertEquals(0, report1.compareTo(report1));
    }
}
