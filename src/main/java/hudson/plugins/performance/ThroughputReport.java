package hudson.plugins.performance;

import java.util.List;

public class ThroughputReport {

    private static final int MILLISECONDS_IN_SECOND = 1000;

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

    private long getUriAverage(final UriReport uriReport) {
        final List<HttpSample> httpSamples = uriReport.getHttpSampleList();

        if (httpSamples.isEmpty()) return 0L;

        final long durationInSeconds = calculateTestingDuration(httpSamples);

        return httpSamples.size() / durationInSeconds;
    }

    private long calculateTestingDuration(final List<HttpSample> httpSamples) {
        final long testStartTime = testingStartTime(httpSamples);
        final long testFinishTime = testingFinishTime(httpSamples);
        final long testingDuration = (testFinishTime - testStartTime) / MILLISECONDS_IN_SECOND;
        return Math.max(testingDuration, 1);
    }

    private long testingStartTime(final List<HttpSample> httpSamples) {
        long min = -1;
        for (final HttpSample httpSample : httpSamples) {
            if (min < 0 || min > httpSample.getDate().getTime()) min = httpSample.getDate().getTime();
        }
        return min;
    }

    private long testingFinishTime(final List<HttpSample> httpSamples) {
        long max = 0;
        for (final HttpSample httpSample : httpSamples) {
            if (max < httpSample.getDate().getTime() + httpSample.getDuration())
                max = httpSample.getDate().getTime() + httpSample.getDuration();
        }
        return max;
    }

}
