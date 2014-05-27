package hudson.plugins.performance;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;

public class ThroughputReportTest {

    private PerformanceReport performanceReport = new PerformanceReport();

    private ThroughputReport throughputReport = new ThroughputReport(performanceReport);

    @Test
    public void shouldReturnZeroIfNoUri() {
        Assert.assertEquals(0, throughputReport.getAverage());
    }

    @Test
    public void shouldReturnZeroIfNoHttpSamplesForAnyUri() {
        UriReport uriReport = new UriReport(performanceReport, "f", "x");
        performanceReport.getUriReportMap().put("x", uriReport);

        Assert.assertEquals(0, throughputReport.getAverage());
    }

    @Test
    public void shouldCalculatePerSecondThroughputEvenIfOneHttpSample() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(1000);

        UriReport uriReport = new UriReport(performanceReport, "f", "x");
        uriReport.addHttpSample(httpSample1);
        performanceReport.getUriReportMap().put("x", uriReport);

        Assert.assertEquals(1, throughputReport.getAverage());
    }

    @Test
    public void shouldReturnZeroThroughputWhenAllRequestsExecutesMoreSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(10000);

        UriReport uriReport = new UriReport(performanceReport, "f", "x");
        uriReport.addHttpSample(httpSample1);
        performanceReport.getUriReportMap().put("x", uriReport);

        Assert.assertEquals(0, throughputReport.getAverage());
    }

    @Test
    public void shouldReturnCountOfRequestIfAllProcessedLessThanOneSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date());

        UriReport uriReport = new UriReport(performanceReport, "f", "x");
        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().put("x", uriReport);

        Assert.assertEquals(2, throughputReport.getAverage());
    }

    @Test
    public void shouldReturnAverageThroughput() {
        long time = System.currentTimeMillis();

        // 0 sec - first request  - 1 sec
        // 0 sec -                - 1 sec - second request - 2 sec
        // 0 sec - third request  - 1 sec
        // 0 sec - four request   - 1 sec
        // 0 sec - total 3        - 1 sec - total 1        - 2 sec
        // average 2

        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date(time));

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date(time + 1000));

        HttpSample httpSample3 = new HttpSample();
        httpSample3.setDate(new Date(time));
        httpSample3.setDuration(500);

        HttpSample httpSample4 = new HttpSample();
        httpSample4.setDate(new Date(time));
        httpSample4.setDuration(10);

        UriReport uriReport = new UriReport(performanceReport, "f", "x");
        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().put("x", uriReport);

        Assert.assertEquals(2, throughputReport.getAverage());
    }

}
