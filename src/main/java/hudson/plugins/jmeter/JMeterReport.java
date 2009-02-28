package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.digester.Digester;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

public class JMeterReport implements ModelObject {

	private JMeterBuildAction buildAction;

	JMeterBuildAction getBuildAction() {
		return buildAction;
	}

	void setBuildAction(JMeterBuildAction buildAction) {
		this.buildAction = buildAction;
	}

	private final Map<String, UriReport> uriReportMap = new HashMap<String, UriReport>();

	JMeterReport() {
	}

	JMeterReport(JMeterBuildAction buildAction, File pFile) throws IOException {
		this.buildAction = buildAction;
		Digester digester = createDigester();
		try {
			digester.parse(pFile);
		} catch (SAXException e) {
			throw new IOException2("Failed to parse " + pFile, e);
		}
	}

	public void addSample(HttpSample pHttpSample) throws SAXException {
		String uri = pHttpSample.getUri();
		if (uri == null) {
			buildAction
					.getHudsonConsoleWriter()
					.println(
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

	public int countErrors() {
		int nbError = 0;
		for (UriReport currentReport : uriReportMap.values()) {
			nbError += currentReport.countErrors();
		}
		return nbError;
	}

	private Digester createDigester() {
		Digester digester = new Digester();
		digester.setClassLoader(getClass().getClassLoader());
		digester.push(this);
		String sampleFormat2_0 = "testResults/sampleResult";
		String sampleFormat2_1 = "testResults/httpSample";
		digester.addObjectCreate(sampleFormat2_0, HttpSample.class);
		digester.addObjectCreate(sampleFormat2_1, HttpSample.class);

		String[] attributeNamesFormat2_0 = new String[] { "label", "timeStamp",
				"time", "success" };
		String[] attributeNamesFormat2_1 = new String[] { "lb", "ts", "t", "s" };
		String[] propertyNames = new String[] { "uri", "date", "duration",
				"successful" };
		digester.addSetProperties(sampleFormat2_0, attributeNamesFormat2_0,
				propertyNames);
		digester.addSetProperties(sampleFormat2_1, attributeNamesFormat2_1,
				propertyNames);
		ConvertUtils.register(new Converter() {
			public Object convert(Class type, Object value) {
				return new Date(Long.valueOf(value.toString()));
			}
		}, Date.class);
		digester.addSetNext(sampleFormat2_0, "addSample");
		digester.addSetNext(sampleFormat2_1, "addSample");
		return digester;
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

	public String getDisplayName() {
		return "JMeter";
	}

	public UriReport getDynamic(String token, StaplerRequest req,
			StaplerResponse rsp) throws IOException {
		return getUriReportMap().get(token);
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

	public Map<String, UriReport> getUriReportMap() {
		return uriReportMap;
	}

	public int size() {
		int size = 0;
		for (UriReport currentReport : uriReportMap.values()) {
			size += currentReport.size();
		}
		return size;
	}

}
