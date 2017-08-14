package hudson.plugins.performance.reports;

import java.util.List;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputReport {

    private static final int MILLISECONDS_IN_SECOND = 1000;

    private final PerformanceReport performanceReport;

    public ThroughputReport(final PerformanceReport performanceReport) {
        this.performanceReport = performanceReport;
    }

    public double get() {
        Long throughput = performanceReport.getThroughput();
        if (throughput != null) {
            return throughput;
        } else {
            final List<UriReport> uriReports = performanceReport.getUriListOrdered();
            if (uriReports.isEmpty()) {
                return 0;
            }


            long sumSamplesCount = 0;
            long startTime = Long.MAX_VALUE;
            long endTime = 0;
            for (UriReport uriReport : uriReports) {
                sumSamplesCount += uriReport.samplesCount();

                if (startTime > uriReport.getStart().getTime()) {
                    startTime = uriReport.getStart().getTime();
                }

                if (endTime < uriReport.getEnd().getTime()) {
                    endTime = uriReport.getEnd().getTime();
                }
            }

            final long duration = endTime - startTime;

            if (duration == 0) {
                return sumSamplesCount; // more than zero requests should always take at least some time. If that didn't get logged, this is the most suitable alternative.
            }

            return (sumSamplesCount / ((double) duration / MILLISECONDS_IN_SECOND));
        }
    }

}
