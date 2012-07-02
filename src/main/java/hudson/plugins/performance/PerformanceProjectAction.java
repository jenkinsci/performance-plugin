package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.*;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;


import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;





public final class PerformanceProjectAction implements Action {

  private static final String CONFIGURE_LINK = "configure";
  private static final String TRENDREPORT_LINK = "trendReport";

  private static final String PLUGIN_NAME = "performance";


  private static final long serialVersionUID = 1L;

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(PerformanceProjectAction.class.getName());

  public final AbstractProject<?, ?> project;

  private transient List<String> performanceReportList;

  public PerformanceSimpleCache simpleCache;

  public String getDisplayName() {
    return Messages.ProjectAction_DisplayName();
  }

  public String getIconFileName() {
    return "graph.gif";
  }

  public String getUrlName() {
    return PLUGIN_NAME;
  }

  public PerformanceProjectAction(AbstractProject project) {
    this.project = project;
    this.simpleCache= new PerformanceSimpleCache();
  }

  public PerformanceSimpleCache getSimpleCache() {
      return simpleCache;
  }


  private JFreeChart createErrorsChart(CategoryDataset dataset) {

    final JFreeChart chart = ChartFactory.createLineChart(
        Messages.ProjectAction_PercentageOfErrors(), // chart title
        null, // unused
        "%", // range axis label
        dataset, // data
        PlotOrientation.VERTICAL, // orientation
        true, // include legend
        true, // tooltips
        false // urls
    );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.RIGHT);

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
    rangeAxis.setUpperBound(100);
    rangeAxis.setLowerBound(0);

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
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
    legend.setPosition(RectangleEdge.RIGHT);

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

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(4.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }


  protected static JFreeChart createSummarizerChart (CategoryDataset dataset, String yAxis, String chartTitle) {

      final JFreeChart chart = ChartFactory.createBarChart(
          chartTitle, // chart title
          null, // unused
          yAxis, // range axis label
          dataset, // data
          PlotOrientation.VERTICAL, // orientation
          true, // include legend
          true, // tooltips
          true // urls
       );

       chart.setBackgroundPaint(Color.white);

       final CategoryPlot plot = chart.getCategoryPlot();

       plot.setBackgroundPaint(Color.WHITE);
       plot.setRangeGridlinesVisible(true);
       plot.setRangeGridlinePaint(Color.black);

       CategoryAxis domainAxis = plot.getDomainAxis();
       domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);


       final BarRenderer renderer = (BarRenderer) plot.getRenderer();
           renderer.setDrawBarOutline(false);
           renderer.setBaseStroke(new BasicStroke(4.0f));
           renderer.setItemMargin(0);
           renderer.setMaximumBarWidth(0.05);
           

