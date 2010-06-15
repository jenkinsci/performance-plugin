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
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformancePublisher extends Recorder {
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public String getDisplayName() {
			return Messages.Publisher_DisplayName();
		}

		@Override
		public String getHelpFile() {
			return "/plugin/performance/help.html";
		}

		public List<PerformanceReportParserDescriptor> getParserDescriptors() {
			return PerformanceReportParserDescriptor.all();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	private int errorFailedThreshold = 0;

	private int errorUnstableThreshold = 0;

	/**
	 * @deprecated as of 1.3. for compatibility
	 */
	private transient String filename;

	/**
	 * Configured report parseres.
	 */
	private List<PerformanceReportParser> parsers;

	@DataBoundConstructor
	public PerformancePublisher(int errorFailedThreshold, int errorUnstableThreshold, List<? extends PerformanceReportParser> parsers) {
		this.errorFailedThreshold = errorFailedThreshold;
		this.errorUnstableThreshold = errorUnstableThreshold;
		if (parsers == null)
			parsers = Collections.emptyList();
		this.parsers = new ArrayList<PerformanceReportParser>(parsers);
	}

	public static File getPerformanceReport(AbstractBuild<?, ?> build, String performanceReportName) {
		return new File(build.getRootDir(), PerformanceReportMap.getPerformanceReportFileRelativePath(getPerformanceReportBuildFileName(performanceReportName)));
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new PerformanceProjectAction(project);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	public List<PerformanceReportParser> getParsers() {
		return parsers;
	}

	/**
	 * <p>
	 * Delete the date suffix appended to the Performance result files by the
	 * Maven Performance plugin
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

	/**
	 * look for performance reports based in the configured parameter includes.
	 * 'includes' is - an Ant-style pattern - a list of files and folders
	 * separated by the characters ;:,
	 */
	protected static List<FilePath> locatePerformanceReports(FilePath workspace, String includes) throws IOException, InterruptedException {

		// First use ant-style pattern
		try {
			FilePath[] ret = workspace.list(includes);
			if (ret.length > 0) {
				return Arrays.asList(ret);
			}
		} catch (IOException e) {
		}

		// If it fails, do a legacy search
		ArrayList<FilePath> files = new ArrayList<FilePath>();
		String parts[] = includes.split("\\s*[;:,]+\\s*");
		for (String path : parts) {
			FilePath src = workspace.child(path);
			if (src.exists()) {
				if (src.isDirectory()) {
					files.addAll(Arrays.asList(src.list("**/*")));
				} else {
					files.add(src);
				}
			}
		}
		return files;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();

		if (errorUnstableThreshold > 0 && errorUnstableThreshold < 100) {
			logger.println("Performance: Percentage of errors greater or equal than " + errorUnstableThreshold + "% sets the build as " + Result.UNSTABLE.toString().toLowerCase());
		} else {
			logger.println("Performance: No threshold configured for making the test " + Result.UNSTABLE.toString().toLowerCase());
		}
		if (errorFailedThreshold > 0 && errorFailedThreshold < 100) {
			logger.println("Performance: Percentage of errors greater or equal than " + errorFailedThreshold + "% sets the build as " + Result.FAILURE.toString().toLowerCase());
		} else {
			logger.println("Performance: No threshold configured for making the test " + Result.FAILURE.toString().toLowerCase());
		}

		// add the report to the build object.
		PerformanceBuildAction a = new PerformanceBuildAction(build, logger, parsers);
		build.addAction(a);

		for (PerformanceReportParser parser : parsers) {
			String glob = parser.glob;
			logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

			List<FilePath> files = locatePerformanceReports(build.getWorkspace(), glob);

			if (files.isEmpty()) {
				if (build.getResult().isWorseThan(Result.UNSTABLE)) {
					return true;
				}
				build.setResult(Result.FAILURE);
				logger.println("Performance: no " + parser.getReportName() + " files matching '" + glob + "' have been found. Has the report generated?. Setting Build to " + build.getResult());
				return true;
			}

			List<File> localReports = copyReportsToMaster(build, logger, files);
			Collection<PerformanceReport> parsedReports = parser.parse(build, localReports, listener);

			// mark the build as unstable or failure depending on the outcome.
			for (PerformanceReport r : parsedReports) {
				r.setBuildAction(a);
				double errorPercent = r.errorPercent();
				if (errorFailedThreshold > 0 && errorPercent >= errorFailedThreshold) {
					build.setResult(Result.FAILURE);
				} else if (errorUnstableThreshold > 0 && errorPercent >= errorUnstableThreshold) {
					build.setResult(Result.UNSTABLE);
				}
				logger.println("Performance: File " + r.getReportFileName() + " reported " + errorPercent + "% of errors during the tests. Build status is: " + build.getResult());
			}
		}

		return true;
	}

	private List<File> copyReportsToMaster(AbstractBuild<?, ?> build, PrintStream logger, List<FilePath> files) throws IOException, InterruptedException {
		List<File> localReports = new ArrayList<File>();
		for (FilePath src : files) {
			final File localReport = getPerformanceReport(build, src.getName());
			if (src.isDirectory()) {
				logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
				continue;
			}
			src.copyTo(new FilePath(localReport));
			localReports.add(localReport);
		}
		return localReports;
	}

	public Object readResolve() {
		// data format migration
		if (parsers == null)
			parsers = new ArrayList<PerformanceReportParser>();
		if (filename != null) {
			parsers.add(new JMeterParser(filename));
			filename = null;
		}
		return this;
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
