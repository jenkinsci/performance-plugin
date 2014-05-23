package hudson.plugins.performance;

import java.util.List;

public class ThroughputReport {

    private final PerformanceReport performanceReport;

    public ThroughputReport(PerformanceReport performanceReport) {
        this.performanceReport = performanceReport;
    }

    public long getAverage() {
        final List<UriReport> uriReports = performanceReport.getUriListOrdered();
        if (uriReports.isEmpty()) return 0L;

        long sumThroughput = 0;
        for (UriReport uriReport : uriReports) sumThroughput += getUriAverage(uriReport);
        // here we assume that all uri executed in parallel and have same test duration
        return sumThroughput;
    }

    private long getUriAverage(UriReport uriReport) {
        final List<HttpSample> httpSamples = uriReport.getHttpSampleList();

        if (httpSamples.isEmpty()) return 0L;

        final long testStartTime = testStartTime(httpSamples);
        final long testFinishTime = testFinishTime(httpSamples);
        final long durationInSeconds = (testFinishTime - testStartTime) / 1000;

        return httpSamples.size() / durationInSeconds;
    }

    private long testStartTime(List<HttpSample> httpSamples) {
        long min = -1;
        for (HttpSample httpSample : httpSamples) {
            if (min < 0 || min > httpSample.getDate().getTime()) min = httpSample.getDate().getTime();
        }
        return min;
    }

    private long testFinishTime(List<HttpSample> httpSamples) {
        long max = 0;
        for (HttpSample httpSample : httpSamples) {
            if (max < httpSample.getDate().getTime()) max = httpSample.getDate().getTime();
        }
        return max;
    }

}
