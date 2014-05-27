package hudson.plugins.performance;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;

public class ThroughputUriReportTest {

    private PerformanceReport performanceReport = new PerformanceReport();
    private UriReport uriReport = new UriReport(performanceReport, "f", "x");

    private ThroughputUriReport throughputUriReport = new ThroughputUriReport(uriReport);

    @Test
    public void shouldReturnZeroForAverageIfNoHttpSamples() {
        Assert.assertEquals(0, throughputUriReport.getAverage());
    }

    @Test
    public void shouldCalculateAveragePerSecondEvenIfOneHttpSample() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(1000);

        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(1, throughputUriReport.getAverage());
    }

    @Test
    public void shouldReturnZeroAverageWhenAllRequestsExecutesMoreSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(10000);

        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(0, throughputUriReport.getAverage());
    }

    @Test
    public void shouldReturnCountOfRequestForAverageIfAllProcessedLessThanOneSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date());

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        Assert.assertEquals(2, throughputUriReport.getAverage());
    }

    @Test
    public void shouldReturnAverage() {
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

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        Assert.assertEquals(2, throughputUriReport.getAverage());
    }

}
