package hudson.plugins.performance;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Kohsuke Kawaguchi
 */
public class PerformancePublisherTest extends HudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        PerformancePublisher before = new PerformancePublisher(10, 20,
                asList(new JMeterParser("**/*.jtl")));

        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        submit(createWebClient().getPage(p,"configure").getFormByName("config"));

        PerformancePublisher after = p.getPublishersList().get(PerformancePublisher.class);
        assertEqualBeans(before,after,"errorFailedThreshold,errorUnstableThreshold");
        assertEquals(before.getParsers().size(), after.getParsers().size());
        assertEqualBeans(before.getParsers().get(0), after.getParsers().get(0), "glob");
        assertEquals(before.getParsers().get(0).getClass(), after.getParsers().get(0).getClass());
    }

    public void testBuild() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(new PerformancePublisher(0,0,
                asList(new JMeterParser("**/*.jtl"))));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);
        assertNotNull(a);

        // poke a few random pages to verify rendering
        WebClient wc = createWebClient();
        wc.getPage(b,"performance");
        wc.getPage(b,"performance/uriReport/test.jtl;Login.endperformanceparameter/");
    }
}
