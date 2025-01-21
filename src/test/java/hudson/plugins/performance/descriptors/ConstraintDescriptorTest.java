package hudson.plugins.performance.descriptors;

import hudson.plugins.performance.constraints.AbsoluteConstraint;
import hudson.plugins.performance.constraints.RelativeConstraint;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class ConstraintDescriptorTest {

    @Test
    void name(JenkinsRule j) throws Exception {
        ConstraintDescriptor descriptor = ConstraintDescriptor.getById(AbsoluteConstraint.DescriptorImpl.class.getName());
        assertNotNull(descriptor);
        assertInstanceOf(AbsoluteConstraint.DescriptorImpl.class, descriptor);

        ConstraintDescriptor descriptor2 = ConstraintDescriptor.getById(RelativeConstraint.DescriptorImpl.class.getName());
        assertNotNull(descriptor2);
        assertInstanceOf(RelativeConstraint.DescriptorImpl.class, descriptor2);

        assertNull(ConstraintDescriptor.getById("null"));
    }
}