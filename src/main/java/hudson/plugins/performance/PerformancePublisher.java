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

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
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

  /**
   * look for performance reports based in the configured parameter includes.
   * 'includes' is 
   *   - an Ant-style pattern
   *   - a list of files and folders separated by the characters ;:,  
   */
  protected static FilePath[] locatePerformanceReports(FilePath workspace,
      String includes) throws IOException, InterruptedException {

    // First use ant-style pattern
    try {
      FilePath[] ret = workspace.list(includes);
      if (ret.length > 0) {
        return ret;
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
    return files.toArray(new FilePath[files.size()]);
  }
	
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    PrintStream logger = listener.getLogger();
    
    if (filename == null || filename.length() == 0) {
      filename = "**/*.jtl";
    }
    
    logger.println("Performance: Recording reports [" + filename + "]");
    
    FilePath[] files = locatePerformanceReports(build.getWorkspace(), filename);
    
    if (files.length == 0) {
      if (build.getResult().isWorseThan(Result.UNSTABLE)) {
        return true;
      }
      build.setResult(Result.FAILURE);
      logger.println("Performance: no files matching '" + filename + 
          "' have been found. Has the report generated?. Setting Build to "
          + build.getResult().toString());
      return true;
    }
    
    if (errorUnstableThreshold > 0 && errorUnstableThreshold < 100) {
      logger.println("Performance: Percentage of errors greater or equal than " + errorUnstableThreshold
          + "% sets the build as " + Result.UNSTABLE.toString().toLowerCase());
    } else {
      logger.println("Performance: No threshold configured for making the test " + Result.UNSTABLE.toString().toLowerCase());
    }
    if (errorFailedThreshold > 0 && errorFailedThreshold < 100) {
      logger.println("Performance: Percentage of errors greater or equal than " + errorFailedThreshold
          + "% sets the build as " + Result.FAILURE.toString().toLowerCase());
    } else {
      logger.println("Performance: No threshold configured for making the test " + Result.FAILURE.toString().toLowerCase());
    }

    
    boolean resultManage = true;

    PerformanceBuildAction performanceBuildAction = new PerformanceBuildAction(build, logger);
    build.addAction(performanceBuildAction);
    List<String> performanceReportListNameFile = new ArrayList<String>(files.length);
    for (FilePath filePath : files) {
      resultManage = resultManage
          && manageOnePerformanceReport(build, filePath, performanceBuildAction, logger);
      performanceReportListNameFile.add(getPerformanceReportBuildFileName(filePath.getName()));

    }

    return resultManage;    
  }	

	/**
	 * <p>
	 * This function is use to analyze One Performance report and save this analyze
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
	private boolean manageOnePerformanceReport(AbstractBuild<?, ?> build, FilePath src, PerformanceBuildAction performanceBuildAction,
			PrintStream logger) throws IOException, InterruptedException {
	  
    logger.println("Performance: Parsing report file " + src.getName());
	  
		final File localReport = getPerformanceReport(build, src.getName());
		if (src.isDirectory()) {
			logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
			return true;
		}
		src.copyTo(new FilePath(localReport));
		if (performanceBuildAction.getPerformanceReportMap().get().isFailed(
				(PerformancePublisher.getPerformanceReportBuildFileName(src.getName())))) {
			build.setResult(Result.UNSTABLE);
			logger.println("Performance: Report analysis failed. Setting Build to " + build.getResult().toString());
			return true;
		}

		double errorPercent = performanceBuildAction.getPerformanceReportMap().get().getPerformanceReport(
				(PerformancePublisher.getPerformanceReportBuildFileName(src.getName()))).errorPercent();
		if (errorFailedThreshold > 0 && errorPercent >= errorFailedThreshold) {
			build.setResult(Result.FAILURE);
		} else if (errorUnstableThreshold > 0 && errorPercent >= errorUnstableThreshold
				&& build.getResult() != Result.FAILURE) {
			build.setResult(Result.UNSTABLE);
		}
		logger.println("Performance: Reported a " + errorPercent + "% of errors during the tests. Build status is: "
				+ build.getResult().toString());

		return true;
	}

	/**
	 * <p>
	 * Read the filename in the conf files, and transform it to a ordenned list
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
