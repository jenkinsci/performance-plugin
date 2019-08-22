package hudson.plugins.performance;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Action;
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
import hudson.plugins.performance.reports.ThroughputReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.plugins.performance.tools.SafeMaths;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

public class PerformancePublisher extends Recorder implements SimpleBuildStep {

    public static final double THRESHOLD_TOLERANCE = 0.00000001;
    private static final double DEFAULT_THRESHOLD = -1;

    private int errorFailedThreshold = -1;

    private int errorUnstableThreshold = -1;

    private String errorUnstableResponseTimeThreshold = "";

    private double relativeFailedThresholdPositive = DEFAULT_THRESHOLD;

    private double relativeFailedThresholdNegative = DEFAULT_THRESHOLD;

    private double relativeUnstableThresholdPositive = DEFAULT_THRESHOLD;

    private double relativeUnstableThresholdNegative = DEFAULT_THRESHOLD;

    private int nthBuildNumber = 0;

    private String configType = "ART";

    private String graphType = "ART";

    private boolean modeOfThreshold = false;

    private boolean failBuildIfNoResultFile = true;

    private boolean compareBuildPrevious = false;

    public static final String ART = "ART";

    public static final String MRT = "MRT";

    public static final String PRT = "PRT";

    public String optionType = "ART";

    private static final String ARCHIVE_DIRECTORY = "archive";

    private boolean modePerformancePerTestCase = false;

    /**
     * Exclude response time of errored samples
     */
    private boolean excludeResponseTime;

    /**
     * @deprecated as of 1.3. for compatibility
     */
    private transient String filename; // NOSONAR On purpose keep of transient, we don't want to save it

    private boolean modeThroughput;

    /**
     * Performance evaluation mode. false = standard mode; true = expert mode
     */
    private boolean modeEvaluation = false;

    /**
     * Configured constraints
     */
    private List<? extends AbstractConstraint> constraints = Collections.emptyList();

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
    private transient List<PerformanceReportParser> parsers; // NOSONAR On purpose keep of transient, we don't want to save it

    private String sourceDataFiles;
    private String filterRegex;

    /**
     * Optional filename indicating whether and where a JUnit compatible XML report should be written
     */
    private String junitOutput = "";

    /**
     * Percentiles that will be display in url tables
     * comma-separated
     */
    private String percentiles = AbstractReport.DEFAULT_PERCENTILES;


    /**
     * Base line build for create performance Trends
     * default '0' - is previous build
     */
    private int baselineBuild;

    /**
     * Legacy constructor used for internal references.
     */
    @Restricted(NoExternalUse.class)
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
        this.optionType = configType;
        this.modeOfThreshold = modeOfThreshold;
        this.failBuildIfNoResultFile = failBuildIfNoResultFile;
        this.compareBuildPrevious = compareBuildPrevious;

