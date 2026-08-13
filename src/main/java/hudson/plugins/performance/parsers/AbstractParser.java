package hudson.plugins.performance.parsers;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.reports.PerformanceReport;

/**
 * An abstraction for parsing data to PerformanceReport instances. This class
 * provides functionality that optimizes the parsing process.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public abstract class AbstractParser extends PerformanceReportParser {

    protected boolean isNumberDateFormat = false;
    protected transient SimpleDateFormat format;

    static final String[] DATE_FORMATS = new String[]{
            "yyyy/MM/dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss,SSS", "yyyy/mm/dd HH:mm:ss"
    };

    protected String percentiles;

    protected String filterRegex;

    public AbstractParser(String glob, String percentiles, String filterRegex) {
        super(glob);
        this.percentiles = percentiles;
        this.filterRegex = filterRegex;
    }

    @Override
    public Collection<PerformanceReport> parse(Run<?, ?> build, Collection<File> reports, TaskListener listener) throws IOException {
        final List<PerformanceReport> result = new ArrayList<>();

        for (File reportFile : reports) {
            try {
                listener.getLogger().println("Performance: Parsing report file '" + reportFile + "' with filterRegex '"+filterRegex+"'.");
                final PerformanceReport report = parse(reportFile);
                result.add(report);
                passBaselineBuild(report);
            } catch (Throwable e) {
                listener.getLogger().println("Performance: Failed to parse file '" + reportFile + "': " + e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
        }
        return result;
    }

    private void passBaselineBuild(PerformanceReport report) {
        report.setBaselineBuild(baselineBuild);
    }

    /**
     * Performs the actual parsing of data. When the implementation throws any
     * exception, the input file is ignored. This does not abort parsing of
     * subsequent files.
     *
     * @param reportFile The source file (cannot be null).
     * @return The parsed data (never null).
     * @throws Throwable On any exception.
     */
    abstract PerformanceReport parse(File reportFile) throws Exception;

    public void clearDateFormat() {
        this.format = null;
        this.isNumberDateFormat = false;
    }

    public Date parseTimestamp(String timestamp) {
        if (this.format == null) {
            initDateFormat(timestamp);
        }

        try {
            return isNumberDateFormat ?
                    new Date(Long.parseLong(timestamp)) :
                    format.parse(timestamp);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse timestamp: " + timestamp +
                    ". Please, use one of supported formats: " + Arrays.toString(DATE_FORMATS), e);
        }
    }

    private void initDateFormat(String timestamp) {
        Date result = null;
        for (String format : DATE_FORMATS) {
            try {
                this.format = new SimpleDateFormat(format);
                result = this.format.parse(timestamp);
            } catch (ParseException ex) {
                // ok
                this.format = null;
            }

            if (result != null) {
                break;
            }
        }

        if (result == null) {
            try {
                Long.valueOf(timestamp);
                isNumberDateFormat = true;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Cannot parse timestamp: " + timestamp +
                        ". Please, use one of supported formats: " + Arrays.toString(DATE_FORMATS), ex);
            }
        }
    }

    protected PerformanceReport createPerformanceReport() {
        return new PerformanceReport(percentiles, filterRegex);
    }
}
