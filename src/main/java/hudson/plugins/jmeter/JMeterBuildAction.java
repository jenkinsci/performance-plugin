package hudson.plugins.jmeter;

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

public class JMeterBuildAction implements Action, StaplerProxy {
	private static final long serialVersionUID = 1L;

	private final AbstractBuild<?, ?> build;

	private transient final PrintStream hudsonConsoleWriter;

	private transient WeakReference<JMeterReportMap> jmeterReportMap;

	private static final Logger logger = Logger.getLogger(JMeterBuildAction.class.getName());
	
	public JMeterBuildAction(AbstractBuild<?, ?> pBuild, PrintStream logger) {
		build = pBuild;
		hudsonConsoleWriter = logger;
		jmeterReportMap = new WeakReference<JMeterReportMap>(new JMeterReportMap(this));
	}

	public String getDisplayName() {
		return "JMeter report";
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public String getUrlName() {
		return "jmeter";
	}

	public Object getTarget() {
		File repo = new File(build.getRootDir(), JMeterReportMap.getJMeterReportDirRelativePath());
		List<File> pFileList = new ArrayList<File>(0);
		for (File file : repo.listFiles()) {
			pFileList.add(file);
		}
		JMeterReportMap jmList = null;
		try {
			jmList = new JMeterReportMap(this, pFileList);
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

	public WeakReference<JMeterReportMap> getJmeterReportMap() {
		JMeterReportMap reportMap;
		if(this.jmeterReportMap == null || this.jmeterReportMap.get() == null) {
			reportMap = new JMeterReportMap(this);
			this.jmeterReportMap = new WeakReference<JMeterReportMap>(reportMap);	
		}
		return this.jmeterReportMap;
	}

	public void setJmeterReportMap(WeakReference<JMeterReportMap> jmeterReportMap) {
		this.jmeterReportMap = jmeterReportMap;
	}
}
