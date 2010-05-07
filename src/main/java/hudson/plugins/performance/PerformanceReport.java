package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Represents a single performance report, which consists of multiple {@link UriReport}s for
 * different URLs that was tested.
 *
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends DefaultHandler implements Comparable<PerformanceReport> {

	private PerformanceBuildAction buildAction;

	private HttpSample httpSample;

	private String reportFileName = null;

    /**
     * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
     */
	private final Map<String, UriReport> uriReportMap = new HashMap<String, UriReport>();

	PerformanceReport() {
	}

	PerformanceReport(PerformanceBuildAction buildAction, File pFile) throws IOException {
		this.buildAction = buildAction;
		this.reportFileName = pFile.getName();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		SAXParser parser;

		try {
			parser = factory.newSAXParser();
			parser.parse(pFile, this);
		} catch (ParserConfigurationException e) {
			throw new IOException2("Failed to create parser ", e);
		} catch (SAXException e) {
			throw new IOException2("Failed to parse " + pFile, e);
		}
	}

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
		return ((double) countErrors()) / size() * 100;
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

	/**
	 * Performance XML log format is in http://jakarta.apache.org/jmeter/usermanual/listeners.html
	 * 
	 * There are two different tags which delimit jmeter samples:
	 *    httpSample for http samples 
	 *    sample     for non http samples
	 *   
	 * There are also two different XML formats which we have to handle: 
	 *   v2.0 = "label", "timeStamp", "time", "success" 
	 *   v2.1 = "lb", "ts", "t", "s"
	 *   
	 * JUnit XML format is different : tag "testcase" with attributes : "name"
	 * and "time". If there is one error, there is an other tag, "failure" in
	 * testcase tag.
	 * For exemple, Junit format is used by SOAPUI.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("httpSample".equalsIgnoreCase(qName)
				|| "sample".equalsIgnoreCase(qName)) {
			HttpSample sample = new HttpSample();
			sample
					.setDate(new Date(
							Long
									.valueOf(attributes.getValue("ts") != null ? attributes
											.getValue("ts")
											: attributes.getValue("timeStamp"))));
			sample.setDuration(Long
					.valueOf(attributes.getValue("t") != null ? attributes
							.getValue("t") : attributes.getValue("time")));
			sample.setSuccessful(Boolean
					.valueOf(attributes.getValue("s") != null ? attributes
							.getValue("s") : attributes.getValue("success")));
			sample.setUri(attributes.getValue("lb") != null ? attributes
					.getValue("lb") : attributes.getValue("label"));
			addSample(sample);
		} else if ("testcase".equalsIgnoreCase(qName)) {
			if (status != 0) {
				addSample(currentSample);
			}
			status = 1;
			currentSample = new HttpSample();
			currentSample.setDate(new Date(0));
			String time = attributes.getValue("time");
			StringTokenizer st = new StringTokenizer(time, ".");
			List<String> listTime = new ArrayList<String>(2);
			while (st.hasMoreTokens()) {
				listTime.add(st.nextToken());
			}
			currentSample.setDuration(Long.valueOf(listTime.get(0)));
			currentSample.setSuccessful(true);
			currentSample.setUri(attributes.getValue("name"));

		} else if ("failure".equalsIgnoreCase(qName) && status != 0) {
			currentSample.setSuccessful(false);
			addSample(currentSample);
			status = 0;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (("testsuite".compareToIgnoreCase(qName) == 0 || "testcase"
				.compareToIgnoreCase(qName) == 0)
				&& status != 0) {
			addSample(currentSample);
			status = 0;
		}
	}

	private static HttpSample currentSample;
	private static int status;

}
