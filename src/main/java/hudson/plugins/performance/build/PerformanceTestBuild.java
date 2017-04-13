package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.performance.Messages;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements BuildStep {

    protected final static String CHECK_BZT_COMMAND = "bzt --help";
    protected final static String CHECK_VIRTUALENV_COMMAND = "virtual --help";
    protected final static String CREATE_LOCAL_PYTHON_COMMAND = "virtualenv --clear --system-site-packages taurus-venv";
    protected final static String VIRTUALENV_PATH = "taurus-venv/bin/";
    protected final static String INSTALL_BZT_COMMAND = VIRTUALENV_PATH + "pip install bzt";
    protected final static String PERFORMANCE_TEST_COMMAND = "bzt";
    protected final static String DEFAULT_CONFIG_FILE = "defaultReport.yml";

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
    public PerformanceTestBuild(String testConfigurationFiles, String testOptions) throws IOException {
        this.testConfigurationFiles = (testConfigurationFiles == null) ? StringUtils.EMPTY : testConfigurationFiles;
        this.testOptions = (testOptions == null) ? StringUtils.EMPTY : testOptions;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean isVirtualenvInstallation = false;
        // Step 1: Check bzt using "bzt --help".
        if (!runCommand(CHECK_BZT_COMMAND, build, launcher, listener)) {                                                 // TODO: off help output?
            // Step 1.1: If bzt not installed check virtualenv using "virtualenv --help".
            if (runCommand(CHECK_VIRTUALENV_COMMAND, build, launcher, listener)) {                                       // TODO: off help output?
                // Step 1.2: Create local python using "virtualenv --clear --system-site-packages taurus-venv".
                if (runCommand(CREATE_LOCAL_PYTHON_COMMAND, build, launcher, listener)) {                                // TODO: off help output?
                    // Step 1.3: Install bzt in virtualenv using "taurus-venv/bin/pip install bzt".
                    if (runCommand(INSTALL_BZT_COMMAND, build, launcher, listener)) {                                    // TODO: off help output?
                        // Step 1.4: Check bzt using "taurus-venv/bin/bzt --help"
                        if (runCommand(VIRTUALENV_PATH + CHECK_BZT_COMMAND, build, launcher, listener)) {                // TODO: off help output?
                            isVirtualenvInstallation = true;
                        } else {
                            // TODO: what we do when bzt in virtualenv doesn't work?
                            return false;
                        }
                    } else {
                        // TODO: what we do when bzt doesn't install in virtualenv?
                        return false;
                    }
                } else {
                    // TODO: what we do when virtualenv does create local python?
                    return false;
                }
            } else {
                // TODO: what we do when virtualenv does not installed?
                return false;
            }
//            return false;
        }

        String bztExecution =
                (isVirtualenvInstallation ? VIRTUALENV_PATH : "") +
                PERFORMANCE_TEST_COMMAND + ' ' +
                extractDefaultReportToWorkspace(build.getWorkspace()) + " " +
                testConfigurationFiles + " " +
                testOptions;

        // Step 2: Run performance test.
        return runCommand(bztExecution, build, launcher, listener);
        // TODO: add post build action
    }

    public static boolean runCommand(String command, AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        return createCommand(command).perform(build, launcher, listener);
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
