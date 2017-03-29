package hudson.plugins.performance.descriptors;

import hudson.plugins.performance.constraints.AbsoluteConstraint;
import hudson.plugins.performance.constraints.RelativeConstraint;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class ConstraintDescriptorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void shutdown() throws Exception {
        j.after();
    }

    @Test
    public void name() throws Exception {
        ConstraintDescriptor descriptor = ConstraintDescriptor.getById(AbsoluteConstraint.DescriptorImpl.class.getName());
        assertNotNull(descriptor);
        assertTrue(descriptor instanceof AbsoluteConstraint.DescriptorImpl);

        ConstraintDescriptor descriptor2 = ConstraintDescriptor.getById(RelativeConstraint.DescriptorImpl.class.getName());
        assertNotNull(descriptor2);
        assertTrue(descriptor2 instanceof RelativeConstraint.DescriptorImpl);

        assertNull(ConstraintDescriptor.getById("null"));
    }
}