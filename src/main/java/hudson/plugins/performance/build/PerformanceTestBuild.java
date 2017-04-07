package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.Messages;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * "Build step" for running performance test
 */
public class PerformanceTestBuild extends Builder implements SimpleBuildStep {


    @Symbol("performanceTest")
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.PerformanceTest_Name();
        }
    }


    private String testConfigurationFiles;
    private String testOptions;

    @DataBoundConstructor
    public PerformanceTestBuild(String testConfigurationFiles, String testOptions) {
        this.testConfigurationFiles = testConfigurationFiles;
        this.testOptions = testOptions;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // TODO:

    }

    public String getTestConfigurationFiles() {
        return testConfigurationFiles;
    }

    public String getTestOptions() {
        return testOptions;
    }

    @DataBoundSetter
    public void setTestConfigurationFiles(String testConfigurationFiles) {
        this.testConfigurationFiles = testConfigurationFiles;
    }

    @DataBoundSetter
    public void setTestOptions(String testOptions) {
        this.testOptions = testOptions;
    }
}
