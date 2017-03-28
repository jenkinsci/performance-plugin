package hudson.plugins.performance.reports.throughput;

import hudson.plugins.performance.reports.UriReport;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputUriReport {

    private static final int MILLISECONDS_IN_SECOND = 1000;

    private final UriReport uriReport;

    public ThroughputUriReport(final UriReport uriReport) {
        this.uriReport = uriReport;
    }

    public double get() {
        Long throughput = uriReport.getThroughput();
        if (throughput != null) {
            return throughput;
        } else  {
            if (uriReport.samplesCount() == 0) {
                return 0;
            }

            long end = uriReport.getEnd().getTime();
            long start = uriReport.getStart().getTime();
            final long duration = end - start;

            if (duration == 0) {
                return uriReport.samplesCount(); // more than zero requests should always take at least some time. If that didn't get logged, this is the most suitable alternative.
            }
            return (uriReport.samplesCount() / ((double) duration / MILLISECONDS_IN_SECOND));
        }
    }
}
