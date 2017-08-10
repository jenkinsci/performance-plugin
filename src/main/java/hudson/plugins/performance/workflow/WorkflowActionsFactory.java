package hudson.plugins.performance.workflow;


import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class WorkflowActionsFactory extends TransientActionFactory<Job> {

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        if (job.getClass().getCanonicalName().startsWith("org.jenkinsci.plugins.workflow")) {
            final Run<?, ?> r = job.getLastSuccessfulBuild();
            if (r != null && !r.getActions(PerformanceBuildAction.class).isEmpty()) {
                return Collections.singletonList(new PerformanceProjectAction(job));
            }
        }
        return Collections.emptyList();
    }
}
