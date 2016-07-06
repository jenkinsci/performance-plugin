package hudson.plugins.performance;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputReportTest {

    private PerformanceReport performanceReport = new PerformanceReport();

    private ThroughputReport throughputReport = new ThroughputReport(performanceReport);

    @Test
    public void shouldReturnZeroIfNoUri() {
        Assert.assertEquals(0.0, throughputReport.get());
    }

    @Test
    public void shouldSummarizeThroughputByDifferentUri() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());

        UriReport uriReport1 = new UriReport(performanceReport, "f", "url1");
        uriReport1.addHttpSample(httpSample1);

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date());

        UriReport uriReport2 = new UriReport(performanceReport, "f", "url2");
        uriReport2.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        Assert.assertEquals(2.0, throughputReport.get());
    }

    @Test
    public void shouldSummarizeThroughputUnder1ByDifferentUri() {
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(1100);

        UriReport uriReport1 = new UriReport("f", "url1");
        uriReport1.addHttpSample(httpSample1);

        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDate(new Date());
        httpSample2.setDuration(1100);

        UriReport uriReport2 = new UriReport("f", "url2");
        uriReport2.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        Assert.assertEquals(2.0/1100*1000, throughputReport.get());
    }

}
