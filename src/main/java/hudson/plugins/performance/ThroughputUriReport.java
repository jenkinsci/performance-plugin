package hudson.plugins.performance;

import java.util.List;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputUriReport {

    private static final int MILLISECONDS_IN_SECOND = 1000;

    private final UriReport uriReport;

    public ThroughputUriReport(final UriReport uriReport) {
        this.uriReport = uriReport;
    }

    public long get() {
        final List<HttpSample> httpSamples = uriReport.getHttpSampleList();
        final long durationInSeconds = calculateTestingDuration(httpSamples);
        return httpSamples.size() / durationInSeconds;
    }

    private long calculateTestingDuration(final List<HttpSample> httpSamples) {
        final long testingStartTime = testingStartTime(httpSamples);
        final long testingFinishTime = testingFinishTime(httpSamples);
        final long testingDuration = (testingFinishTime - testingStartTime) / MILLISECONDS_IN_SECOND;
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
