package hudson.plugins.performance.constraints;

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.constraints.blocks.PreviousResultsBlock;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import hudson.plugins.performance.descriptors.ConstraintDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.plugins.performance.tools.SafeMaths;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.RunList;

/**
 * Compares new load test results with 1 or more load test results in the past in a dynamically
 * manner.
 *
 * @author Rene Kugel
 */
public class RelativeConstraint extends AbstractConstraint {
    /**
     * Percentage value of the tolerance
     */
    private double tolerance = 0;
    /**
     * True: relative constraint includes a user defined number of builds into the evaluation False:
     * relative constraint includes all builds that have taken place in an user defined time frame
     */
    private boolean choicePreviousResults = true;

    /**
     * Start of the time frame (for internal use)
     */
    private Date timeframeStart = new Date();
    /**
     * End of the time frame (for internal use)
     */
    private Date timeframeEnd = new Date();
    /**
     * Holds the relevant information to determine which builds get included into the evaluation
     */
    private PreviousResultsBlock previousResultsBlock;
    /**
     * Start of the time frame (for UI use)
     */
    private String timeframeStartString = "";
    /**
     * End of the time frame (for UI use)
     */
    private String timeframeEndString = "";
    /**
     * Holds the user defined number of builds which are to include to the evaluation (for internal
     * use)
     */
    private int previousResults = 0;
    /**
     * Holds the user defined number of builds which are to include to the evaluation (for UI use)
     */
    private String previousResultsString = "";

    @Symbol("relative")
    @Extension
    public static class DescriptorImpl extends ConstraintDescriptor {

        @Override
        public String getDisplayName() {
            return "Relative Constraint";
        }