        this.modePerformancePerTestCase = modePerformancePerTestCase;
        this.modeThroughput = modeThroughput;
    }

    @DataBoundConstructor
    public PerformancePublisher(String sourceDataFiles) {
        this.sourceDataFiles = sourceDataFiles;
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
            String[] parts = includes.split("\\s*[;:,]+\\s*");

            List<FilePath> files = new ArrayList<>();
            for (String path : parts) {
                FilePath[] ret = workspace.list(path);
                if (ret.length > 0) {
                    files.addAll(Arrays.asList(ret));
                }
            }
            if (!files.isEmpty())
                return files;

        } catch (IOException ignored) {
            // NOOP
        }

        // Agoley: seems like this block doesn't work
        // If it fails, do a legacy search
        ArrayList<FilePath> files = new ArrayList<>();
        String[] parts = includes.split("\\s*[;:,]+\\s*");
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
        final List<PerformanceReportParser> parsers = new ArrayList<>();
        if (sourceDataFiles != null) {
            for (String filePath : sourceDataFiles.split(";")) {
                if (!filePath.isEmpty()) {
                    try {
                        logger.println("Creating parser with percentiles:'" + percentiles + ",' filterRegex:" + filterRegex);
                        parsers.addAll(ParserFactory.getParser(build, workspace, logger, filePath, env, percentiles, filterRegex));
                    } catch (IOException ex) {
                        logger.println("Cannot detect file type because of error: " + ex.getMessage());
                    }
                }
            }
        }
        return parsers;
    }

    /**
     * Used to migrate from user selected parser to autodetected parser
     */
    private void migrateParsers() {
        if (parsers != null && !this.parsers.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (PerformanceReportParser p : this.parsers) {
                builder.append(p.glob).append(';');
            }
            builder.setLength(builder.length() - 1);
            if (this.sourceDataFiles == null || this.sourceDataFiles.equals("")) {
                this.sourceDataFiles = builder.toString();
            } else {
                this.sourceDataFiles = this.sourceDataFiles + ";" + builder.toString();
            }
            this.parsers = null;
        }
    }

    /**
     * This method, invoked after object is resurrected from persistence
     */
    public Object readResolve() {
        // data format migration
        if (parsers == null)
            parsers = new ArrayList<>();
        if (filename != null) {
            parsers.add(new JMeterParser(filename, percentiles, filterRegex));
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
        if (!parsers.isEmpty()) {
            prepareParsers(parsers);

            Collection<PerformanceReport> parsedReports = prepareEvaluation(run, workspace, listener, parsers);
            if (parsedReports == null) {
                return;
            }

            if (!modeEvaluation) {
                evaluateInStandardMode(run, workspace, parsedReports, listener, parsers);
                writeStandardResultsToXML(run, parsedReports);
            } else {
                evaluateInExpertMode(run, workspace, listener);
            }
        } else {
            if (failBuildIfNoResultFile) {
                run.setResult(Result.FAILURE);
            }
        }
    }

    /**
     * preparing evaluation - this is necessary regardless of the mode of
     * evaluation
     */
    public Collection<PerformanceReport> prepareEvaluation(Run<?, ?> run, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers)
            throws IOException, InterruptedException {

        // add the report to the build object.
        PerformanceBuildAction a = new PerformanceBuildAction(run, listener.getLogger(), parsers);
        run.addAction(a);

        Collection<PerformanceReport> parsedReports = locatePerformanceReports(run, workspace, listener, parsers);
        if (parsedReports == null) {
            return Collections.emptyList();
        }

        addExternalReportActionsToBuild(run, parsers);

        for (PerformanceReport r : parsedReports) {
            r.setBuildAction(a);
        }

        return parsedReports;
    }

    private void addExternalReportActionsToBuild(Run<?, ?> run, List<PerformanceReportParser> parsers) {
        for (PerformanceReportParser parser : parsers) {
            if (parser.reportURL != null && !parser.reportURL.isEmpty()) {
                run.addAction(new ExternalBuildReportAction(parser.reportURL));
            }
        }
    }

    private Collection<PerformanceReport> locatePerformanceReports(Run<?, ?> run, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers) throws IOException, InterruptedException {
        Collection<PerformanceReport> performanceReports = new ArrayList<>();
        PrintStream logger = listener.getLogger();
        EnvVars env = run.getEnvironment(listener);
        String glob;
        for (PerformanceReportParser parser : parsers) {
            glob = parser.glob;
            // Replace any runtime environment variables such as ${sample_var}
            glob = env.expand(glob);
            logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

            List<FilePath> files = locatePerformanceReports(workspace, glob);
            if (files.isEmpty()) {
                if (run.getResult().isWorseThan(Result.UNSTABLE)) {
                    return Collections.emptyList();
                }

                if (failBuildIfNoResultFile) {
                    run.setResult(Result.FAILURE);
                }
                logger.println("Performance: no " + parser.getReportName() + " files matching '" + glob
                        + "' have been found. Has the report generated?. Setting Build to " + run.getResult());
                return Collections.emptyList();
            }

            logger.println("Performance: " + parser.getReportName() + " copying reports to master, files '" + files+"'");
            List<File> localReports = copyReportsToMaster(run, logger, files, parser.getDescriptor().getDisplayName());
            logger.println("Performance: " + parser.getReportName() + " parsing local reports '" + localReports
                    + "'");
            performanceReports.addAll(parser.parse(run, localReports, listener));
        }
        return performanceReports;
    }

    /**
     *
     */
    private void prepareParsers(Collection<PerformanceReportParser> performanceReportParsers) {
        for (PerformanceReportParser parser : performanceReportParsers) {
            parser.setExcludeResponseTime(excludeResponseTime);
            parser.setBaselineBuild(baselineBuild);
        }
    }

    protected List<UriReport> getBuildUriReports(Run<?, ?> build, FilePath workspace, TaskListener listener,
                                               List<PerformanceReportParser> parsers, boolean locatePerformanceReports)
            throws IOException, InterruptedException {

        List<UriReport> uriReports = new ArrayList<>();

        if (locatePerformanceReports) {
            Collection<PerformanceReport> performanceReports = locatePerformanceReports(build, workspace, listener, parsers);
            if (performanceReports == null) {
                return Collections.emptyList();
            }

            for (PerformanceReport r : performanceReports) {
                // URI list is the list of labels in the current JMeter results
                // file
                uriReports.addAll(r.getUriListOrdered());
            }
        } else {
            Set<PerformanceReport> parsedReports = new HashSet<>();

            for (PerformanceReportParser parser : parsers) {
                // add the report to the build object.
                List<File> localReports = getExistingReports(build, listener.getLogger(), parser.getDescriptor().getDisplayName());

                // For more than one parser of the same type, existing reports will be found multiple times, 
                // so we collect them in a Set to avoid duplicates (based on comparison by report filename).
                parsedReports.addAll(parser.parse(build, localReports, listener));
            }

            for (PerformanceReport r : parsedReports) {
                // uri list is the list of labels in the previous jmeter results
                // file
                uriReports.addAll(r.getUriListOrdered());
            }

        }
        return uriReports;
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
            printInfoAboutErrorThreshold(logger);
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

        // check average response time values
        Result res = checkAverageResponseTime(performanceReport, responseTimeThresholdMap, logger);
        if (res != null) {
            result = res;
        }

        // check failed and unstable values
        if (errorFailedThreshold >= 0 && errorPercent - errorFailedThreshold > THRESHOLD_TOLERANCE) {
            result = Result.FAILURE;
        } else if (errorUnstableThreshold >= 0 && errorPercent - errorUnstableThreshold > THRESHOLD_TOLERANCE) {
            result = Result.UNSTABLE;
        }

        // set result. It'll be set only when result is worse
        run.setResult(result);

        logger.println("Performance: File " + performanceReport.getReportFileName() + " reported " + errorPercent
                + "% of errors [" + result + "]. Build status is: " + run.getResult());
    }

    // check average response time values
    private Result checkAverageResponseTime(PerformanceReport performanceReport, HashMap<String, String> responseTimeThresholdMap, PrintStream logger) {
        long average = performanceReport.getAverage();
        try {
            if ((responseTimeThresholdMap != null && responseTimeThresholdMap.get(performanceReport.getReportFileName()) != null) &&
                    (Long.parseLong(responseTimeThresholdMap.get(performanceReport.getReportFileName())) <= average)) {
                logger.println("UNSTABLE: " + performanceReport.getReportFileName() + " has exceeded the threshold of ["
                        + Long.parseLong(responseTimeThresholdMap.get(performanceReport.getReportFileName())) + "] with the time of ["
                        + Long.toString(average) + "]");
                return Result.UNSTABLE;
            }
        } catch (NumberFormatException nfe) {
            logger.println("ERROR: Threshold set to a non-number ["
                    + responseTimeThresholdMap.get(performanceReport.getReportFileName()) + "]");
            return Result.FAILURE;
        }
        return null;
    }

    // write 'standard mode' report in xml containing each of the populated columns otherwise found in the jelly output.
    // producing an on disk, machine parseable format allows for archiving and automation.
    private void writeStandardResultsToXML(Run<?, ?> run, Collection<PerformanceReport> parsedReports) throws IOException {
        File xmlDirectory = createArchiveDirectoryIfMissing(run);
        File xmlfile = new File(xmlDirectory, "standardResults.xml");
        try (FileWriter fw = new FileWriter(xmlfile);
                BufferedWriter bw = new BufferedWriter(fw)){
            
            String xml = new StringBuilder("<?xml version=\"1.0\"?>\n")
                    .append("<results>\n")
                    .append(appendStandardResultsStatsToXml(parsedReports))
                    .append("</results>\n").toString();

            bw.write(xml);
        }
    }

    private String appendStandardResultsStatsToXml(Collection<PerformanceReport> reports) {
        StringBuilder xmlSB = new StringBuilder();
        for (PerformanceReport perfReport : reports) {
            for (UriReport report : perfReport.getUriListOrdered()) {
                xmlSB.append("<api>\n\t");
                xmlSB.append("<uri>").append(report.getUri()).append("</uri>\n\t");
                xmlSB.append("<samples>").append(report.samplesCount()).append("</samples>\n\t");
                xmlSB.append("<average>").append(report.getAverage()).append("</average>\n\t");
                xmlSB.append("<min>").append(report.getMin()).append("</min>\n\t");
                xmlSB.append("<median>").append(report.getMedian()).append("</median>\n\t");
                xmlSB.append("<ninetieth>").append(report.get90Line()).append("</ninetieth>\n\t");
                xmlSB.append("<max>").append(report.getMax()).append("</max>\n\t");
                xmlSB.append("<httpCode>").append(report.getHttpCode()).append("</httpCode>\n\t");
                xmlSB.append("<errors>").append(report.errorPercent()).append("</errors>\n");
                xmlSB.append("</api>\n");
            }
        }
        return xmlSB.toString();
    }

    // write report in xml, when checked Error Threshold comparison
    private void writeErrorThresholdReportInXML(Run<?, ?> run, PerformanceReport performanceReport) throws IOException {
        File xmlDirectory = createArchiveDirectoryIfMissing(run);
        
        String glob = performanceReport.getReportFileName();
        String[] arr = glob.split("/");

        File xmlfile = new File(xmlDirectory, "/dashBoard_" + arr[arr.length - 1].split("\\.")[0] + ".xml");

        try (FileWriter fw = new FileWriter(xmlfile);
                BufferedWriter bw = new BufferedWriter(fw)) {            
            String xml = "<?xml version=\"1.0\"?>\n";
            xml += "<results>\n";
            xml += "<absoluteDefinition>\n";
            xml += "\t<unstable>" + errorUnstableThreshold + "</unstable>\n";
            xml += "\t<failed>" + errorFailedThreshold + "</failed>\n";
            xml += "\t<calculated>" + performanceReport.errorPercent() + "</calculated>\n";
            xml += "</absoluteDefinition>\n";

            xml += appendStatsToXml(performanceReport.getUriListOrdered());

            xml += "</results>";

            bw.write(xml);
        }
    }

    private String appendStatsToXml(List<UriReport> reports) {
        final StringBuilder averageBuffer = new StringBuilder("<average>\n");
        final StringBuilder medianBuffer = new StringBuilder("<median>\n");
        final StringBuilder percentileBuffer = new StringBuilder("<percentile>\n");

        for (UriReport uriReport : reports) {
            averageBuffer.append("\t<").append(uriReport.getStaplerUri()).append(">\n");
            averageBuffer.append("\t\t<currentBuildAvg>").append(uriReport.getAverage()).append("</currentBuildAvg>\n");
            averageBuffer.append("\t</").append(uriReport.getStaplerUri()).append(">\n");

            medianBuffer.append("\t<").append(uriReport.getStaplerUri()).append(">\n");
            medianBuffer.append("\t\t<currentBuildMed>").append(uriReport.getMedian()).append("</currentBuildMed>\n");
            medianBuffer.append("\t</").append(uriReport.getStaplerUri()).append(">\n");

            percentileBuffer.append("\t<").append(uriReport.getStaplerUri()).append(">\n");
            percentileBuffer.append("\t\t<currentBuild90Line>").append(uriReport.get90Line()).append("</currentBuild90Line>\n");
            percentileBuffer.append("\t</").append(uriReport.getStaplerUri()).append(">\n");

        }

        averageBuffer.append("</average>\n");
        medianBuffer.append("</median>\n");
        percentileBuffer.append("</percentile>\n");

        StringBuilder result = new StringBuilder();
        result.append(averageBuffer);
        result.append(medianBuffer);
        result.append(percentileBuffer);
        return result.toString();
    }


    private HashMap<String, String> getResponseTimeThresholdMap(PrintStream logger) {
        HashMap<String, String> responseTimeThresholdMap = null;
        if (!"".equals(this.errorUnstableResponseTimeThreshold) && this.errorUnstableResponseTimeThreshold != null) {

            responseTimeThresholdMap = new HashMap<>();
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
        try {
            printInfoAboutRelativeThreshold(logger);

            List<UriReport> currentUriReports = getBuildUriReports(run, workspace, listener, parsers, true);
            if (currentUriReports == null) {
                return;
            }

            StringBuilder averageBuffer = null;
            StringBuilder medianBuffer = null;
            StringBuilder percentileBuffer = null;

            // getting previous build/nth previous build..
            Run<?, ?> buildForComparison = compareBuildPrevious ? run.getPreviousSuccessfulBuild() : getnthBuild(run);

            if (buildForComparison != null) {
                logger.print("\nComparison build no. - " + buildForComparison.number + " and " + run.number + " using ");

                int maxUriColumnWidth = 0;
                for (UriReport report : currentUriReports) {
                    maxUriColumnWidth = Math.max(report.getStaplerUri().length(), maxUriColumnWidth);
                }

                printInfoAboutCompareBasedOn(logger, "%-"+maxUriColumnWidth+"s%20s%20s%20s%20s");

                compareUriReports(run,
                        currentUriReports,
                        // getting files related to the previous build selected
                        getBuildUriReports(buildForComparison, workspace, listener, parsers, false),
                        logger,
                        // open xml tags
                        (averageBuffer = new StringBuilder("<average>\n")),
                        (medianBuffer = new StringBuilder("<median>\n")),
                        (percentileBuffer = new StringBuilder("<percentile>\n")),
                        "%1$-"+maxUriColumnWidth+"s%2$20d%3$20d%4$20.0f%5$19.2f%%"
                );

                // close xml tags
                averageBuffer.append("</average>\n");
                medianBuffer.append("</median>\n");
                percentileBuffer.append("</percentile>");
            }

            writeRelativeThresholdReportInXML(run, averageBuffer, medianBuffer, percentileBuffer);


        } catch (Exception e) {
            logger.println("ERROR: Exception while determining relative comparison between builds");
            e.printStackTrace(logger);
        }

    }


    // Comparing both builds based on either average, median or 90
    // percentile response time...
    private void compareUriReports(Run<?, ?> run, List<UriReport> currentUriReports, List<UriReport> reportsForComparison, PrintStream logger,
                                   StringBuilder averageBuffer, StringBuilder medianBuffer, StringBuilder percentileBuffer, String logFormat) {

        // comparing the labels and calculating the differences...
        for (UriReport reportForComparison : reportsForComparison) {
            for (UriReport currentUriReport : currentUriReports) {
                if (reportForComparison.getStaplerUri().equalsIgnoreCase(currentUriReport.getStaplerUri())) {

                    appendRelativeInfoAboutAverage(currentUriReport, reportForComparison, averageBuffer);
                    appendRelativeInfoAboutMedian(currentUriReport, reportForComparison, medianBuffer);
                    appendRelativeInfoAbout90Line(currentUriReport, reportForComparison, percentileBuffer);

                    calculateBuildStatus(run, logger, reportForComparison.getStaplerUri(),
                            calculateRelativeDiffInPercent(currentUriReport, reportForComparison, logger, logFormat));
                }
            }
        }
    }

    // setting the build status based on the differences
    // calculated...
    private void calculateBuildStatus(Run<?, ?> run, PrintStream logger, String staplerUri, double relativeDiffPercent) {
        Result result = null;
        if (relativeDiffPercent < 0) {
            if (calculateRelativeFailedThresholdNegative(relativeDiffPercent)) {
                result = Result.FAILURE;
            } else if (calculateRelativeUnstableThresholdNegative(relativeDiffPercent)) {
                result = Result.UNSTABLE;
            }
        } else if (relativeDiffPercent >= 0) {
            if (calculateRelativeFailedThresholdPositive(relativeDiffPercent)) {
                result = Result.FAILURE;
            } else if (calculateRelativeUnstableThresholdPositive(relativeDiffPercent)) {
                result = Result.UNSTABLE;
            }
        }

        if (result != null) {
            // set result. It'll be set only when result is worse
            run.setResult(result);
            logger.print(
                    (result == Result.FAILURE) ?
                            "\nThe label \"" + staplerUri + "\"" + " caused the build to fail\n" :
                            "\nThe label \"" + staplerUri + "\"" + " made the build unstable\n"
            );
        }
    }

    private double calculateDiffInPercents(double value1, double value2) {
        return Math.round(SafeMaths.safeDivide(value1 * 100, value2) * 100)  / 100d;
    }

    private boolean calculateRelativeFailedThresholdNegative(double relativeDiffPercent) {
        return (relativeFailedThresholdNegative >= 0
                && Math.abs(relativeDiffPercent) - relativeFailedThresholdNegative > THRESHOLD_TOLERANCE);
    }

    private boolean calculateRelativeUnstableThresholdNegative(double relativeDiffPercent) {
        return (relativeUnstableThresholdNegative >= 0
                && Math.abs(relativeDiffPercent) - relativeUnstableThresholdNegative > THRESHOLD_TOLERANCE);
    }

    private boolean calculateRelativeFailedThresholdPositive(double relativeDiffPercent) {
        return (relativeFailedThresholdPositive >= 0
                && Math.abs(relativeDiffPercent) - relativeFailedThresholdPositive > THRESHOLD_TOLERANCE);
    }

    private boolean calculateRelativeUnstableThresholdPositive(double relativeDiffPercent) {
        return (relativeUnstableThresholdPositive >= 0
                && Math.abs(relativeDiffPercent) - relativeUnstableThresholdPositive > THRESHOLD_TOLERANCE);
    }

    private double calculateRelativeDiffInPercent(UriReport currentReport, UriReport reportForComparison, PrintStream logger, String logFormat) {
        double relativeDiff;
        double relativeDiffPercent = 0;

        if (configType.equalsIgnoreCase("ART")) {
            relativeDiff = (double) currentReport.getAverage() - reportForComparison.getAverage();
            relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.getAverage());

            logger.println(String.format(logFormat, reportForComparison.getStaplerUri(),
                reportForComparison.getAverage(), currentReport.getAverage(),
                relativeDiff, relativeDiffPercent));

        } else if (configType.equalsIgnoreCase("MRT")) {
            relativeDiff = (double) currentReport.getMedian() - reportForComparison.getMedian();
            relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.getMedian());

            logger.println(String.format(logFormat, reportForComparison.getStaplerUri(),
                reportForComparison.getMedian(), currentReport.getMedian(),
                relativeDiff, relativeDiffPercent));

        } else if (configType.equalsIgnoreCase("PRT")) {
            relativeDiff = (double) currentReport.get90Line() - reportForComparison.get90Line();
            relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.get90Line());

            logger.println(String.format(logFormat, reportForComparison.getStaplerUri(),
                reportForComparison.get90Line(), currentReport.get90Line(),
                relativeDiff, relativeDiffPercent));
        }
        return relativeDiffPercent;
    }

    private void writeRelativeThresholdReportInXML(Run<?, ?> run, StringBuilder averageBuffer,
                                                   StringBuilder medianBuffer, StringBuilder percentileBuffer) throws IOException {
        File xmlDirectory = createArchiveDirectoryIfMissing(run);

        File xmlfile = new File(xmlDirectory, "dashBoard_results.xml");

        try (FileWriter fw = new FileWriter(xmlfile);
                BufferedWriter bw = new BufferedWriter(fw)) {            

            String buildNo = "\t<buildNum>" + (compareBuildPrevious ? "previous" : nthBuildNumber) + "</buildNum>\n";

            String unstable = "\t<unstable>\n";
            unstable += "\t\t<negative>" + relativeUnstableThresholdNegative + "</negative>\n";
            unstable += "\t\t<positive>" + relativeUnstableThresholdPositive + "</positive>\n";
            unstable += "\t</unstable>\n";

            String failed = "\t<failed>\n";
            failed += "\t\t<negative>" + relativeFailedThresholdNegative + "</negative>\n";
            failed += "\t\t<positive>" + relativeFailedThresholdPositive + "</positive>\n";
            failed += "\t</failed>\n";

            String relative = "<relativeDefinition>\n";
            relative += buildNo + unstable + failed;
            relative += "</relativeDefinition>";

            bw.write("<?xml version=\"1.0\"?>\n");
            bw.write("<results>\n");
            bw.write(relative + "\n");

            if (averageBuffer != null) {
                bw.write(averageBuffer.toString());
            }

            if (medianBuffer != null) {
                bw.write(medianBuffer.toString());
            }

            if (percentileBuffer != null) {
                bw.write(percentileBuffer.toString());
            }

            bw.write("</results>");
        }
    }

    private File createArchiveDirectoryIfMissing(Run<?, ?> run) {
        File xmlDirectory = new File(run.getRootDir(), ARCHIVE_DIRECTORY);
        if (!xmlDirectory.exists() && !xmlDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create archive directory "+xmlDirectory.getAbsolutePath());
        }
        return xmlDirectory;
    }


    private void appendRelativeInfoAboutAverage(UriReport currentReport, UriReport reportForComparison, StringBuilder averageBuffer) {
        double relativeDiff = (double) currentReport.getAverage() - reportForComparison.getAverage();
        double relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.getAverage());

        averageBuffer.append("\t<").append(currentReport.getStaplerUri()).append(">\n");
        averageBuffer.append("\t\t<previousBuildAvg>").append(reportForComparison.getAverage()).append("</previousBuildAvg>\n");
        averageBuffer.append("\t\t<currentBuildAvg>").append(currentReport.getAverage()).append("</currentBuildAvg>\n");
        averageBuffer.append("\t\t<relativeDiff>").append(relativeDiff).append("</relativeDiff>\n");
        averageBuffer.append("\t\t<relativeDiffPercent>").append(relativeDiffPercent).append("</relativeDiffPercent>\n");
        averageBuffer.append("\t</").append(currentReport.getStaplerUri()).append(">\n");
    }

    private void appendRelativeInfoAboutMedian(UriReport currentReport, UriReport reportForComparison, StringBuilder medianBuffer) {
        double relativeDiff = (double) currentReport.getMedian() - reportForComparison.getMedian();
        double relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.getMedian());

        medianBuffer.append("\t<").append(currentReport.getStaplerUri()).append(">\n");
        medianBuffer.append("\t\t<previousBuildMed>").append(reportForComparison.getMedian()).append("</previousBuildMed>\n");
        medianBuffer.append("\t\t<currentBuildMed>").append(currentReport.getMedian()).append("</currentBuildMed>\n");
        medianBuffer.append("\t\t<relativeDiff>").append(relativeDiff).append("</relativeDiff>\n");
        medianBuffer.append("\t\t<relativeDiffPercent>").append(relativeDiffPercent).append("</relativeDiffPercent>\n");
        medianBuffer.append("\t</").append(currentReport.getStaplerUri()).append(">\n");
    }

    private void appendRelativeInfoAbout90Line(UriReport currentReport, UriReport reportForComparison, StringBuilder percentileBuffer) {
        double relativeDiff = (double) currentReport.get90Line() - reportForComparison.get90Line();
        double relativeDiffPercent = calculateDiffInPercents(relativeDiff, reportForComparison.get90Line());

        percentileBuffer.append("\t<").append(currentReport.getStaplerUri()).append(">\n");
        percentileBuffer.append("\t\t<previousBuild90Line>").append(reportForComparison.get90Line()).append("</previousBuild90Line>\n");
        percentileBuffer.append("\t\t<currentBuild90Line>").append(currentReport.get90Line()).append("</currentBuild90Line>\n");
        percentileBuffer.append("\t\t<relativeDiff>").append(relativeDiff).append("</relativeDiff>\n");
        percentileBuffer.append("\t\t<relativeDiffPercent>").append(relativeDiffPercent).append("</relativeDiffPercent>\n");
        percentileBuffer.append("\t</").append(currentReport.getStaplerUri()).append(">\n");
    }

    // Print information about Unstable & Failed Threshold
    private void printInfoAboutErrorThreshold(PrintStream logger) {
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

    // Print information about Relative Threshold
    private void printInfoAboutRelativeThreshold(PrintStream logger) {
        if (relativeFailedThresholdNegative >= 0) {
            logger.printf("Performance: Percentage of relative difference less than -%s %% sets the build as [%s]%n",
                    relativeFailedThresholdNegative, Result.FAILURE.toString());
        }

        if (relativeFailedThresholdPositive >= 0) {
            logger.printf("Performance: Percentage of relative difference more than %s %% sets the build as [%s]%n",
                    relativeFailedThresholdPositive, Result.FAILURE.toString());
        }

        if (relativeUnstableThresholdNegative >= 0) {
            logger.printf("Performance: Percentage of relative difference less than -%s %% sets the build as [%s]%n",
                    relativeUnstableThresholdNegative, Result.UNSTABLE.toString());
        }

        if (relativeUnstableThresholdPositive >= 0) {
            logger.printf("Performance: Percentage of relative difference more than %s %% sets the build as [%s]%n",
                    relativeUnstableThresholdPositive, Result.UNSTABLE.toString());
        }
    }


    private void printInfoAboutCompareBasedOn(PrintStream logger, String logFormat) {
        if (configType.equalsIgnoreCase("ART")) {
            logger.println("Average response time\n\n");
            logger.println(String.format(logFormat,
                "URI", "PrevBuildURIAvg", "CurrentBuildURIAvg", "RelativeDiff", "RelativeDiff(%)"));
        } else if (configType.equalsIgnoreCase("MRT")) {
            logger.println("Median response time\n\n");
            logger.println(String.format(logFormat,
                "URI", "PrevBuildURIMed", "CurrentBuildURIMed", "RelativeDiff", "RelativeDiff(%)"));
        } else if (configType.equalsIgnoreCase("PRT")) {
            logger.println("90 Percentile response time\n\n");
            logger.println(String.format(logFormat,
                "URI", "PrevBuildURI90%", "CurrentBuildURI90%", "RelativeDiff", "RelativeDiff(%)"));
        }
    }

    /*
     * For mode "expert evaluation"
     */
    public void evaluateInExpertMode(Run<?, ?> run, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        ConstraintFactory factory = new ConstraintFactory();
        ConstraintSettings settings = new ConstraintSettings(listener, ignoreFailedBuilds, ignoreUnstableBuilds,
                persistConstraintLog, getBaselineBuild());
        ConstraintChecker checker = new ConstraintChecker(settings, run.getParent().getBuilds());
        ArrayList<ConstraintEvaluation> ceList = new ArrayList<>();
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

      /*
       * Write JUnit report back to workspace, for other post-build actions to pick up
       */
        if (junitOutput != null && !junitOutput.isEmpty()) {
            listener.getLogger().println("Performance: Generating JUnit output: "+junitOutput);
            FilePath output = new FilePath(workspace, junitOutput);
            output.getParent().mkdirs();
            try {
                output.write(cr.getJunitReport(), null);
            }
            catch (IOException ex) {
                listener.getLogger().println("Failed to write JUnit file: "+ex.getMessage());
            }
        }
    }

    private List<File> copyReportsToMaster(Run<?, ?> build, PrintStream logger, List<FilePath> files,
                                           String parserDisplayName) throws IOException, InterruptedException {
        List<File> localReports = new ArrayList<>();
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

    @DataBoundSetter
    public void setErrorFailedThreshold(int errorFailedThreshold) {
        this.errorFailedThreshold = (errorFailedThreshold == -1) ? (-1) : Math.max(0, Math.min(errorFailedThreshold, 100));
    }

    public int getErrorUnstableThreshold() {
        return errorUnstableThreshold;
    }

    @DataBoundSetter
    public void setErrorUnstableThreshold(int errorUnstableThreshold) {
        this.errorUnstableThreshold = (errorUnstableThreshold == -1) ? (-1) : Math.max(0, Math.min(errorUnstableThreshold, 100));
    }

    public String getErrorUnstableResponseTimeThreshold() {
        return this.errorUnstableResponseTimeThreshold;
    }

    @DataBoundSetter
    public void setErrorUnstableResponseTimeThreshold(String errorUnstableResponseTimeThreshold) {
        this.errorUnstableResponseTimeThreshold = errorUnstableResponseTimeThreshold;
    }

    public boolean isModePerformancePerTestCase() {
        return modePerformancePerTestCase;
    }

    @DataBoundSetter
    public void setModePerformancePerTestCase(boolean modePerformancePerTestCase) {
        this.modePerformancePerTestCase = modePerformancePerTestCase;
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

    @DataBoundSetter
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
        File folder = new File(build.getRootDir(), PerformanceReportMap.getPerformanceReportFileRelativePath(parserDisplayName, ""));
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
    public Run<?, ?> getnthBuild(Run<?, ?> build) {
        Run<?, ?> nthBuild = build;

        int nextBuildNumber = build.number - nthBuildNumber;

        for (int i = 1; i <= nextBuildNumber; i++) {
            nthBuild = nthBuild.getPreviousBuild();
            if (nthBuild == null)
                return null;
        }
        return (nthBuildNumber == 0) ? null : nthBuild;
    }

    private List<File> getExistingReports(Run<?, ?> build, PrintStream logger, String parserDisplayName) {
        List<File> localReports = new ArrayList<>();
        final File[] localReport = getPerformanceReportDirectory(build, parserDisplayName, logger);

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

    public double getRelativeFailedThresholdPositive() {
        return relativeFailedThresholdPositive;
    }

    public double getRelativeFailedThresholdNegative() {
        return relativeFailedThresholdNegative;
    }

    @DataBoundSetter
    public void setRelativeFailedThresholdPositive(double relativeFailedThresholdPositive) {
        this.relativeFailedThresholdPositive = relativeFailedThresholdPositive;
    }

    @DataBoundSetter
    public void setRelativeFailedThresholdNegative(double relativeFailedThresholdNegative) {
        this.relativeFailedThresholdNegative = relativeFailedThresholdNegative;
    }

    public double getRelativeUnstableThresholdPositive() {
        return relativeUnstableThresholdPositive;
    }

    public double getRelativeUnstableThresholdNegative() {
        return relativeUnstableThresholdNegative;
    }

    @DataBoundSetter
    public void setRelativeUnstableThresholdPositive(double relativeUnstableThresholdPositive) {
        this.relativeUnstableThresholdPositive = relativeUnstableThresholdPositive;
    }

    @DataBoundSetter
    public void setRelativeUnstableThresholdNegative(double relativeUnstableThresholdNegative) {
        this.relativeUnstableThresholdNegative = relativeUnstableThresholdNegative;
    }

    public int getNthBuildNumber() {
        return nthBuildNumber;
    }

    @DataBoundSetter
    public void setNthBuildNumber(int nthBuildNumber) {
        this.nthBuildNumber = Math.max(0, Math.min(nthBuildNumber, Integer.MAX_VALUE));
    }

    public String getConfigType() {
        return configType;
    }

    @DataBoundSetter
    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getGraphType() {
        return graphType;
    }

    @DataBoundSetter
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }

    public boolean getModeOfThreshold() {
        return modeOfThreshold;
    }

    @DataBoundSetter
    public void setModeOfThreshold(boolean modeOfThreshold) {
        this.modeOfThreshold = modeOfThreshold;
    }

    public boolean getCompareBuildPrevious() {
        return compareBuildPrevious;
    }

    @DataBoundSetter
    public void setCompareBuildPrevious(boolean compareBuildPrevious) {
        this.compareBuildPrevious = compareBuildPrevious;
    }

    public boolean isModeThroughput() {
        return modeThroughput;
    }

    @DataBoundSetter
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

    public void setSourceDataFiles(String sourceDataFiles) {
        this.sourceDataFiles = sourceDataFiles;
    }

    public List<PerformanceReportParser> getParsers() {
        return parsers;
    }

    @DataBoundSetter
    public void setParsers(List<PerformanceReportParser> parsers) {
        this.parsers = parsers;
        migrateParsers();
    }

    public boolean isExcludeResponseTime() {
        return excludeResponseTime;
    }

    @DataBoundSetter
    public void setExcludeResponseTime(boolean excludeResponseTime) {
        this.excludeResponseTime = excludeResponseTime;
    }

    public String getJunitOutput() {
        return junitOutput;
    }

    @DataBoundSetter
    public void setJunitOutput(String junitOutput) {
        this.junitOutput = junitOutput;
    }

    public String getPercentiles() {
        return percentiles;
    }

    @DataBoundSetter
    public void setPercentiles(String percentiles) {
        this.percentiles = percentiles;
    }


    public int getBaselineBuild() {
        return baselineBuild;
    }

    @DataBoundSetter
    public void setBaselineBuild(int baselineBuild) {
        this.baselineBuild = baselineBuild;
    }

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

        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.reports.throughput.ThroughputReport", ThroughputReport.class);
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.reports.ThroughputReport", ThroughputReport.class);
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

        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.reports.throughput.ThroughputReport", ThroughputReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.reports.ThroughputReport", ThroughputReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.AbstractReport", AbstractReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.constraints.ConstraintReport", ConstraintReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.PerformanceReport", PerformanceReport.class);
        Run.XSTREAM2.addCompatibilityAlias("hudson.plugins.performance.UriReport", UriReport.class);
    }

    @Symbol({"perfReport", "performanceReport"})
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
            return getResponseTimeOptions();
        }

        public ListBoxModel doFillGraphTypeItems() {
            return getResponseTimeOptions();
        }

        private ListBoxModel getResponseTimeOptions() {
            ListBoxModel items = new ListBoxModel();
            items.add("Average Response Time", "ART");
            items.add("Median Response Time", "MRT");
            items.add("Percentile Response Time", "PRT");
            return items;
        }
    }

    /**
     * @return the filterRegex
     */
    public String getFilterRegex() {
        return filterRegex;
    }

    /**
     * @param filterRegex the filterRegex to set
     */
    @DataBoundSetter
    public void setFilterRegex(String filterRegex) {
        this.filterRegex = filterRegex;
    }
}

