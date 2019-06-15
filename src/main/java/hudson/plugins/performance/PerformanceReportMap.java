package hudson.plugins.performance;

import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.plugins.performance.data.ReportValueSelector;
import hudson.plugins.performance.details.GraphConfigurationDetail;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import hudson.plugins.performance.reports.AbstractReport;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.data.PerformanceReportPosition;
import hudson.plugins.performance.reports.ThroughputReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Root object of a performance report.
 */
public class PerformanceReportMap implements ModelObject {

    /**
     * The {@link PerformanceBuildAction} that this report belongs to.
     */
    private transient PerformanceBuildAction buildAction;
    /**
     * {@link PerformanceReport}s are keyed by
     * {@link PerformanceReport#reportFileName}
     * <p>
     * Test names are arbitrary human-readable and URL-safe string that identifies
     * an individual report.
     */
    private Map<String, PerformanceReport> performanceReportMap = new LinkedHashMap<>();
    private static final String PERFORMANCE_REPORTS_DIRECTORY = "performance-reports";
    private static final String PLUGIN_NAME = "performance";
    private static final String TRENDREPORT_LINK = "trendReport";

    public PerformanceReportMap(final PerformanceBuildAction buildAction,
                                TaskListener listener) throws IOException {
        this(buildAction, listener, true);
    }

    /**
     * Parses the reports and build a {@link PerformanceReportMap}.
     *
     * @throws IOException If a report fails to parse.
     */
    public PerformanceReportMap(final PerformanceBuildAction buildAction,
                         TaskListener listener, boolean isTopLevel) throws IOException {
        this.buildAction = buildAction;
        parseReports(getBuild(), listener, new PerformanceReportCollector() {

            public void addAll(Collection<PerformanceReport> reports) {
                for (PerformanceReport r : reports) {
                    r.setBuildAction(buildAction);
                    performanceReportMap.put(r.getReportFileName(), r);
                }
            }
        }, null);

        if (isTopLevel) {
            addPreviousBuildReports();
        }
    }



    private void addAll(Collection<PerformanceReport> reports) {
        for (PerformanceReport r : reports) {
            r.setBuildAction(buildAction);
            performanceReportMap.put(r.getReportFileName(), r);
        }
    }

    public Run<?, ?> getBuild() {
        return buildAction.getBuild();
    }

    PerformanceBuildAction getBuildAction() {
        return buildAction;
    }

    public String getDisplayName() {
        return Messages.Report_DisplayName();
    }

    public List<PerformanceReport> getPerformanceListOrdered() {
        List<PerformanceReport> listPerformance = new ArrayList<PerformanceReport>(
                getPerformanceReportMap().values());
        Collections.sort(listPerformance);
        return listPerformance;
    }

    protected PerformancePublisher getPublisher() {
        if (buildAction != null) {
            Run<?, ?> build = buildAction.getBuild();
            if (build != null) {
                Job<?, ?> job = build.getParent();
                if (job instanceof AbstractProject) {
                    AbstractProject project = (AbstractProject) job;
                    Describable describable = project.getPublishersList().get(PerformancePublisher.class);
                    return (describable != null) ? (PerformancePublisher) describable : null;
                }
            }
        }
        return null;
    }

    public boolean ifModeThroughputUsed() {
        PerformancePublisher publisher = getPublisher();
        return publisher == null || publisher.isModeThroughput();
    }

    public boolean ifModePerformancePerTestCaseUsed() {
        PerformancePublisher publisher = getPublisher();
        return publisher == null || publisher.isModePerformancePerTestCase();
    }

    public Map<String, PerformanceReport> getPerformanceReportMap() {
        return performanceReportMap;
    }

    /**
     * <p>
     * Give the Performance report with the parameter for name in Bean
     * </p>
     *
     * @param performanceReportName
     * @return
     */
    public PerformanceReport getPerformanceReport(String performanceReportName) {
        return performanceReportMap.get(performanceReportName);
    }