        public FormValidation doCheckRelatedPerfReport(@QueryParameter String relatedPerfReport) {
            if (relatedPerfReport == null || relatedPerfReport.isEmpty()) {
                return FormValidation.error("This field must not be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestCase(@QueryParameter String testCase) {
            if (testCase == null || testCase.isEmpty()) {
                return FormValidation.error("This field must not be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTimeframeStartString(@QueryParameter String timeframeStartString) {
            return dateCheck(timeframeStartString);
        }

        public FormValidation doCheckTimeframeEndString(@QueryParameter String timeframeEndString) {
            if (NOW.equals(timeframeEndString)) {
                return FormValidation.ok();
            }
            return dateCheck(timeframeEndString);
        }

        private FormValidation dateCheck(String dateString) {
            final SimpleDateFormat dfLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            final SimpleDateFormat dfShort = new SimpleDateFormat("yyyy-MM-dd");
            dfLong.setLenient(false);
            dfShort.setLenient(false);
            try {
                if (
                        (dfShort.parse(dateString) != null && dateString.length() == 10) 
                        || (dfLong.parse(dateString) != null && dateString.length() == 16)) { 
                    return FormValidation.ok();
                }
            } catch (ParseException e1) {
                return FormValidation.error("Not a valid date!");
            }
            return FormValidation.error("Not a valid date!");
        }

        public FormValidation doCheckPreviousResultsString(@QueryParameter String previousResultsString, @AncestorInPath AbstractProject<?, ?> project) {
            if (ANY.equals(previousResultsString)) {
                return FormValidation.ok();
            }
            int previousResults;
            try {
                previousResults = Integer.parseInt(previousResultsString);
            } catch (NumberFormatException e) {
                return FormValidation.error("This is not a valid number");
            }
            if (previousResults < 1) {
                return FormValidation.error("This value can't be smaller 1");
            }
            /*
			 * Problem description: if you want to evaluate the 15 last builds in a relative
			 * constraint, but you only store 10 builds of your job you will get in trouble.
			 * similiar problem: you store 10 builds in you job and evaluate the last 7 SUCCESSFUL
			 * builds with a relative constraint. what if 5 of your 10 stored builds are in status
			 * FAILED or UNSTABLE? -> problem. this form validation solves this problem if your
			 * enter a number greater than the available builds (regarding your confiugration
			 * 'ignoreFailed' and 'ignoreUnstable') you will get a form validation error note: if
			 * you change 'ignoreFailed' or 'ignoreUnstable' you first have to save your
			 * configuration before you change the number of previous builds
			 */
            if (project == null) { // Counting builds makes no sense when in Pipeline snippet generator
                return FormValidation.ok();
            }
            RunList<?> builds = project.getBuilds();
            int buildsToAnalyze = 0;
            int successBuilds = 0;
            int failedBuilds = 0;
            int unstableBuilds = 0;
            String buildSizeMessage = "This value cant be bigger than the amount of stored builds with the status: SUCCESS";
            ListIterator<?> it = builds.listIterator();
            while (it.hasNext()) {
                Object next = it.next();
                if (next instanceof FreeStyleBuild) {
                    FreeStyleBuild b = (FreeStyleBuild) next;
                    Result buildResult = b.getResult();
                    if (buildResult != null) {
                        if (buildResult.equals(Result.FAILURE)) {
                            failedBuilds++;
                        } else if (buildResult.equals(Result.UNSTABLE)) {
                            unstableBuilds++;
                        } else if (buildResult.equals(Result.SUCCESS)) {
                            successBuilds++;
                        }
                    }
                }
            }
            buildsToAnalyze = successBuilds;
            boolean ignoreFailedBuilds = false;
            boolean ignoreUnstableBuilds = false;
            List<Publisher> list = project.getPublishersList().toList();
            for (Publisher p : list) {
                if (p instanceof PerformancePublisher) {
                    PerformancePublisher pp = (PerformancePublisher) p;

                    // MWA: uncomment and check
					ignoreFailedBuilds = pp.isIgnoreFailedBuilds();
					ignoreUnstableBuilds = pp.isIgnoreUnstableBuilds();
					break;
                }
            }
            if (!ignoreUnstableBuilds) {
                buildsToAnalyze += unstableBuilds;
                buildSizeMessage = buildSizeMessage + ", UNSTABLE";
            }
            if (!ignoreFailedBuilds) {
                buildsToAnalyze += failedBuilds;
                buildSizeMessage = buildSizeMessage + ", FAILED";
            }
            if (previousResults > buildsToAnalyze+1) { // at the time of evaluation there will be one more build (the next build that is run, which could be the very first or only build)
                return FormValidation.error(buildSizeMessage);
            } else {
                return FormValidation.ok();
            }
        }
    }

     @DataBoundConstructor
    public RelativeConstraint(Metric meteredValue, Operator operator, String relatedPerfReport, Escalation escalationLevel, boolean success, TestCaseBlock testCaseBlock,
                              PreviousResultsBlock previousResultsBlock, double tolerance) {

        super(meteredValue, operator, relatedPerfReport, escalationLevel, success, testCaseBlock);
        this.tolerance = tolerance;
        this.previousResultsBlock = previousResultsBlock;
        if (this.previousResultsBlock.isChoicePreviousResults()) {
            if (ANY.equals(this.previousResultsBlock.getPreviousResultsString())) {
                this.previousResults = -1;
            } else {
                try {
                    this.previousResults = Integer.parseInt(this.previousResultsBlock.getPreviousResultsString());
                } catch (NumberFormatException ex) {
                    this.previousResults = -1;
                }
            }
            this.previousResultsString = this.previousResultsBlock.getPreviousResultsString();
        }
        if (this.previousResultsBlock.isChoiceTimeframe()) {
            this.timeframeStartString = this.previousResultsBlock.getTimeframeStartString();
            this.timeframeEndString = this.previousResultsBlock.getTimeframeEndString();
            if (this.timeframeStartString.length() == 10) {
                this.timeframeStartString = this.timeframeStartString + " 00:00";
            }
            if (this.timeframeEndString.length() == 10) {
                this.timeframeEndString = this.timeframeEndString + " 23:59";
            }
            try {
                final SimpleDateFormat dfLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                this.timeframeStart = dfLong.parse(this.timeframeStartString);
                if (!NOW.equals(this.timeframeEndString)) {
                    this.timeframeEnd = dfLong.parse(this.timeframeEndString);
                }
            } catch (ParseException e) {
                PrintStream logger = getSettings().getListener().getLogger();
                logger.print("Error occurred parsing one of those dates, timeframeStartString:"+timeframeStartString
                        +", timeframeEndString:"+timeframeEndString
                        +" using format:yyyy-MM-dd HH:mm, message:"+e.getMessage());
                e.printStackTrace(logger);
            }
        }
        if (this.previousResultsBlock.isChoiceBaselineBuild()) {
             // nothing to do, baseline build number comes from settings
        }
    }

    /**
     * Cloning of a RelativeConstraint Note that this is not from the Interface Clonable
     *
     * @return clone of this object
     */
    public RelativeConstraint clone() {
        return new RelativeConstraint(this.getMeteredValue(), this.getOperator(), 
                this.getRelatedPerfReport(), this.getEscalationLevel(), this.getSuccess(), 
                new TestCaseBlock(this.getTestCaseBlock().getTestCase()),
                new PreviousResultsBlock(this.getPreviousResultsBlock().getValue(), this.getPreviousResultsString(),
                this.getTimeframeStartString(), this.getTimeframeEndString()), this.getTolerance());
    }

    @Override
    public ConstraintEvaluation evaluate(List<? extends Run<?, ?>> builds) throws AbortException, ParseException {
        if (builds.isEmpty()) {
            throw new AbortException("Performance: No builds found to evaluate!");
        }
        checkForDefectiveParams(builds);
        PerformanceReport pr = builds.get(0).getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceReport(getRelatedPerfReport());
        double calValue = 0;
        if (!isSpecifiedTestCase()) {
            calValue = checkMetredValueforPerfReport(getMeteredValue(), pr);
        } else {
            List<UriReport> uriList = pr.getUriListOrdered();
            for (UriReport ur : uriList) {
                if (getTestCaseBlock().getTestCase().equals(ur.getUri())) {
                    calValue = checkMetredValueforUriReport(getMeteredValue(), ur);
                    break;
                }
            }
        }
        return check(builds, calValue);
    }

    /**
     * Compares the values and sets the success and a result message of a constraint.
     *
     * @param builds   all builds that are saved in Jenkins
     * @param newValue value of the measured metric of the new build
     * @return evaluated constraint
     */
    private ConstraintEvaluation check(List<? extends Run<?, ?>> builds, double newValue) {
        double calculatedValue = calcAveOfReports(builds);
		/*
		 * If calculatedValue == Long.MIN_VALUE there was no build found to evaluate this constraint
		 * The process should not get aborted, but this constraint should be marked as failed, unless it is the very first or only build.
		 */
        if (calculatedValue == Long.MIN_VALUE) {
            boolean isVeryFirstBuild = (builds.size() == 1);
            setSuccess(isVeryFirstBuild);
            setResultMessage("Relative constraint " + (isVeryFirstBuild ? "skipped." : "failed!") + " - Report: " + getRelatedPerfReport() + "\n" + "There were no builds found to evaluate!");
            return new ConstraintEvaluation(this, 0, 0);
        }
        double result = 0;
        if (getOperator().equals(Operator.NOT_GREATER)) {
            result = calculatedValue * (1 + getTolerance() / 100);
        } else if (getOperator().equals(Operator.NOT_LESS)) {
            result = calculatedValue * (1 - getTolerance() / 100);
        } else {
            try {
                throw new AbortException("Performance Plugin: Relative Constraints can only handle \"not greater than\" and \"not less than\" operators. Please check your constraint configuration");
            } catch (AbortException e) {
                PrintStream logger = getSettings().getListener().getLogger();
                e.printStackTrace(logger);
            }
        }

        switch (getOperator()) {
            case NOT_LESS:
                setSuccess(result < newValue);
                break;
            case NOT_GREATER:
                setSuccess(result >= newValue);
                break;
            default:
                setSuccess(false);
                break;
        }
        ConstraintEvaluation evaluation = new ConstraintEvaluation(this, result, calculatedValue);

        String measuredLevel = isSpecifiedTestCase() ? getTestCaseBlock().getTestCase() : "all test cases";
        if (getSuccess()) {
            setResultMessage("Relative constraint successful! - Report: " + getRelatedPerfReport() + "\n" + "The constraint says: " + getMeteredValue() + " of " + measuredLevel + " must "
                    + getOperator().text + " " + result + "\n" + "Measured value for " + getMeteredValue() + ": " + newValue + "\n" + "Included builds: " + getPreviousResults() + " builds \n"
                    + "Escalation Level: " + getEscalationLevel());
        } else {
            setResultMessage("Relative constraint failed! - Report: " + getRelatedPerfReport() + "\n" + "The constraint says: " + getMeteredValue() + " of " + measuredLevel + " must "
                    + getOperator().text + " " + result + "\n" + "Measured value for " + getMeteredValue() + ": " + newValue + "\n" + "Included builds: Last " + getPreviousResults() + " builds \n"
                    + "Escalation Level: " + getEscalationLevel());
        }

        String unit = getMeteredValue()==Metric.ERRORPRC ? "percent" : "milliseconds";
        setJunitResult(String.format("<testcase classname=\"%s\" name=\"%s of %s must %s %.3f percent above/below previous\">%n",
            getRelatedPerfReport(), getMeteredValue(), measuredLevel, getOperator().text, getTolerance())
            + (getSuccess() ? "" :
                String.format("    <failure type=\"%s\">Measured value for %s: %.0f %s. Previous value: %.0f %s. Deviation: %.3f %%</failure>%n",
                getEscalationLevel(), getMeteredValue(), newValue, unit, calculatedValue, unit, (newValue/calculatedValue-1)*100))
            + "</testcase>\n");

        return evaluation;
    }

    /**
     * Calculates the average of an meteredValue from UriReports/PerfomanceReports over several
     * builds.
     *
     * @param builds all builds that are saved in Jenkins
     * @return average of measured metric over included builds
     */
    private long calcAveOfReports(List<? extends Run<?, ?>> builds) {
        List<Run<?, ?>> buildsToAnalyze = new ArrayList<>();
        long tmpResult = 0;
        int counter = 0;
        long result = 0;
        Run<?, ?> newBuild = builds.get(0);
        if (getPreviousResultsBlock().isChoiceTimeframe()) {
            buildsToAnalyze.addAll(evaluateDate(builds));
        }
        if (getPreviousResultsBlock().isChoicePreviousResults()) {
            buildsToAnalyze.addAll(evaluatePreviousBuilds(builds));
        }
        if (getPreviousResultsBlock().isChoiceBaselineBuild()) {
            buildsToAnalyze.add(builds.get(getSettings().getBaselineBuild()));
        }
        setPreviousResults(buildsToAnalyze.size());
        if (!buildsToAnalyze.isEmpty()) {
            for (Run<?, ?> actBuild : buildsToAnalyze) {
                if (actBuild.getAction(PerformanceBuildAction.class) != null && !actBuild.equals(newBuild)) {
                    List<PerformanceReport> tmpList = actBuild.getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceListOrdered();
                    for (PerformanceReport pr : tmpList) {
                        if (getRelatedPerfReport().equals(pr.getReportFileName())) {
                            if (!isSpecifiedTestCase()) {
                                tmpResult += checkMetredValueforPerfReport(getMeteredValue(), pr);
                            } else {
                                tmpResult += getUriValue(pr);
                            }
                            counter++;
                        }
                    }
                } else {
                    PrintStream logger = getSettings().getListener().getLogger();
                    logger.println("Performance: There are no comaparable data available for build #" + actBuild.getNumber() + ". Skipping this build!");
                    setPreviousResults(getPreviousResults() - 1);
                }
            }
            result = (long)SafeMaths.safeDivide(tmpResult, counter);
        } else {
			/*
			 * If no build was found to analyze return Long.MIN_VALUE. This will cause the
			 * constraint to be marked as failed (except for the first/only build).
			 */
            PrintStream logger = getSettings().getListener().getLogger();
            logger.println("Performance: There were no builds found to evaluate for a relative constraint!");
            return Long.MIN_VALUE;
        }
        return result;
    }

    /**
     * Is executed when the RadioButton "Compare with builds in a timeframe" is choosen. Determines
     * the builds that are included in the evaluation based on the constraint settings and the given
     * timeframe.
     *
     * @param builds all builds that are saved in Jenkins
     * @return builds list of builds that have taken place in a user defined time frame respecting
     * the constraint settings
     */
    private List<Run<?, ?>> evaluateDate(List<? extends Run<?, ?>> builds) {
        List<Run<?, ?>> result = new ArrayList<>();
        Calendar timeframeStartAsCalendar = Calendar.getInstance();
        timeframeStartAsCalendar.setTime(getTimeframeStart());
        Calendar timeframeEndAsCalendar = Calendar.getInstance();
        timeframeEndAsCalendar.setTime(getTimeframeEnd());

        if (NOW.equals(getTimeframeEndString())) {
            timeframeEndAsCalendar.setTime(new Date());
        }
        
        for (Run<?, ?> build : builds) {
            Result buildResult = build.getResult();
            if (buildResult != null && 
                    (buildResult.equals(Result.SUCCESS) 
                        || (buildResult.equals(Result.UNSTABLE) 
                        && !getSettings().isIgnoreUnstableBuilds()) 
                        || (buildResult.equals(Result.FAILURE)
                        && !getSettings().isIgnoreFailedBuilds()))
                        && (!build.getTimestamp().before(timeframeStartAsCalendar) 
                                && !build.getTimestamp().after(timeframeEndAsCalendar) 
                                && !build.equals(builds.get(0)))) {
                result.add(build);
            }
        }
        return result;
    }

    /**
     * Is executed when the RadioButton "Compare with previous builds" is chosen. Determines the
     * builds that are included in the evaluation based on the constraint settings
     *
     * @param builds all builds that are saved in Jenkins
     * @return build list of previous builds that get included into the evaluation
     */
    private List<Run<?, ?>> evaluatePreviousBuilds(List<? extends Run<?, ?>> builds) {
        List<Run<?, ?>> result = new ArrayList<>();
        if (getPreviousResults() == -1) {
            setPreviousResults(builds.size() - 1);
        }
        int i = 1;
        int j = 0;
        while (j < getPreviousResults() && i < builds.size()) {
            if (builds.get(i).getResult().equals(Result.SUCCESS) 
                    || (builds.get(i).getResult().equals(Result.UNSTABLE) 
                    && !getSettings().isIgnoreUnstableBuilds())
                    || (builds.get(i).getResult().equals(Result.FAILURE) && !getSettings().isIgnoreFailedBuilds())) {
                result.add(builds.get(i));
                j++;
            }
            i++;
        }
        return result;
    }

    /**
     * Searches a value in a URIReport. Metric and PerformanceReport are defined in the constraint.
     *
     * @param pr performance report where to search for the URI report
     * @return value of the specified metric
     */
    private double getUriValue(PerformanceReport pr) {
        double result = 0;
        for (UriReport ur : pr.getUriListOrdered()) {
            if (getTestCaseBlock().getTestCase().equals(ur.getUri())) {
                result = checkMetredValueforUriReport(getMeteredValue(), ur);
            }
        }
        return result;
    }

    public int getPreviousResults() {
        return previousResults;
    }

    public void setPreviousResults(int previousResults) {
        this.previousResults = previousResults;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double d) {
        this.tolerance = d;
    }

    public boolean getChoicePreviousResults() {
        return choicePreviousResults;
    }

    public void setChoicePreviousResults(boolean choicePreviousResults) {
        this.choicePreviousResults = choicePreviousResults;
    }

    public String getTimeframeStartString() {
        return timeframeStartString;
    }

    public void setTimeframeStartString(String timeframeStartString) {
        this.timeframeStartString = timeframeStartString;
    }

    public String getTimeframeEndString() {
        return timeframeEndString;
    }

    public void setTimeframeEndString(String timeframeEndString) {
        this.timeframeEndString = timeframeEndString;
    }

    public Date getTimeframeStart() {
        return timeframeStart;
    }

    public void setTimeframeStart(Date timeframeStart) {
        this.timeframeStart = timeframeStart;
    }

    public Date getTimeframeEnd() {
        return timeframeEnd;
    }

    public void setTimeframeEnd(Date timeframeEnd) {
        this.timeframeEnd = timeframeEnd;
    }

    public PreviousResultsBlock getPreviousResultsBlock() {
        return previousResultsBlock;
    }

    public void setPreviousResultsBlock(PreviousResultsBlock previousResultsBlock) {
        this.previousResultsBlock = previousResultsBlock;
    }

    public String getPreviousResultsString() {
        return previousResultsString;
    }

    public void setPreviousResultsString(String previousResultsString) {
        this.previousResultsString = previousResultsString;
    }
}
