package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.util.ArrayList;
import java.util.List;

public class UriReport implements ModelObject {

	private final JMeterReport jmeterReport;

	private final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

	private String uri;

	private final String staplerUri;

	UriReport(JMeterReport jmeterReport, String staplerUri, String uri) {
		this.jmeterReport = jmeterReport;
		this.staplerUri = staplerUri;
		setUri(uri);
	}

	public void addHttpSample(HttpSample httpSample) {
		httpSampleList.add(httpSample);
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

	public long getAverage() {
		long average = 0;
		for (HttpSample currentSample : httpSampleList) {
			average += currentSample.getDuration();
		}
		return average / size();
	}

	public AbstractBuild getBuild() {
		return jmeterReport.getBuild();
	}

	public List<HttpSample> getHttpSampleList() {
		return httpSampleList;
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

	public String getDisplayName() {
		return getUri();
	}

	public String getStaplerUri() {
		return staplerUri;
	}
}
