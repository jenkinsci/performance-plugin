package hudson.plugins.performance;

import hudson.model.AbstractBuild;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single performance report, which consists of multiple {@link UriReport}s for
 * different URLs that was tested.
 *
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractReport implements Comparable<PerformanceReport> {

	private PerformanceBuildAction buildAction;

	private HttpSample httpSample;

	private String reportFileName = null;

    /**
     * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
     */
	private final Map<String, UriReport> uriReportMap = new HashMap<String, UriReport>();
	
	public void addSample(HttpSample pHttpSample) throws SAXException {
		String uri = pHttpSample.getUri();
		if (uri == null) {
			buildAction.getHudsonConsoleWriter().println(
					"label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
			return;
		}
		String staplerUri = uri.replace("http:", "").replaceAll("/", "_");
		UriReport uriReport = uriReportMap.get(staplerUri);
		if (uriReport == null) {
			uriReport = new UriReport(this, staplerUri, uri);
			uriReportMap.put(staplerUri, uriReport);
		}
		uriReport.addHttpSample(pHttpSample);
	}

	public int compareTo(PerformanceReport jmReport) {
		if (this == jmReport) {
			return 0;
		}
		return getReportFileName().compareTo(jmReport.getReportFileName());
	}

	public int countErrors() {
		int nbError = 0;
		for (UriReport currentReport : uriReportMap.values()) {
			nbError += currentReport.countErrors();
		}
		return nbError;
	}

	
	public double errorPercent() {
		return size() == 0 ? 0 : ((double) countErrors()) / size() * 100;
	}

	public long getAverage() {
		long result = 0;
		int size = size();
		if (size != 0) {
			long average = 0;
			for (UriReport currentReport : uriReportMap.values()) {
				average += currentReport.getAverage() * currentReport.size();
			}
			result = average / size;
		}
		return result;
	}
	
	public AbstractBuild<?, ?> getBuild() {
		return buildAction.getBuild();
	}

	PerformanceBuildAction getBuildAction() {
		return buildAction;
	}

	public String getDisplayName() {
		return  Messages.Report_DisplayName();
	}

	public UriReport getDynamic(String token) throws IOException {
		return getUriReportMap().get(token);
	}

	public HttpSample getHttpSample() {
		return httpSample;
	}

	public long getMax() {
		long max = Long.MIN_VALUE;
		for (UriReport currentReport : uriReportMap.values()) {
			max = Math.max(currentReport.getMax(), max);
		}
		return max;
	}
	
	public long getMin() {
		long min = Long.MAX_VALUE;
		for (UriReport currentReport : uriReportMap.values()) {
			min = Math.min(currentReport.getMin(), min);
		}
		return min;
	}

	public String getReportFileName() {
		return reportFileName;
	}

	public List<UriReport> getUriListOrdered() {
		Collection<UriReport> uriCollection = getUriReportMap().values();
		List<UriReport> UriReportList = new ArrayList<UriReport>(uriCollection);
		Collections.sort(UriReportList);
		return UriReportList;
	}

	public Map<String, UriReport> getUriReportMap() {
		return uriReportMap;
	}

	void setBuildAction(PerformanceBuildAction buildAction) {
		this.buildAction = buildAction;
	}

	public void setHttpSample(HttpSample httpSample) {
		this.httpSample = httpSample;
	}

	public void setReportFileName(String reportFileName) {
		this.reportFileName = reportFileName;
	}

	public int size() {
		int size = 0;
		for (UriReport currentReport : uriReportMap.values()) {
			size += currentReport.size();
		}
		return size;
	}
}