    /**
     * Get a URI report within a Performance report file
     *
     * @param uriReport "Performance report file name";"URI name"
     * @return
     */
    public UriReport getUriReport(String uriReport) {
        if (uriReport != null) {
            String uriReportDecoded;
            try {
                uriReportDecoded = URLDecoder
                        .decode(uriReport.replace(UriReport.END_PERFORMANCE_PARAMETER, ""),
                                "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            StringTokenizer st = new StringTokenizer(uriReportDecoded,
                    GraphConfigurationDetail.SEPARATOR);
            return getPerformanceReportMap().get(st.nextToken()).getUriReportMap()
                    .get(st.nextToken());
        } else {
            return null;
        }
    }

    public String getUrlName() {
        return PLUGIN_NAME;
    }

    public void setBuildAction(PerformanceBuildAction buildAction) {
        this.buildAction = buildAction;
    }

    public void setPerformanceReportMap(
            Map<String, PerformanceReport> performanceReportMap) {
        this.performanceReportMap = performanceReportMap;
    }

    public static String getPerformanceReportFileRelativePath(
            String parserDisplayName, String reportFileName) {
        return getRelativePath(parserDisplayName, reportFileName);
    }

    public static String getPerformanceReportDirRelativePath() {
        return getRelativePath();
    }

    private static String getRelativePath(String... suffixes) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(PERFORMANCE_REPORTS_DIRECTORY);
        for (String suffix : suffixes) {
            sb.append(File.separator).append(suffix);
        }
        return sb.toString();
    }

    /**
     * <p>
     * Verify if the PerformanceReport exist the performanceReportName must to be
     * like it is in the build
     * </p>
     *
     * @param performanceReportName
     * @return boolean
     */
    public boolean isFailed(String performanceReportName) {
        return getPerformanceReport(performanceReportName) == null;
    }

