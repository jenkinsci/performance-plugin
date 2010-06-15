package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * A report about a particular tested URI.
 * 
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements ModelObject, Comparable<UriReport> {

	public static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

	/**
	 * Individual HTTP invocations to this URI and how they went.
	 */
	private final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

	/**
	 * The parent object to which this object belongs.
	 */
	private final PerformanceReport performanceReport;

	/**
	 * Escaped {@link #uri} that doesn't contain any letters that cannot be used
	 * as a token in URL.
	 */
	private final String staplerUri;

	private String uri;

	UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
		this.performanceReport = performanceReport;
		this.staplerUri = staplerUri;
		setUri(uri);
	}

	public void addHttpSample(HttpSample httpSample) {
		httpSampleList.add(httpSample);
	}

	public int compareTo(UriReport uriReport) {
		if (uriReport == this) {
			return 0;
		}
		return uriReport.getUri().compareTo(this.getUri());
	}

	public int countErrors() {
		int nbError = 0;
		for (HttpSample currentSample : httpSampleList) {
			if (!currentSample.isSuccessful()) {
				nbError++;
			}
		}
		return nbError;
	}

	public double errorPercent() {
		return ((double) countErrors()) / size() * 100;
	}

	public long getAverage() {
		long average = 0;
		for (HttpSample currentSample : httpSampleList) {
			average += currentSample.getDuration();
		}
		return average / size();
	}

	public AbstractBuild<?, ?> getBuild() {
		return performanceReport.getBuild();
	}

	public String getDisplayName() {
		return getUri();
	}

	public List<HttpSample> getHttpSampleList() {
		return httpSampleList;
	}

	public PerformanceReport getPerformanceReport() {
		return performanceReport;
	}

	public long getMax() {
		long max = Long.MIN_VALUE;
		for (HttpSample currentSample : httpSampleList) {
			max = Math.max(max, currentSample.getDuration());
		}
		return max;
	}

	public long getMin() {
		long min = Long.MAX_VALUE;
		for (HttpSample currentSample : httpSampleList) {
			min = Math.min(min, currentSample.getDuration());
		}
		return min;
	}

	public String getStaplerUri() {
		return staplerUri;
	}

	public String getUri() {
		return uri;
	}

	public boolean isFailed() {
		return countErrors() != 0;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int size() {
		return httpSampleList.size();
	}

	public String encodeUriReport() throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(120);
		sb.append(performanceReport.getReportFileName()).append(GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(END_PERFORMANCE_PARAMETER);
		return URLEncoder.encode(sb.toString(), "UTF-8");
	}

}
