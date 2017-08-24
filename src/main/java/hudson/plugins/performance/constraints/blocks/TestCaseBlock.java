package hudson.plugins.performance.constraints.blocks;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * Holds the testCase information for constraints.
 *
 * @author Rene Kugel
 */
public class TestCaseBlock extends AbstractDescribableImpl<TestCaseBlock> {

    @Symbol("testCase")
    @Extension
    public static class DescriptorImpl extends Descriptor<TestCaseBlock> {
        @Override
        public String getDisplayName() {
            return "TestCaseBlock";
        }
    }

    private String testCase;

    @DataBoundConstructor
    public TestCaseBlock(String testCase) {
        this.testCase = testCase;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

}