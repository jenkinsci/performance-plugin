package hudson.plugins.jmeter;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * Entry point of a plugin.
 * 
 * <p>
 * There must be one {@link Plugin} class in each plugin. See javadoc of
 * {@link Plugin} for more about what can be done on this class.
 * 
 * @author Denis Vergnes
 */
public class PluginImpl extends Plugin {
	@Override
	public void start() throws Exception {
		// plugins normally extend Hudson by providing custom implementations
		// of 'extension points'. In this example, we'll add one builder.
		BuildStep.PUBLISHERS.addRecorder(JMeterPublisher.DESCRIPTOR);
	}
}
