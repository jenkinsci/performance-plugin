package hudson.plugins.performance.reports;

import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.parsers.TaurusParser;
import hudson.util.StreamTaskListener;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static hudson.plugins.performance.reports.PerformanceReportTest.DEFAULT_PERCENTILES;
import static org.junit.Assert.assertEquals;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputReportTest {

    private final static double DELTA = 0.000001;
    private PerformanceReport performanceReport = new PerformanceReport(DEFAULT_PERCENTILES);

    private ThroughputReport throughputReport = new ThroughputReport(performanceReport);

    @Test
    public void shouldReturnZeroIfNoUri() {
        assertEquals(0.0, throughputReport.get(), DELTA);
    }

    @Test
    public void shouldSummarizeThroughputByDifferentUri() {
        HttpSample httpSample1 = new HttpSample();
        Date date = new Date();
        httpSample1.setDate(date);

        UriReport uriReport1 = new UriReport(performanceReport, "f", "url1");
        uriReport1.addHttpSample(httpSample1);

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(date);

        UriReport uriReport2 = new UriReport(performanceReport, "f", "url2");
        uriReport2.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().clear();
        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        assertEquals(2.0, throughputReport.get(), DELTA);
    }

    @Test
    public void shouldSummarizeThroughputUnder1ByDifferentUri() {
        HttpSample httpSample1 = new HttpSample();
        Date date = new Date();
        httpSample1.setDate(date);
        httpSample1.setDuration(1100);

        PerformanceReport report = new PerformanceReport(DEFAULT_PERCENTILES);

        UriReport uriReport1 = new UriReport(report, "f", "url1");
        uriReport1.addHttpSample(httpSample1);

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(date);
        httpSample2.setDuration(1100);

        UriReport uriReport2 = new UriReport(report, "f", "url2");
        uriReport2.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().clear();
        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        assertEquals(2.0 / 1100 * 1000, throughputReport.get(), DELTA);
    }

    @Test
    public void testThroughputJMeterReport() throws Exception {
        long time = System.currentTimeMillis();

        HttpSample firstSample = new HttpSample();
        firstSample.setDate(new Date(time));
        firstSample.setDuration(1000);

        HttpSample secondSample = new HttpSample();
        secondSample.setDate(new Date(time + 2000));
        secondSample.setDuration(1000);

        HttpSample thirdSample = new HttpSample();
        thirdSample.setDate(new Date(time + 3000));
        thirdSample.setDuration(1000);

        HttpSample lastSample = new HttpSample();
        lastSample.setDate(new Date(time + 8000));
        lastSample.setDuration(1000);


        PerformanceReport report = new PerformanceReport(DEFAULT_PERCENTILES);

        UriReport uriReport1 = new UriReport(report, "f", "url1");
        uriReport1.addHttpSample(firstSample);
        uriReport1.addHttpSample(thirdSample);


        UriReport uriReport2 = new UriReport(report, "f", "url2");
        uriReport2.addHttpSample(secondSample);
        uriReport2.addHttpSample(lastSample);

        performanceReport.getUriReportMap().clear();
        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        assertEquals((4 / (9000.0 / 1000)), throughputReport.get(), DELTA);
    }

    @Test
    public void testThroughputTaurusReport() throws Exception {
        performanceReport.getUriReportMap().clear();

        TaurusFinalStats stats = new TaurusFinalStats();
        stats.setThroughput(777);
        stats.setLabel("777");
        performanceReport.addSample(stats, true);

        assertEquals(777, throughputReport.get(), DELTA);
    }

    @Test
    public void testDuration() throws IOException, URISyntaxException {
        File report = new File(getClass().getResource("/TaurusXmlWithDuration.xml").getPath());
        TaurusParser parser = new TaurusParser(report.getAbsolutePath(), DEFAULT_PERCENTILES);
        PerformanceReport performanceReport = parser.parse(null, Collections.singleton(report), new StreamTaskListener(System.out)).iterator().next();
        ThroughputReport throughputReport = new ThroughputReport(performanceReport);

        Map<String, UriReport> uriReportMap = performanceReport.getUriReportMap();
        assertEquals(3, uriReportMap.size());

        int samplesCount = 29658 + 29656;
        long duration = (long) Math.ceil((float)3141 / 1000);
        assertEquals(samplesCount/ duration, throughputReport.get(), DELTA);
    }

    @Test
    public void testDurationBackwardCompatibility() throws IOException, URISyntaxException {
        PerformanceReport performanceReport = new PerformanceReport(DEFAULT_PERCENTILES);

        UriReport report1 = new UriReport(performanceReport, "f1", "uri1");
        report1.setThroughput(111L);
        UriReport report2 = new UriReport(performanceReport, "f2", "uri2");
        report2.setThroughput(666L);

        performanceReport.getUriReportMap().put("f1", report1);
        performanceReport.getUriReportMap().put("f2", report2);

        performanceReport.readResolve();
        ThroughputReport throughputReport = new ThroughputReport(performanceReport);

        assertEquals(777.0, throughputReport.get(), DELTA);
    }
}
