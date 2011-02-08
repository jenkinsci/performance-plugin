package hudson.plugins.performance;

import org.kohsuke.stapler.Stapler;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Abstract class for classes with size, error, mean, average, 90 line, min and max attributes
 */
public abstract class AbstractReport {

  private NumberFormat percentFormat;
  private NumberFormat dataFormat;

  abstract public int countErrors();

  abstract public double errorPercent();

  public AbstractReport() {
    if (Stapler.getCurrentRequest() != null) {
      Locale.setDefault(Stapler.getCurrentRequest().getLocale());
    }
    percentFormat = new DecimalFormat("0.0");
    dataFormat = new DecimalFormat("#,###");
  }

  public String errorPercentFormated() {
    Stapler.getCurrentRequest().getLocale();
    return percentFormat.format(errorPercent());

  }

  abstract public long getAverage();

  public String getAverageFormated() {
    return dataFormat.format(getAverage());
  }

  abstract public long getMedian();

  public String getMeanFormated() {
    return dataFormat.format(getMedian());
  }

  abstract public long get90Line();

  public String get90LineFormated() {
    return dataFormat.format(get90Line());
  }

  abstract public long getMax();

  public String getMaxFormated() {
    return dataFormat.format(getMax());
  }

  abstract public long getMin();

  abstract public int size();
}
