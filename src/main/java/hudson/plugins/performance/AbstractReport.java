package hudson.plugins.performance;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.kohsuke.stapler.Stapler;

/**
 * Abstract class for classes with size, error, mean, average, 90 line, min and max attributes
 */
public abstract class AbstractReport {

  protected final DecimalFormat percentFormat;
  protected final DecimalFormat dataFormat; // three decimals

  abstract public int countErrors();

  abstract public double errorPercent();

  public AbstractReport() {
    final Locale useThisLocale = ( Stapler.getCurrentRequest() != null ) ? Stapler.getCurrentRequest().getLocale() : Locale.getDefault();

    percentFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance( useThisLocale ));
    dataFormat = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance( useThisLocale ));
  }

  public String errorPercentFormated() {
    Stapler.getCurrentRequest().getLocale();
    synchronized (percentFormat) {
      return percentFormat.format(errorPercent());
    }
  }

  abstract public long getAverage();

  public String getAverageFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getAverage());
    }
  }

  abstract public long getMedian();

  public String getMeanFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getMedian());
    }
  }

  abstract public long get90Line();

  public String get90LineFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(get90Line());
    }
  }

  abstract public long getMax();

  public String getMaxFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getMax());
    }
  }

  abstract public long getMin();

  abstract public int size();

  abstract public String getHttpCode();

  abstract public long getAverageDiff();

  abstract public long getMedianDiff();

  abstract public double getErrorPercentDiff();

  abstract public String getLastBuildHttpCodeIfChanged();

  abstract public int getSizeDiff();
}
