package hudson.plugins.performance.workflow;


import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Extension
public class WorkflowActionsFactory extends TransientActionFactory<Job> {

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        final List<Action> actions = new LinkedList<>();
        if (job.getClass().getCanonicalName().startsWith("org.jenkinsci.plugins.workflow")) {
            final Run<?,?> r = job.getLastSuccessfulBuild();
            if (r != null) {
                actions.add(new PerformanceProjectAction(job));
            }
        }
        return actions;
    }
}
