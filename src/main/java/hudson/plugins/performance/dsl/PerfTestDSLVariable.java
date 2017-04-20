package hudson.plugins.performance.dsl;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.io.Reader;

@Extension
public class PerfTestDSLVariable extends GlobalVariable {

    @Nonnull
    @Override
    public String getName() {
        return "bzt";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript script) throws Exception {
        Binding binding = script.getBinding();

        CpsThread c = CpsThread.current();
        if (c == null)
            throw new IllegalStateException("Expected to be called from CpsThread");

        ClassLoader cl = getClass().getClassLoader();

        String scriptPath = "hudson/plugins/performance/dsl/" + getName() + ".groovy";
        Reader r = new InputStreamReader(cl.getResourceAsStream(scriptPath), "UTF-8");

        GroovyCodeSource gsc = new GroovyCodeSource(r, getName() + ".groovy", cl.getResource(scriptPath).getFile());
        gsc.setCachable(true);


        Object pipelineDSL = c.getExecution()
                .getShell()
                .getClassLoader()
                .parseClass(gsc)
                .newInstance();
        binding.setVariable(getName(), pipelineDSL);
        r.close();

        return pipelineDSL;
    }
}
