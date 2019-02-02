package hudson.plugins.performance.constraints.blocks;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Holds the testCase information for constraints.
 *
 * @author Rene Kugel
 */
public class TestCaseBlock extends AbstractDescribableImpl<TestCaseBlock> {
    private String testCase;

    @Symbol("testCase")
    @Extension
    public static class DescriptorImpl extends Descriptor<TestCaseBlock> {
        @Override
        public String getDisplayName() {
            return "TestCaseBlock";
        }
    }

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