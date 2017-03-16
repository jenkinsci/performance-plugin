package hudson.plugins.performance;

import java.util.List;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputReport {

    private final PerformanceReport performanceReport;

    public ThroughputReport(final PerformanceReport performanceReport) {
        this.performanceReport = performanceReport;
    }

    public double get() {
        final List<UriReport> uriReports = performanceReport.getUriListOrdered();
        if (uriReports.isEmpty()) return 0;

        double sumThroughput = 0;
        for (UriReport uriReport : uriReports) {
            sumThroughput += new ThroughputUriReport(uriReport).get();
        }

        // here we assume that all uri executed in parallel and have same test duration
        return sumThroughput;
    }

}
