package hudson.plugins.performance.reports;

import org.kohsuke.stapler.Stapler;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Abstract class for classes with samplesCount, error, mean, average, 90 line, min and max attributes
 */
public abstract class AbstractReport {

    protected final ThreadLocal<DecimalFormat> percentFormat;
    protected final ThreadLocal<DecimalFormat> dataFormat; // three decimals

    abstract public int countErrors();

    abstract public double errorPercent();

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

    abstract public long getAverage();

    public String getAverageFormated() {
        return dataFormat.get().format(getAverage());
    }

    abstract public long getMedian();

    public String getMeanFormated() {
        return dataFormat.get().format(getMedian());
    }

    abstract public long get90Line();

    public String get90LineFormated() {
        return dataFormat.get().format(get90Line());
    }

    abstract public long getMax();

    public String getMaxFormated() {
        return dataFormat.get().format(getMax());
    }

    abstract public long getMin();

    abstract public int samplesCount();

    abstract public String getHttpCode();

    abstract public long getAverageDiff();

    abstract public long getMedianDiff();

    abstract public double getErrorPercentDiff();

    abstract public String getLastBuildHttpCodeIfChanged();

    abstract public int getSamplesCountDiff();
}
