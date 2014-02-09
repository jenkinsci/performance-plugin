package hudson.plugins.performance;

import hudson.model.ModelObject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.performance.PerformanceProjectAction.Range;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

/**
 * Configures the trend graph of this plug-in.
 */
public class TestSuiteReportDetail implements ModelObject {

  private AbstractProject<?, ?> project;
  private String filename;
  private Range buildsLimits;

  private transient List<String> performanceReportTestCaseList;

  public TestSuiteReportDetail(final AbstractProject<?, ?> project,
      final String pluginName, final StaplerRequest request, String filename,
      Range buildsLimits) {
    this.project = project;
    this.filename = filename;
    this.buildsLimits = buildsLimits;
  }

  public void doRespondingTimeGraphPerTestCaseMode(StaplerRequest request,
      StaplerResponse response) throws IOException {
    String testUri = request.getParameter("performanceReportTest");
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    String performanceReportNameFile = performanceReportPosition
        .getPerformanceReportPosition();
    if (performanceReportNameFile == null) {
      if (getPerformanceReportTestCaseList().size() == 1) {
        performanceReportNameFile = getPerformanceReportTestCaseList().get(0);
      } else {
        return;
      }
    }
    if (ChartUtil.awtProblemCause != null) {
      // not available. send out error message
      response.sendRedirect2(request.getContextPath() + "/images/headless.png");
      return;
    }
    DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
    Range buildsLimits = this.buildsLimits;

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> build : builds) {
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

        List<HttpSample> allSamples = new ArrayList<HttpSample>();
        for (UriReport currentReport : performanceReport.getUriReportMap()
            .values()) {
          allSamples.addAll(currentReport.getHttpSampleList());
        }
        Collections.sort(allSamples);
        for (HttpSample sample : allSamples) {
          if (sample.getUri().equals(testUri)) {
            if (sample.hasError()) {
              // we set duration as 0 for tests failed because of errors
              dataSetBuilderAverage.add(0, sample.getUri(), label);
            } else {
              dataSetBuilderAverage.add(sample.getDuration(), sample.getUri(),
                  label);
            }
          }
        }
      }
      nbBuildsToAnalyze--;
    }
    ChartUtil.generateGraph(request, response,
        createRespondingTimeChart(dataSetBuilderAverage.build()), 600, 200);
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

    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();

    builds.size();
    for (AbstractBuild<?, ?> build : builds) {

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

      List<HttpSample> allSamples = new ArrayList<HttpSample>();
      for (UriReport currentReport : performanceReport.getUriReportMap()
          .values()) {
        allSamples.addAll(currentReport.getHttpSampleList());
      }
      Collections.sort(allSamples);
      for (HttpSample sample : allSamples) {
        if (!performanceReportTestCaseList.contains(sample.getUri())) {
          performanceReportTestCaseList.add(sample.getUri());
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
