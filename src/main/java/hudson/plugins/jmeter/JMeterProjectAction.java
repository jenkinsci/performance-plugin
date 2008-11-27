package hudson.plugins.jmeter;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.Result;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
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

public class JMeterProjectAction implements Action {

	private static final long serialVersionUID = 1L;

	private final Project<?, ?> project;

	public JMeterProjectAction(Project<?, ?> project) {
		this.project = project;
	}

	private boolean checkIfGraphModified(StaplerRequest request,
			StaplerResponse response) throws IOException {
		AbstractBuild<?, ?> build = getProject().getLastBuild();
		Calendar t = build.getTimestamp();

		return request.checkIfModified(t, response);
	}

	private JFreeChart createErrorsChart(CategoryDataset dataset) {

		final JFreeChart chart = ChartFactory.createLineChart(
				"Percentage of errors", // chart
				// title
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

		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot
				.getRenderer();
		renderer.setStroke(new BasicStroke(4.0f));
		ColorPalette.apply(renderer);

		// crop extra space around the graph
		plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

		return chart;
	}

	private JFreeChart createRespondingTimeChart(CategoryDataset dataset) {

		final JFreeChart chart = ChartFactory.createLineChart(
				"Responding time", // chart
				// title
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

		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot
				.getRenderer();
		renderer.setStroke(new BasicStroke(4.0f));
		ColorPalette.apply(renderer);

		// crop extra space around the graph
		plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

		return chart;
	}

	public void doErrorsGraph(StaplerRequest request, StaplerResponse response)
			throws IOException {
		if (ChartUtil.awtProblem) {
			// not available. send out error message
			response.sendRedirect2(request.getContextPath()
					+ "/images/headless.png");
			return;
		}
		if (checkIfGraphModified(request, response)) {
			return;
		}

		DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderErrors = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		List<?> builds = getProject().getBuilds();
		for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
			AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator
					.next();
			if (Result.SUCCESS.equals(currentBuild.getResult())) {
				NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(
						currentBuild);
				JMeterBuildAction jmeterBuildAction = currentBuild
						.getAction(JMeterBuildAction.class);
				JMeterReport jmeterReport = jmeterBuildAction.getJmeterReport();
				dataSetBuilderErrors.add(((double) jmeterReport.countErrors())
						/ jmeterReport.size() * 100, "errors", label);

			}
		}

		ChartUtil.generateGraph(request, response,
				createErrorsChart(dataSetBuilderErrors.build()), 400, 200);
	}

	public void doRespondingTimeGraph(StaplerRequest request,
			StaplerResponse response) throws IOException {
		if (ChartUtil.awtProblem) {
			// not available. send out error message
			response.sendRedirect2(request.getContextPath()
					+ "/images/headless.png");
			return;
		}
		if (checkIfGraphModified(request, response)) {
			return;
		}

		DataSetBuilder<String, NumberOnlyBuildLabel> dataSetBuilderAverage = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		List<?> builds = getProject().getBuilds();
		for (Iterator<?> iterator = builds.iterator(); iterator.hasNext();) {
			AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator
					.next();
			if (Result.SUCCESS.equals(currentBuild.getResult())) {
				NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(
						currentBuild);
				JMeterBuildAction jmeterBuildAction = currentBuild
						.getAction(JMeterBuildAction.class);
				JMeterReport jmeterReport = jmeterBuildAction.getJmeterReport();
				dataSetBuilderAverage.add(jmeterReport.getMax(), "max", label);
				dataSetBuilderAverage.add(jmeterReport.getAverage(), "average",
						label);
				dataSetBuilderAverage.add(jmeterReport.getMin(), "min", label);

			}
		}

		ChartUtil.generateGraph(request, response,
				createRespondingTimeChart(dataSetBuilderAverage.build()), 400,
				200);
	}

	public String getDisplayName() {
		return "JMeter trend";
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public Project<?, ?> getProject() {
		return project;
	}

	public String getUrlName() {
		return "jmeter";
	}

}
