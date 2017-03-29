package hudson.plugins.performance.constraints;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import hudson.plugins.performance.data.ConstraintSettings;
import hudson.plugins.performance.descriptors.ConstraintDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import jenkins.model.Jenkins;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Parent class for AbsoluteConstraint and RelativeConstraint
 *
 * @author Rene Kugel
 */
public abstract class AbstractConstraint implements Describable<AbstractConstraint>, ExtensionPoint {

    /**
     * Holds the information whether constraint is fulfilled(true) or violated(false)
     */
    private boolean success = false;
    /**
     * True if constraint refers to a test case False if constraint refers to a whole report
     */
    private boolean isSpecifiedTestCase = false;
    /**
     * Metric which should be evaluated
     */
    private Metric meteredValue;
    /**
     * Operator which is used to compare values
     */
    private Operator operator;
    /**
     * Determines the build result if constraint is violated
     */
    private Escalation escalationLevel;
    /**
     * Holds relevant information about the evaluation
     */
    private String resultMessage = "";
    /**
     * The report file the constraint refers to
     */
    private String relatedPerfReport;
    /**
     * null if isSpecifiedTestCase == false Holds the test case if isSpecifiedTestCase == true
     */
    private TestCaseBlock testCaseBlock;
    /**
     * Reference for global constraint settings
     */
    private ConstraintSettings settings;

