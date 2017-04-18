package hudson.plugins.performance.build;

import hudson.EnvVars;
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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements SimpleBuildStep {

    protected final static String PERFORMANCE_TEST_COMMAND = "bzt";
    protected final static String VIRTUALENV_COMMAND = "virtualenv";
    protected final static String HELP_COMMAND = "--help";
    protected final static String VIRTUALENV_PATH = "taurus-venv/bin/";
    protected final static String[] CHECK_BZT_COMMAND = new String[]{PERFORMANCE_TEST_COMMAND, HELP_COMMAND};
    protected final static String[] CHECK_VIRTUALENV_BZT_COMMAND = new String[]{VIRTUALENV_PATH + PERFORMANCE_TEST_COMMAND, HELP_COMMAND};
    protected final static String[] CHECK_VIRTUALENV_COMMAND = new String[]{VIRTUALENV_COMMAND, HELP_COMMAND};
    protected final static String[] CREATE_LOCAL_PYTHON_COMMAND = new String[]{VIRTUALENV_COMMAND, "--clear", /*"--system-site-packages",*/ "taurus-venv"};
    protected final static String[] INSTALL_BZT_COMMAND = new String[]{VIRTUALENV_PATH + "pip", /*"--no-cache-dir",*/ "install", "https://github.com/Blazemeter/taurus.git"};
    protected final static String DEFAULT_CONFIG_FILE = "defaultReport.yml";

    protected final static String PERFORMANCE_TEST_LOG_FILE = "performanceTest.log";

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


    private String params;

    @DataBoundConstructor
    public PerformanceTestBuild(String params) throws IOException {
        this.params = params;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        boolean isVirtualenvInstallation = false;
        boolean isBztInstalled = false;

        PrintStream performanceTestLogger = getPerformanceTestLogger(run, logger);

        EnvVars envVars = run.getEnvironment(listener);

        logger.println("Performance test: Checking bzt installed on your machine.");
        performanceTestLogger.println("Dump performance logger:\r\nPerformance logger: Checking bzt installed on your machine.");
        // Step 1: Check bzt using "bzt --help".
        if (!runCmd(CHECK_BZT_COMMAND, workspace, performanceTestLogger, launcher, envVars)) {
            logger.println("Performance test: You have not bzt on your machine. Next step is checking virtualenv.");
            performanceTestLogger.println("Performance logger: You have not bzt on your machine. Next step is checking virtualenv.");
            // Step 1.1: If bzt not installed check virtualenv using "virtualenv --help".
            if (runCmd(CHECK_VIRTUALENV_COMMAND, workspace, performanceTestLogger, launcher, envVars)) {
                logger.println("Performance test: Checking virtualenv is OK. Next step is creation isolated Python environments.");
                performanceTestLogger.println("Performance logger: Checking virtualenv is OK. Next step is creation isolated Python environments.");
                // Step 1.2: Create local python using "virtualenv --clear --system-site-packages taurus-venv".
                if (runCmd(CREATE_LOCAL_PYTHON_COMMAND, workspace, performanceTestLogger, launcher, envVars)) {
                    logger.println("Performance test: Creation isolated Python environments is OK. Next step is install bzt.");
                    performanceTestLogger.println("Performance logger: Creation isolated Python environments is OK. Next step is install bzt.");
                    // Step 1.3: Install bzt in virtualenv using "taurus-venv/bin/pip install bzt".
                    if (runCmd(INSTALL_BZT_COMMAND, workspace, performanceTestLogger, launcher, envVars)) {
                        logger.println("Performance test: bzt installed successfully. Checking bzt.");
                        performanceTestLogger.println("Performance logger: bzt installed successfully. Checking bzt.");
                        // Step 1.4: Check bzt using "taurus-venv/bin/bzt --help"
                        if (runCmd(CHECK_VIRTUALENV_BZT_COMMAND, workspace, performanceTestLogger, launcher, envVars)) {
                            logger.println("Performance test: bzt is working.");
                            isVirtualenvInstallation = true;
                        }
                    } else {
                        logger.println("Performance test: Failed to install bzt into isolated Python environments \"taurus-venv\"");
                        performanceTestLogger.close();
                        printPerformanceTestLog(run, logger);
                    }
                } else {
                    logger.println("Performance test: Failed to create isolated Python environments \"taurus-venv\"");
                }
            } else {
                logger.println("Performance test: You have not virtualenv on your machine. Please, install virtualenv on your machine.");
            }
        } else {
            logger.println("Performance test: bzt is installed on your machine.");
            isBztInstalled = true;
        }


        if (isBztInstalled || isVirtualenvInstallation) {
            // Step 2: Run performance test.
            String[] params = this.params.split(" ");
            final List<String> testCommand = new ArrayList<String>(params.length + 2);
            testCommand.add((isVirtualenvInstallation ? VIRTUALENV_PATH : "") + PERFORMANCE_TEST_COMMAND);
            for (String param : params) {
                if (!param.isEmpty()) {
                    testCommand.add(param);
                }
            }
            testCommand.add(extractDefaultReportToWorkspace(workspace));
            logger.println("Performance test: run " + Arrays.toString(testCommand.toArray()));
            if (runCmd(testCommand.toArray(new String[testCommand.size()]), workspace, logger, launcher, envVars)) {
                run.setResult(Result.SUCCESS);
                performanceTestLogger.close();
                return;
            }
        }

        run.setResult(Result.FAILURE);
        performanceTestLogger.close();
    }

    protected void printPerformanceTestLog(Run<?, ?> run, PrintStream logger) throws IOException {
        File perfLog = new File(run.getRootDir(), PERFORMANCE_TEST_LOG_FILE);
        BufferedReader reader = new BufferedReader(new FileReader(perfLog));
        try {
            String line = reader.readLine();
            while (line != null) {
                logger.println(line);
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
    }

    protected PrintStream getPerformanceTestLogger(Run<?, ?> run, PrintStream logger) throws IOException {
        File perfLog = new File(run.getRootDir(), PERFORMANCE_TEST_LOG_FILE);
        perfLog.delete();
        perfLog.createNewFile();
        logger.println("Create performance test log file: " + perfLog.getAbsolutePath());
        return new PrintStream(perfLog);
    }

    public static boolean runCmd(String[] commands, FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException {
        try {
            return launcher.launch().cmds(commands).envs(envVars).stdout(logger).stderr(logger).pwd(workspace).start().join() == 0;
        } catch (IOException ex) {
            ex.printStackTrace(logger);
            return false;
        }
    }

    protected String extractDefaultReportToWorkspace(FilePath workspace) throws IOException, InterruptedException {
        FilePath defaultConfig = workspace.child(DEFAULT_CONFIG_FILE);
        defaultConfig.copyFrom(getClass().getResourceAsStream(DEFAULT_CONFIG_FILE));
        return defaultConfig.getRemote();
    }

    public String getParams() {
        return params;
    }

    @DataBoundSetter
    public void setParams(String params) {
        this.params = params;
    }
}
