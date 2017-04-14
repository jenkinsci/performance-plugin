package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.Messages;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements SimpleBuildStep {

    protected final static String CHECK_BZT_COMMAND = "bzt --help";
    protected final static String CHECK_VIRTUALENV_COMMAND = "virtualenv --help";
    protected final static String CREATE_LOCAL_PYTHON_COMMAND = "virtualenv --clear --system-site-packages taurus-venv";
    protected final static String VIRTUALENV_PATH = "taurus-venv/bin/";
    protected final static String INSTALL_BZT_COMMAND = VIRTUALENV_PATH + "pip --no-cache-dir install bzt";
    protected final static String PERFORMANCE_TEST_COMMAND = "bzt";
    protected final static String DEFAULT_CONFIG_FILE = "defaultReport.yml";


    @Symbol("bzt")
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
    public PerformanceTestBuild(String testConfigurationFiles, String testOptions) throws IOException {
        this.testConfigurationFiles = (testConfigurationFiles == null) ? StringUtils.EMPTY : testConfigurationFiles;
        this.testOptions = (testOptions == null) ? StringUtils.EMPTY : testOptions;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        boolean isVirtualenvInstallation = false;
        boolean isBztInstalled = false;

        logger.println("Performance test: Checking bzt installed on your machine.");
        // Step 1: Check bzt using "bzt --help".
        if (!runCommand(CHECK_BZT_COMMAND, build, launcher, listener)) {                                                 // TODO: off help output?
            logger.println("Performance test: You have not got bzt on your machine. Next step is checking virtualenv.");
            // Step 1.1: If bzt not installed check virtualenv using "virtualenv --help".
            if (runCommand(CHECK_VIRTUALENV_COMMAND, build, launcher, listener)) {                                       // TODO: off help output?
                logger.println("Performance test: Checking virtualenv is OK. Next step is creation local python.");
                // Step 1.2: Create local python using "virtualenv --clear --system-site-packages taurus-venv".
                if (runCommand(CREATE_LOCAL_PYTHON_COMMAND, build, launcher, listener)) {                                // TODO: off help output?
                    logger.println("Performance test: Creation local python is OK. Next step is install bzt.");
                    // Step 1.3: Install bzt in virtualenv using "taurus-venv/bin/pip install bzt".
                    if (runCommand(INSTALL_BZT_COMMAND, build, launcher, listener)) {                                    // TODO: off help output?
                        logger.println("Performance test: bzt installed successfully. Checking bzt.");
                        // Step 1.4: Check bzt using "taurus-venv/bin/bzt --help"
                        if (runCommand(VIRTUALENV_PATH + CHECK_BZT_COMMAND, build, launcher, listener)) {                // TODO: off help output?
                            logger.println("Performance test: bzt is working.");
                            isVirtualenvInstallation = true;
                        } else {
                            // TODO: what we do when bzt in virtualenv doesn't work?
                        }
                    } else {
                        // TODO: what we do when bzt doesn't install in virtualenv?
                    }
                } else {
                    // TODO: what we do when virtualenv does create local python?
                }
            } else {
                // TODO: what we do when virtualenv does not installed?
            }
        } else {
            isBztInstalled = true;
        }

        if (isBztInstalled || isVirtualenvInstallation) {
            String bztExecution =
                    (isVirtualenvInstallation ? VIRTUALENV_PATH : "") +
                            PERFORMANCE_TEST_COMMAND + ' ' +
                            extractDefaultReportToWorkspace(workspace) + " " +
                            testConfigurationFiles + " " +
                            testOptions;

            // Step 2: Run performance test.
            if (runCommand(bztExecution, build, launcher, listener)) {
                build.setResult(Result.SUCCESS);
                return;
            }
        }

        build.setResult(Result.FAILURE);

        // TODO: add post build action
    }

    public static boolean runCommand(String command, Run<?, ?> build, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        return createCommand(command).perform((AbstractBuild<?, ?>) build, launcher, (BuildListener) listener);
    }

    public static Builder createCommand(String command) {
        return Functions.isWindows() ? new BatchFile(command) : new Shell(command);
    }


    protected String extractDefaultReportToWorkspace(FilePath workspace) throws IOException, InterruptedException {
        FilePath defaultConfig = workspace.child(DEFAULT_CONFIG_FILE);
        defaultConfig.copyFrom(getClass().getResourceAsStream(DEFAULT_CONFIG_FILE));
        return defaultConfig.getRemote();
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
