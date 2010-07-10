package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import hudson.model.TaskListener;
import org.apache.log4j.Logger;

/**
 * Root object of a performance report.
 */
public class PerformanceReportMap implements ModelObject {
    /**
     * The {@link PerformanceBuildAction} that this report belongs to.
     */
	private transient PerformanceBuildAction buildAction;

    /**
     * {@link PerformanceReport}s are keyed by {@link PerformanceReport#reportFileName}
     *
     * Test names are arbitrary human-readable and URL-safe string that identifies an individual report.
     */
    private Map<String, PerformanceReport> performanceReportMap = new HashMap<String, PerformanceReport>();

	private static final String PERFORMANCE_REPORTS_DIRECTORY = "performance-reports";

    /**
     * Parses the reports and build a {@link PerformanceReportMap}.
     *
     * @throws IOException
     *      If a report fails to parse.
     */
	PerformanceReportMap(PerformanceBuildAction buildAction, TaskListener listener) throws IOException {
		this.buildAction = buildAction;

        File repo = new File(getBuild().getRootDir(), PerformanceReportMap.getPerformanceReportDirRelativePath());

        // files directly under the directory is for JMeter, for a compatibility reasons.
        List<File> pFileList = Arrays.asList(repo.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return !f.isDirectory();
            }
        }));
        addAll(new JMeterParser("").parse(buildAction.getBuild(),pFileList,listener));

        // otherwise subdirectory name designates the parser ID.
        for (File dir : repo.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        })) {
            PerformanceReportParser p = buildAction.getParserById(dir.getName());
            if (p!=null)
                addAll(p.parse(buildAction.getBuild(),Arrays.asList(dir.listFiles()),listener));
        }
	}

    private void addAll(Collection<PerformanceReport> reports) {
        for (PerformanceReport r : reports) {
            r.setBuildAction(buildAction);
            performanceReportMap.put(r.getReportFileName(), r);
        }
    }

	public AbstractBuild<?, ?> getBuild() {
		return buildAction.getBuild();
	}

	PerformanceBuildAction getBuildAction() {
		return buildAction;
	}

	public String getDisplayName() {
		return Messages.Report_DisplayName();
	}

	public List<PerformanceReport> getPerformanceListOrdered() {
        List<PerformanceReport> listPerformance = new ArrayList<PerformanceReport>(getPerformanceReportMap().values());
		Collections.sort(listPerformance);
		return listPerformance;
	}

	public Map<String, PerformanceReport> getPerformanceReportMap() {
		return performanceReportMap;
	}

	/**
	 * <p>
	 * Give the Performance report with the parameter for name in Bean
	 * </p>
	 * 
	 * @param performanceReportName
	 * @return
	 */
	public PerformanceReport getPerformanceReport(String performanceReportName) {
        return performanceReportMap.get(performanceReportName);
	}

	/**
	 * Get a URI report within a Performance report file
	 * 
	 * @param uriReport
	 *            "Performance report file name";"URI name"
	 * @return
	 */
	public UriReport getUriReport(String uriReport) {
		if (uriReport != null) {
			String uriReportDecoded;
			try {
				uriReportDecoded = URLDecoder.decode(uriReport.replace(UriReport.END_PERFORMANCE_PARAMETER, ""), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
			StringTokenizer st = new StringTokenizer(uriReportDecoded, GraphConfigurationDetail.SEPARATOR);
			return getPerformanceReportMap().get(st.nextToken()).getUriReportMap().get(st.nextToken());
		} else {
			return null;
		}
	}

	public String getUrlName() {
		return "performanceReportList";
	}

	void setBuildAction(PerformanceBuildAction buildAction) {
		this.buildAction = buildAction;
	}

	public void setPerformanceReportMap(Map<String, PerformanceReport> performanceReportMap) {
		this.performanceReportMap = performanceReportMap;
	}

	public static String getPerformanceReportFileRelativePath(String reportFileName) {
		return getRelativePath(reportFileName);
	}

	public static String getPerformanceReportDirRelativePath() {
		return getRelativePath(null);
	}

	private static String getRelativePath(String reportFileName) {
		StringBuilder sb = new StringBuilder(100);
		sb.append(PERFORMANCE_REPORTS_DIRECTORY);
		if (reportFileName != null) {
			sb.append("/").append(reportFileName);
		}
		return sb.toString();
	}

	/**
	 * <p>
	 * Verify if the PerformanceReport exist the performanceReportName must to be like it
	 * is in the build
	 * </p>
	 * 
	 * @param performanceReportName
	 * @return boolean
	 */
	public boolean isFailed(String performanceReportName) {
		return getPerformanceReport(performanceReportName) == null;
	}
}
