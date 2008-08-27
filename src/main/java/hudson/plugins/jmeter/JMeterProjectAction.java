package hudson.plugins.jmeter;

import hudson.model.Action;

public class JMeterProjectAction implements Action {

	public String getDisplayName() {
		return "JMeter trend";
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public String getUrlName() {
		return "jmeter";
	}

}
