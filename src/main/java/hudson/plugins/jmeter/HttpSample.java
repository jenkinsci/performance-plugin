package hudson.plugins.jmeter;

import java.util.Date;

public class HttpSample implements Comparable<HttpSample> {

	private long duration;

	private boolean successful;

	private Date date;

	private String uri;

	public long getDuration() {
		return duration;
	}

	public Date getTime() {
		return date;
	}

	public String getUri() {
		return uri;
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

	public void setTime(Date time) {
		this.date = time;
	}

	public void setTime(long time) {
		this.duration = time;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int compareTo(HttpSample o) {
		return (int) (o.getDuration() - getDuration());
	}

}
