package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerProxy;

public class JMeterBuildAction implements Action, StaplerProxy {

	private static final Logger logger = Logger
			.getLogger(JMeterBuildAction.class.getName());

	private static final long serialVersionUID = 1L;

	private transient WeakReference<JMeterReport> jmeterReport;

	private final AbstractBuild<?, ?> build;

	public JMeterBuildAction(AbstractBuild<?, ?> pBuild) {
		build = pBuild;
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

	public JMeterReport getJmeterReport() {
		JMeterReport meterReport = null;
		if ((jmeterReport == null) || (jmeterReport.get() == null)) {
			File reportFile = JMeterPublisher.getJMeterReport(getBuild());
			try {
				meterReport = new JMeterReport(this, reportFile);
				jmeterReport = new WeakReference<JMeterReport>(meterReport);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to load " + reportFile, e);
			}
		} else {
			meterReport = jmeterReport.get();
		}
		return meterReport;
	}

	public boolean isFailed() {
		return getJmeterReport() == null;
	}

	public Object getTarget() {
		return getJmeterReport();
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}
}
