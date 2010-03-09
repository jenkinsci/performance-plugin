package hudson.plugins.performance;

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

public class PerformancePublisher extends Recorder {

	@Override
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final BuildStepDescriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(PerformancePublisher.class);
		}

		@Override
		public String getDisplayName() {
			return Messages.Publisher_DisplayName();
		}

		@Override
		public String getHelpFile() {
			return "/plugin/performance/help.html";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws hudson.model.Descriptor.FormException {
			PerformancePublisher performancePublisher = new PerformancePublisher();
			req.bindParameters(performancePublisher, "performance.");
			return performancePublisher;
		}

	}

	private int errorFailedThreshold = 0;

	private int errorUnstableThreshold = 0;

	private String filename;

	public static File getPerformanceReport(AbstractBuild<?, ?> build, String performanceReportName) {
		return new File(build.getRootDir(), PerformanceReportMap
				.getPerformanceReportFileRelativePath(getPerformanceReportBuildFileName(performanceReportName)));
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new PerformanceProjectAction(project);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * <p>
	 * Delete the date suffix appended to the Performance result files by the Maven
	 * Performance plugin
	 * </p>
	 * 
	 * @param performanceReportWorkspaceName
	 * @return the name of the PerformanceReport in the Build
	 */
	public static String getPerformanceReportBuildFileName(String performanceReportWorkspaceName) {
		String result = performanceReportWorkspaceName;
		if (performanceReportWorkspaceName != null) {
			Pattern p = Pattern.compile("-[0-9]*\\.xml");
			Matcher matcher = p.matcher(performanceReportWorkspaceName);
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
		logger.println("Recording Performance reports " + getFilename());
		List<String> filenameList = manageFilename(filename);
		boolean result = true;
		PerformanceBuildAction performanceBuildAction = new PerformanceBuildAction(build, logger);
		build.addAction(performanceBuildAction);
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
					logger.println("Performance file " + src + " not found. Has the report generated? Setting Build to "
							+ build.getResult().toString());
					return true;
				}
				;
				if (!src.isDirectory()) {
					result = result && manageOnePerformanceReport(build, src, performanceBuildAction, logger);
				} else {
					List<FilePath> listSrc = new ArrayList<FilePath>(0);
					listSrc = src.list();
					Boolean resultManage = true;
					List<String> performanceReportListNameFile = new ArrayList<String>(listSrc.size());
					for (FilePath filePath : listSrc) {
						resultManage = resultManage
								&& manageOnePerformanceReport(build, filePath, performanceBuildAction, logger);
						performanceReportListNameFile.add(getPerformanceReportBuildFileName(filePath.getName()));
					}

					result = result && resultManage;
				}
			}

		}
		return result;
	}

	/**
	 * <p>
	 * This function is use to analyse One Performance report and save this analyze
	 * in global variable
	 * </p>
	 * 
	 * @param build
	 * @param src
	 * @param performanceBuildAction
	 * @param logger
	 * @return boolean
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Boolean manageOnePerformanceReport(AbstractBuild<?, ?> build, FilePath src, PerformanceBuildAction performanceBuildAction,
			PrintStream logger) throws IOException, InterruptedException {
		final File localReport = getPerformanceReport(build, src.getName());
		if (!localReport.getParentFile().exists()) {
			localReport.getParentFile().mkdirs();
		}
		if (src.isDirectory()) {
			logger.println("File : "+src.getName()+" is a directory, ant not a Performance Report");
			return true;
		}
		src.copyTo(new FilePath(localReport));
		if (performanceBuildAction.getPerformanceReportMap().get().isFailed(
				(PerformancePublisher.getPerformanceReportBuildFileName(src.getName())))) {
			build.setResult(Result.UNSTABLE);
			logger.println("Performance report analysis failed. Setting Build to " + build.getResult().toString());
			return true;
		}

		if (errorUnstableThreshold > 0 && errorUnstableThreshold < 100) {
			logger.println("Performance's percentage error greater or equal than " + errorUnstableThreshold
					+ "% sets the build as " + Result.UNSTABLE.toString().toLowerCase());
		}
		if (errorFailedThreshold > 0 && errorFailedThreshold < 100) {
			logger.println("Performance's percentage error greater or equal than " + errorFailedThreshold
					+ "% sets the build as " + Result.FAILURE.toString().toLowerCase());
		}
		double errorPercent = performanceBuildAction.getPerformanceReportMap().get().getPerformanceReport(
				(PerformancePublisher.getPerformanceReportBuildFileName(src.getName()))).errorPercent();
		if (errorFailedThreshold > 0 && errorPercent >= errorFailedThreshold) {
			build.setResult(Result.FAILURE);
		} else if (errorUnstableThreshold > 0 && errorPercent >= errorUnstableThreshold
				&& build.getResult() != Result.FAILURE) {
			build.setResult(Result.UNSTABLE);
		}
		logger.println("Performance has reported a " + errorPercent + "% of errors running the tests. Setting Build to "
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
