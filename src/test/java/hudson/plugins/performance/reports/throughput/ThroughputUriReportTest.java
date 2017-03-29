package hudson.plugins.performance.reports.throughput;

import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputUriReportTest {

    private PerformanceReport performanceReport = new PerformanceReport();
    private UriReport uriReport = new UriReport(performanceReport, "f", "x");

    private ThroughputUriReport throughputUriReport = new ThroughputUriReport(uriReport);

    @Test
    public void shouldReturnZeroIfNoHttpSamples() {
        Assert.assertEquals(0.0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnThroughputEvenIfOneHttpSample() {
        HttpSample httpSample1 = createHttpSample(new Date(), 1000);
        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(1.0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnZeroWhenAllRequestsTookMoreSecond() {
        HttpSample httpSample1 = createHttpSample(new Date(), 10000);
        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(0.1, throughputUriReport.get());
    }

    @Test
    public void shouldReturnCountOfRequestIfAllRequestsTookLessThanOneSecond() {
        HttpSample httpSample1 = createHttpSample(new Date(), 0);
        HttpSample httpSample2 = createHttpSample(new Date(), 0);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        Assert.assertEquals(2.0, throughputUriReport.get());
    }

    @Test
    public void shouldCalculateThroughput1() {
        long time = System.currentTimeMillis();

        // 0 sec - first request  - 1 sec
        // 0 sec -                - 1 sec - second request - 2 sec
        // 0 sec - third request  - 1 sec
        // 0 sec - four request   - 1 sec
        // 0 sec - total 3        - 1 sec - total 1        - 2 sec
        // throughput (per second) 2

        HttpSample httpSample1 = createHttpSample(new Date(), 0);
        HttpSample httpSample2 = createHttpSample(new Date(time + 1000), 0);
        HttpSample httpSample3 = createHttpSample(new Date(), 500);
        HttpSample httpSample4 = createHttpSample(new Date(), 10);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        Assert.assertEquals(2.0, throughputUriReport.get());
    }

    @Test
    public void shouldCalculateThroughput2() {
        long time = System.currentTimeMillis();

        // 0 sec - start first request, start second r
        // 1 sec - finish first r, finish second r
        // 2 sec -
        // 3 sec - start 3 r
        // 4 sec -
        // 5 sec -
        // 6 sec - finish 3 r
        // throughput per second 1

        HttpSample httpSample1 = createHttpSample(new Date(), 1000);
        HttpSample httpSample2 = createHttpSample(new Date(), 1000);
        HttpSample httpSample3 = createHttpSample(new Date(time + 3000), 3000);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);
        uriReport.addHttpSample(httpSample3);

        Assert.assertEquals(0.5, throughputUriReport.get());
    }

    @Test // JENKINS-27373
    public void durationOf1ShouldNotThrowDivideByZeroException() {
        HttpSample httpSample1 = createHttpSample(new Date(), 1);

        uriReport.addHttpSample(httpSample1);
        Assert.assertEquals(1000.0, throughputUriReport.get());
    }

    private HttpSample createHttpSample(Date date, long duration) {
        HttpSample httpSample = new HttpSample();
        httpSample.setDate(date);
        httpSample.setDuration(duration);
        return httpSample;
    }
}
