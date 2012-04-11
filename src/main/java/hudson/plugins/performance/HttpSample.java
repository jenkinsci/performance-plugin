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
  
  public int compareTo(HttpSample o) {
    return (int) (getDuration() - o.getDuration());
  }
}
