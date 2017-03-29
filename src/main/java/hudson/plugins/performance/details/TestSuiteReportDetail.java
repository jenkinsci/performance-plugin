package hudson.plugins.performance.details;

import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.plugins.performance.Messages;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.actions.PerformanceProjectAction.Range;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.awt.Color;
import java.awt.BasicStroke;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configures the trend graph of this plug-in.
 */
public class TestSuiteReportDetail implements ModelObject {

    private final AbstractProject<?, ?> project;
    private final String filename;
    private final Range buildsLimits;

    private transient List<String> performanceReportTestCaseList;

    public TestSuiteReportDetail(final AbstractProject<?, ?> project, String filename,
                                 Range buildsLimits) {
        this.project = project;
        this.filename = filename;
        this.buildsLimits = buildsLimits;
    }

    public void doRespondingTimeGraphPerTestCaseMode(StaplerRequest request,
                                                     StaplerResponse response) throws IOException {
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        String testUri = request.getParameter("performanceReportTest");
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(getChartDatasetBuilderForBuilds(testUri, getProject().getBuilds()).build()), 600, 200);
    }

    DataSetBuilder<String, NumberOnlyBuildLabel> getChartDatasetBuilderForBuilds(String testUri, List<? extends Run<?, ?>> builds) {
        DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
        int nbBuildsToAnalyze = builds.size();
        for (Run<?, ?> build : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {

                if (!buildsLimits.includedByStep(build.number)) {
                    continue;
                }
                NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
                PerformanceBuildAction performanceBuildAction = build
                        .getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                PerformanceReport performanceReport = performanceBuildAction
                        .getPerformanceReportMap().getPerformanceReport(this.filename);
                if (performanceReport == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }

                String testStaplerUri = PerformanceReport.asStaplerURI(testUri);
                UriReport reportForTestUri = performanceReport.getUriReportMap().get(testStaplerUri);
                if (reportForTestUri != null) {
                    dataSetBuilderAverage.add(reportForTestUri.getAverage(), testUri, label);
                }
            }
            nbBuildsToAnalyze--;
        }
        return dataSetBuilderAverage;
    }

    protected static JFreeChart createRespondingTimeChart(CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createLineChart(
                Messages.ProjectAction_RespondingTime(), // charttitle
                null, // unused
                "ms", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot
                .getRenderer();
        renderer.setBaseStroke(new BasicStroke(4.0f));
        ColorPalette.apply(renderer);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

        return chart;
    }

    public List<String> getPerformanceReportTestCaseList() {
        this.performanceReportTestCaseList = new ArrayList<String>(0);
        String performanceReportNameFile = this.getFilename();

        List<? extends Run<?, ?>> builds = getProject().getBuilds();

        builds.size();
        for (Run<?, ?> build : builds) {

            PerformanceBuildAction performanceBuildAction = build
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

            for (UriReport currentReport : performanceReport.getUriReportMap().values()) {
                if (!performanceReportTestCaseList.contains(currentReport.getUri())) {
                    performanceReportTestCaseList.add(currentReport.getUri());
                }
            }
        }

        Collections.sort(performanceReportTestCaseList);

        return this.performanceReportTestCaseList;
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public String getFilename() {
        return filename;
    }

    public String getDisplayName() {
        return Messages.TestSuiteReportDetail_DisplayName();
    }

}
