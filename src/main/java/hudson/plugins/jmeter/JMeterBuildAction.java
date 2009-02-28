package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;

import org.apache.log4j.Logger;
import org.kohsuke.stapler.StaplerProxy;

public class JMeterBuildAction implements Action, StaplerProxy {

	private static final Logger logger = Logger
			.getLogger(JMeterBuildAction.class.getName());

	private static final long serialVersionUID = 1L;

	private transient WeakReference<JMeterReport> jmeterReport;

	private transient final PrintStream hudsonConsoleWriter;

	private final AbstractBuild<?, ?> build;

	public JMeterBuildAction(AbstractBuild<?, ?> pBuild, PrintStream logger) {
		build = pBuild;
		hudsonConsoleWriter = logger;
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	public String getDisplayName() {
		return "JMeter report";
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public JMeterReport getJmeterReport() {
		JMeterReport meterReport = null;
		if ((jmeterReport == null) || (jmeterReport.get() == null)) {
			File reportFile = JMeterPublisher.getJMeterReport(getBuild());
			try {
				meterReport = new JMeterReport(this, reportFile);
				if (meterReport.size() == 0) {
					hudsonConsoleWriter
							.println("jmeter report analysis is empty, ensure your jtl file is filled with samples.");
				}
				jmeterReport = new WeakReference<JMeterReport>(meterReport);
			} catch (IOException e) {
				logger.warn("Failed to load " + reportFile, e);
				Throwable ex = e;
				do {
					hudsonConsoleWriter.println(ex.getLocalizedMessage());
					ex = ex.getCause();
				} while (ex != null);
			}
		} else {
			meterReport = jmeterReport.get();
		}
		return meterReport;
	}

	PrintStream getHudsonConsoleWriter() {
		return hudsonConsoleWriter;
	}

	public Object getTarget() {
		return getJmeterReport();
	}

	public String getUrlName() {
		return "jmeter";
	}

	public boolean isFailed() {
		return getJmeterReport() == null;
	}
}
