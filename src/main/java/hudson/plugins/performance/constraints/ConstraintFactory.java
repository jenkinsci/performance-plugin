package hudson.plugins.performance.constraints;

import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Evaluates the entries in the testCase field if test cases are specified and create clones of the
 * constraint for every given test case There are two possibilities: 1. In the testCase field a
 * comma seperated list of test cases is given 2. A '*' is given
 *
 * @author Rene Kugel
 */
public class ConstraintFactory {

    /**
     * Creates clones of a constraint if there is more than one test case specified in the UI
     *
     * @param build       all builds that are saved in Jenkins
     * @param constraints all constraints defined in the UI
     * @return list of all constraint clones that will get evaluated
     */
    public List<? extends AbstractConstraint> createConstraintClones(Run<?, ?> build, List<? extends AbstractConstraint> constraints) {
        /*
		 * Checking the test case field and handle comma separated lists and wildcard
		 */
        List<AbstractConstraint> createdConstraints = new ArrayList<AbstractConstraint>();
        for (AbstractConstraint c : constraints) {
            List<String> testCases = new ArrayList<String>();
            if (c.isSpecifiedTestCase()) {
                String testCase = c.getTestCaseBlock().getTestCase();
                if ("*".equals(testCase)) {
                    PerformanceReport pr = build.getAction(PerformanceBuildAction.class).getPerformanceReportMap().getPerformanceReport(c.getRelatedPerfReport());
                    for (UriReport ur : pr.getUriListOrdered()) {
                        testCases.add(ur.getUri());
                    }
                } else {
                    String[] tmpTestCases = testCase.split(",");
                    String[] trimmedTestCases = new String[tmpTestCases.length];
                    for (int i = 0; i < tmpTestCases.length; i++) {
                        trimmedTestCases[i] = tmpTestCases[i].trim();
                    }
                    testCases = Arrays.asList(trimmedTestCases);
                }
				/*
				 * Creating clones based on the test cases
				 */
                for (String s : testCases) {
                    AbstractConstraint constraint = (AbstractConstraint) c.clone();
                    constraint.getTestCaseBlock().setTestCase(s);
                    createdConstraints.add(constraint);
                }
            } else {
                createdConstraints.add(c);
            }
        }
        return createdConstraints;
    }
}