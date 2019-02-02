package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.tools.SafeMaths;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.util.Date;
import java.util.Scanner;

/**
 * Parser for wrk (https://github.com/wg/wrk)
 * <p>
 * <p>
 * Note that Wrk does not produce request-level data, and can only be processed
 * in it's summarized form (unless extended to do otherwise).
 *
 * @author John Murray me@johnmurray.io
 */
public class WrkSummarizerParser extends AbstractParser {

    private enum LineType {
        RUNNING,
        THREAD_CONN_COUNT,
        OUTPUT_HEADER,
        LATENCY_DIST,
        LATENCY_DIST_BUCKET_HEADER,
        LATENCY_DIST_BUCKET,
        REQ_SEC_DIST,
        SUMMARY,
        REQ_SEC,
        TRANSFER_SEC,
        ERROR_COUNT,
        UNKNOWN
    }

    public enum TimeUnit {
        MILLISECOND(1),
        SECOND(1000),
        MINUTE(1000 * 60),
        HOUR(1000 * 60 * 60);

        private final int factor;

        TimeUnit(int factor) {
            this.factor = factor;
        }

        public int getFactor() {
            return this.factor;
        }
    }

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "wrk";
        }
    }

    public WrkSummarizerParser(String glob, String percentiles) {
        super(glob, percentiles, PerformanceReport.INCLUDE_ALL);
    }
    
    @DataBoundConstructor
    public WrkSummarizerParser(String glob, String percentiles, String filterRegex) {
        super(glob, percentiles, filterRegex);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.wrk";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        final PerformanceReport r = createPerformanceReport();
        r.setExcludeResponseTime(excludeResponseTime);
        r.setReportFileName(reportFile.getName());

        Scanner s = null;
        try {
            s = new Scanner(reportFile);
            HttpSample sample = new HttpSample();

            while (s.hasNextLine()) {
                Scanner scanner = null;
                try {
                    String line = s.nextLine();
                    scanner = new Scanner(line.toLowerCase().replaceAll(
                            "(\\d)s|ms|%|mb|kb(\\b)", "$1$2"));

                    String firstToken = scanner.next();
                    String secondToken = scanner.next();

                    switch (determineLineType(firstToken, secondToken)) {
                        case RUNNING:
                            // extract URI
                            scanner.next();
                            scanner.next();
                            String uri = scanner.next();

                            sample.setUri(uri);
                            break;
                        case LATENCY_DIST:
                            Scanner latencyScanner = new Scanner(line.toLowerCase());
                            latencyScanner.next(); // header (skip)
                            long latencyAvg = getTime(latencyScanner.next(),
                                    TimeUnit.MILLISECOND);
                            latencyScanner.next(); // stdDev (skipping)
                            long latencyMax = getTime(latencyScanner.next(),
                                    TimeUnit.MILLISECOND);

                            sample.setDuration(latencyAvg);
                            sample.setSummarizerMax(latencyMax);
                            break;
                        case REQ_SEC_DIST:
                            // float reqSecAvg = Float.parseFloat(secondToken);
                            // float reqSecStdDev = scanner.nextFloat();
                            // float reqSecMax = scanner.nextFloat();
                            // float reqSecPercentInOneStdDev = scanner.nextFloat();
                            break;
                        case SUMMARY:
                            long totalReq = Long.parseLong(firstToken);
                            Scanner summaryScanner = new Scanner(line.toLowerCase());
                            summaryScanner.next();
                            summaryScanner.next();
                            summaryScanner.next();
                            // long totalTime = getTime(summaryScanner.next(), logger,
                            // TimeUnit.SECOND);
                            sample.setSummarizer(true);
                            sample.setSummarizerSamples(totalReq);
                            summaryScanner.close();
                            break;
                        case ERROR_COUNT:
                            scanner.next();
                            scanner.next();
                            int numErrors = scanner.nextInt();

                            sample.setSummarizerErrors(numErrors);
                            break;
                        case REQ_SEC:
                        case TRANSFER_SEC:
                            // not currently used by performance-plugin
                            break;
                        case THREAD_CONN_COUNT:
                        case OUTPUT_HEADER:
                        case LATENCY_DIST_BUCKET_HEADER:
                        case LATENCY_DIST_BUCKET:
                        case UNKNOWN:
                            // do nothing, don't need output
                            break;
                    }
                } finally {
                    if (scanner != null)
                        scanner.close();
                }
            }

            sample.setSuccessful(true);
            sample.setDate(new Date());
            r.addSample(sample);
        } finally {
            if (s != null)
                s.close();
        }

        return r;
    }

    /**
     * Given a time string (eg: 0ms, 1m, 2s, 3h, etc.) parse and yield the time in
     * a specified time unit (millisecond, second, minute, hour)
     * <p>
     * <p>
     * If no result can be returned, a 0 value will result and any errors
     * encountered will be logged.
     *
     * @param timeString String representation from `wrk` command-output
     * @param tu         Time unit to return time string in
     * @return Time in seconds, as parsed from input
     */
    public long getTime(String timeString, TimeUnit tu) {
        double factor = 0;

        timeString = timeString.trim().replaceAll("[^\\d\\.smh]", "");
        String timeUnitString = timeString.replaceAll("[\\d\\.]", "");
        String timeValueString = timeString.replaceAll("[smh]", "");

    /*
     * Calculate 'factor' so that we can get the input time in ms (eg: 5m =
     * 300000, 3s = 3000)
     */
        if ("ms".equals(timeUnitString)) {
            factor = 1;
        } else if ("s".equals(timeUnitString)) {
            factor = 1000;
        } else if ("m".equals(timeUnitString)) {
            factor = 1000 * 60d;
        } else if ("h".equals(timeUnitString)) {
            factor = 1000 * 60 * 60d;
        }

        double timeValue = Double.parseDouble(timeValueString);
        double timeInMilliSeconds = timeValue * factor;
        double timeInReturnFormat = SafeMaths.safeDivide(timeInMilliSeconds, tu.getFactor());

        return (int) Math.floor(timeInReturnFormat);
    }

    /**
     * Given the first couple of tokens from a line, determine what type of line
     * it is (by returning a LineType) so that is can be processed accordingly.
     *
     * @param t1 First token in the processed line
     * @param t2 Second token in the processed line
     * @return LineType indicating how the rest of the line should be processed
     */
    public LineType determineLineType(String t1, String t2) {
        if ("running".equals(t1)) {
            return LineType.RUNNING;
        } else if ("thread".equals(t1)) {
            return LineType.OUTPUT_HEADER;
        } else if ("latency".equals(t1)) {
            if ("distribution".equals(t2))
                return LineType.LATENCY_DIST_BUCKET_HEADER;
            else
                return LineType.LATENCY_DIST;
        } else if ("req/sec".equals(t1)) {
            return LineType.REQ_SEC_DIST;
        } else if ("requests/sec:".equals(t1)) {
            return LineType.REQ_SEC;
        } else if ("transfer/sec:".equals(t1)) {
            return LineType.TRANSFER_SEC;
        } else if ("non-2xx".equals(t1)) {
            return LineType.ERROR_COUNT;
        } else {
            try {
                Long.parseLong(t1);
                if ("threads".equals(t2)) {
                    return LineType.THREAD_CONN_COUNT;
                } else if ("requests".equals(t2)) {
                    return LineType.SUMMARY;
                } else {
                    try {
                        Float.parseFloat(t2);
                        return LineType.LATENCY_DIST_BUCKET;
                    } catch (NumberFormatException e) {
                        return LineType.UNKNOWN;
                    }
                }
            } catch (NumberFormatException e) {
                return LineType.UNKNOWN;
            }
        }
    }
}