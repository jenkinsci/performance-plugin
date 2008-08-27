package hudson.plugins.jmeter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class JMeterPublisher extends Publisher {

	private static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		protected DescriptorImpl() {
			super(JMeterPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Record JMeter report";
		}

		@Override
		public String getHelpFile() {
			return "/totoro/test/help.html";
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

	public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	private String filename;

	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		logger.println("Recording JMeter reports " + getFilename());

		final FilePath src = build.getProject().getWorkspace().child(
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

		JMeterBuildAction jmeterBuildAction = new JMeterBuildAction(build);
		build.addAction(jmeterBuildAction);

		if (jmeterBuildAction.isFailed()) {
			logger
					.println("JMeter report analysis failed. Setting Build to unstable.");
			build.setResult(Result.UNSTABLE);
		}

		return true;
	}

	public static File getJMeterReport(AbstractBuild<?, ?> build) {
		return new File(build.getRootDir(), "jmeter.xml");
	}

	@Override
	public Action getProjectAction(Project project) {
		return new JMeterProjectAction();
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

}