    public void doRespondingTimeGraph(StaplerRequest request,
                                      StaplerResponse response) throws IOException {
        String parameter = request.getParameter("performanceReportPosition");
        Run<?, ?> previousBuild = getBuild();
        final Map<Run<?, ?>, Map<String, PerformanceReport>> buildReports = getBuildReports(parameter, previousBuild);
        // Now we should have the data necessary to generate the graphs!
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        ReportValueSelector valueSelector = ReportValueSelector.get(getBuild().getParent());
        String keyLabel = getKeyLabel(valueSelector.getGraphType());
        for (Run<?, ?> currentBuild : buildReports.keySet()) {
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
            PerformanceReport report = buildReports.get(currentBuild).get(parameter);
            dataSetBuilder.add(valueSelector.getValue(report),
                    keyLabel, label);
        }
        String legendLimit = request.getParameter("legendLimit");
        int limit = (legendLimit != null && !legendLimit.isEmpty()) ? Integer.parseInt(legendLimit) : Integer.MAX_VALUE;
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(dataSetBuilder.build(), limit), 400, 200);
    }

    public void doThroughputGraph(StaplerRequest request, StaplerResponse response) throws IOException {
        String parameter = request.getParameter("performanceReportPosition");
        if (parameter == null) {
            return;
        }

        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }

        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<>();
        List<? extends Run<?, ?>> builds = buildAction.getBuild().getParent().getBuilds();

        for (final Run<?, ?> build : builds) {
                final PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }

                final PerformanceReport performanceReport = performanceBuildAction
                        .getPerformanceReportMap().getPerformanceReport(parameter);
                if (performanceReport == null) {
                    continue;
                }

                final ThroughputReport throughputReport = new ThroughputReport(performanceReport);
                final NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
                dataSetBuilder.add(throughputReport.get(), Messages.ProjectAction_RequestsPerSeconds(), label);
        }

        ChartUtil.generateGraph(request, response,
                createThroughputChart((dataSetBuilder.build())), 400, 200);
    }

    protected JFreeChart createThroughputChart(CategoryDataset dataset) {
        return PerformanceProjectAction.createThroughputChart(dataset);
    }


    public void doRespondingTimeGraphPerTestCaseMode(
            StaplerRequest request, StaplerResponse response) throws IOException {
        final String performanceReportNameFile = request.getParameter("performanceReportPosition");
        if (performanceReportNameFile == null) {
            return;
        }

        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }

        final DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<>();
        List<? extends Run<?, ?>> builds = buildAction.getBuild().getParent().getBuilds();

        ReportValueSelector valueSelector = ReportValueSelector.get(getPublisher());


        for (Run<?, ?> build : builds) {
                NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);

            final PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
                continue;
            }

            final PerformanceReport performanceReport = performanceBuildAction
                    .getPerformanceReportMap().getPerformanceReport(performanceReportNameFile);
            if (performanceReport == null) {
                continue;
            }

            List<UriReport> uriListOrdered = performanceReport.getUriListOrdered();
            for (UriReport uriReport : uriListOrdered) {
                dataSetBuilder.add(valueSelector.getValue(uriReport), uriReport.getUri(), label);
            }
        }

        String legendLimit = request.getParameter("legendLimit");
        int limit = (legendLimit != null && !legendLimit.isEmpty()) ? Integer.parseInt(legendLimit) : Integer.MAX_VALUE;
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(dataSetBuilder.build(), limit), 600, 200);
    }

    public void doErrorsGraph(StaplerRequest request, StaplerResponse response)
            throws IOException {
        final String performanceReportNameFile = request.getParameter("performanceReportPosition");
        if (performanceReportNameFile == null) {
            return;
        }

        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        List<? extends Run<?, ?>> builds = buildAction.getBuild().getParent().getBuilds();

        for (Run<?, ?> currentBuild : builds) {
                NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
                PerformanceBuildAction performanceBuildAction = currentBuild
                        .getAction(PerformanceBuildAction.class);

                if (performanceBuildAction == null) {
                    continue;
                }
                PerformanceReport performanceReport = performanceBuildAction
                        .getPerformanceReportMap().getPerformanceReport(
                                performanceReportNameFile);
                if (performanceReport == null) {
                    continue;
                }

                dataSetBuilderErrors.add(performanceReport.errorPercent(),
                        Messages.ProjectAction_Errors(), label);
        }
        ChartUtil.generateGraph(request, response,
                createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
    }

    protected JFreeChart createErrorsChart(CategoryDataset dataset) {
        return PerformanceProjectAction.createErrorsChart(dataset);
    }


    protected JFreeChart createRespondingTimeChart(CategoryDataset dataset, int legendLimit) {
        return PerformanceProjectAction.doCreateRespondingTimeChart(dataset, legendLimit);
    }

    private String getKeyLabel(String configType) {
        if (configType.equals(PerformancePublisher.MRT))
            return Messages.ProjectAction_Median();
        if (configType.equals(PerformancePublisher.PRT))
            return Messages.ProjectAction_Line90();
        return Messages.ProjectAction_Average();
    }

    private Map<Run<?, ?>, Map<String, PerformanceReport>> getBuildReports(String parameter, Run<?, ?> previousBuild) throws IOException {
        final Map<Run<?, ?>, Map<String, PerformanceReport>> buildReports = new LinkedHashMap<Run<?, ?>, Map<String, PerformanceReport>>();
        while (previousBuild != null) {
            final Run<?, ?> currentBuild = previousBuild;
            parseReports(currentBuild, TaskListener.NULL,
                    new PerformanceReportCollector() {

                        public void addAll(Collection<PerformanceReport> parse) {
                            for (PerformanceReport performanceReport : parse) {
                                if (buildReports.get(currentBuild) == null) {
                                    Map<String, PerformanceReport> map = new LinkedHashMap<String, PerformanceReport>();
                                    buildReports.put(currentBuild, map);
                                }
                                buildReports.get(currentBuild).put(
                                        performanceReport.getReportFileName(), performanceReport);
                            }
                        }
                    }, parameter);
            previousBuild = previousBuild.getPreviousCompletedBuild();
        }

        return buildReports;
    }

    public void doSummarizerGraph(StaplerRequest request, StaplerResponse response)
            throws IOException {
        String parameter = request.getParameter("performanceReportPosition");
        Run<?, ?> previousBuild = getBuild();
        Map<Run<?, ?>, Map<String, PerformanceReport>> buildReports = getBuildReports(parameter, previousBuild);
        DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizer = new DataSetBuilder<NumberOnlyBuildLabel, String>();
        ReportValueSelector valueSelector = ReportValueSelector.get(getBuild().getParent());
        for (Run<?, ?> currentBuild : buildReports.keySet()) {
            NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
            PerformanceReport report = buildReports.get(currentBuild).get(parameter);

            // Now we should have the data necessary to generate the graphs!
            for (String key : report.getUriReportMap().keySet()) {
                long methodValue = valueSelector.getValue(report.getUriReportMap().get(key));
                dataSetBuilderSummarizer.add(methodValue, label, key);
            }

        }
        ChartUtil.generateGraph(
                request,
                response,
                createSummarizerChart(dataSetBuilderSummarizer.build()), 400, 200);
    }

    protected JFreeChart createSummarizerChart(CategoryDataset dataset) {
        return PerformanceProjectAction.doCreateSummarizerChart(dataset, "ms", Messages.ProjectAction_RespondingTime());
    }

    protected void parseReports(Run<?, ?> build, TaskListener listener,
                              PerformanceReportCollector collector, final String filename)
            throws IOException {
        File repo = new File(build.getRootDir(),
                PerformanceReportMap.getPerformanceReportDirRelativePath());

        // files directly under the directory are for JMeter, for compatibility
        // reasons.
        File[] files = repo.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return !f.isDirectory() && !f.getName().endsWith(".serialized");
            }
        });
        // this may fail, if the build itself failed, we need to recover gracefully
        if (files != null) {
            addAll(new JMeterParser("", AbstractReport.DEFAULT_PERCENTILES).parse(build, Arrays.asList(files), listener));
        }

        // otherwise subdirectory name designates the parser ID.
        File[] dirs = repo.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        // this may fail, if the build itself failed, we need to recover gracefully
        if (dirs != null) {
            for (File dir : dirs) {
                PerformanceReportParser p = buildAction.getParserByDisplayName(dir
                        .getName());
                if (p != null) {
                    File[] listFiles = dir.listFiles(new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            if (filename == null && !name.endsWith(".serialized")) {
                                return true;
                            }
                            if (name.equals(filename)) {
                                return true;
                            }
                            return false;
                        }
                    });
                    try {
                        collector.addAll(p.parse(build, Arrays.asList(listFiles), listener));
                    } catch (IOException ex) {
                        listener.getLogger().println("Unable to process directory '" + dir + "'.");
                        ex.printStackTrace(listener.getLogger());
                    }
                }
            }
        }

        //addPreviousBuildReports();
    }

    private void loadPreviousBuilds() {
        Run<?, ?> prev = getBuild().getPreviousCompletedBuild();
        while (prev != null) {
            getReportMap(prev);
            prev = prev.getPreviousCompletedBuild();
        }
    }

    private void addPreviousBuildReports() {
        for (Map.Entry<String, PerformanceReport> item : getPerformanceReportMap().entrySet()) {
            PerformanceReport curReport = item.getValue();
            int baselineBuild = curReport.getBaselineBuild();
            PerformanceReport reportForCompare = (baselineBuild == 0) ?
                    getPerformanceReportForBuild(getBuild().getPreviousCompletedBuild(), item.getKey()) :
                    getPerformanceReportForBuild(getBuild(baselineBuild), item.getKey());
            if (reportForCompare != null) {
                curReport.setLastBuildReport(reportForCompare);
            }
        }
    }

    protected PerformanceReportMap getReportMap(Run<?, ?> build) {
        if (build == null) {
            return null;
        }

        PerformanceBuildAction action = build.getAction(PerformanceBuildAction.class);
        if (action == null) {
            return null;
        }

        return action.getPerformanceReportMap(false);
    }

    protected PerformanceReport getPerformanceReportForBuild(Run<?, ?> build, String key) {
        PerformanceReportMap reportMap = getReportMap(build);
        if (reportMap == null) {
            return null;
        }

        return reportMap.getPerformanceReportMap().get(key);
    }

    protected Run<?, ?> getBuild(int buildNumber) {
        Run<?, ?> r = getBuild();
        while (r != null && buildNumber != r.getNumber()) {
            r = r.getPreviousBuild();
        }
        return r;
    }

    protected interface PerformanceReportCollector {

        void addAll(Collection<PerformanceReport> parse);
    }

    public Object getDynamic(final String link, final StaplerRequest request,
                             final StaplerRequest response) {
        if (TRENDREPORT_LINK.equals(link)) {
            return createTrendReportGraphs(request);
        } else {
            return null;
        }
    }

    public Object createTrendReportGraphs(final StaplerRequest request) {
        String filename = getTrendReportFilename(request);
        PerformanceReport report = performanceReportMap.get(filename);
        Run<?, ?> build = getBuild();

        TrendReportGraphs trendReport = new TrendReportGraphs(build.getParent(),
                build, request, filename, report);

        return trendReport;
    }

    private String getTrendReportFilename(final StaplerRequest request) {
        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        return performanceReportPosition.getPerformanceReportPosition();
    }
}
