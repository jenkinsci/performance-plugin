package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.Messages;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements SimpleBuildStep {

    protected final static String CHECK_COMMAND = "bzt --help";
    protected final static String PERFORMANCE_TEST_COMMAND = "bzt";
    protected final String DEFAULT_REPORTING_CONFIG = getClass().getResource("defaultReport.yml").getPath();


    @Symbol("performanceTest")
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.PerformanceTest_Name();
        }
    }


    private String testConfigurationFiles;
    private String testOptions;

    @DataBoundConstructor
    public PerformanceTestBuild(String testConfigurationFiles, String testOptions) {
        this.testConfigurationFiles = (testConfigurationFiles == null) ? StringUtils.EMPTY : testConfigurationFiles;
        this.testOptions = (testOptions == null) ? StringUtils.EMPTY : testOptions;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();

        Runtime runtime = Runtime.getRuntime();
        final Process checkProcess = runtime.exec(CHECK_COMMAND);
        int checkProcessCode = checkProcess.waitFor();
        if (checkProcessCode != 0) {
            printStreamToLogger(checkProcess.getErrorStream(), logger);
            run.setResult(Result.FAILURE);
            return;
        }

        final Process runPerformanceTestProcess = runtime.exec(PERFORMANCE_TEST_COMMAND + ' ' +
                DEFAULT_REPORTING_CONFIG + " " +
                testConfigurationFiles + " " +
                testOptions);

        runPerformanceTestProcess.getOutputStream().close(); // Taurus =(

        int code = runPerformanceTestProcess.waitFor();

        printStreamToLogger(checkProcess.getInputStream(), logger);
        if (code != 0) {
            printStreamToLogger(checkProcess.getErrorStream(), logger);
            run.setResult(Result.FAILURE);
            return;
        }

        run.setResult(Result.SUCCESS);
        // TODO: add post build action
    }

    protected static void printStreamToLogger(InputStream source, PrintStream target) {
        BufferedReader input = new BufferedReader(new InputStreamReader(source));
        String line;

        try {
            while ((line = input.readLine()) != null)
                target.println(line);
        } catch (IOException e) {
            target.println("Reading of error stream caused next exception: " + e.getMessage());
        }
    }

    public String getTestConfigurationFiles() {
        return testConfigurationFiles;
    }

    public String getTestOptions() {
        return testOptions;
    }

    @DataBoundSetter
    public void setTestConfigurationFiles(String testConfigurationFiles) {
        this.testConfigurationFiles = testConfigurationFiles;
    }

    @DataBoundSetter
    public void setTestOptions(String testOptions) {
        this.testOptions = testOptions;
    }
}
