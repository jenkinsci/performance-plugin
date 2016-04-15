package hudson.plugins.performance.constraints;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;

public abstract class ConstraintDescriptor extends Descriptor<AbstractConstraint> {

	public final String getId() {
		return getClass().getName();
	}

	public static DescriptorExtensionList<AbstractConstraint, ConstraintDescriptor> all() {
		return Hudson.getInstance().<AbstractConstraint, ConstraintDescriptor> getDescriptorList(AbstractConstraint.class);
	}

	public static ConstraintDescriptor getById(String id) {
		for (ConstraintDescriptor d : all())
			if (d.getId().equals(id))
				return d;
		return null;
	}
}
