package hudson.plugins.performance.parsers;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParserFactory {
    private static final Logger LOGGER = Logger.getLogger(ParserFactory.class.getName());

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
        String expandGlob = env.expand(glob);
        if (defaultGlobPatterns.containsKey(expandGlob)) {
            return getParser(defaultGlobPatterns.get(expandGlob), expandGlob);
        }

        File path = new File(expandGlob);
        return path.isAbsolute() ? getParserWithAbsolutePath(build, workspace, logger, path) : getParserWithRelativePath(build, workspace, logger, expandGlob);
    }

    private static PerformanceReportParser getParserWithRelativePath(Run<?, ?> build, FilePath workspace, PrintStream logger, String glob) throws IOException, InterruptedException {
        PerformanceReportParser result = getParserUsingAntPatternRelativePath(build, workspace, logger, glob);
        if (result != null) {
            return result;
        }

        File report = new File(workspace.getRemote() + '/' + glob);
        if (!report.exists()) {
            // if report on remote slave
            FilePath localReport = new FilePath(new File(build.getRootDir(), "/temp/" + glob));
            localReport.copyFrom(new FilePath(workspace, glob));
            return getParser(ParserDetector.detect(localReport.getRemote()), glob);
        }

        return getParser(ParserDetector.detect(workspace.getRemote() + '/' + glob), workspace.getRemote() + '/' + glob);
    }

    private static PerformanceReportParser getParserUsingAntPatternRelativePath(Run<?, ?> build, FilePath workspace, PrintStream logger, String glob) throws InterruptedException {
        try {
            FilePath[] pathList = workspace.list(glob);
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
            LOGGER.log(Level.FINE, "Cannot find report file using Ant pattern", ignored);
        }
        return null;
    }


    private static PerformanceReportParser getParserWithAbsolutePath(Run<?, ?> build, FilePath workspace, PrintStream logger, File path) throws IOException, InterruptedException {
        PerformanceReportParser result = getParserUsingAntPatternAbsolutePath(build, workspace, logger, path);
        if (result != null) {
            return result;
        }

        if (!path.exists()) {
            // if report on remote slave
            FilePath localReport = new FilePath(new File(build.getRootDir(), "/temp/" + path.getName()));
            localReport.copyFrom(new FilePath(workspace.getChannel(), path.getAbsolutePath()));
            return getParser(ParserDetector.detect(localReport.getRemote()), path.getName());
        }

        return getParser(ParserDetector.detect(path.getAbsolutePath()), path.getAbsolutePath());
    }

    private static PerformanceReportParser getParserUsingAntPatternAbsolutePath(Run<?, ?> build, FilePath wsp, PrintStream logger, File path) throws InterruptedException {
        try {
            File parent = path.getParentFile();
            FilePath workspace = new FilePath(wsp.getChannel(), parent.getAbsolutePath());
            while (!workspace.exists()) {
                parent = parent.getParentFile();
                if (parent != null) {
                    workspace = new FilePath(wsp.getChannel(), parent.getAbsolutePath());
                } else {
                    return null;
                }
            }

            String glob = path.getAbsolutePath().substring(parent.getAbsolutePath().length() + 1);
            FilePath[] pathList = workspace.list(glob);
            for (FilePath src : pathList) {
                // copy file (it can be on remote slave) to "../build/../temp/" folder
                final File localReport = new File(build.getRootDir(), "/temp/" + src.getName());
                if (src.isDirectory()) {
                    logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
                    continue;
                }
                src.copyTo(new FilePath(localReport));
                return getParser(ParserDetector.detect(localReport.getPath()), localReport.getPath());
            }
        } catch (IOException ignored) {
            LOGGER.log(Level.FINE, "Cannot find report file using Ant pattern", ignored);
        }
        return null;
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
