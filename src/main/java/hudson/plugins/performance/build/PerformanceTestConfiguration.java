package hudson.plugins.performance.build;

import hudson.Extension;
import hudson.plugins.performance.Messages;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;

public class PerformanceTestConfiguration extends ToolInstallation {

    @Extension
    public static final class Descriptor extends ToolDescriptor<PerformanceTestConfiguration> {

//        public Descriptor() {
//            setInstallations();
//            load();
//        }

        @Override
        public String getDisplayName() {
            return Messages.PerformanceTest_Config();
        }

        @Override
        public PerformanceTestConfiguration newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
            return (PerformanceTestConfiguration) super.newInstance(req, formData.getJSONObject("gravenInstallation"));
        }
    }

    @DataBoundConstructor
    public PerformanceTestConfiguration(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }


}
