package hudson.plugins.performance.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;

public class ParserFactory {
    private static final Logger LOGGER = Logger.getLogger(ParserFactory.class.getName());

    protected static final Map<String, String> defaultGlobPatterns = Collections.synchronizedMap(new HashMap<String, String>());
    private static final String TEMP_FOLDER = "/temp/";
    static {
        defaultGlobPatterns.put("parrot-server-stats.log", IagoParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.csv", JMeterCsvParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.jtl", JMeterParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.log", JmeterSummarizerParser.class.getSimpleName());
        defaultGlobPatterns.put("**/TEST-*.xml", JUnitParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.xml", TaurusParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.wrk", WrkSummarizerParser.class.getSimpleName());
        defaultGlobPatterns.put("**/*.mdb", LoadRunnerParser.class.getSimpleName());
    }

    public static List<PerformanceReportParser> getParser(Run<?, ?> build, FilePath workspace, PrintStream logger, String glob, 
            EnvVars env, String percentiles, String filterRegex) throws IOException, InterruptedException {
        String expandGlob = env.expand(glob);
        if (defaultGlobPatterns.containsKey(expandGlob)) {
            return Collections.singletonList(getParser(defaultGlobPatterns.get(expandGlob), expandGlob, percentiles, filterRegex));
        }

        File path = new File(expandGlob);
        return path.isAbsolute() ? getParserWithAbsolutePath(build, workspace, logger, path, percentiles, filterRegex) : 
            getParserWithRelativePath(build, workspace, logger, expandGlob, percentiles, filterRegex);
    }

    private static List<PerformanceReportParser> getParserWithRelativePath(Run<?, ?> build, FilePath workspace, PrintStream logger, String glob, String percentiles, String filterRegex) throws IOException, InterruptedException {
        List<PerformanceReportParser> result = getParserUsingAntPatternRelativePath(build, workspace, logger, glob, percentiles, filterRegex);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        File report = new File(workspace.getRemote() + '/' + glob);
        if (!report.exists()) {
            // if report on remote slave
            FilePath localReport = new FilePath(new File(build.getRootDir(), TEMP_FOLDER + glob));
            localReport.copyFrom(new FilePath(workspace, glob));
            return Collections.singletonList(getParser(ParserDetector.detect(localReport.getRemote()), glob, percentiles, filterRegex));
        }

        return Collections.singletonList(getParser(ParserDetector.detect(workspace.getRemote() + '/' + glob), workspace.getRemote() + '/' + glob, percentiles, filterRegex));
    }

    private static List<PerformanceReportParser> getParserUsingAntPatternRelativePath(Run<?, ?> build, FilePath workspace, PrintStream logger, 
            String glob, String percentiles, String filterRegex) throws InterruptedException {
        try {
            FilePath[] pathList = workspace.list(glob);
            List<PerformanceReportParser> result = new ArrayList<>();
            for (FilePath src : pathList) {
                // copy file (it can be on remote slave) to "../build/../temp/" folder
                final File localReport = new File(build.getRootDir(), TEMP_FOLDER + src.getName());
                if (src.isDirectory()) {
                    logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
                    continue;
                }
                src.copyTo(new FilePath(localReport));
                result.add(getParser(ParserDetector.detect(localReport.getPath()), glob, percentiles, filterRegex));
            }
            return result;
        } catch (IOException ignored) {
            LOGGER.log(Level.FINE, "Cannot find report file using Ant pattern", ignored);
        }
        return Collections.emptyList();
    }


    private static List<PerformanceReportParser> getParserWithAbsolutePath(Run<?, ?> build, FilePath workspace, PrintStream logger, File path, String percentiles, String filterRegex) throws IOException, InterruptedException {
        List<PerformanceReportParser> result = getParserUsingAntPatternAbsolutePath(build, workspace, logger, path, percentiles, filterRegex);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        if (!path.exists()) {
            // if report on remote slave
            FilePath localReport = new FilePath(new File(build.getRootDir(), TEMP_FOLDER + path.getName()));
            localReport.copyFrom(new FilePath(workspace.getChannel(), path.getAbsolutePath()));
            return Collections.singletonList(getParser(ParserDetector.detect(localReport.getRemote()), path.getName(), percentiles, filterRegex));
        }


        return Collections.singletonList(getParser(ParserDetector.detect(path.getAbsolutePath()), path.getAbsolutePath(), percentiles, filterRegex));
    }

    private static List<PerformanceReportParser> getParserUsingAntPatternAbsolutePath(Run<?, ?> build, FilePath wsp, PrintStream logger, File path, String percentiles, String filterRegex) throws InterruptedException {
        try {
            File parent = path.getParentFile();
            FilePath workspace = new FilePath(wsp.getChannel(), parent.getAbsolutePath());
            while (!workspace.exists()) {
                parent = parent.getParentFile();
                if (parent != null) {
                    workspace = new FilePath(wsp.getChannel(), parent.getAbsolutePath());
                } else {
                    return Collections.emptyList();
                }
            }

            String glob = path.getAbsolutePath().substring(parent.getAbsolutePath().length() + 1);
            FilePath[] pathList = workspace.list(glob);
            List<PerformanceReportParser> parsers = new ArrayList<>();
            for (FilePath src : pathList) {
                // copy file (it can be on remote slave) to "../build/../temp/" folder
                final File localReport = new File(build.getRootDir(), TEMP_FOLDER + src.getName());
                if (src.isDirectory()) {
                    logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
                    continue;
                }
                src.copyTo(new FilePath(localReport));

                parsers.add(getParser(ParserDetector.detect(localReport.getPath()), localReport.getPath(), percentiles, filterRegex));
            }
            return parsers;
        } catch (IOException ignored) {
            LOGGER.log(Level.FINE, "Cannot find report file using Ant pattern", ignored);
        }
        return Collections.emptyList();
    }


    private static PerformanceReportParser getParser(String parserName, String glob, String percentiles, String filterRegex) {
        if (parserName.equals(JMeterParser.class.getSimpleName())) {
            return new JMeterParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(JMeterCsvParser.class.getSimpleName())) {
            return new JMeterCsvParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(JUnitParser.class.getSimpleName())) {
            return new JUnitParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(TaurusParser.class.getSimpleName())) {
            return new TaurusParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(WrkSummarizerParser.class.getSimpleName())) {
            return new WrkSummarizerParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(JmeterSummarizerParser.class.getSimpleName())) {
            return new JmeterSummarizerParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(IagoParser.class.getSimpleName())) {
            return new IagoParser(glob, percentiles, filterRegex);
        } else if (parserName.equals(LoadRunnerParser.class.getSimpleName())) {
            return new LoadRunnerParser(glob, percentiles, filterRegex);
        } else {
            throw new IllegalArgumentException("Unknown parser type: " + parserName);
        }
    }

}
