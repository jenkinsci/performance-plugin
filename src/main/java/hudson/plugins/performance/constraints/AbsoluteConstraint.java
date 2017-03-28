package hudson.plugins.performance.constraints;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import hudson.plugins.performance.descriptors.ConstraintDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Absolute Constraints compare the result of a new load test against some user defined values.
 *
 * @author Rene Kugel
 */
public class AbsoluteConstraint extends AbstractConstraint {

    @Extension
    public static class DescriptorImpl extends ConstraintDescriptor {

        @Override
        public String getDisplayName() {
            return "Absolute Constraint";
        }

        public FormValidation doCheckRelatedPerfReport(@QueryParameter String relatedPerfReport) {
            if (StringUtils.isEmpty(relatedPerfReport)) {
                return FormValidation.error("This field must not be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestCase(@QueryParameter String testCase) {
            if (StringUtils.isEmpty(testCase)) {
                return FormValidation.error("This field must not be empty");
            }
            return FormValidation.ok();
        }
    }

    /**
     * User defined absolute value which must not be exceeded
     */
    private long value = 0;

    @DataBoundConstructor
    public AbsoluteConstraint(Metric meteredValue, Operator operator, String relatedPerfReport, Escalation escalationLevel, boolean success, TestCaseBlock testCaseBlock, long value) {
        super(meteredValue, operator, relatedPerfReport, escalationLevel, success, testCaseBlock);
        this.value = value;

    }

    /**
     * Cloning of a AbsoluteConstraint Note that this is not from the Interface Clonable
     *
     * @return clone of this object
     */
    public AbsoluteConstraint clone() {
        AbsoluteConstraint clone = new AbsoluteConstraint(this.getMeteredValue(), this.getOperator(), this.getRelatedPerfReport(), this.getEscalationLevel(), this.getSuccess(), new TestCaseBlock(this
                .getTestCaseBlock().getTestCase()), this.getValue());
        return clone;
    }

    @Override
    public ConstraintEvaluation evaluate(List<? extends Run<?, ?>> builds) throws InvocationTargetException, AbortException {
        if (builds.isEmpty()) {
            throw new AbortException("Performance: No builds found to evaluate!");
        }
        checkForDefectiveParams(builds);
        PerformanceReport pr = builds.get(0).getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceReport(getRelatedPerfReport());
        // calculate the
        double newValue = 0;
        if (!isSpecifiedTestCase()) {
            newValue = checkMetredValueforPerfReport(getMeteredValue(), pr);
        } else {
            List<UriReport> uriList = pr.getUriListOrdered();
            for (UriReport ur : uriList) {
                if (getTestCaseBlock().getTestCase().equals(ur.getUri())) {
                    newValue = checkMetredValueforUriReport(getMeteredValue(), ur);
                    break;
                }
            }
        }
        return check(newValue);
    }

    /**
     * Compares the values and sets the success and a result message of a constraint.
     *
     * @param newValue measured value of a specified metric of the newly created test
     * @return evaluated constraint
     */
    private ConstraintEvaluation check(double newValue) {
        switch (getOperator()) {
            case NOT_LESS:
                if (newValue >= getValue()) {
                    setSuccess(true);
                } else {
                    setSuccess(false);
                }
                break;
            case NOT_GREATER:
                if (newValue <= getValue()) {
                    setSuccess(true);
                } else {
                    setSuccess(false);
                }
                break;
            case NOT_EQUAL:
                if (newValue != getValue()) {
                    setSuccess(true);
                } else {
                    setSuccess(false);
                }
                break;
        }

        ConstraintEvaluation evaluation = new ConstraintEvaluation(this, getValue(), newValue);

        String measuredLevel = isSpecifiedTestCase() ? getTestCaseBlock().getTestCase() : "all test cases";
        if (getSuccess()) {
            setResultMessage("Absolute constraint successful! - Report: " + getRelatedPerfReport() + " \n" + "The constraint says: " + getMeteredValue() + " of " + measuredLevel + " must "
                    + getOperator().text + " " + getValue() + "\n" + "Measured value for " + getMeteredValue() + ": " + newValue + "\n" + "Escalation Level: " + getEscalationLevel());
        } else {
            setResultMessage("Absolute constraint failed! - Report: " + getRelatedPerfReport() + " \n" + "The constraint says: " + getMeteredValue() + " of " + measuredLevel + " must "
                    + getOperator().text + " " + getValue() + "\n" + "Measured value for " + getMeteredValue() + ": " + newValue + "\n" + "Escalation Level: " + getEscalationLevel());
        }
        return evaluation;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
