package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.kohsuke.stapler.StaplerProxy;

public class PerformanceBuildAction implements Action, StaplerProxy {
	private static final long serialVersionUID = 1L;

	private final AbstractBuild<?, ?> build;

	private transient final PrintStream hudsonConsoleWriter;

	private transient WeakReference<PerformanceReportMap> performanceReportMap;

	private static final Logger logger = Logger.getLogger(PerformanceBuildAction.class.getName());
	
	public PerformanceBuildAction(AbstractBuild<?, ?> pBuild, PrintStream logger) {
		build = pBuild;
		hudsonConsoleWriter = logger;
		performanceReportMap = new WeakReference<PerformanceReportMap>(new PerformanceReportMap(this));
	}

	public String getDisplayName() {
		return Messages.BuildAction_DisplayName();
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public String getUrlName() {
		return "performance";
	}

	public Object getTarget() {
		File repo = new File(build.getRootDir(), PerformanceReportMap.getPerformanceReportDirRelativePath());
		List<File> pFileList = new ArrayList<File>(0);
		for (File file : repo.listFiles()) {
			pFileList.add(file);
		}
		PerformanceReportMap jmList = null;
		try {
			jmList = new PerformanceReportMap(this, pFileList);
		} catch (IOException e) {
			logger.error(e);
		}
		return jmList;
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	PrintStream getHudsonConsoleWriter() {
		return hudsonConsoleWriter;
	}

	public PerformanceReportMap getPerformanceReportMap() {
		PerformanceReportMap reportMap;
        WeakReference<PerformanceReportMap> wr = this.performanceReportMap;
        if (wr!=null) {
            reportMap = wr.get();
            if (reportMap!=null)
                return reportMap;
        }

        reportMap = new PerformanceReportMap(this);
        this.performanceReportMap = new WeakReference<PerformanceReportMap>(reportMap);
		return reportMap;
	}

	public void setPerformanceReportMap(WeakReference<PerformanceReportMap> performanceReportMap) {
		this.performanceReportMap = performanceReportMap;
	}
}
