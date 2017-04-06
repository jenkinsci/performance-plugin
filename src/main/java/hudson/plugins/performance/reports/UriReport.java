package hudson.plugins.performance.reports;

import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.details.GraphConfigurationDetail;
import hudson.util.ChartUtil;
import org.apache.commons.lang.StringUtils;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A report about a particular tested URI.
 * <p>
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements Serializable, ModelObject,
        Comparable<UriReport> {

    private static final long serialVersionUID = -5269155428479638524L;

    public final static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

    /**
     * Escaped {@link #uri} that doesn't contain any letters that cannot be used
     * as a token in URL.
     */
    private final String staplerUri;

    private UriReport lastBuildUriReport;

    /**
     * The parent object to which this object belongs.
     */
    private final PerformanceReport performanceReport;

    private String uri;

    /**
     * The amount of http samples that are not successful.
     */
    private int nbError = 0;

    /**
     * A list that contains the date and duration (in milliseconds) of all individual samples.
     */
    private final List<Sample> samples = new ArrayList<Sample>(); // retain insertion order.

    /**
     * A lazy cache of all duration values in {@link #samples}, insertion order (same as {@link #samples}
     */
    private transient List<Long> durationsIO = new ArrayList<Long>();

    /**
     * A lazy cache of all duration values in {@link #samples}, ordered by duration.
     */
    private transient List<Long> durationsSortedBySize = new ArrayList<Long>();

    /**
     * Indicates if the collection {@link #durationsSortedBySize} is in a sorted state.
     */
    private transient boolean isSorted = false;

    /**
     * The duration of all samples combined, in milliseconds.
     */
    private long totalDuration = 0; // note that this is the sum of all elements in #durations, but need not be recalculated every time.

    /**
     * The set of (unique) HTTP status codes from all samples.
     */
    private Set<String> httpCodes = new HashSet<String>();

    /**
     * The sum of summarizerSample values from all samples;
     */
    private long summarizerSize = 0;

    /**
     * The sum of summarizerErrors values from all samples;
     */
    private float summarizerErrors = 0;

    /**
     * The point in time of the start of the oldest sample.
     */
    private Date start = null;

    /**
     * The point in time of the end of the youngest sample.
     */
    private Date end = null;


    private Long average;
    private Long perc0;
    private Long perc50;
    private Long perc90;
    private Long perc100;
    private Long throughput;

    private int samplesCount;


    public UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
        this.performanceReport = performanceReport;
        this.staplerUri = staplerUri;
        this.uri = uri;
    }

    public void addHttpSample(HttpSample sample) {
        if (!sample.isSuccessful()) {
            nbError++;
        }
        synchronized (samples) {
            if (samples.add(new Sample(sample.getHttpCode(), sample.getDate(), sample.getDuration()))) {
                isSorted = false;
                samplesCount++;
            }
        }
        totalDuration += sample.getDuration();
        httpCodes.add(sample.getHttpCode()); // The Set implementation will ensure that no duplicates will be saved.
        summarizerSize += sample.getSummarizerSamples();
        summarizerErrors += sample.getSummarizerErrors();

        if (start == null || sample.getDate().before(start)) {
            start = sample.getDate();
        }
        Date finish = new Date(sample.getDate().getTime() + sample.getDuration());
        if (end == null || finish.after(end)) {
            end = finish;
        }
    }

    public void setFromTaurusFinalStats(TaurusFinalStats report) {
        average = (long) report.getAverageResponseTime();
        perc0 = (long) report.getPerc0();
        perc50 = (long) report.getPerc50();
        perc90 = (long) report.getPerc90();
        perc100 = (long) report.getPerc100();

        throughput = report.getThroughput();
        
        summarizerSize = report.getBytes();
        summarizerErrors = report.getFail();
        nbError = report.getFail();

        synchronized (samples) {
            samplesCount = report.getSucc() + report.getFail();
        }
    }

    public int compareTo(UriReport uriReport) {
        if (uriReport == this) {
            return 0;
        }
        return uriReport.getUri().compareTo(this.getUri());
    }

    public int countErrors() {
        return nbError;
    }

    public double errorPercent() {
        return Math.round((((double) countErrors()) / samplesCount() * 100) * 1000.0) / 1000.0;
    }

    public long getAverage() {
        if (average == null) {
            int samplesCount = samplesCount();
            average = (samplesCount == 0) ? 0 : totalDuration / samplesCount;
        }
        return average;
    }

    private long getDurationAt(double percentage) {
        if (percentage < 0 || percentage > 1) {
            throw new IllegalArgumentException("Argument 'percentage' must be a value between 0 and 1 (inclusive)");
        }

        synchronized (samples) {
            final List<Long> durations = getSortedDuration();

            if (durations.isEmpty()) {
                return 0;
            }

            return durations.get((int) (samples.size() * percentage));
        }
    }

    public long get90Line() {
        if (perc90 == null) {
            perc90 = getDurationAt(0.9);
        }
        return perc90;
    }

    public String getHttpCode() {
        return StringUtils.join(httpCodes, ',');
    }

    public long getMedian() {
        if (perc50 == null) {
            perc50 = getDurationAt(0.5);
        }
        return perc50;
    }

    public Run<?, ?> getBuild() {
        return performanceReport.getBuild();
    }

    public String getDisplayName() {
        return getUri();
    }

    public List<Sample> getHttpSampleList() {
        return samples;
    }

    public PerformanceReport getPerformanceReport() {
        return performanceReport;
    }

    protected List<Long> getSortedDuration() {
        synchronized (samples) {
            if (!isSorted || durationsSortedBySize == null || durationsSortedBySize.size() != samples.size()) {

                durationsSortedBySize = new ArrayList<Long>(samplesCount());
                for (Sample sample : samples) {
                    durationsSortedBySize.add(sample.duration);
                }
                Collections.sort(durationsSortedBySize);
                isSorted = true;
            }

            return durationsSortedBySize;
        }
    }

    public List<Long> getDurations() {
        synchronized (samples) {
            if (durationsIO == null || durationsIO.size() != samples.size()) {
                durationsIO = new ArrayList<Long>(samples.size());
                for (Sample sample : samples) {
                    durationsIO.add(sample.duration);
                }
            }
            return durationsIO;
        }
    }

    public long getMax() {
        if (perc100 == null) {
            final List<Long> durations = getSortedDuration();
            perc100 = durations.isEmpty() ? 0 : durations.get(durations.size() - 1);
        }
        return perc100;
    }

    public long getMin() {
        if (perc0 == null) {
            final List<Long> durations = getSortedDuration();
            perc0 = durations.isEmpty() ? 0 : durations.get(0);
        }
        return perc0;
    }

    public String getStaplerUri() {
        return staplerUri;
    }

    public String getUri() {
        return uri;
    }

    public String getShortUri() {
        if (uri.length() > 130) {
            return uri.substring(0, 129);
        }
        return uri;
    }

    public boolean isFailed() {
        return countErrors() != 0;
    }

    public int samplesCount() {
        synchronized (samples) {
            return samplesCount;
        }
    }

    public String encodeUriReport() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(120);
        sb.append(performanceReport.getReportFileName()).append(
                GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(
                END_PERFORMANCE_PARAMETER);
        return URLEncoder.encode(sb.toString(), "UTF-8");
    }

    public void addLastBuildUriReport(UriReport lastBuildUriReport) {
        this.lastBuildUriReport = lastBuildUriReport;
    }

    public long getAverageDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return getAverage() - lastBuildUriReport.getAverage();
    }

    public long getMedianDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return getMedian() - lastBuildUriReport.getMedian();
    }

    public double getErrorPercentDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return Math.round((errorPercent() - lastBuildUriReport.errorPercent()) * 1000.0) / 1000.0;
    }

    public String getLastBuildHttpCodeIfChanged() {
        if (lastBuildUriReport == null) {
            return "";
        }

        if (lastBuildUriReport.getHttpCode().equals(getHttpCode())) {
            return "";
        }

        return lastBuildUriReport.getHttpCode();
    }

    public int getSamplesCountDiff() {
        if (lastBuildUriReport == null) {
            return 0;
        }
        return samplesCount() - lastBuildUriReport.samplesCount();
    }

    public float getSummarizerErrors() {
        return summarizerErrors / summarizerSize * 100;
    }

    public void doSummarizerTrendGraph(StaplerRequest request, StaplerResponse response) throws IOException {
        TimeSeries responseTimes = new TimeSeries("Response Time", FixedMillisecond.class);
        synchronized (samples) {
            for (Sample sample : samples) {
                responseTimes.addOrUpdate(new FixedMillisecond(sample.date), sample.duration);
            }
        }

        TimeSeriesCollection resp = new TimeSeriesCollection();
        resp.addSeries(responseTimes);

        ArrayList<XYDataset> dataset = new ArrayList<XYDataset>();
        dataset.add(resp);

        ChartUtil.generateGraph(request, response,
                PerformanceProjectAction.createSummarizerTrend(dataset, uri), 400, 200);
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public static class Sample implements Serializable, Comparable<Sample> {

        private static final long serialVersionUID = 4458431861223813407L;

        final Date date;
        final long duration;
        final String httpCode;

        public Sample(String httpCode, Date date, long duration) {
            this.httpCode = httpCode;
            this.date = date;
            this.duration = duration;
        }

        public String getHttpCode() {
            return httpCode;
        }

        public Date getDate() {
            return date;
        }

        public long getDuration() {
            return duration;
        }

        /**
         * Compare first based on duration, next on date.
         */
        public int compareTo(Sample other) {
            if (this == other) return 0;
            if (this.duration < other.duration) return -1;
            if (this.duration > other.duration) return 1;
            if (this.date == null || other.date == null) return 0;
            if (this.date.before(other.date)) return -1;
            if (this.date.after(other.date)) return 1;
            return 0;
        }
    }

    public Long getThroughput() {
        return throughput;
    }
}
