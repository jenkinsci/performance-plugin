package hudson.plugins.performance.reports;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;

import hudson.plugins.performance.data.HttpSample;

/**
 * Abstract class for classes with samplesCount, error, mean, average, 90 line, min and max attributes
 */
public abstract class AbstractReport {
    public static final Logger LOGGER = Logger.getLogger(AbstractReport.class.getName());
    public static final double ZERO_PERCENT = 0;
    public static final double ONE_HUNDRED_PERCENT = 100;
    public static final double NINETY_PERCENT = 90;
    public static final double FIFTY_PERCENT = 50;
    public static final String DEFAULT_PERCENTILES = "0,50,90,100";

    protected final ThreadLocal<DecimalFormat> percentFormat;
    protected final ThreadLocal<DecimalFormat> dataFormat; // three decimals

    protected Map<Double, Long> percentilesValues = new TreeMap<>();
    protected Map<Double, Long> percentilesDiffValues = new TreeMap<>();
    protected boolean isCalculatedPercentilesValues = false;

    /**
     * Exclude response time of errored samples
     */
    protected boolean excludeResponseTime;

    public abstract int countErrors();

    public abstract double errorPercent();

    public abstract void calculatePercentiles();

    public abstract void calculateDiffPercentiles();

    public AbstractReport() {
        final Locale useThisLocale = (Stapler.getCurrentRequest() != null) ? Stapler.getCurrentRequest().getLocale() : Locale.getDefault();

        percentFormat = new ThreadLocal<DecimalFormat>() {

            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(useThisLocale));
            }
        };

        dataFormat = new ThreadLocal<DecimalFormat>() {

            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(useThisLocale));
            }
        };
    }

    public String errorPercentFormated() {
        Stapler.getCurrentRequest().getLocale();
        return percentFormat.get().format(errorPercent());
    }

    protected void checkPercentileAndSet(Double key, Long value) {
        if (value != null) {
            percentilesValues.put(key, value);
            isCalculatedPercentilesValues = true;
        }
    }

    protected List<Double> parsePercentiles(String percentiles) {
        final List<Double> res = new ArrayList<>();
        if (!StringUtils.isBlank(percentiles)) {
            String[] percs = percentiles.split(",");
            for (String perc : percs) {
                try {
                    res.add(Double.parseDouble(perc));
                } catch (NumberFormatException ex) {
                    LOGGER.log(Level.WARNING, "Cannot parse percentile value " + perc);
                }
            }
        }
        return res;
    }

    public abstract long getAverage();

    public String getAverageFormated() {
        return dataFormat.get().format(getAverage());
    }

    public abstract long getMedian();

    public String getMeanFormated() {
        return dataFormat.get().format(getMedian());
    }

    public abstract long get90Line();

    public String get90LineFormated() {
        return dataFormat.get().format(get90Line());
    }

    public abstract long getMax();

    public String getMaxFormated() {
        return dataFormat.get().format(getMax());
    }

    public abstract long getMin();

    public abstract int samplesCount();

    public abstract String getHttpCode();

    public abstract long getAverageDiff();

    public abstract long getMedianDiff();

    public abstract long get90LineDiff();

    public abstract double getErrorPercentDiff();

    public abstract String getLastBuildHttpCodeIfChanged();

    public abstract int getSamplesCountDiff();

    public boolean isExcludeResponseTime() {
        return excludeResponseTime;
    }

    public void setExcludeResponseTime(boolean excludeResponseTime) {
        this.excludeResponseTime = excludeResponseTime;
    }

    protected boolean isIncludeResponseTime(HttpSample sample) {
        return !(sample.isFailed() && excludeResponseTime && !sample.isSummarizer());
    }

    public Map<Double, Long> getPercentilesValues() {
        if (!isCalculatedPercentilesValues) {
            calculatePercentiles();
            calculateDiffPercentiles();
        }
        return percentilesValues;
    }

    public Map<Double, Long> getPercentilesDiffValues() {
        if (!isCalculatedPercentilesValues) {
            calculatePercentiles();
            calculateDiffPercentiles();
        }
        return percentilesDiffValues;
    }

    public String getPercentileLabel(Double perc) {
        if (perc == 0.0) {
            return "Min(ms)";
        } else if (perc == 50.0) {
            return "Median(ms)";
        } else if (perc == 100.0) {
            return "Max(ms)";
        } else {
            return "Line " + perc + "(ms)";
        }
    }
}
