package hudson.plugins.performance.build;

import com.google.common.base.Throwables;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.Messages;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.output.NullOutputStream;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements SimpleBuildStep {
    public static final Logger LOGGER = Logger.getLogger(PerformanceTestBuild.class.getName());

    protected final static String PERFORMANCE_TEST_COMMAND = "bzt";
    protected final static String VIRTUALENV_COMMAND = "virtualenv";
    protected final static String HELP_OPTION = "--help";
    protected final static String VIRTUALENV_PATH_UNIX = "/taurus-venv/bin/";
    protected final static String VIRTUALENV_PATH_WINDOWS = "\\taurus-venv\\Scripts\\";

    protected final static String[] CHECK_BZT_COMMAND = new String[]{PERFORMANCE_TEST_COMMAND, HELP_OPTION};
    protected final static String[] CHECK_VIRTUALENV_COMMAND = new String[]{VIRTUALENV_COMMAND, HELP_OPTION};

    protected final static String[] CREATE_LOCAL_PYTHON_COMMAND_WITH_SYSTEM_PACKAGES_OPTION =
            new String[]{VIRTUALENV_COMMAND, "--clear", "--system-site-packages", "taurus-venv"};
    protected final static String[] CREATE_LOCAL_PYTHON_COMMAND = new String[]{VIRTUALENV_COMMAND, "--clear", "taurus-venv"};

    protected final static String DEFAULT_CONFIG_FILE = "jenkins-report.yml";


    @Symbol({"bzt","performanceTest"})
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
    private boolean printDebugOutput = false;
    private boolean alwaysUseVirtualenv = false;
    private boolean useSystemSitePackages = true;
    private boolean generatePerformanceTrend = true;
    private boolean useBztExitCode = true;
    private String bztVersion = "";
    private String workingDirectory = "";
    /**
     * Use 'workingDirectory' for set bzt working directory
     */
    @Deprecated
    private transient String workspace = "";

    @DataBoundConstructor
    public PerformanceTestBuild(String params) {
        this.params = params;
    }

    /**
     * This method, invoked after object is resurrected from persistence
     */
    public Object readResolve() {
        if (workspace != null && !workspace.isEmpty()) {
            workingDirectory = workspace;
            workspace = "";
        }
        return this;
    }


    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return generatePerformanceTrend  ? new PerformanceProjectAction(project) : null;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        EnvVars envVars = run.getEnvironment(listener);
        addPipelineEnvVars(run, envVars);

        FilePath virtualenvWorkspace;
        try {
            virtualenvWorkspace = getVirtualenvWorkspace(run, workspace, logger);
        } catch (Exception ex) {
            logger.println("[ERROR] Performance test: " + ex.getMessage());
            run.setResult(Result.FAILURE);
            return;
        }

        boolean isVirtualenvInstallation = false;
        if ((!alwaysUseVirtualenv && isGlobalBztInstalled(workspace, logger, launcher, envVars)) ||
                (isVirtualenvInstallation = installBztAndCheck(virtualenvWorkspace, logger, launcher, envVars))) {


            FilePath bztWorkingDirectory = getBztWorkingDirectory(workspace);
            try {
                bztWorkingDirectory.mkdirs();
            } catch (IOException ex) {
                logger.println("Performance test: Cannot create working directory because of error: " + ex.getMessage());
                run.setResult(Result.FAILURE);
                return;
            }

            int testExitCode = runPerformanceTest(bztWorkingDirectory, virtualenvWorkspace, logger, launcher, envVars, isVirtualenvInstallation);

            run.setResult(useBztExitCode ?
                    getBztJobResult(testExitCode) :
                    getJobResult(testExitCode)
            );

            if (generatePerformanceTrend && run.getResult().isBetterThan(Result.FAILURE)) {
                generatePerformanceTrend(bztWorkingDirectory.getRemote(), run, workspace, launcher, listener);
            }

            return;
        }

        run.setResult(Result.FAILURE);
    }

    private void addPipelineEnvVars(Run<?, ?> run, EnvVars envVars) {
        if (run.getClass().getCanonicalName().startsWith("org.jenkinsci.plugins.workflow")) {
            List<? extends Action> allActions = run.getActions();
            if (!allActions.isEmpty()) {
                for (Action action : allActions) {
                    if ("org.jenkinsci.plugins.workflow.cps.EnvActionImpl".equals(action.getClass().getCanonicalName())) {
                        addEnvVars(action, envVars);
                    }
                }
            }
        }
    }

    private void addEnvVars(Action action, EnvVars envVars) {
        try {
            Class<? extends Action> actionClass = action.getClass();
            Method method = actionClass.getMethod("getOverriddenEnvironment");
            Map<String, String> map = (Map<String, String>) method.invoke(action);
            envVars.overrideAll(map);
        } catch (Throwable ex) {
            LOGGER.warning("Failed to add envVars from action: " + action.getClass());
        }
    }

    private FilePath getVirtualenvWorkspace(Run<?, ?> run, FilePath workspace, PrintStream logger) throws Exception {
        return workspace.getRemote().contains(" ") ?
                createTemporaryWorkspace(run, workspace, logger) :
                workspace;
    }

    private FilePath createTemporaryWorkspace(Run<?, ?> run, FilePath workspace, PrintStream logger) throws Exception {
        logger.println("[WARNING] Performance test: Job workspace contains spaces in path. Virtualenv does not support such path. Creating temporary workspace for virtualenv.");
        File baseTmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (baseTmpDir.getAbsolutePath().contains(" ")) {
            logger.println("[WARNING] Performance test: Temporary folder contains spaces in path.");
            throw new InvalidPathException(baseTmpDir.getAbsolutePath(), "Virtualenv cannot be installed in workspace that contains spaces in path.");
        }
        File tempDir = new File(baseTmpDir.getAbsolutePath(), "perf-test-virtualenv-workspace-" + configJobName(run.getParent().getName()));
        FilePath tempWorkspace = new FilePath(workspace.getChannel(), tempDir.getAbsolutePath());
        tempWorkspace.mkdirs();
        return tempWorkspace;
    }

    private String configJobName(String displayName) {
        return displayName.replaceAll(" ", "-");
    }

    protected FilePath getBztWorkingDirectory(FilePath jobWorkspace) {
        return (workingDirectory != null && !workingDirectory.isEmpty()) ?
                (isAbsoluteFilePath() ?
                        // absolute workspace
                        new FilePath(jobWorkspace.getChannel(), workingDirectory) :
                        //relative workspace
                        new FilePath(jobWorkspace, workingDirectory)
                ) :
                jobWorkspace;
    }

    private boolean isAbsoluteFilePath() {
        return new File(workingDirectory).isAbsolute();
    }

    protected void generatePerformanceTrend(String path, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        new PerformancePublisher(path + "/aggregate-results.xml", -1, -1, "", 0, 0, 0, 0, 0, false, "", false, false, false, false, null).
                perform(run, workspace, launcher, listener);
    }

    private boolean installBztAndCheck(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        return installBzt(workspace, logger, launcher, envVars) &&
                isVirtualenvBztInstalled(workspace, logger, launcher, envVars);
    }

    private boolean installBzt(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        return isVirtualenvInstalled(workspace, logger, launcher, envVars) &&
                createVirtualenvAndInstallBzt(workspace, logger, launcher, envVars);
    }

    private boolean createVirtualenvAndInstallBzt(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        return createIsolatedPython(workspace, logger, launcher, envVars) &&
                installBztInVirtualenv(workspace, logger, launcher, envVars);
    }

    // Step 1.1: Check bzt using "bzt --help".
    private boolean isGlobalBztInstalled(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        logger.println("Performance test: Checking global bzt installation...");
        boolean result = isSuccessCode(runCmd(CHECK_BZT_COMMAND, workspace, new NullOutputStream(), launcher, envVars));
        logger.println(result ?
                "Performance test: Found global bzt installation." :
                "Performance test: You don't have global bzt installed on this Jenkins host. Installing it globally will speed up job. Run 'sudo pip install bzt' to install it."
        );
        return result;
    }

    // Step 1.2: If bzt not installed check virtualenv using "virtualenv --help".
    private boolean isVirtualenvInstalled(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        logger.println("Performance test: Checking virtualenv tool availability...");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean result = isSuccessCode(runCmd(CHECK_VIRTUALENV_COMMAND, workspace, outputStream, launcher, envVars));
        logger.println(result ?
                "Performance test: Found virtualenv tool." :
                "Performance test: No virtualenv found on this Jenkins host. Install it with 'sudo pip install virtualenv'."
        );
        if (!result || printDebugOutput) {
            logger.write(outputStream.toByteArray());
        }
        return result;
    }

    // Step 1.3: Create local python using "virtualenv --clear taurus-venv".
    private boolean createIsolatedPython(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        logger.println("Performance test: Creating virtualev at 'taurus-venv'...");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean result = isSuccessCode(runCmd(useSystemSitePackages ?
                        CREATE_LOCAL_PYTHON_COMMAND_WITH_SYSTEM_PACKAGES_OPTION :
                        CREATE_LOCAL_PYTHON_COMMAND,
                workspace, outputStream, launcher, envVars));
        logger.println(result ?
                "Performance test: Done creating virtualenv." :
                "Performance test: Failed to create virtualenv at 'taurus-venv'"
        );
        if (!result || printDebugOutput) {
            logger.write(outputStream.toByteArray());
        }
        return result;
    }

    // Step 1.4: Install bzt in virtualenv using "taurus-venv/bin/pip install bzt".
    private boolean installBztInVirtualenv(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        logger.println("Performance test: Installing bzt into 'taurus-venv'");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean result = isSuccessCode(runCmd(getBztInstallCommand(workspace), workspace, outputStream, launcher, envVars));
        logger.println(result ?
                "Performance test: bzt installed successfully." :
                "Performance test: Failed to install bzt into 'taurus-venv'"
        );
        if (!result || printDebugOutput) {
            logger.write(outputStream.toByteArray());
        }
        return result;
    }

    // Step 1.5: Check bzt using "taurus-venv/bin/bzt --help"
    private boolean isVirtualenvBztInstalled(FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        logger.println("Performance test: Checking installed bzt...");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean result = isSuccessCode(runCmd(getBztCheckCommand(workspace), workspace, outputStream, launcher, envVars));
        logger.println(result ?
                "Performance test: bzt is operational." :
                "Performance test: Failed to run bzt inside virtualenv."
        );
        if (!result || printDebugOutput) {
            logger.write(outputStream.toByteArray());
        }
        return result;
    }

    // Step 2: Run performance test.
    private int runPerformanceTest(FilePath bztWorkingDirectory, FilePath workspace, PrintStream logger, Launcher launcher, EnvVars envVars, boolean isVirtualenvInstallation) throws InterruptedException, IOException {
        final List<String> testCommand = new ArrayList<>();

        testCommand.add((isVirtualenvInstallation ? getVirtualenvPath(workspace) : "") + PERFORMANCE_TEST_COMMAND);
        String[] parsedParams;
        try {
            parsedParams = CommandLineUtils.translateCommandline(envVars.expand(this.params));
        } catch (Exception e) {
            logger.println("Failed parse Taurus parameters");
            e.printStackTrace(logger);
            return 1;
        }

        for (String param : parsedParams) {
            if (!param.isEmpty()) {
                testCommand.add(param);
            }
        }

        if (generatePerformanceTrend) {
            testCommand.add(extractDefaultReportToWorkingDirectory(bztWorkingDirectory));
        }

        logger.println("Performance test: run " + Arrays.toString(testCommand.toArray()));
        return runCmd(testCommand.toArray(new String[testCommand.size()]), bztWorkingDirectory, logger, launcher, envVars);
    }

    public boolean isSuccessCode(int code) {
        return code == 0;
    }

    public Result getJobResult(int code) {
        if (code == 0) {
            return Result.SUCCESS;
        } else {
            return Result.FAILURE;
        }
    }


    public Result getBztJobResult(int code) {
        if (code == 0) {
            return Result.SUCCESS;
        } else if (code == 1) {
            return Result.FAILURE;
        } else {
            return Result.UNSTABLE;
        }
    }

    private String getVirtualenvPath(FilePath workspace) {
        return workspace.getRemote() + (Functions.isWindows() ?
                VIRTUALENV_PATH_WINDOWS :
                VIRTUALENV_PATH_UNIX);
    }

    // return bzt install command
    private String[] getBztInstallCommand(FilePath workspace) throws IOException, InterruptedException {
        return new String[]{
                getVirtualenvPath(workspace) + "pip", "install", getInstallCommand(workspace)};
    }

    private String getInstallCommand(FilePath workspace) throws IOException, InterruptedException {
        if (bztVersion != null && !bztVersion.isEmpty()) {
            return (isPathToFile(workspace) || isURLToFile()) ?
                    bztVersion :
                    (PERFORMANCE_TEST_COMMAND + "==" + bztVersion);
        } else {
            return PERFORMANCE_TEST_COMMAND;
        }
    }

    private boolean isURLToFile() {
        try {
            if (bztVersion.startsWith("git+")) {
                new URL(bztVersion.substring(4));
            } else {
                new URL(bztVersion);
            }
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean isPathToFile(FilePath workspace) throws IOException, InterruptedException {
        return new FilePath(workspace.getChannel(), bztVersion).exists();
    }

    // return bzt check command
    private String[] getBztCheckCommand(FilePath workspace) {
        return new String[]{getVirtualenvPath(workspace) + "bzt", HELP_OPTION};
    }

    public int runCmd(String[] commands, FilePath workspace, OutputStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
        try {
            return launcher.launch().cmds(commands).envs(envVars).stdout(logger).stderr(logger).pwd(workspace).start().join();
        } catch (IOException ex) {
            logger.write(ex.getMessage().getBytes());
            if (printDebugOutput) {
                logger.write(Throwables.getStackTraceAsString(ex).getBytes());
            }
            return 1;
        }
    }

    protected String extractDefaultReportToWorkingDirectory(FilePath workingDirectory) throws IOException, InterruptedException {
        FilePath defaultConfig = workingDirectory.child(DEFAULT_CONFIG_FILE);
        defaultConfig.copyFrom(getClass().getResourceAsStream(DEFAULT_CONFIG_FILE));
        return defaultConfig.getRemote();
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public boolean isPrintDebugOutput() {
        return printDebugOutput;
    }

    public boolean isAlwaysUseVirtualenv() {
        return alwaysUseVirtualenv;
    }

    @DataBoundSetter
    public void setAlwaysUseVirtualenv(boolean alwaysUseVirtualenv) {
        this.alwaysUseVirtualenv = alwaysUseVirtualenv;
    }

    @DataBoundSetter
    public void setPrintDebugOutput(boolean printDebugOutput) {
        this.printDebugOutput = printDebugOutput;
    }

    public boolean isUseSystemSitePackages() {
        return useSystemSitePackages;
    }

    @DataBoundSetter
    public void setUseSystemSitePackages(boolean useSystemSitePackages) {
        this.useSystemSitePackages = useSystemSitePackages;
    }

    public boolean isGeneratePerformanceTrend() {
        return generatePerformanceTrend;
    }

    @DataBoundSetter
    public void setGeneratePerformanceTrend(boolean generatePerformanceTrend) {
        this.generatePerformanceTrend = generatePerformanceTrend;
    }

    public boolean isUseBztExitCode() {
        return useBztExitCode;
    }

    @DataBoundSetter
    public void setUseBztExitCode(boolean useBztExitCode) {
        this.useBztExitCode = useBztExitCode;
    }

    public String getWorkspace() {
        return workspace;
    }

    @DataBoundSetter
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
        readResolve();
    }

    public String getBztVersion() {
        return bztVersion;
    }

    @DataBoundSetter
    public void setBztVersion(String bztVersion) {
        this.bztVersion = bztVersion;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @DataBoundSetter
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
