package hudson.plugins.performance;

import hudson.Launcher;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

import static java.util.Arrays.asList;

/**
 * @author Kohsuke Kawaguchi
 */
  public class PerformancePublisherTest extends HudsonTestCase{
    public void testConfigRoundtrip() throws Exception {
        PerformancePublisher before = new PerformancePublisher(10, 20, "",0,0,0,0,0,false,"",false,false,
                asList(new JMeterParser("**/*.jtl")),false);

        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);

        try{
            submit(createWebClient().getPage(p, "configure")
                    .getFormByName("config"));
            PerformancePublisher after = p.getPublishersList().get(
                    PerformancePublisher.class);
            assertEqualBeans(before, after,
                    "errorFailedThreshold,errorUnstableThreshold");
            assertEquals(before.getParsers().size(), after.getParsers().size());
            assertEqualBeans(before.getParsers().get(0), after.getParsers().get(0),
                    "glob");
            assertEquals(before.getParsers().get(0).getClass(), after.getParsers()
                    .get(0).getClass());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void testBuild() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
		p.getBuildersList().add(new TestBuilder() {
			@Override
			public boolean perform(AbstractBuild<?, ?> build,
					Launcher launcher, BuildListener listener)
					throws InterruptedException, IOException {
				build.getWorkspace().child("test.jtl").copyFrom(
						getClass().getResource("/JMeterResults.jtl"));
				return true;
			}
		});
        p.getPublishersList().add(
                new PerformancePublisher(0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, asList(new JMeterParser(
                        "**/*.jtl")),false));

		FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
		PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try{
          //assertNotNull(a);
          // poke a few random pages to verify rendering
          WebClient wc = createWebClient();
		  wc.getPage(b, "performance");
		  wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        }
        catch(Exception e){
            e.printStackTrace();
        }
	}

    public void testBuildUnstableResponseThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher(0, 0, "test.jtl:100", 0, 0, 0, 0, 0, false, "", false, false, asList(new JMeterParser(
                        "**/*.jtl")),false));

        FreeStyleBuild b = assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try{
          //assertNotNull(a);

          // poke a few random pages to verify rendering
          WebClient wc = createWebClient();
          wc.getPage(b, "performance");
          wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void testBuildStableResponseThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher(0, 0, "test.jtl:5000", 0, 0, 0, 0, 0, false, "", false, false, asList(new JMeterParser(
                        "**/*.jtl")),false));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try{
            //assertNotNull(a);

            // poke a few random pages to verify rendering
            WebClient wc = createWebClient();
            wc.getPage(b, "performance");
            wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Bug(22011)
    public void testBuildUnstableAverageResponseTimeRelativeThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        p.getPublishersList().add(
                new PerformancePublisher(0, 0, null, 100.0d, 0, 50.0d, 0, 0, false, "ART", true, true, asList(new JUnitParser(
                        "**/*.xml")), false));
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test1.xml").copyFrom(
                        getClass().getResource("/TEST-JUnitResults-relative-thrashould.xml"));
                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());


        // second build with high time
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test2.xml").copyFrom(
                        getClass().getResource("/TEST-JUnitResults-relative-thrashould-2.xml"));
                return true;
            }
        });

        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());

    }
}
