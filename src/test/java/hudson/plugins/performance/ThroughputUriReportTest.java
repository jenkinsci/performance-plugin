package hudson.plugins.performance;

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
        Assert.assertEquals(0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnThroughputEvenIfOneHttpSample() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(1000);

        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(1, throughputUriReport.get());
    }

    @Test
    public void shouldReturnZeroWhenAllRequestsTookMoreSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(10000);

        uriReport.addHttpSample(httpSample1);

        Assert.assertEquals(0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnCountOfRequestIfAllRequestsTookLessThanOneSecond() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date());

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        Assert.assertEquals(2, throughputUriReport.get());
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

        Assert.assertEquals(2, throughputUriReport.get());
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

        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date(time));
        httpSample1.setDuration(1000);

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date(time));
        httpSample2.setDuration(1000);

        HttpSample httpSample3 = new HttpSample();
        httpSample3.setDate(new Date(time + 3000));
        httpSample3.setDuration(3000);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);
        uriReport.addHttpSample(httpSample3);

        Assert.assertEquals(0, throughputUriReport.get());
    }

}
