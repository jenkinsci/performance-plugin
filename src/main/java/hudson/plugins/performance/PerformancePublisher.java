package hudson.plugins.performance;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Items;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.ExternalBuildReportAction;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.plugins.performance.constraints.AbstractConstraint;
import hudson.plugins.performance.constraints.ConstraintChecker;
import hudson.plugins.performance.constraints.ConstraintEvaluation;
import hudson.plugins.performance.constraints.ConstraintFactory;
import hudson.plugins.performance.constraints.blocks.PreviousResultsBlock;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import hudson.plugins.performance.cookie.CookieHandler;
import hudson.plugins.performance.data.ConstraintSettings;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.data.PerformanceReportPosition;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.descriptors.ConstraintDescriptor;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.details.GraphConfigurationDetail;
import hudson.plugins.performance.details.TestSuiteReportDetail;
import hudson.plugins.performance.details.TrendReportDetail;
import hudson.plugins.performance.parsers.AbstractParser;
import hudson.plugins.performance.parsers.IagoParser;
import hudson.plugins.performance.parsers.JMeterCsvParser;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.plugins.performance.parsers.JmeterSummarizerParser;
import hudson.plugins.performance.parsers.ParserFactory;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import hudson.plugins.performance.parsers.TaurusParser;
import hudson.plugins.performance.parsers.WrkSummarizerParser;
import hudson.plugins.performance.reports.AbstractReport;
import hudson.plugins.performance.reports.ConstraintReport;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.plugins.performance.reports.throughput.ThroughputReport;
import hudson.plugins.performance.reports.throughput.ThroughputUriReport;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformancePublisher extends Recorder implements SimpleBuildStep {

    /**
     * Mapping classes after refactoring for backward compatibility.
     */
    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        // Items.XSTREAM2 is used for serializing project configuration,
        // and Run.XSTREAM2 is used for serializing build and its associated Actions.

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceBuildAction", PerformanceBuildAction.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceProjectAction", PerformanceProjectAction.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ExternalBuildReport", ExternalBuildReportAction.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.PreviousResultsBlock", PreviousResultsBlock.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.TestCaseBlock", TestCaseBlock.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.CookieHandler", CookieHandler.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintSettings", ConstraintSettings.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.HttpSample", HttpSample.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportPosition", PerformanceReportPosition.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TaurusStatusReport", TaurusFinalStats.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintDescriptor", ConstraintDescriptor.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportParserDescriptor", PerformanceReportParserDescriptor.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.GraphConfigurationDetail", GraphConfigurationDetail.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TestSuiteReportDetail", TestSuiteReportDetail.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TrendReportDetail", TrendReportDetail.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.AbstractParser", AbstractParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.IagoParser", IagoParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JMeterCsvParser", JMeterCsvParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JMeterParser", JMeterParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JmeterSummarizerParser", JmeterSummarizerParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JUnitParser", JUnitParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportParser", PerformanceReportParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TaurusParser", TaurusParser.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.WrkSummarizerParser", WrkSummarizerParser.class);

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ThroughputReport", ThroughputReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ThroughputUriReport", ThroughputUriReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.AbstractReport", AbstractReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintReport", ConstraintReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReport", PerformanceReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.UriReport", UriReport.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceBuildAction", PerformanceBuildAction.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceProjectAction", PerformanceProjectAction.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ExternalBuildReport", ExternalBuildReportAction.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.PreviousResultsBlock", PreviousResultsBlock.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.TestCaseBlock", TestCaseBlock.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.CookieHandler", CookieHandler.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintSettings", ConstraintSettings.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.HttpSample", HttpSample.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportPosition", PerformanceReportPosition.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TaurusStatusReport", TaurusFinalStats.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintDescriptor", ConstraintDescriptor.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportParserDescriptor", PerformanceReportParserDescriptor.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.GraphConfigurationDetail", GraphConfigurationDetail.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TestSuiteReportDetail", TestSuiteReportDetail.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TrendReportDetail", TrendReportDetail.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.AbstractParser", AbstractParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.IagoParser", IagoParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JMeterCsvParser", JMeterCsvParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JMeterParser", JMeterParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JmeterSummarizerParser", JmeterSummarizerParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.JUnitParser", JUnitParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReportParser", PerformanceReportParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.TaurusParser", TaurusParser.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.WrkSummarizerParser", WrkSummarizerParser.class);

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ThroughputReport", ThroughputReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.ThroughputUriReport", ThroughputUriReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.AbstractReport", AbstractReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintReport", ConstraintReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReport", PerformanceReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.UriReport", UriReport.class);
    }

    public static final double THRESHOLD_TOLERANCE = 0.00000001;


    @Symbol("performanceReport")
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

        public List<ConstraintDescriptor> getConstraintDescriptors() {
            return ConstraintDescriptor.all();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /**
         * Populate the comparison type dynamically based on the user selection from
         * the previous time
         *
         * @return the name of the option selected in the previous run
         */
        public ListBoxModel doFillConfigTypeItems() {
            ListBoxModel items = new ListBoxModel();

            // getting the user selected value
            String temp = getOptionType();

            if (temp.equalsIgnoreCase("ART")) {

                items.add("Average Response Time", "ART");
                items.add("Median Response Time", "MRT");
                items.add("Percentile Response Time", "PRT");
            } else if (temp.equalsIgnoreCase("MRT")) {

                items.add("Median Response Time", "MRT");
                items.add("Percentile Response Time", "PRT");
                items.add("Average Response Time", "ART");
            } else if (temp.equalsIgnoreCase("PRT")) {

                items.add("Percentile Response Time", "PRT");
                items.add("Average Response Time", "ART");
                items.add("Median Response Time", "MRT");
            }

            return items;
        }
    }

    private int errorFailedThreshold = 0;

    private int errorUnstableThreshold = 0;

    private String errorUnstableResponseTimeThreshold = "";

    private double relativeFailedThresholdPositive = 0;

    private double relativeFailedThresholdNegative = 0;

    private double relativeUnstableThresholdPositive = 0;

    private double relativeUnstableThresholdNegative = 0;

    private int nthBuildNumber = 0;

    private boolean modeRelativeThresholds = false;

    private String configType = "ART";

    private boolean modeOfThreshold = false;

    private boolean failBuildIfNoResultFile = false;

    private boolean compareBuildPrevious = false;

    public static final String ART = "ART";

    public static final String MRT = "MRT";

    public static final String PRT = "PRT";

    public static String optionType = "ART";

    File xmlfile = null;

    String xmlDir = null;

    String xml = "";

    private static final String archive_directory = "archive";

    private boolean modePerformancePerTestCase = false;

    /**
     * @deprecated as of 1.3. for compatibility
     */
    private transient String filename;

    private boolean modeThroughput;

    /**
     * Performance evaluation mode. false = standard mode; true = expert mode
     */
    private boolean modeEvaluation = false;

    /**
     * Configured constraints
     */
    private List<? extends AbstractConstraint> constraints;

    /**
     * Constraint settings
     */
    private boolean ignoreFailedBuilds;
    private boolean ignoreUnstableBuilds;
    private boolean persistConstraintLog;

    /**
     * @deprecated as of 2.2. for compatibility
     * Migrate into String reportFiles with autodetect parser type.
     * Now this param use for restore previous job configs in GUI mode.
     */
    @Deprecated
    private transient List<PerformanceReportParser> parsers;

    private String sourceDataFiles;

    @DataBoundConstructor
    public PerformancePublisher(String sourceDataFiles,
                                int errorFailedThreshold,
                                int errorUnstableThreshold,
                                String errorUnstableResponseTimeThreshold,
                                double relativeFailedThresholdPositive,
                                double relativeFailedThresholdNegative,
                                double relativeUnstableThresholdPositive,
                                double relativeUnstableThresholdNegative,
                                int nthBuildNumber,
                                boolean modePerformancePerTestCase,
                                String configType,
                                boolean modeOfThreshold,
                                boolean failBuildIfNoResultFile,
                                boolean compareBuildPrevious,
                                boolean modeThroughput,
                                /**
                                 * Deprecated. Now use for support previous pipeline jobs.
                                 */
                                List<PerformanceReportParser> parsers) {
        this.parsers = parsers;
        this.sourceDataFiles = sourceDataFiles;
        migrateParsers();

        this.errorFailedThreshold = errorFailedThreshold;
        this.errorUnstableThreshold = errorUnstableThreshold;
        this.errorUnstableResponseTimeThreshold = errorUnstableResponseTimeThreshold;

        this.relativeFailedThresholdPositive = relativeFailedThresholdPositive;
        this.relativeFailedThresholdNegative = relativeFailedThresholdNegative;
        this.relativeUnstableThresholdPositive = relativeUnstableThresholdPositive;
        this.relativeUnstableThresholdNegative = relativeUnstableThresholdNegative;

        this.nthBuildNumber = nthBuildNumber;
        this.configType = configType;
        PerformancePublisher.optionType = configType;
        this.modeOfThreshold = modeOfThreshold;
        this.failBuildIfNoResultFile = failBuildIfNoResultFile;
        this.compareBuildPrevious = compareBuildPrevious;

        this.modePerformancePerTestCase = modePerformancePerTestCase;
        this.modeThroughput = modeThroughput;
    }

    public static File getPerformanceReport(Run<?, ?> build, String parserDisplayName,
                                            String performanceReportName) {
        return new File(build.getRootDir(), PerformanceReportMap.getPerformanceReportFileRelativePath(parserDisplayName,
                getPerformanceReportBuildFileName(performanceReportName)));
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new PerformanceProjectAction(project);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    /**
     * <p>
     * Delete the date suffix appended to the Performance result files by the
     * Maven Performance plugin
     * </p>
     *
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
    protected static List<FilePath> locatePerformanceReports(FilePath workspace, String includes)
            throws IOException, InterruptedException {

        // First use ant-style pattern
    /*
     * try { FilePath[] ret = workspace.list(includes); if (ret.length > 0) {
     * return Arrays.asList(ret); }
     */
        // Agoley : Possible fix, if we specify more than one result file pattern
        try {
            String parts[] = includes.split("\\s*[;:,]+\\s*");

            List<FilePath> files = new ArrayList<FilePath>();
            for (String path : parts) {
                FilePath[] ret = workspace.list(path);
                if (ret.length > 0) {
                    files.addAll(Arrays.asList(ret));
                }
            }
            if (!files.isEmpty())
                return files;

        } catch (IOException ignored) {
        }

        // Agoley: seems like this block doesn't work
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
        if (!files.isEmpty())
            return files;

        // give up and just try direct matching on string
        File directFile = new File(includes);
        if (directFile.exists())
            files.add(new FilePath(directFile));
        return files;
    }

    protected List<PerformanceReportParser> getParsers(Run<?, ?> build, FilePath workspace, PrintStream logger, EnvVars env) throws IOException, InterruptedException {
        final List<PerformanceReportParser> parsers = new ArrayList<PerformanceReportParser>();
        if (sourceDataFiles != null) {
            for (String filePath : sourceDataFiles.split(";")) {
                if (!filePath.isEmpty()) {
                    parsers.add(ParserFactory.getParser(build, workspace, logger, filePath, env));
                }
            }
        }
        return parsers;
    }

    /**
     * Used for migrate from user choose of parser to autodetect parser
     */
    private void migrateParsers() {
        if (parsers != null && !this.parsers.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (PerformanceReportParser p : this.parsers) {
                builder.append(p.glob).append(';');
            }
            builder.setLength(builder.length() - 1);
            this.sourceDataFiles = builder.toString();
            this.parsers = null;
        }
    }

    /**
     * This method, invoked after object is resurrected from persistence
     */
    public Object readResolve() {
        // data format migration
        if (parsers == null)
            parsers = new ArrayList<PerformanceReportParser>();
        if (filename != null) {
            parsers.add(new JMeterParser(filename));
            filename = null;
        }
        // Migrate parsers to simple field sourceDataFiles.
        migrateParsers();
        return this;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        run.setResult(Result.SUCCESS);

        final List<PerformanceReportParser> parsers = getParsers(run, workspace, listener.getLogger(), run.getEnvironment(listener));

        Collection<PerformanceReport> parsedReports = prepareEvaluation(run, workspace, listener, parsers);
        if (parsedReports == null) {
            return;
        }

        if (!modeEvaluation) {
            evaluateInStandardMode(run, workspace, parsedReports, listener, parsers);
        } else {
            evaluateInExpertMode(run, listener);
        }
    }

    /**
     * preparing evaluation - this is necessary regardless of the mode of
     * evaluation
     */
    public Collection<PerformanceReport> prepareEvaluation(Run<?, ?> run, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers)
            throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();
        EnvVars env = run.getEnvironment(listener);
        String glob;

        Collection<PerformanceReport> parsedReports = Collections.emptyList();
        // add the report to the build object.
        PerformanceBuildAction a = new PerformanceBuildAction(run, logger, parsers);
        run.addAction(a);

        for (PerformanceReportParser parser : parsers) {
            glob = parser.glob;
            // Replace any runtime environment variables such as ${sample_var}
            glob = env.expand(glob);
            logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

            List<FilePath> files = locatePerformanceReports(workspace, glob);

            if (files.isEmpty()) {
                if (run.getResult().isWorseThan(Result.UNSTABLE)) {
                    return null;
                }

                run.setResult(Result.FAILURE);
                logger.println("Performance: no " + parser.getReportName() + " files matching '" + glob
                        + "' have been found. Has the report generated?. Setting Build to " + run.getResult());
                return null;
            }

            List<File> localReports = copyReportsToMaster(run, logger, files, parser.getDescriptor().getDisplayName());
            parsedReports = parser.parse(run, localReports, listener);

            if (parser.reportURL != null && !parser.reportURL.isEmpty()) {
                run.addAction(new ExternalBuildReportAction(parser.reportURL));
            }
        }

        for (PerformanceReport r : parsedReports) {
            r.setBuildAction(a);
        }

        return parsedReports;
    }


    // for mode "standard evaluation"
    public void evaluateInStandardMode(Run<?, ?> run, FilePath workspace, Collection<PerformanceReport> parsedReports,
                                       TaskListener listener, List<PerformanceReportParser> parsers)
            throws IOException, InterruptedException {

        if (!modeOfThreshold) {
            compareWithAbsoluteThreshold(run, listener, parsedReports);
        } else {
            compareWithRelativeThreshold(run, workspace, listener, parsers);
        }
    }


    // For absolute error/unstable threshold..
    public void compareWithAbsoluteThreshold(Run<?, ?> run, TaskListener listener, Collection<PerformanceReport> parsedReports) {
        PrintStream logger = listener.getLogger();
        try {
            printInfoAboutThreshold(logger);
            HashMap<String, String> responseTimeThresholdMap = getResponseTimeThresholdMap(logger);
            // add the report to the build object.
            // mark the build as unstable or failure depending on the outcome.
            for (PerformanceReport performanceReport : parsedReports) {
                analyzeErrorThreshold(run, performanceReport, responseTimeThresholdMap, logger);
                writeErrorThresholdReportInXML(run, performanceReport);
            }
        } catch (Exception e) {
            logger.println("ERROR: Exception while determining absolute error/unstable threshold evaluation");
            e.printStackTrace(logger);
        }
    }

    // analyze Unstable and Failed thresholds values and set build result to UNSTABLE or FAILURE if needed
    private void analyzeErrorThreshold(Run<?, ?> run, PerformanceReport performanceReport, HashMap<String, String> responseTimeThresholdMap, PrintStream logger) {
        Result result = Result.SUCCESS;
        double errorPercent = performanceReport.errorPercent();

        // check failed and unstable values
        if (errorFailedThreshold >= 0 && errorPercent - errorFailedThreshold > THRESHOLD_TOLERANCE) {
            result = Result.FAILURE;
        } else if (errorUnstableThreshold >= 0 && errorPercent - errorUnstableThreshold > THRESHOLD_TOLERANCE) {
            result = Result.UNSTABLE;
        }

        // check average response time values
        long average = performanceReport.getAverage();
        try {
            if (responseTimeThresholdMap != null && responseTimeThresholdMap.get(performanceReport.getReportFileName()) != null) {
                if (Long.parseLong(responseTimeThresholdMap.get(performanceReport.getReportFileName())) <= average) {
                    logger.println("UNSTABLE: " + performanceReport.getReportFileName() + " has exceeded the threshold of ["
                            + Long.parseLong(responseTimeThresholdMap.get(performanceReport.getReportFileName())) + "] with the time of ["
                            + Long.toString(average) + "]");
                    result = Result.UNSTABLE;
                }
            }
        } catch (NumberFormatException nfe) {
            logger.println("ERROR: Threshold set to a non-number ["
                    + responseTimeThresholdMap.get(performanceReport.getReportFileName()) + "]");
            result = Result.FAILURE;
        }
        // set result. It'll be set only when result is worse
        run.setResult(result);

        logger.println("Performance: File " + performanceReport.getReportFileName() + " reported " + errorPercent
                + "% of errors [" + result + "]. Build status is: " + run.getResult());
    }

    // write report in xml, when checked Error Threshold comparison
    private void writeErrorThresholdReportInXML(Run<?, ?> run, PerformanceReport performanceReport) throws IOException {
        xmlDir = run.getRootDir().getAbsolutePath();
        xmlDir += "/" + archive_directory;

        String glob = performanceReport.getReportFileName();
        String[] arr = glob.split("/");
        if (!new File(xmlDir).exists()) {
            new File(xmlDir).mkdirs();
        }

        xmlfile = new File(xmlDir + "/dashBoard_" + arr[arr.length - 1].split("\\.")[0] + ".xml");
        xmlfile.createNewFile();

        FileWriter fw = new FileWriter(xmlfile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        xml = "<?xml version=\"1.0\"?>\n";
        xml += "<results>\n";
        xml += "<absoluteDefinition>\n";

        String unstable = "\t<unstable>";
        String failed = "\t<failed>";
        String calc = "\t<calculated>";

        unstable += errorUnstableThreshold;
        failed += errorFailedThreshold;

        String avg = "", med = "", perct = "";

        avg += "<average>\n";
        med += "<median>\n";
        perct += "<percentile>\n";
        double errorPercent = performanceReport.errorPercent();
        calc += errorPercent;

        List<UriReport> uriReports = performanceReport.getUriListOrdered();


        for (int i = 0; i < uriReports.size(); i++) {
            avg += "\t<" + uriReports.get(i).getStaplerUri() + ">\n";
            avg += "\t\t<currentBuildAvg>" + uriReports.get(i).getAverage() + "</currentBuildAvg>\n";
            avg += "\t</" + uriReports.get(i).getStaplerUri() + ">\n";

            med += "\t<" + uriReports.get(i).getStaplerUri() + ">\n";
            med += "\t\t<currentBuildMed>" + uriReports.get(i).getMedian() + "</currentBuildMed>\n";
            med += "\t</" + uriReports.get(i).getStaplerUri() + ">\n";

            perct += "\t<" + uriReports.get(i).getStaplerUri() + ">\n";
            perct += "\t\t<currentBuild90Line>" + uriReports.get(i).get90Line() + "</currentBuild90Line>\n";
            perct += "\t</" + uriReports.get(i).getStaplerUri() + ">\n";

        }
        unstable += "</unstable>";
        failed += "</failed>";
        calc += "</calculated>";

        avg += "</average>\n";
        med += "</median>\n";
        perct += "</percentile>\n";

        xml += unstable + "\n";
        xml += failed + "\n";
        xml += calc + "\n";
        xml += "</absoluteDefinition>\n";

        xml += avg;
        xml += med;
        xml += perct;
        xml += "</results>";

        bw.write(xml);
        bw.close();
        fw.close();
    }

    // Print information about Unstable & Failed Threshold
    private void printInfoAboutThreshold(PrintStream logger) {
        logger.println(
            (errorUnstableThreshold >= 0 && errorUnstableThreshold <= 100) ?
                "Performance: Percentage of errors greater or equal than " + errorUnstableThreshold
                    + "% sets the build as " + Result.UNSTABLE.toString().toLowerCase() :
                "Performance: No threshold configured for making the test " + Result.UNSTABLE.toString().toLowerCase()
        );

        logger.println(
            (errorFailedThreshold >= 0 && errorFailedThreshold <= 100) ?
                "Performance: Percentage of errors greater or equal than " + errorFailedThreshold
                    + "% sets the build as " + Result.FAILURE.toString().toLowerCase() :
                "Performance: No threshold configured for making the test " + Result.FAILURE.toString().toLowerCase()
        );
    }

    private HashMap<String, String> getResponseTimeThresholdMap(PrintStream logger) {
        HashMap<String, String> responseTimeThresholdMap = null;
        if (!"".equals(this.errorUnstableResponseTimeThreshold) && this.errorUnstableResponseTimeThreshold != null) {

            responseTimeThresholdMap = new HashMap<String, String>();
            String[] lines = this.errorUnstableResponseTimeThreshold.split("\n");

            for (String line : lines) {
                String[] components = line.split(":");
                if (components.length == 2) {
                    logger.println("Setting threshold: " + components[0] + ":" + components[1]);
                    responseTimeThresholdMap.put(components[0], components[1]);
                }
            }
        }
        return responseTimeThresholdMap;
    }


    // For relative comparisons between builds...
    public void compareWithRelativeThreshold(Run<?, ?> run, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers)
            throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        EnvVars env = run.getEnvironment(listener);

        try {
            Result result = Result.SUCCESS;
            String glob;
            String name = "";
            FileWriter fw = null;
            BufferedWriter bw = null;

            String relative = "<relativeDefinition>\n";
            String unstable = "\t<unstable>\n";
            String failed = "\t<failed>\n";
            String buildNo = "\t<buildNum>";

            String inside = "";
            String avg = "", med = "", perct = "";

            unstable += "\t\t<negative>" + relativeUnstableThresholdNegative + "</negative>\n";
            unstable += "\t\t<positive>" + relativeUnstableThresholdPositive + "</positive>\n";

            failed += "\t\t<negative>" + relativeFailedThresholdNegative + "</negative>\n";
            failed += "\t\t<positive>" + relativeFailedThresholdPositive + "</positive>\n";

            unstable += "\t</unstable>\n";
            failed += "\t</failed>\n";

            avg += "<average>\n";
            med += "<median>\n";
            perct += "<percentile>\n";

            if (relativeFailedThresholdNegative <= 100 && relativeFailedThresholdPositive <= 100) {
                logger.println("Performance: Percentage of relative difference outside -" + relativeFailedThresholdNegative
                        + " to +" + relativeFailedThresholdPositive + " % sets the build as "
                        + Result.FAILURE.toString().toLowerCase());
            } else {
                logger.println(
                        "Performance: No threshold configured for making the test " + Result.FAILURE.toString().toLowerCase());
            }

            if (relativeUnstableThresholdNegative <= 100 && relativeUnstableThresholdPositive <= 100) {
                logger.println("Performance: Percentage of relative difference outside -"
                        + relativeUnstableThresholdNegative + " to +" + relativeUnstableThresholdPositive
                        + " % sets the build as " + Result.UNSTABLE.toString().toLowerCase());
            } else {
                logger.println(
                        "Performance: No threshold configured for making the test " + Result.UNSTABLE.toString().toLowerCase());
            }

            List<UriReport> curruriList = null;
            // add the report to the build object.

            for (PerformanceReportParser parser : parsers) {
                glob = parser.glob;
                glob = env.expand(glob);
                name = glob;
                List<FilePath> files = locatePerformanceReports(workspace, glob);

                if (files.isEmpty()) {
                    if (run.getResult().isWorseThan(Result.UNSTABLE)) {
                        return;
                    }
                    run.setResult(Result.FAILURE);
                    logger.println("Performance: no " + parser.getReportName() + " files matching '" + glob
                            + "' have been found. Has the report generated?. Setting Build to " + run.getResult());
                }

                List<File> localReports = copyReportsToMaster(run, logger, files,
                        parser.getDescriptor().getDisplayName());
                Collection<PerformanceReport> parsedReports = parser.parse(run, localReports, listener);

                for (PerformanceReport r : parsedReports) {
                    // URI list is the list of labels in the current JMeter results
                    // file
                    curruriList = r.getUriListOrdered();
                    break;
                }
            }

            xmlDir = run.getRootDir().getAbsolutePath();
            xmlDir += "/" + archive_directory;

            String[] arr = name.split("/");
            if (!new File(xmlDir).exists()) {
                new File(xmlDir).mkdirs();
            }

            xmlfile = new File(xmlDir + "/dashBoard_" + arr[arr.length - 1].split("\\.")[0] + ".xml");
            xmlfile.createNewFile();

            fw = new FileWriter(xmlfile.getAbsoluteFile());
            bw = new BufferedWriter(fw);

            bw.write("<?xml version=\"1.0\"?>\n");
            bw.write("<results>\n");

            // getting previous build/nth previous build..
            Run<?, ?> prevBuild;

            if (compareBuildPrevious) {
                buildNo += "previous";
                prevBuild = run.getPreviousSuccessfulBuild();
            } else {
                buildNo += nthBuildNumber;
                prevBuild = getnthBuild(run);
            }

            buildNo += "</buildNum>\n";
            relative += buildNo + unstable + failed;
            relative += "</relativeDefinition>";

            bw.write(relative + "\n");

            List<UriReport> prevuriList = null;

            if (prevBuild != null) {
                // getting files related to the previous build selected
                for (PerformanceReportParser parser : parsers) {
                    glob = parser.glob;
                    logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

                    List<File> localReports = getExistingReports(prevBuild, logger, parser.getDescriptor().getDisplayName());
                    Collection<PerformanceReport> parsedReports = parser.parse(prevBuild, localReports, listener);

                    for (PerformanceReport r : parsedReports) {
                        // uri list is the list of labels in the previous jmeter results
                        // file
                        prevuriList = r.getUriListOrdered();
                        break;
                    }
                }

                result = Result.SUCCESS;
                String failedLabel = null, unStableLabel = null;
                double relativeDiff = 0, relativeDiffPercent = 0;

                logger.print("\nComparison build no. - " + prevBuild.number + " and " + run.number + " using ");

                // Comparing both builds based on either average, median or 90
                // percentile response time...
                if (configType.equalsIgnoreCase("ART")) {

                    logger.println("Average response time\n\n");
                    logger.println(
                            "====================================================================================================================================");
                    logger.println(
                            "PrevBuildURI\tCurrentBuildURI\t\tPrevBuildURIAvg\t\tCurrentBuildURIAvg\tRelativeDiff\tRelativeDiffPercentage ");
                    logger.println(
                            "====================================================================================================================================");
                } else if (configType.equalsIgnoreCase("MRT")) {

                    logger.println("Median response time\n\n");
                    logger.println(
                            "====================================================================================================================================");
                    logger.println(
                            "PrevBuildURI\tCurrentBuildURI\t\tPrevBuildURIMed\t\tCurrentBuildURIMed\tRelativeDiff\tRelativeDiffPercentage ");
                    logger.println(
                            "====================================================================================================================================");
                } else if (configType.equalsIgnoreCase("PRT")) {

                    logger.println("90 Percentile response time\n\n");
                    logger.println(
                            "====================================================================================================================================");
                    logger.println(
                            "PrevBuildURI\tCurrentBuildURI\t\tPrevBuildURI90%\t\tCurrentBuildURI90%\tRelativeDiff\tRelativeDiffPercentage ");
                    logger.println(
                            "====================================================================================================================================");
                }

                // comparing the labels and calculating the differences...
                for (int i = 0; i < prevuriList.size(); i++) {
                    for (int j = 0; j < curruriList.size(); j++) {
                        if (prevuriList.get(i).getStaplerUri().equalsIgnoreCase(curruriList.get(j).getStaplerUri())) {

                            relativeDiff = curruriList.get(j).getAverage() - prevuriList.get(i).getAverage();
                            relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).getAverage();
                            relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                            relativeDiffPercent = relativeDiffPercent / 100;

                            avg += "\t<" + curruriList.get(j).getStaplerUri() + ">\n";
                            avg += "\t\t<previousBuildAvg>" + prevuriList.get(i).getAverage() + "</previousBuildAvg>\n";
                            avg += "\t\t<currentBuildAvg>" + curruriList.get(j).getAverage() + "</currentBuildAvg>\n";
                            avg += "\t\t<relativeDiff>" + relativeDiff + "</relativeDiff>\n";
                            avg += "\t\t<relativeDiffPercent>" + relativeDiffPercent + "</relativeDiffPercent>\n";
                            avg += "\t</" + curruriList.get(j).getStaplerUri() + ">\n";

                            relativeDiff = curruriList.get(j).getMedian() - prevuriList.get(i).getMedian();
                            relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).getMedian();
                            relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                            relativeDiffPercent = relativeDiffPercent / 100;

                            med += "\t<" + curruriList.get(j).getStaplerUri() + ">\n";
                            med += "\t\t<previousBuildMed>" + prevuriList.get(i).getMedian() + "</previousBuildMed>\n";
                            med += "\t\t<currentBuildMed>" + curruriList.get(j).getMedian() + "</currentBuildMed>\n";
                            med += "\t\t<relativeDiff>" + relativeDiff + "</relativeDiff>\n";
                            med += "\t\t<relativeDiffPercent>" + relativeDiffPercent + "</relativeDiffPercent>\n";
                            med += "\t</" + curruriList.get(j).getStaplerUri() + ">\n";

                            relativeDiff = curruriList.get(j).get90Line() - prevuriList.get(i).get90Line();
                            relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).get90Line();
                            relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                            relativeDiffPercent = relativeDiffPercent / 100;

                            perct += "\t<" + curruriList.get(j).getStaplerUri() + ">\n";
                            perct += "\t\t<previousBuild90Line>" + prevuriList.get(i).get90Line() + "</previousBuild90Line>\n";
                            perct += "\t\t<currentBuild90Line>" + curruriList.get(j).get90Line() + "</currentBuild90Line>\n";
                            perct += "\t\t<relativeDiff>" + relativeDiff + "</relativeDiff>\n";
                            perct += "\t\t<relativeDiffPercent>" + relativeDiffPercent + "</relativeDiffPercent>\n";
                            perct += "\t</" + curruriList.get(j).getStaplerUri() + ">\n";

                            if (configType.equalsIgnoreCase("ART")) {

                                relativeDiff = curruriList.get(j).getAverage() - prevuriList.get(i).getAverage();
                                relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).getAverage();

                                relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                                relativeDiffPercent = relativeDiffPercent / 100;

                                logger.println(prevuriList.get(i).getStaplerUri() + "\t" + curruriList.get(j).getStaplerUri()
                                        + "\t\t" + prevuriList.get(i).getAverage() + "\t\t\t" + curruriList.get(j).getAverage()
                                        + "\t\t\t" + relativeDiff + "\t\t" + relativeDiffPercent);

                            } else if (configType.equalsIgnoreCase("MRT")) {

                                relativeDiff = curruriList.get(j).getMedian() - prevuriList.get(i).getMedian();
                                relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).getMedian();

                                relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                                relativeDiffPercent = relativeDiffPercent / 100;

                                logger.println(prevuriList.get(i).getStaplerUri() + "\t" + curruriList.get(j).getStaplerUri()
                                        + "\t\t" + prevuriList.get(i).getMedian() + "\t\t\t" + curruriList.get(j).getMedian() + "\t\t\t"
                                        + relativeDiff + "\t\t" + relativeDiffPercent);

                            } else if (configType.equalsIgnoreCase("PRT")) {

                                relativeDiff = curruriList.get(j).get90Line() - prevuriList.get(i).get90Line();
                                relativeDiffPercent = ((double) relativeDiff * 100) / prevuriList.get(i).get90Line();

                                relativeDiffPercent = Math.round(relativeDiffPercent * 100);
                                relativeDiffPercent = relativeDiffPercent / 100;

                                logger.println(prevuriList.get(i).getStaplerUri() + "\t" + curruriList.get(j).getStaplerUri()
                                        + "\t\t" + prevuriList.get(i).get90Line() + "\t\t\t" + curruriList.get(j).get90Line() + "\t\t\t"
                                        + relativeDiff + "\t\t" + relativeDiffPercent);

                            }

                            // setting the build status based on the differences
                            // calculated...
                            if (relativeDiffPercent < 0) {
                                if (relativeFailedThresholdNegative >= 0
                                        && Math.abs(relativeDiffPercent) - relativeFailedThresholdNegative > THRESHOLD_TOLERANCE) {

                                    result = Result.FAILURE;
                                    run.setResult(Result.FAILURE);
                                    failedLabel = prevuriList.get(i).getStaplerUri();

                                } else if (relativeUnstableThresholdNegative >= 0
                                        && Math.abs(relativeDiffPercent) - relativeUnstableThresholdNegative > THRESHOLD_TOLERANCE) {

                                    result = Result.UNSTABLE;
                                    run.setResult(Result.UNSTABLE);
                                    unStableLabel = prevuriList.get(i).getStaplerUri();
                                }
                            } else if (relativeDiffPercent >= 0) {

                                if (relativeFailedThresholdPositive >= 0
                                        && Math.abs(relativeDiffPercent) - relativeFailedThresholdPositive > THRESHOLD_TOLERANCE) {

                                    result = Result.FAILURE;
                                    run.setResult(Result.FAILURE);
                                    failedLabel = prevuriList.get(i).getStaplerUri();

                                } else if (relativeUnstableThresholdPositive >= 0
                                        && Math.abs(relativeDiffPercent) - relativeUnstableThresholdPositive > THRESHOLD_TOLERANCE) {

                                    result = Result.UNSTABLE;
                                    run.setResult(Result.UNSTABLE);
                                    unStableLabel = prevuriList.get(i).getStaplerUri();
                                }
                            }

                            if (result.isWorseThan(run.getResult())) {
                                run.setResult(result);
                            }

                        }

                    }
                }

                logger.println(
                        "------------------------------------------------------------------------------------------------------------------------------------");
                String labelResult = "\nThe label ";
                logger.print((failedLabel != null) ? labelResult + "\"" + failedLabel + "\"" + " caused the build to fail\n"
                        : (unStableLabel != null) ? labelResult + "\"" + unStableLabel + "\"" + " made the build unstable\n"
                        : "");

                avg += "</average>\n";
                med += "</median>\n";
                perct += "</percentile>";

                inside += avg + med + perct;
                bw.write(inside + "\n");

            }
            bw.write("</results>");
            bw.close();
            fw.close();

        } catch (Exception e) {
            logger.println("ERROR: Exception while determining relative comparison between builds");
            e.printStackTrace(logger);
        }

    }

    /*
     * For mode "expert evaluation"
     */
    public void evaluateInExpertMode(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        ConstraintFactory factory = new ConstraintFactory();
        ConstraintSettings settings = new ConstraintSettings((BuildListener) listener, ignoreFailedBuilds, ignoreUnstableBuilds,
                persistConstraintLog);
        ConstraintChecker checker = new ConstraintChecker(settings, run.getParent().getBuilds());
        ArrayList<ConstraintEvaluation> ceList = new ArrayList<ConstraintEvaluation>();
        try {
            ceList = checker.checkAllConstraints(factory.createConstraintClones(run, constraints));
        } catch (Exception e) {
            e.printStackTrace(logger);
        }
      /*
       * Create Report of evaluated constraints
       */
        ConstraintReport cr = new ConstraintReport(ceList, run.getParent().getBuilds().get(0), persistConstraintLog);
        logger.print(cr.getLoggerMsg());
      /*
       * Determine build result
       */
        run.setResult(cr.getBuildResult());
    }

    private List<File> copyReportsToMaster(Run<?, ?> build, PrintStream logger, List<FilePath> files,
                                           String parserDisplayName) throws IOException, InterruptedException {
        List<File> localReports = new ArrayList<File>();
        for (FilePath src : files) {
            final File localReport = getPerformanceReport(build, parserDisplayName, src.getName());
            if (src.isDirectory()) {
                logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
                continue;
            }
            src.copyTo(new FilePath(localReport));
            localReports.add(localReport);
        }
        return localReports;
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

    public String getErrorUnstableResponseTimeThreshold() {
        return this.errorUnstableResponseTimeThreshold;
    }

    public void setErrorUnstableResponseTimeThreshold(String errorUnstableResponseTimeThreshold) {
        this.errorUnstableResponseTimeThreshold = errorUnstableResponseTimeThreshold;
    }

    public boolean isModePerformancePerTestCase() {
        return modePerformancePerTestCase;
    }

    public void setModePerformancePerTestCase(boolean modePerformancePerTestCase) {
        this.modePerformancePerTestCase = modePerformancePerTestCase;
    }

    public boolean getModePerformancePerTestCase() {
        return modePerformancePerTestCase;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isFailBuildIfNoResultFile() {
        return failBuildIfNoResultFile;
    }

    public void setFailBuildIfNoResultFile(boolean failBuildIfNoResultFile) {
        this.failBuildIfNoResultFile = failBuildIfNoResultFile;
    }

    public boolean isART() {
        return configType.compareToIgnoreCase(PerformancePublisher.ART) == 0;
    }

    public boolean isMRT() {
        return configType.compareToIgnoreCase(PerformancePublisher.MRT) == 0;
    }

    public boolean isPRT() {
        return configType.compareToIgnoreCase(PerformancePublisher.PRT) == 0;
    }

    public static File[] getPerformanceReportDirectory(Run<?, ?> build, String parserDisplayName,
                                                       PrintStream logger) {
        File folder = new File(
                build.getRootDir() + "/" + PerformanceReportMap.getPerformanceReportFileRelativePath(parserDisplayName, ""));
        return folder.listFiles();
    }

    /**
     * Gets the Build object entered in the text box "Compare with nth Build"
     *
     * @param build, listener
     * @param build
     * @return build object
     * @throws IOException
     */

    // @psingh5 -
    public Run<?, ?> getnthBuild(Run<?, ?> build) throws IOException {
        Run<?, ?> nthBuild = build;

        int nextBuildNumber = build.number - nthBuildNumber;

        for (int i = 1; i <= nextBuildNumber; i++) {
            nthBuild = nthBuild.getPreviousBuild();
            if (nthBuild == null)
                return null;
        }
        return (nthBuildNumber == 0) ? null : nthBuild;
    }

    private List<File> getExistingReports(Run<?, ?> build, PrintStream logger, String parserDisplayName)
            throws IOException, InterruptedException {
        List<File> localReports = new ArrayList<File>();
        final File localReport[] = getPerformanceReportDirectory(build, parserDisplayName, logger);

        for (int i = 0; i < localReport.length; i++) {

            String name = localReport[i].getName();
            String[] arr = name.split("\\.");

            // skip the serialized jmeter report file
            if (arr[arr.length - 1].equalsIgnoreCase("serialized"))
                continue;

            localReports.add(localReport[i]);
        }
        return localReports;
    }

    public static String getOptionType() {
        return optionType;
    }

    public double getRelativeFailedThresholdPositive() {
        return relativeFailedThresholdPositive;
    }

    public double getRelativeFailedThresholdNegative() {
        return relativeFailedThresholdNegative;
    }

    public void setRelativeFailedThresholdPositive(double relativeFailedThresholdPositive) {
        this.relativeFailedThresholdPositive = Math.max(0, Math.min(relativeFailedThresholdPositive, 100));
    }

    public void setRelativeFailedThresholdNegative(double relativeFailedThresholdNegative) {
        this.relativeFailedThresholdNegative = Math.max(0, Math.min(relativeFailedThresholdNegative, 100));
    }

    public double getRelativeUnstableThresholdPositive() {
        return relativeUnstableThresholdPositive;
    }

    public double getRelativeUnstableThresholdNegative() {
        return relativeUnstableThresholdNegative;
    }

    public void setRelativeUnstableThresholdPositive(double relativeUnstableThresholdPositive) {
        this.relativeUnstableThresholdPositive = Math.max(0, Math.min(relativeUnstableThresholdPositive, 100));
    }

    public void setRelativeUnstableThresholdNegative(double relativeUnstableThresholdNegative) {
        this.relativeUnstableThresholdNegative = Math.max(0, Math.min(relativeUnstableThresholdNegative, 100));
    }

    public int getNthBuildNumber() {
        return nthBuildNumber;
    }

    public void setNthBuildNumber(int nthBuildNumber) {
        this.nthBuildNumber = Math.max(0, Math.min(nthBuildNumber, Integer.MAX_VALUE));
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public boolean getModeOfThreshold() {
        return modeOfThreshold;
    }

    public void setModeOfThreshold(boolean modeOfThreshold) {
        this.modeOfThreshold = modeOfThreshold;
    }

    public boolean getCompareBuildPrevious() {
        return compareBuildPrevious;
    }

    public void setCompareBuildPrevious(boolean compareBuildPrevious) {
        this.compareBuildPrevious = compareBuildPrevious;
    }

    public void setModeRelativeThresholds(boolean modeRelativeThresholds) {
        this.modeRelativeThresholds = modeRelativeThresholds;
    }

    public boolean getModeRelativeThresholds() {
        return modeRelativeThresholds;
    }

    public boolean isModeThroughput() {
        return modeThroughput;
    }

    public void setModeThroughput(boolean modeThroughput) {
        this.modeThroughput = modeThroughput;
    }

    public List<? extends AbstractConstraint> getConstraints() {
        return constraints;
    }

    @DataBoundSetter
    public void setConstraints(List<? extends AbstractConstraint> constraints) {
        this.constraints = constraints;
    }

    @DataBoundSetter
    public void setIgnoreFailedBuilds(boolean ignoreFailedBuilds) {
        this.ignoreFailedBuilds = ignoreFailedBuilds;
    }

    public boolean isIgnoreFailedBuilds() {
        return ignoreFailedBuilds;
    }

    @DataBoundSetter
    public void setIgnoreUnstableBuilds(boolean ignoreUnstableBuilds) {
        this.ignoreUnstableBuilds = ignoreUnstableBuilds;
    }

    public boolean isIgnoreUnstableBuilds() {
        return ignoreUnstableBuilds;
    }

    public boolean isPersistConstraintLog() {
        return persistConstraintLog;
    }

    @DataBoundSetter
    public void setPersistConstraintLog(boolean persistConstraintLog) {
        this.persistConstraintLog = persistConstraintLog;
    }

    public boolean isModeEvaluation() {
        return modeEvaluation;
    }

    @DataBoundSetter
    public void setModeEvaluation(boolean modeEvaluation) {
        this.modeEvaluation = modeEvaluation;
    }

    public String getSourceDataFiles() {
        return sourceDataFiles;
    }

    @DataBoundSetter
    public void setSourceDataFiles(String sourceDataFiles) {
        this.sourceDataFiles = sourceDataFiles;
    }

    public List<PerformanceReportParser> getParsers() {
        return parsers;
    }

    @DataBoundSetter
    public void setParsers(List<PerformanceReportParser> parsers) {
        this.parsers = parsers;
    }
}

