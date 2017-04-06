package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder {
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Run Performance test";
        }
    }


    private final String testConfigurationFiles;
    private final String testOptions;

    @DataBoundConstructor
    public PerformanceTestBuild(String testConfigurationFiles, String testOptions) {
        this.testConfigurationFiles = testConfigurationFiles;
        this.testOptions = testOptions;
    }

}
