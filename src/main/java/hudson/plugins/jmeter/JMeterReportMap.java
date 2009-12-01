package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class JMeterReportMap implements ModelObject {

	private transient JMeterBuildAction buildAction;

	private static final String JMETER_REPORTS_DIRECTORY = "jmeter-reports";

	private static final Logger logger = Logger.getLogger(JMeterReportMap.class.getName());

	private Map<String, JMeterReport> jmeterReportMap = new HashMap<String, JMeterReport>();

	JMeterReportMap() {
	}

	JMeterReportMap(JMeterBuildAction buildAction) {
		this.buildAction = buildAction;
	}

	JMeterReportMap(JMeterBuildAction buildAction, List<File> pFileList) throws IOException {
		this.buildAction = buildAction;
		for (File pFile : pFileList) {
			jmeterReportMap.put(pFile.getName(), new JMeterReport(buildAction, pFile));
		}
	}

	public AbstractBuild<?, ?> getBuild() {
		return buildAction.getBuild();
	}

	JMeterBuildAction getBuildAction() {
		return buildAction;
	}

	public String getDisplayName() {
		return "JMeter";
	}

	public List<JMeterReport> getJmeterListOrdered() {
		Collection<JMeterReport> uriCollection = getJmeterReportMap().values();
		List<JMeterReport> listJmeter = new ArrayList<JMeterReport>(uriCollection.size());
		for (JMeterReport jMeterReport : uriCollection) {
			listJmeter.add(jMeterReport);
		}
		Collections.sort(listJmeter);
		return listJmeter;
	}

	public Map<String, JMeterReport> getJmeterReportMap() {
		return jmeterReportMap;
	}

	/**
	 * <p>
	 * Give the Jmeter rapport with the parameter for name in Bean
	 * </p>
	 * 
	 * @param jmeterReportName
	 * @return
	 */
	public JMeterReport getJmeterReport(String jmeterReportName) {
		JMeterReport meterReport = null;
		if ((jmeterReportMap == null) || (jmeterReportMap.get(jmeterReportName) == null)
				|| (jmeterReportMap.get(jmeterReportName) == null)) {
			File reportFile = new File(getBuild().getRootDir(), getJMeterReportFileRelativePath(jmeterReportName));
			try {
				meterReport = new JMeterReport(buildAction, reportFile);
				if (meterReport.size() == 0) {
					logger.warn("jmeter report analysis is empty, ensure your jtl file is filled with samples.");
				}
				if (jmeterReportMap == null) {
					jmeterReportMap = new HashMap<String, JMeterReport>();
				}
				jmeterReportMap.put(jmeterReportName, meterReport);
			} catch (IOException e) {
				logger.warn("Failed to load " + reportFile, e);
				Throwable ex = e;
				do {
					logger.warn(ex.getLocalizedMessage());
					ex = ex.getCause();
				} while (ex != null);
			}
		} else {
			meterReport = jmeterReportMap.get(jmeterReportName);
		}
		return meterReport;
	}

	/**
	 * Get a URI report within a JMeter report file
	 * 
	 * @param uriReport
	 *            "JMeter report file name";"URI name"
	 * @return
	 */
	public UriReport getUriReport(String uriReport) {
		if (uriReport != null) {
			String uriReportDecoded;
			try {
				uriReportDecoded = URLDecoder.decode(uriReport.replace(UriReport.END_JMETER_PARAMETER, ""), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
			StringTokenizer st = new StringTokenizer(uriReportDecoded, GraphConfigurationDetail.SEPARATOR);
			return getJmeterReportMap().get(st.nextToken()).getUriReportMap().get(st.nextToken());
		} else {
			return null;
		}
	}

	public String getUrlName() {
		return "jmeterReportList";
	}

	void setBuildAction(JMeterBuildAction buildAction) {
		this.buildAction = buildAction;
	}

	public void setJmeterReportMap(Map<String, JMeterReport> jmeterReportMap) {
		this.jmeterReportMap = jmeterReportMap;
	}

	public static String getJMeterReportFileRelativePath(String reportFileName) {
		return getRelativePath(reportFileName);
	}

	public static String getJMeterReportDirRelativePath() {
		return getRelativePath(null);
	}

	private static String getRelativePath(String reportFileName) {
		StringBuilder sb = new StringBuilder(100);
		sb.append(JMETER_REPORTS_DIRECTORY);
		if (reportFileName != null) {
			sb.append("/").append(reportFileName);
		}
		return sb.toString();
	}

	/**
	 * <p>
	 * Verify if the JmeterReport exist the jmeterReportName must to be like it
	 * is in the build
	 * </p>
	 * 
	 * @param jmeterReportName
	 * @return boolean
	 */
	public boolean isFailed(String jmeterReportName) {
		return getJmeterReport(jmeterReportName) == null;
	}
}
