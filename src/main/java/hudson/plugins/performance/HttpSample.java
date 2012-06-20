package hudson.plugins.performance;

import java.util.Date;

/**
 * Information about a particular HTTP request and how that went.
 *
 * This object belongs under {@link UriReport}.
 */
public class HttpSample implements Comparable<HttpSample> {

  private long duration;

  private boolean successful;

  private Date date;

  private String uri;
  
  private String httpCode = "";

  // Summarizer fields
  private long summarizerMin;

  private long summarizerMax;

  private float summarizerErrors;

  private long summarizerSamples;
    

  public long getDuration() {
    return duration;
  }

  public Date getDate() {
    return date;
  }

  public String getUri() {
    return uri;
  }
  
  public String getHttpCode() {
      return httpCode;
  }

  public long getSummarizerSamples() {
    return summarizerSamples;
  }

   public long getSummarizerMin() {
    return summarizerMin;
  }

   public long getSummarizerMax() {
    return summarizerMax;
  }

   public float getSummarizerErrors() {
    return summarizerErrors;
  }

  public boolean isFailed() {
    return !isSuccessful();
  }

  public boolean isSuccessful() {
    return successful;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  public void setDate(Date time) {
    this.date = time;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
  
  public void setHttpCode(String httpCode) {
    this.httpCode = httpCode;
  }

  public void setSummarizerSamples(long summarizerSamples) {
    this.summarizerSamples = summarizerSamples;
  }

   public void setSummarizerMin(long summarizerMin) {
    this.summarizerMin = summarizerMin;
  }

   public void setSummarizerMax(long summarizerMax) {
    this.summarizerMax = summarizerMax;
  }

   public void setSummarizerErrors(float summarizerErrors) {
    this.summarizerErrors= summarizerErrors;
  }

  public int compareTo(HttpSample o) {
    return (int) (getDuration() - o.getDuration());
  }
}
