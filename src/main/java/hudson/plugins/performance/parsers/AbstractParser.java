package hudson.plugins.performance.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;

/**
 * An abstraction for parsing data to PerformanceReport instances. This class
 * provides functionality that optimizes the parsing process, such as caching as
 * well as saving/loaded parsed data in serialized form to/from disc.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public abstract class AbstractParser extends PerformanceReportParser {
    private static final Logger LOGGER = Logger.getLogger(JMeterParser.class.getName());

    /**
     * A suffix to be used for files in which a serialized PerformanceReport instance is stored.
     */
    private static final String SERIALIZED_DATA_FILE_SUFFIX = ".serialized";

    /**
     * A cache that contains serialized PerformanceReport instances. This cache intends to limit disc IO.
     */
    private static final Cache<String, PerformanceReport> CACHE = CacheBuilder.newBuilder().maximumSize(1000).softValues().build();

    protected boolean isNumberDateFormat = false;
    protected SimpleDateFormat format;

    protected static final String[] DATE_FORMATS = new String[]{
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
            // Attempt to load previously serialized instances from file or cache.
            final PerformanceReport deserializedReport = loadSerializedReport(reportFile);
            if (deserializedReport != null) {
                result.add(deserializedReport);
                continue;
            }

            // When serialized data cannot be used, the original JMeter files are to be processed.
            try {
                listener.getLogger().println("Performance: Parsing report file '" + reportFile + "' with filterRegex '"+filterRegex+"'.");
                final PerformanceReport report = parse(reportFile);
                result.add(report);
                passBaselineBuild(report);
                saveSerializedReport(reportFile, report);
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

    /**
     * Returns a PerformanceReport instance for the provided report file, based on
     * previously serialized data.
     * <p>
     * This method first attempts to load data from an internal cache. If the data
     * is not in cache, data is obtained from a file on disc.
     * <p>
     * When no PerformanceReport instance has previously been serialized (or when
     * such data cannot be read, for instance because of class file changes), this
     * method returns null.
     *
     * @param reportFile Report for which to return data. Cannot be null.
     * @return deserialized data, possibly null.
     */
    protected static PerformanceReport loadSerializedReport(File reportFile) {
        if (reportFile == null) {
            throw new IllegalArgumentException("Argument 'reportFile' cannot be null.");
        }
        final String serialized = reportFile.getPath() + SERIALIZED_DATA_FILE_SUFFIX;
        File file = new File(serialized);
        synchronized (CACHE) {
            PerformanceReport report = CACHE.getIfPresent(serialized);
            if (report == null && file.exists() && file.canRead()) {
                try (FileInputStream fis = new FileInputStream(serialized);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        ObjectInputStream in = new ObjectInputStreamWithClassMapping(bis)) {
                    report = (PerformanceReport) in.readObject();
                    CACHE.put(serialized, report);
                    return report;
                } catch (FileNotFoundException ex) {
                    // That's OK
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Reading serialized PerformanceReport instance from file '" + serialized + "' failed.", ex);
                }
            }
            return report;
        }
    }

    /**
     * Saves a PerformanceReport instance as serialized data into a file on disc.
     *
     * @param reportFile The file from which the original data is obtained (<em>not</em>
     *                   the file into which serialized data is to be saved!) Cannot be
     *                   null.
     * @param report     The instance to serialize. Cannot be null.
     */
    protected static void saveSerializedReport(File reportFile, PerformanceReport report) {
        if (reportFile == null) {
            throw new IllegalArgumentException("Argument 'reportFile' cannot be null.");
        }
        if (report == null) {
            throw new IllegalArgumentException("Argument 'report' cannot be null.");
        }
        final String serialized = reportFile.getPath() + SERIALIZED_DATA_FILE_SUFFIX;

        synchronized (CACHE) {
            CACHE.put(serialized, report);
        }
        try (FileOutputStream fos = new FileOutputStream(serialized);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(report);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Saving serialized PerformanceReport instance to file '" + serialized + "' failed.", ex);
        }
    }

    public static class ObjectInputStreamWithClassMapping extends ObjectInputStream {
        protected Hashtable<String, Class> classMapping = new Hashtable<>(); 

        public ObjectInputStreamWithClassMapping(InputStream in) throws IOException {
            super(in);
            classMapping.put("hudson.plugins.performance.PerformanceReport", PerformanceReport.class);
            classMapping.put("hudson.plugins.performance.UriReport", UriReport.class);
            classMapping.put("hudson.plugins.performance.UriReport$Sample", UriReport.Sample.class);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
                ClassNotFoundException {
            return (classMapping.containsKey(desc.getName())) ?
                    classMapping.get(desc.getName()) :
                    super.resolveClass(desc);
        }
    }


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
                    new Date(Long.valueOf(timestamp)) :
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
