package hudson.plugins.performance;

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
        if (uriReport.size() == 0) {
          return 0;
        }
        
        long end = uriReport.getEnd().getTime();
        long start = uriReport.getStart().getTime();
        final long duration = end - start;
        
        if (duration == 0) {
          return uriReport.size(); // more than zero requests should always take at least some time. If that didn't get logged, this is the most suitable alternative.
        }
        return (uriReport.size() / ((double)duration / MILLISECONDS_IN_SECOND));
    }
}
