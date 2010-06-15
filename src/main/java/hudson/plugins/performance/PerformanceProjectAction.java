package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public final class PerformanceProjectAction implements Action {

	private static final String CONFIGURE_LINK = "configure";
	private static final String TRENDREPORT_LINK = "trendReport";

  private static final String PLUGIN_NAME = "performance";

  private static final long serialVersionUID = 1L;

	/** Logger. */
	private static final Logger LOGGER = Logger.getLogger(PerformanceProjectAction.class.getName());

	public final AbstractProject<?, ?> project;

	private transient List<String> performanceReportList;

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

	private JFreeChart createRespondingTimeChart(CategoryDataset dataset) {

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

	public void doErrorsGraph(StaplerRequest request, StaplerResponse response) throws IOException {
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
		List<?> builds = getProject().getBuilds();
		List<Integer> buildsLimits = getFirstAndLastBuild(request, builds);

		int nbBuildsToAnalyze = builds.size();
		for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
			AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
			if (nbBuildsToAnalyze <= buildsLimits.get(1) && buildsLimits.get(0) <= nbBuildsToAnalyze) {
				NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
				PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
				if (performanceBuildAction == null) {
					continue;
				}
				PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(performanceReportNameFile);
				if (performanceReport == null) {
					nbBuildsToAnalyze--;
					continue;
				}
				dataSetBuilderErrors.add(performanceReport.errorPercent(), Messages.ProjectAction_Errors(), label);
			}
			nbBuildsToAnalyze--;
		}
		ChartUtil.generateGraph(request, response, createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
	}

	public void doRespondingTimeGraph(StaplerRequest request, StaplerResponse response) throws IOException {
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
		List<?> builds = getProject().getBuilds();
		List<Integer> buildsLimits = getFirstAndLastBuild(request, builds);

		int nbBuildsToAnalyze = builds.size();
		for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
			AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
			if (nbBuildsToAnalyze <= buildsLimits.get(1) && buildsLimits.get(0) <= nbBuildsToAnalyze) {
				NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
				PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
				if (performanceBuildAction == null) {
					continue;
				}
				PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(performanceReportNameFile);
				if (performanceReport == null) {
					nbBuildsToAnalyze--;
					continue;
				}
				dataSetBuilderAverage.add(performanceReport.getMax(), Messages.ProjectAction_Maximum(), label);
				dataSetBuilderAverage.add(performanceReport.getAverage(), Messages.ProjectAction_Average(), label);
				dataSetBuilderAverage.add(performanceReport.getMin(), Messages.ProjectAction_Minimum(), label);
			}
			nbBuildsToAnalyze--;
		}
		ChartUtil.generateGraph(request, response, createRespondingTimeChart(dataSetBuilderAverage.build()), 400, 200);
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
	private List<Integer> getFirstAndLastBuild(StaplerRequest request, List<?> builds) {
		List<Integer> outList = new ArrayList<Integer>(2);
		GraphConfigurationDetail graphConf = (GraphConfigurationDetail) createUserConfiguration(request);
		String configType = graphConf.getConfigType();
		if (configType.compareToIgnoreCase(GraphConfigurationDetail.BUILD_CONFIG) == 0) {
			if (graphConf.getBuildCount() <= 0) {
				configType = GraphConfigurationDetail.NONE_CONFIG;
			} else {
				if (builds.size() - graphConf.getBuildCount() > 0) {
					outList.add(builds.size() - graphConf.getBuildCount() + 1);
				} else {
					outList.add(1);
				}
				outList.add(builds.size());
			}
		} else if (configType.compareToIgnoreCase(GraphConfigurationDetail.DATE_CONFIG) == 0) {
			if (GraphConfigurationDetail.DEFAULT_DATE.compareTo(graphConf.getFirstDayCount()) == 0
					&& GraphConfigurationDetail.DEFAULT_DATE.compareTo(graphConf.getLastDayCount()) == 0) {
				configType = GraphConfigurationDetail.NONE_CONFIG;
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
					LOGGER.log(Level.SEVERE,"Error during the manage of the Calendar",e);
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
				outList.add(firstBuild);
				outList.add(lastBuild);
			}
		}
		if (configType.compareToIgnoreCase(GraphConfigurationDetail.NONE_CONFIG) == 0) {
			outList.add(1);
			outList.add(builds.size());
		}
		return outList;
	}

	public AbstractProject<?, ?> getProject() {
		return project;
	}

	public List<String> getPerformanceReportList() {
		this.performanceReportList = new ArrayList<String>(0);
		if (this.project != null && this.project.getSomeBuildWithWorkspace() != null) {
			File file = new File(this.project.getSomeBuildWithWorkspace().getRootDir(), PerformanceReportMap
					.getPerformanceReportDirRelativePath());
			if (file.exists()) {
				for (File performanceReportFile : file.listFiles()) {
					this.performanceReportList.add(performanceReportFile.getName());
				}
			}
		}
		if (this.performanceReportList != null) {
			Collections.sort(performanceReportList);
		}
		return this.performanceReportList;
	}

	public void setPerformanceReportList(List<String> performanceReportList) {
		this.performanceReportList = performanceReportList;
	}

	public boolean isTrendVisibleOnProjectDashboard() {
		if (getPerformanceReportList() != null && getPerformanceReportList().size() == 1) {
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
	public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
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
		GraphConfigurationDetail graph = new GraphConfigurationDetail(project, PLUGIN_NAME, request);
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
		CategoryDataset dataSet = getTrendReportData (request, filename).build();
		TrendReportDetail report = new TrendReportDetail(project, PLUGIN_NAME, request, filename, dataSet);
		return report;
	}

	private String getTrendReportFilename(final StaplerRequest request) {
		PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
		request.bindParameters(performanceReportPosition);
		return performanceReportPosition.getPerformanceReportPosition();
	}

	private DataSetBuilder getTrendReportData(final StaplerRequest request, String performanceReportNameFile) {

		DataSetBuilder<String, NumberOnlyBuildLabel> dataSet = new DataSetBuilder<String, NumberOnlyBuildLabel>();
		List<?> builds = getProject().getBuilds();
		List<Integer> buildsLimits = getFirstAndLastBuild(request, builds);

		int nbBuildsToAnalyze = builds.size();
		for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
			AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
			if (nbBuildsToAnalyze <= buildsLimits.get(1) && buildsLimits.get(0) <= nbBuildsToAnalyze) {
				NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(currentBuild);
				PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
				if (performanceBuildAction == null) {
					continue;
				}
				PerformanceReport report = null;
				report = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(performanceReportNameFile);
				if (report == null) {
					nbBuildsToAnalyze--;
					continue;
				}
				dataSet.add(report.getMax(), Messages.ProjectAction_Maximum(), label);
				dataSet.add(report.getAverage(), Messages.ProjectAction_Average(), label);
				dataSet.add(report.getMin(), Messages.ProjectAction_Minimum(), label);
			}
			nbBuildsToAnalyze--;
		}
		return dataSet;
	}
}