      return chart;
    }


  protected static JFreeChart createSummarizerTrend (ArrayList<XYDataset> dataset, String uri) {

      final JFreeChart chart = ChartFactory.createTimeSeriesChart(
            uri  ,
            "Time",
            "Response Time",
            dataset.get(0),
            true,
            true,
            false);
      chart.setBackgroundPaint(Color.white);

      final XYPlot plot = chart.getXYPlot();
      plot.setBackgroundPaint(Color.white);
      plot.setDomainGridlinePaint(Color.black);
      plot.setRangeGridlinePaint(Color.black);

      plot.setDomainCrosshairVisible(true);
      plot.setRangeCrosshairVisible(true);

/*    final NumberAxis axis2 = new NumberAxis("Errors");
      axis2.isAutoRange();
      axis2.setLowerBound(0);
      plot.setRangeAxis(1, axis2);
      plot.setDataset(1, dataset.get(1));
      plot.mapDatasetToRangeAxis(1, 1);

      final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
      renderer2.setSeriesPaint(0, Color.black);
      plot.setRenderer(1, renderer2);
*/
      final DateAxis axis = (DateAxis) plot.getDomainAxis();
      axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));

      final XYItemRenderer renderer =  plot.getRenderer();
      renderer.setSeriesPaint(0, ColorPalette.RED);

      return chart;
  }


  public void doErrorsGraph(StaplerRequest request, StaplerResponse response)
      throws IOException {
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
    if (performanceReportNameFile == null) {
      if (getPerformanceReportList().size() == 1) {
        performanceReportNameFile = getPerformanceReportList().get(0);
      } else {
        return;
      }
    }
    if (ChartUtil.awtProblemCause != null) {
      // not available. send out error message
      response.sendRedirect2(request.getContextPath() + "/images/headless.png");
      return;
    }
    DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
    Range buildsLimits = getFirstAndLastBuild(request, builds);

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> currentBuild :builds) {
      if (buildsLimits.in(nbBuildsToAnalyze)) {
        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
        PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
        if (performanceBuildAction == null) {
          continue;
        }
        PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
            performanceReportNameFile);
        if (performanceReport == null) {
          nbBuildsToAnalyze--;
          continue;
        }
        dataSetBuilderErrors.add(performanceReport.errorPercent(),
            Messages.ProjectAction_Errors(), label);
      }
      nbBuildsToAnalyze--;
    }
    ChartUtil.generateGraph(request, response,
        createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
  }

    public void doRespondingTimeGraph(StaplerRequest request,
      StaplerResponse response) throws IOException {
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
    if (performanceReportNameFile == null) {
      if (getPerformanceReportList().size() == 1) {
        performanceReportNameFile = getPerformanceReportList().get(0);
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
    Range buildsLimits = getFirstAndLastBuild(request, builds);

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> build : builds) {
      if (buildsLimits.in(nbBuildsToAnalyze)) {
        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
        PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
        if (performanceBuildAction == null) {
          continue;
        }
        PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
            performanceReportNameFile);
        if (performanceReport == null) {
          nbBuildsToAnalyze--;
          continue;
        }
        dataSetBuilderAverage.add(performanceReport.getMedian(),
            Messages.ProjectAction_Median(), label);
        dataSetBuilderAverage.add(performanceReport.getAverage(),
            Messages.ProjectAction_Average(), label);
        dataSetBuilderAverage.add(performanceReport.get90Line(),
            Messages.ProjectAction_Line90(), label);
      }
      nbBuildsToAnalyze--;
    }
    ChartUtil.generateGraph(request, response,
        createRespondingTimeChart(dataSetBuilderAverage.build()), 400, 200);
  }


  public void doSummarizerGraph(StaplerRequest request,
                                StaplerResponse response) throws IOException {

      PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
      request.bindParameters(performanceReportPosition);
      String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
      if (performanceReportNameFile == null) {
        if (getPerformanceReportList().size() == 1) {
          performanceReportNameFile = getPerformanceReportList().get(0);
        } else {
          return;
        }
      }
      if (ChartUtil.awtProblemCause != null) {
        // not available. send out error message
        //response.sendRedirect2(request.getContextPath() + "/images/headless.png");
        return;
      }
      DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizer = new DataSetBuilder<NumberOnlyBuildLabel, String>();
      DataSetBuilder<NumberOnlyBuildLabel, String> dataSetBuilderSummarizerErrors = new DataSetBuilder<NumberOnlyBuildLabel, String>();
      
      List<?> builds = getProject().getBuilds();
      Range buildsLimits = getFirstAndLastBuild(request, builds);

      int nbBuildsToAnalyze = builds.size();
      for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
        AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
        if (buildsLimits.in(nbBuildsToAnalyze)) {
          NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
          PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
          if (performanceBuildAction == null) {
            continue;
          }
          PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
              performanceReportNameFile);

          if (performanceReport == null) {
            nbBuildsToAnalyze--;
            continue;
          }

          for (String key:performanceReport.getUriReportMap().keySet()) {
              Long methodAvg=performanceReport.getUriReportMap().get(key).getAverage();
              float methodErrors= Float.valueOf(performanceReport.getUriReportMap().get(key).getSummarizerErrors());
              dataSetBuilderSummarizer.add(methodAvg, label, key);
              dataSetBuilderSummarizerErrors.add(methodErrors, label, key);
          };
        }
       nbBuildsToAnalyze--;
      }

      String summarizerReportType = performanceReportPosition.getSummarizerReportType();

      if (summarizerReportType != null) {
        ChartUtil.generateGraph(request, response,
        createSummarizerChart(dataSetBuilderSummarizerErrors.build(),"%",Messages.ProjectAction_PercentageOfErrors()), 400, 200);
      }
      else {
        ChartUtil.generateGraph(request, response,
        createSummarizerChart(dataSetBuilderSummarizer.build(),"ms",Messages.ProjectAction_RespondingTime()), 400, 200);
      }
  }


  /**
   * <p>
   * give a list of two Integer : the smallest build to use and the biggest.
   * </p>
   * 
   * @param request
   * @param builds
   * @return outList
   */

  private Range getFirstAndLastBuild(StaplerRequest request, List<?> builds) {
    Range range = new Range();
    GraphConfigurationDetail graphConf = (GraphConfigurationDetail) createUserConfiguration(request);

    if (graphConf.isNone()) {
          return all(builds);
    }

    if (graphConf.isBuildCount()) {
      if (graphConf.getBuildCount() <= 0) {
          return all(builds);
      } else {
          int first = builds.size() - graphConf.getBuildCount();
          return new Range( first > 0 ? first + 1 : 1,
                            builds.size());
      }
    } else if (graphConf.isDate()) {
      if (graphConf.isDefaultDates()) {
          return all(builds);
      } else {
        int firstBuild = -1;
        int lastBuild = -1;
        int var = builds.size();
        GregorianCalendar firstDate = null;
        GregorianCalendar lastDate = null;
        try {
          firstDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getFirstDayCount());
          lastDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getLastDayCount());
          lastDate.set(GregorianCalendar.HOUR_OF_DAY, 23);
          lastDate.set(GregorianCalendar.MINUTE, 59);
          lastDate.set(GregorianCalendar.SECOND, 59);
        } catch (ParseException e) {
          LOGGER.log(Level.SEVERE, "Error during the manage of the Calendar", e);
        }
        for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
          AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
          GregorianCalendar buildDate = new GregorianCalendar();
          buildDate.setTime(currentBuild.getTimestamp().getTime());
          if (firstDate.getTime().before(buildDate.getTime())) {
            firstBuild = var;
          }
          if (lastBuild < 0 && lastDate.getTime().after(buildDate.getTime())) {
            lastBuild = var;
          }
          var--;
        }
        return new Range(firstBuild,lastBuild);
      }
    }
    throw new IllegalArgumentException("unsupported configType + " + graphConf.getConfigType());
  }

  private Range all(List<?> builds) {
      return new Range(1, builds.size());
  }

  public AbstractProject<?, ?> getProject() {
    return project;
  }

  public List<String> getPerformanceReportList() {
    this.performanceReportList = new ArrayList<String>(0);
    if (null == this.project) {
      return performanceReportList;
    }
    if (null == this.project.getSomeBuildWithWorkspace()) {
      return performanceReportList;
    }
    File file = new File(this.project.getSomeBuildWithWorkspace().getRootDir(),
        PerformanceReportMap.getPerformanceReportDirRelativePath());
    if (!file.isDirectory()) {
      return performanceReportList;
    }

    for (File entry : file.listFiles()) {
      if (entry.isDirectory()) {
        for (File e : entry.listFiles()) {
          this.performanceReportList.add(e.getName());
        }
      } else {
        this.performanceReportList.add(entry.getName());
      }
        
    }

    Collections.sort(performanceReportList);

    return this.performanceReportList;
  }

  public void setPerformanceReportList(List<String> performanceReportList) {
    this.performanceReportList = performanceReportList;
  }

  public boolean isTrendVisibleOnProjectDashboard() {
    if (getPerformanceReportList() != null
        && getPerformanceReportList().size() == 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns the graph configuration for this project.
   * 
   * @param link
   *            not used
   * @param request
   *            Stapler request
   * @param response
   *            Stapler response
   * @return the dynamic result of the analysis (detail page).
   */
  public Object getDynamic(final String link, final StaplerRequest request,
      final StaplerResponse response) {
    if (CONFIGURE_LINK.equals(link)) {
      return createUserConfiguration(request);
    } else if (TRENDREPORT_LINK.equals(link)) {
      return createTrendReport(request);
    } else {
      return null;
    }
  }

  /**
   * Creates a view to configure the trend graph for the current user.
   * 
   * @param request
   *            Stapler request
   * @return a view to configure the trend graph for the current user
   */
  private Object createUserConfiguration(final StaplerRequest request) {
    GraphConfigurationDetail graph = new GraphConfigurationDetail(project,
        PLUGIN_NAME, request);
    return graph;
  }

  /**
   * Creates a view to configure the trend graph for the current user.
   * 
   * @param request
   *            Stapler request
   * @return a view to configure the trend graph for the current user
   */
  private Object createTrendReport(final StaplerRequest request) {
    String filename = getTrendReportFilename(request);
    CategoryDataset dataSet = getTrendReportData(request, filename).build();
    TrendReportDetail report = new TrendReportDetail(project, PLUGIN_NAME,
        request, filename, dataSet);
    return report;
  }

  private String getTrendReportFilename(final StaplerRequest request) {
    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);
    return performanceReportPosition.getPerformanceReportPosition();
  }

  private DataSetBuilder getTrendReportData(final StaplerRequest request,
      String performanceReportNameFile) {

    DataSetBuilder<String, NumberOnlyBuildLabel> dataSet = new DataSetBuilder<String, NumberOnlyBuildLabel>();
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
    Range buildsLimits = getFirstAndLastBuild(request, builds);

    int nbBuildsToAnalyze = builds.size();
    for (AbstractBuild<?, ?> currentBuild : builds) {
      if (buildsLimits.in(nbBuildsToAnalyze)) {
        NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
        PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
        if (performanceBuildAction == null) {
          continue;
        }
        PerformanceReport report = null;
        report = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(
            performanceReportNameFile);
        if (report == null) {
          nbBuildsToAnalyze--;
          continue;
        }
        dataSet.add(Math.round(report.getAverage()),
            Messages.ProjectAction_Average(), label);
        dataSet.add(Math.round(report.getMedian()),
            Messages.ProjectAction_Median(), label);
        dataSet.add(Math.round(report.get90Line()),
            Messages.ProjectAction_Line90(), label);
        dataSet.add(Math.round(report.getMin()),
            Messages.ProjectAction_Minimum(), label);
        dataSet.add(Math.round(report.getMax()),
            Messages.ProjectAction_Maximum(), label);
        dataSet.add(Math.round(report.errorPercent()),
            Messages.ProjectAction_PercentageOfErrors(), label);
        dataSet.add(Math.round(report.countErrors()),
            Messages.ProjectAction_Errors(), label);
      }
      nbBuildsToAnalyze--;
    }
    return dataSet;
  }


  public boolean ifSummarizerParserUsed(String filename) {
      boolean b = false;
      String  fileExt;
      List<PerformanceReportParser> list =  project.getPublishersList().get(PerformancePublisher.class).getParsers();

      for ( int i=0; i < list.size(); i++) {
          if (list.get(i).getDescriptor().getDisplayName()=="JmeterSummarizer") {
              fileExt = list.get(i).glob;
              String parts[] = fileExt.split("\\s*[;:,]+\\s*");
              for (String path : parts) {
                  if (filename.endsWith(path.substring(5))) {
                      b=true;
                  }
              }
          }
      }
    return b;
  }



  private static class Range {

      public int first;

      public int last;

      private Range() {
      }

      private Range(int first, int last) {
          this.first = first;
          this.last = last;
      }

      public boolean in(int nbBuildsToAnalyze) {
          return nbBuildsToAnalyze <= last
              && first <= nbBuildsToAnalyze;
      }
  }

}
