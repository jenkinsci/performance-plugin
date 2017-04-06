package hudson.plugins.performance.reports;

import hudson.model.Run;
import hudson.plugins.performance.Messages;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single performance report, which consists of multiple
 * {@link UriReport}s for different URLs that was tested.
 * <p>
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractReport implements Serializable,
        Comparable<PerformanceReport> {
    private static final long serialVersionUID = 675698410989941826L;
    private static final double ZERO_PERCENT = 0;
    private static final double ONE_HUNDRED_PERCENT = 100;
    private static final double NINETY_PERCENT = 90;
    private static final double FIFTY_PERCENT = 50;

    private transient PerformanceBuildAction buildAction;


    private String reportFileName = null;

    /**
     * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
     */
    private final Map<String, UriReport> uriReportMap = new LinkedHashMap<String, UriReport>();

    private PerformanceReport lastBuildReport;

    /**
     * A lazy cache of all duration values of all HTTP samples in all UriReports, ordered by duration.
     */
    private transient List<Long> durationsSortedBySize = null;

    /**
     * A lazy cache of all UriReports, reverse-ordered.
     */
    private transient List<UriReport> uriReportsOrdered = null;

    /**
     * The amount of http samples that are not successful.
     */
    private int nbError = 0;

    /**
     * The sum of summarizerErrors values from all samples;
     */
    private float summarizerErrors = 0;

    /**
     * The amount of samples in all uriReports combined.
     */
    private int size;
    private int samplesCount;

    /**
     * The duration of all samples combined, in milliseconds.
     */
    private long totalDuration = 0;

    /**
     * The size of all samples combined, in kilobytes.
     */
    private double totalSizeInKB = 0;
    private long summarizerMin;
    private long summarizerMax;
    private long summarizerAvg;
    private String summarizerErrorPercent = null;
    private long summarizerSize;

    private Long average;
    private Long perc0;
    private Long perc50;
    private Long perc90;
    private Long perc100;

    public Object readResolve() {
        if (size != 0) {
            samplesCount = size;
        }

        return this;
    }

    public static String asStaplerURI(String uri) {
        return uri.replace("http:", "").replace("https:", "").replaceAll("/", "_").replaceAll(":", "_");
    }

    public void addSample(HttpSample pHttpSample) throws SAXException {
        String uri = pHttpSample.getUri();
        if (uri == null) {
            buildAction
                    .getHudsonConsoleWriter()
                    .println("label cannot be empty, please ensure your jmx file specifies "
                            + "name properly for each http sample: skipping sample");
            return;
        }
        String staplerUri = PerformanceReport.asStaplerURI(uri);
        synchronized (uriReportMap) {
            UriReport uriReport = uriReportMap.get(staplerUri);
            if (uriReport == null) {
                uriReport = new UriReport(this, staplerUri, uri);
                uriReportMap.put(staplerUri, uriReport);
            }
            uriReport.addHttpSample(pHttpSample);

            // reset the lazy loaded caches.
            durationsSortedBySize = null;
            uriReportsOrdered = null;
        }

        if (!pHttpSample.isSuccessful()) {
            nbError++;
        }
        summarizerErrors += pHttpSample.getSummarizerErrors();
        samplesCount++;
        totalDuration += pHttpSample.getDuration();
        totalSizeInKB += pHttpSample.getSizeInKb();
    }

    public void addSample(TaurusFinalStats sample, boolean isSummaryReport) {
        String uri = sample.getLabel();
        if (uri == null) {
            buildAction
                    .getHudsonConsoleWriter()
                    .println("label cannot be empty, please ensure your jmx file specifies "
                            + "name properly for each http sample: skipping sample");
            return;
        }

        if (isSummaryReport) {
            summarizerErrors = nbError = sample.getFail();
            int sampleCount = sample.getFail() + sample.getSucc();
            samplesCount = sampleCount;
            totalDuration = (long) sample.getAverageResponseTime() * sampleCount;
            totalSizeInKB = sample.getBytes();

            average = (long) sample.getAverageResponseTime();
            perc50 = (long) sample.getPerc50();
            perc90 = (long) sample.getPerc90();
            perc0 = (long) sample.getPerc0();
            perc100 = (long) sample.getPerc100();
        } else {
            String staplerUri = PerformanceReport.asStaplerURI(uri);
            synchronized (uriReportMap) {
                UriReport uriReport = uriReportMap.get(staplerUri);
                if (uriReport == null) {
                    uriReport = new UriReport(this, staplerUri, uri);
                    uriReportMap.put(staplerUri, uriReport);
                }
                uriReport.setFromTaurusFinalStats(sample);

                // reset the lazy loaded caches.
                durationsSortedBySize = null;
                uriReportsOrdered = null;
            }
        }
    }

    public int compareTo(PerformanceReport jmReport) {
        if (this == jmReport) {
            return 0;
        }
        return getReportFileName().compareTo(jmReport.getReportFileName());
    }

    public int countErrors() {
        return nbError;
    }

    public double errorPercent() {
        if (ifSummarizerParserUsed(reportFileName)) {
            if (uriReportMap.size() == 0) return 0;
            return Math.round((summarizerErrors / uriReportMap.size()) * 1000.0) / 1000.0;
        } else {
            return Math.round((samplesCount() == 0 ? 0 : ((double) countErrors()) / samplesCount() * 100) * 1000.0) / 1000.0;
        }
    }

    public long getAverage() {
        if (average == null) {
            average = (samplesCount == 0) ? 0 : (totalDuration / samplesCount);
        }
        return average;
    }

    public double getAverageSizeInKb() {
        if (samplesCount == 0) {
            return 0;
        }
        return roundTwoDecimals(totalSizeInKB / samplesCount);
    }

    /**
     * 0 percent will give the first value from ordered list of durations
     * 100 percent will give the last value from ordered list of durations
     *
     * @param percentage must be a value between 0 and 100 (inclusive)
     * @return value at the percentage specified.
     */
    public long getDurationAt(double percentage) {
        if (percentage < ZERO_PERCENT || percentage > ONE_HUNDRED_PERCENT) {
            throw new IllegalArgumentException("Argument 'percentage' must be a value between 0 and 100 (inclusive)");
        }

        if (samplesCount == 0) {
            return 0;
        }

        synchronized (uriReportMap) {
            if (durationsSortedBySize == null) {
                durationsSortedBySize = new ArrayList<Long>();
                for (UriReport currentReport : uriReportMap.values()) {
                    durationsSortedBySize.addAll(currentReport.getDurations());
                }
                Collections.sort(durationsSortedBySize);
            }

            final double percentInDecimals = percentage / 100;
            int indexToReturn = ((int) (durationsSortedBySize.size() * percentInDecimals)) - 1;

            // Make sure a valid index is used.
            if (indexToReturn < 0) {
                indexToReturn = 0;
            } else if (indexToReturn >= durationsSortedBySize.size()) {
                indexToReturn = durationsSortedBySize.size() - 1;
            }
            return durationsSortedBySize.get(indexToReturn);
        }
    }

    public long get90Line() {
        if (perc90 == null) {
            perc90 = getDurationAt(NINETY_PERCENT);
        }
        return perc90;
    }

    public long getMedian() {
        if (perc50 == null) {
            perc50 = getDurationAt(FIFTY_PERCENT);
        }
        return perc50;
    }

    public String getHttpCode() {
        return "";
    }

    public Run<?, ?> getBuild() {
        return buildAction.getBuild();
    }

    PerformanceBuildAction getBuildAction() {
        return buildAction;
    }

    public String getDisplayName() {
        return Messages.Report_DisplayName();
    }

    public UriReport getDynamic(String token) throws IOException {
        return getUriReportMap().get(token);
    }

    public long getMax() {
        if (perc100 == null) {
            perc100 = getDurationAt(ONE_HUNDRED_PERCENT);
        }
        return perc100;
    }

    public double getTotalTrafficInKb() {
        return roundTwoDecimals(totalSizeInKB);
    }

    public long getMin() {
        if (perc0 == null) {
            perc0 = getDurationAt(ZERO_PERCENT);
        }
        return perc0;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public List<UriReport> getUriListOrdered() {
        synchronized (uriReportMap) {
            if (uriReportsOrdered == null) {
                uriReportsOrdered = new ArrayList<UriReport>(uriReportMap.values());
                Collections.sort(uriReportsOrdered, Collections.reverseOrder());
            }
            return uriReportsOrdered;
        }
    }

    public Map<String, UriReport> getUriReportMap() {
        return uriReportMap;
    }

    public void setBuildAction(PerformanceBuildAction buildAction) {
        this.buildAction = buildAction;
    }

    public void setReportFileName(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    public int samplesCount() {
        return samplesCount;
    }

    public void setLastBuildReport(PerformanceReport lastBuildReport) {
        Map<String, UriReport> lastBuildUriReportMap = lastBuildReport
                .getUriReportMap();
        for (Map.Entry<String, UriReport> item : uriReportMap.entrySet()) {
            UriReport lastBuildUri = lastBuildUriReportMap.get(item.getKey());
            if (lastBuildUri != null) {
                item.getValue().addLastBuildUriReport(lastBuildUri);
            }
        }
        this.lastBuildReport = lastBuildReport;
    }

    public long getAverageDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return getAverage() - lastBuildReport.getAverage();
    }

    public long getMedianDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return getMedian() - lastBuildReport.getMedian();
    }

    public double getErrorPercentDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return Math.round((errorPercent() - lastBuildReport.errorPercent()) * 1000.0) / 1000.0;
    }

    public String getLastBuildHttpCodeIfChanged() {
        return "";
    }

    public int getSamplesCountDiff() {
        if (lastBuildReport == null) {
            return 0;
        }
        return samplesCount() - lastBuildReport.samplesCount();
    }

    /**
     * Check if the filename of the file being parsed is being parsed by a
     * summarized parser (JMeterSummarizer).
     *
     * @param filename name of the file being parsed
     * @return boolean indicating usage of summarized parser
     */
    public boolean ifSummarizerParserUsed(String filename) {
        PerformanceReportParser parser = buildAction.getParserByDisplayName("JmeterSummarizer");
        if (parser != null) {
            String fileExt = parser.glob;
            String parts[] = fileExt.split("\\s*[;:,]+\\s*");
            for (String path : parts) {
                if (filename.endsWith(path.substring(5))) {
                    return true;
                }
            }
        }
        parser = buildAction.getParserByDisplayName("Iago");
        if (parser != null) {
            return true;
        }
        return false;
    }

    private double roundTwoDecimals(double d) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void setSummarizerSize(long summarizerSize) {
        this.summarizerSize = summarizerSize;
    }

    public long getSummarizerSize() {
        return summarizerSize;
    }

    public void setSummarizerMin(long summarizerMin) {
        this.summarizerMin = summarizerMin;
    }

    public long getSummarizerMin() {
        return summarizerMin;
    }

    public void setSummarizerMax(long summarizerMax) {
        this.summarizerMax = summarizerMax;
    }

    public long getSummarizerMax() {
        return summarizerMax;
    }

    public void setSummarizerAvg(long summarizerAvg) {
        this.summarizerAvg = summarizerAvg;
    }

    public long getSummarizerAvg() {
        return summarizerAvg;
    }

    public void setSummarizerErrors(String summarizerErrorPercent) {
        this.summarizerErrorPercent = summarizerErrorPercent;
    }

    public String getSummarizerErrors() {
        return summarizerErrorPercent;
    }

}
