package hudson.plugins.performance.parsers;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

public class ParserFactory {

    protected static final Map<String, String> defaultGlobPatterns = new Hashtable<String, String>();

    static {
        defaultGlobPatterns.put("parrot-server-stats.log", IagoParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.csv", JMeterCsvParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.jtl", JMeterParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.log", JmeterSummarizerParser.class.getSimpleName());
        defaultGlobPatterns.put("**/TEST-*.xml", JUnitParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.xml", TaurusParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.wrk", WrkSummarizerParser.class.getSimpleName());
    }

    public static PerformanceReportParser getParser(Run<?, ?> build, FilePath workspace, PrintStream logger, String glob, EnvVars env) throws IOException, InterruptedException {
        if (defaultGlobPatterns.containsKey(glob)) {
            return getParser(defaultGlobPatterns.get(glob), glob);
        }

        String expandGlob = env.expand(glob);
        try {
            FilePath[] pathList = workspace.list(expandGlob);
            for (FilePath src : pathList) {
                // copy file (it can be on remote slave) to "../build/../temp/" folder
                final File localReport = new File(build.getRootDir(), "/temp/" + src.getName());
                if (src.isDirectory()) {
                    logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
                    continue;
                }
                src.copyTo(new FilePath(localReport));
                return getParser(ParserDetector.detect(localReport.getPath()), glob);
            }
        } catch (IOException ignored) {
        }

        return getParser(ParserDetector.detect(workspace.getRemote() + '/' + glob), workspace.getRemote() + '/' + glob);
    }

    private static PerformanceReportParser getParser(String parserName, String glob) {
        if (parserName.equals(JMeterParser.class.getSimpleName())) {
            return new JMeterParser(glob);
        } else if (parserName.equals(JMeterCsvParser.class.getSimpleName())) {
            return new JMeterCsvParser(glob);
        } else if (parserName.equals(JUnitParser.class.getSimpleName())) {
            return new JUnitParser(glob);
        } else if (parserName.equals(TaurusParser.class.getSimpleName())) {
            return new TaurusParser(glob);
        } else if (parserName.equals(WrkSummarizerParser.class.getSimpleName())) {
            return new WrkSummarizerParser(glob);
        } else if (parserName.equals(JmeterSummarizerParser.class.getSimpleName())) {
            return new JmeterSummarizerParser(glob);
        } else if (parserName.equals(IagoParser.class.getSimpleName())) {
            return new IagoParser(glob);
        } else {
            throw new IllegalArgumentException("Unknown parser type: " + parserName);
        }
    }

}
