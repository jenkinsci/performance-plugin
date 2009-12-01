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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private int errorFailedThreshold = 0;

	private int errorUnstableThreshold = 0;

	private String filename;

	public static File getJMeterReport(AbstractBuild<?, ?> build, String jmeterReportName) {
		return new File(build.getRootDir(), JMeterReportMap
				.getJMeterReportFileRelativePath(getJMeterReportBuildFileName(jmeterReportName)));
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new JMeterProjectAction(project);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * <p>
	 * Delete the date suffix appended to the JMeter result files by the Maven
	 * JMeter plugin
	 * </p>
	 * 
	 * @param jmeterReportWorkspaceName
	 * @return the name of the jmeterReport in the Build
	 */
	public static String getJMeterReportBuildFileName(String jmeterReportWorkspaceName) {
		String result = jmeterReportWorkspaceName;
		if (jmeterReportWorkspaceName != null) {
			Pattern p = Pattern.compile("-[0-9]*\\.xml");
			Matcher matcher = p.matcher(jmeterReportWorkspaceName);
			if (matcher.find()) {
				result = matcher.replaceAll(".xml");
			}
		}
		return result;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		logger.println("Recording JMeter reports " + getFilename());
		List<String> filenameList = manageFilename(filename);
		boolean result = true;
		JMeterBuildAction jmeterBuildAction = new JMeterBuildAction(build, logger);
		build.addAction(jmeterBuildAction);
		for (String filename : filenameList) {
			if (filename.compareTo("") != 0) {
				final FilePath src = build.getWorkspace().child(filename);

				if (!src.exists()) {
					if (build.getResult().isWorseThan(Result.UNSTABLE)) {
						// build has failed, so that's probably why this was not
						// generated.
						// so don't report an error
						return true;
					}
					build.setResult(Result.FAILURE);
					logger.println("JMeter file " + src + " not found. Has the report generated? Setting Build to "
							+ build.getResult().toString());
					return true;
				}
				;
				if (!src.isDirectory()) {
					result = result && manageOneJMeterReport(build, src, jmeterBuildAction, logger);
				} else {
					List<FilePath> listSrc = new ArrayList<FilePath>(0);
					listSrc = src.list();
					Boolean resultManage = true;
					List<String> jmeterReportListNameFile = new ArrayList<String>(listSrc.size());
					for (FilePath filePath : listSrc) {
						resultManage = resultManage
								&& manageOneJMeterReport(build, filePath, jmeterBuildAction, logger);
						jmeterReportListNameFile.add(getJMeterReportBuildFileName(filePath.getName()));
					}

					result = result && resultManage;
				}
			}

		}
		return result;
	}

	/**
	 * <p>
	 * This function is use to analyse One jmeter report and save this analyze
	 * in global variable
	 * </p>
	 * 
	 * @param build
	 * @param src
	 * @param jmeterBuildAction
	 * @param logger
	 * @return boolean
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Boolean manageOneJMeterReport(AbstractBuild<?, ?> build, FilePath src, JMeterBuildAction jmeterBuildAction,
			PrintStream logger) throws IOException, InterruptedException {
		final File localReport = getJMeterReport(build, src.getName());
		if (!localReport.getParentFile().exists()) {
			localReport.getParentFile().mkdirs();
		}
		if (src.isDirectory()) {
			logger.println("File : "+src.getName()+" is a directory, ant not a JMeter Report");
			return true;
		}
		src.copyTo(new FilePath(localReport));
		if (jmeterBuildAction.getJmeterReportMap().get().isFailed(
				(JMeterPublisher.getJMeterReportBuildFileName(src.getName())))) {
			build.setResult(Result.UNSTABLE);
			logger.println("JMeter report analysis failed. Setting Build to " + build.getResult().toString());
			return true;
		}

		if (errorUnstableThreshold > 0 && errorUnstableThreshold < 100) {
			logger.println("JMeter's percentage error greater or equal than " + errorUnstableThreshold
					+ "% sets the build as " + Result.UNSTABLE.toString().toLowerCase());
		}
		if (errorFailedThreshold > 0 && errorFailedThreshold < 100) {
			logger.println("JMeter's percentage error greater or equal than " + errorFailedThreshold
					+ "% sets the build as " + Result.FAILURE.toString().toLowerCase());
		}
		double errorPercent = jmeterBuildAction.getJmeterReportMap().get().getJmeterReport(
				(JMeterPublisher.getJMeterReportBuildFileName(src.getName()))).errorPercent();
		if (errorFailedThreshold > 0 && errorPercent >= errorFailedThreshold) {
			build.setResult(Result.FAILURE);
		} else if (errorUnstableThreshold > 0 && errorPercent >= errorUnstableThreshold
				&& build.getResult() != Result.FAILURE) {
			build.setResult(Result.UNSTABLE);
		}
		logger.println("JMeter has reported a " + errorPercent + "% of errors running the tests. Setting Build to "
				+ build.getResult().toString());

		return true;
	}

	/**
	 * <p>
	 * Read the filename in the conf files, and transform it to a ordonned list
	 * of repository/files
	 * </p>
	 * 
	 * @param filename
	 * @return
	 */
	private List<String> manageFilename(String filename) {
		StringTokenizer st = new StringTokenizer(filename, ";");
		ArrayList<String> filenameList = new ArrayList<String>(0);
		while (st.hasMoreTokens()) {
			filenameList.add(st.nextToken());
		}
		Collections.sort(filenameList);
		return filenameList;
	}

	public int getErrorFailedThreshold() {
		return errorFailedThreshold;
	}

	public void setErrorFailedThreshold(int errorFailedThreshold) {
		this.errorFailedThreshold = Math.max(0, Math.min(errorFailedThreshold, 100));
	}

	public int getErrorUnstableThreshold() {
		return errorUnstableThreshold;
	}

	public void setErrorUnstableThreshold(int errorUnstableThreshold) {
		this.errorUnstableThreshold = Math.max(0, Math.min(errorUnstableThreshold, 100));
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}
