package hudson.plugins.jmeter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class JMeterPublisher extends Recorder {

	@Override
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final BuildStepDescriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(JMeterPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Publish JMeter test result report";
		}

		@Override
		public String getHelpFile() {
			return "/plugin/jmeter/help.html";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws hudson.model.Descriptor.FormException {
			JMeterPublisher jmeterPublisher = new JMeterPublisher();
			req.bindParameters(jmeterPublisher, "jmeter.");
			return jmeterPublisher;
		}

	}

	private int errorUnstableThreshold = 0;

	private int errorFailedThreshold = 0;

	public int getErrorUnstableThreshold() {
		return errorUnstableThreshold;
	}

	public void setErrorUnstableThreshold(int errorUnstableThreshold) {
		this.errorUnstableThreshold = Math.max(0, Math.min(
				errorUnstableThreshold, 100));
	}

	public int getErrorFailedThreshold() {
		return errorFailedThreshold;
	}

	public void setErrorFailedThreshold(int errorFailedThreshold) {
		this.errorFailedThreshold = Math.max(0, Math.min(errorFailedThreshold,
				100));
	}

	public static File getJMeterReport(AbstractBuild<?, ?> build) {
		return new File(build.getRootDir(), "jmeter.xml");
	}

	private String filename;

	public String getFilename() {
		return filename;
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new JMeterProjectAction(project);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		logger.println("Recording JMeter reports " + getFilename());

		final FilePath src = build.getWorkspace().child(getFilename());

		if (!src.exists()) {
			if (build.getResult().isWorseThan(Result.UNSTABLE)) {
				// build has failed, so that's probably why this was not
				// generated.
				// so don't report an error
				return true;
			}
			build.setResult(Result.FAILURE);
			logger.println("JMeter file " + src
					+ " not found. Has the report generated? Setting Build to "
					+ build.getResult().toString());
			return true;
		}

		final File localReport = getJMeterReport(build);
		src.copyTo(new FilePath(localReport));

		JMeterBuildAction jmeterBuildAction = new JMeterBuildAction(build,
				logger);
		build.addAction(jmeterBuildAction);

		if (jmeterBuildAction.isFailed()) {
			build.setResult(Result.UNSTABLE);
			logger.println("JMeter report analysis failed. Setting Build to "
					+ build.getResult().toString());
			return true;
		}

		if (errorUnstableThreshold > 0 && errorUnstableThreshold < 100) {
			logger.println("JMeter's percentage error greater or equal than "
					+ errorUnstableThreshold + "% sets the build as "
					+ Result.UNSTABLE.toString().toLowerCase());
		}
		if (errorFailedThreshold > 0 && errorFailedThreshold < 100) {
			logger.println("JMeter's percentage error greater or equal than "
					+ errorFailedThreshold + "% sets the build as "
					+ Result.FAILURE.toString().toLowerCase());
		}
		double errorPercent = jmeterBuildAction.getJmeterReport()
				.errorPercent();
		if (errorFailedThreshold > 0 && errorPercent >= errorFailedThreshold) {
			build.setResult(Result.FAILURE);
		} else if (errorUnstableThreshold > 0
				&& errorPercent >= errorUnstableThreshold) {
			build.setResult(Result.UNSTABLE);
		}  
		logger.println("JMeter has reported a " + errorPercent
				+ "% of errors running the tests. Setting Build to "
				+ build.getResult().toString());

		return true;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