    public ConstraintDescriptor getDescriptor() {
        return (ConstraintDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static ExtensionList<AbstractConstraint> all() {
        return Jenkins.getInstance().getExtensionList(AbstractConstraint.class);
    }

    protected AbstractConstraint(Metric meteredValue, Operator operator, String relatedPerfReport, Escalation escalationLevel, boolean success, TestCaseBlock testCaseBlock) {
        this.relatedPerfReport = relatedPerfReport;
        this.success = success;
        this.meteredValue = meteredValue;
        this.operator = operator;
        this.escalationLevel = escalationLevel;
        if (testCaseBlock != null) {
            this.setSpecifiedTestCase(true);
            this.testCaseBlock = testCaseBlock;
        } else {
            this.setSpecifiedTestCase(false);
        }
    }

    /**
     * Cloning of a constraint Note that this is not from the Interface Clonable {@inheritDoc}
     */
    public abstract AbstractConstraint clone();

    /**
     * Evaluates whether the constraint is fulfilled or violated
     *
     * @param builds all builds that are saved in Jenkins
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws AbortException
     * @throws ParseException
     */
    public abstract ConstraintEvaluation evaluate(List<? extends Run<?, ?>> builds) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, AbortException,
            ParseException;

    /**
     * Grabs a specified Metric in a specified UriReport
     *
     * @param meteredValue the metric that should be evaluated
     * @param ur           the UriReport where the metric should be measured
     * @return the value of the specified metric in the specified UriReport
     */
    protected double checkMetredValueforUriReport(Metric meteredValue, UriReport ur) {
        switch (meteredValue) {
            case ERRORPRC:
                return ur.errorPercent();
            case AVERAGE:
                return (double) ur.getAverage();
            case LINE90:
                return (double) ur.get90Line();
            case MEDIAN:
                return (double) ur.getMedian();
            case MINIMUM:
                return (double) ur.getMin();
            case MAXIMUM:
                return (double) ur.getMax();
            default:
                return (double) ur.getAverage();
        }
    }

    /**
     * Grabs a specified Metric in a specified PerformanceReport
     *
     * @param meteredValue the metric that should be evaluated
     * @param pr           the PerformanceReport where the metric should be measured
     * @return the value of the specified metric in the specified PerformanceReport
     */
    protected double checkMetredValueforPerfReport(Metric meteredValue, PerformanceReport pr) {
        switch (meteredValue) {
            case ERRORPRC:
                return pr.errorPercent();
            case AVERAGE:
                return (double) pr.getAverage();
            case LINE90:
                return (double) pr.get90Line();
            case MEDIAN:
                return (double) pr.getMedian();
            case MINIMUM:
                return (double) pr.getMin();
            case MAXIMUM:
                return (double) pr.getMax();
            default:
                return (double) pr.getAverage();
        }
    }

    /**
     * Checks whether all parameters given in the UI are processable.
     *
     * @param builds all stored jenkins builds
     * @throws AbortException if a parameter in the UI is not processable
     * @throws ParseException if a timeframe string in the UI is not processable
     */
    protected void checkForDefectiveParams(List<? extends Run<?, ?>> builds) throws AbortException {
        boolean found = false;

        if (builds.get(0).getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceReport(getRelatedPerfReport()) == null) {
            throw new AbortException("Performance Plugin: Could't find a report specified in the performance constraints! Report: \"" + getRelatedPerfReport() + "\"");
        } else {
            PerformanceReport pr = builds.get(0).getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceReport(getRelatedPerfReport());
            if (isSpecifiedTestCase()) {
                for (UriReport ur : pr.getUriListOrdered()) {
                    if (ur.getUri().equals(getTestCaseBlock().getTestCase())) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new AbortException("Performance Plugin: Could't find a test case specified in the performance constraints! TestCase: \"" + getTestCaseBlock().getTestCase() + "\" Report: \""
                            + getRelatedPerfReport() + "\"");
                }
            }
        }
        if (this instanceof AbsoluteConstraint) {
            AbsoluteConstraint ac = (AbsoluteConstraint) this;
            if (ac.getValue() < 0) {
                throw new AbortException("Performance Plugin: The value of a Absolute Constraint can't be negative!");
            }
        }
        if (this instanceof RelativeConstraint) {
            RelativeConstraint rc = (RelativeConstraint) this;
            if (rc.getTolerance() < 0) {
                throw new AbortException("Performance Plugin: The tolerance of a Relative Constraint can't be negative!");
            }
            if (rc.getTimeframeStart().after(rc.getTimeframeEnd())) {
                throw new AbortException("Performance Plugin: The start date of a Relative Constraint can't be after the end date");
            }
            if (!rc.getPreviousResultsBlock().isChoicePreviousResults()) {
                final SimpleDateFormat dfLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                try {
                    rc.setTimeframeStart(dfLong.parse(rc.getTimeframeStartString()));
                    if (!rc.getTimeframeEndString().equals("now")) {
                        rc.setTimeframeEnd(dfLong.parse(rc.getTimeframeEndString()));
                    }
                } catch (ParseException e) {
                    throw new AbortException("Performance Plugin: Couldn't parse date in Relative Constraint! Please check the configuration of your constraints");
                }
            }
        }
    }

    public enum Metric {
        AVERAGE("Average", false), MEDIAN("Median", false), LINE90("90% Line", false), MAXIMUM("Maximum", false), MINIMUM("Minimum", false), ERRORPRC("Error %", false);

        private final String text;
        private boolean isSelected;

        private Metric(final String text, boolean isSelected) {
            this.text = text;
            this.setSelected(isSelected);
        }

        @Override
        public String toString() {
            return text;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }
    }

    public enum Escalation {
        INFORMATION("Information", false), WARNING("Warning", false), ERROR("Error", false);

        private final String text;
        private boolean isSelected;

        private Escalation(final String text, boolean isSelected) {
            this.text = text;
            this.setSelected(isSelected);
        }

        @Override
        public String toString() {
            return text;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }
    }

    public enum Operator {
        NOT_GREATER("not be greater than", false), NOT_LESS("not be less than", false), NOT_EQUAL("not be equal to", false);

        public final String text;
        private boolean isSelected;

        private Operator(final String text, boolean isSelected) {
            this.text = text;
            this.setSelected(isSelected);
        }

        @Override
        public String toString() {
            return text;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean getSuccess() {
        return this.success;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getRelatedPerfReport() {
        return relatedPerfReport;
    }

    public void setRelatedPerfReport(String relatedPerfReport) {
        this.relatedPerfReport = relatedPerfReport;
    }

    public Metric getMeteredValue() {
        return meteredValue;
    }

    public void setMeteredValue(Metric meteredValue) {
        this.meteredValue = meteredValue;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Escalation getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Escalation escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public TestCaseBlock getTestCaseBlock() {
        return testCaseBlock;
    }

    public void setTestCaseBlock(TestCaseBlock testCaseBlock) {
        this.testCaseBlock = testCaseBlock;
    }

    public boolean isSpecifiedTestCase() {
        return isSpecifiedTestCase;
    }

    public void setSpecifiedTestCase(boolean isSpecifiedTestCase) {
        this.isSpecifiedTestCase = isSpecifiedTestCase;
    }

    public ConstraintSettings getSettings() {
        return settings;
    }

    public void setSettings(ConstraintSettings settings) {
        this.settings = settings;
    }

    public String getTestCase() {
        if (getTestCaseBlock() != null) {
            return getTestCaseBlock().getTestCase();
        } else {
            return null;
        }
    }

    public void setTestCase(String testCase) {
        this.getTestCaseBlock().setTestCase(testCase);
    }

}
