package hudson.plugins.jmeter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Project;
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

	@Extension
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
			return Project.class.isAssignableFrom(jobType);
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws hudson.model.Descriptor.FormException {
			JMeterPublisher jmeterPublisher = new JMeterPublisher();
			req.bindParameters(jmeterPublisher, "jmeter.");
			return jmeterPublisher;
		}

	}

	public static File getJMeterReport(AbstractBuild<?, ?> build) {
		return new File(build.getRootDir(), "jmeter.xml");
	}

	private String filename;

	public String getFilename() {
		return filename;
	}

	@Override
	public Action getProjectAction(AbstractProject<?,?> project) {
		return project instanceof Project ? new JMeterProjectAction((Project)project) : null;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		logger.println("Recording JMeter reports " + getFilename());

		final FilePath src = build.getWorkspace().child(
				getFilename());

		if (!src.exists()) {
			if (build.getResult().isWorseThan(Result.UNSTABLE)) {
				// build has failed, so that's probably why this was not
				// generated.
				// so don't report an error
				return true;
			}
			logger.println("JMeter file " + src
					+ " not found. Has the report generated?");
			build.setResult(Result.FAILURE);
			return true;
		}

		final File localReport = getJMeterReport(build);
		src.copyTo(new FilePath(localReport));

		JMeterBuildAction jmeterBuildAction = new JMeterBuildAction(build,
				logger);
		build.addAction(jmeterBuildAction);

		if (jmeterBuildAction.isFailed()) {
			logger
					.println("JMeter report analysis failed. Setting Build to unstable.");
			build.setResult(Result.UNSTABLE);
		}

		return true;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
