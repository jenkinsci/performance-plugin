package hudson.plugins.performance.details;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import hudson.plugins.performance.Messages;
import hudson.plugins.performance.cookie.CookieHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Configures the trend graph of this plug-in.
 */
public class GraphConfigurationDetail implements ModelObject {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GraphConfigurationDetail.class.getName());

    public static final String LEGACY_SEPARATOR = ";";
    public static final String SEPARATOR = ":";
    /**
     * The number of builds to consider.
     */
    private int buildCount;
    /**
     * The first days to consider.
     */
    private String firstDayCount;
    /**
     * The last days to consider.
     */
    private String lastDayCount;
    /**
     * The type of config to use.
     */
    private String configType;
    /**
     * The build step to consider.
     */
    private int buildStep;


    public static final int DEFAULT_COUNT = 0;

    public static final int DEFAULT_STEP = 1;

    public static final String DEFAULT_DATE = "dd/MM/yyyy";

    public static final String NONE_CONFIG = "NONE";

    public static final String BUILD_CONFIG = "BUILD";

    public static final String DATE_CONFIG = "DATE";

    public static final String BUILDNTH_CONFIG = "BUILDNTH";

    public boolean isNone() {
        return configType.compareToIgnoreCase(GraphConfigurationDetail.NONE_CONFIG) == 0;
    }

    public boolean isBuildCount() {
        return configType.compareToIgnoreCase(GraphConfigurationDetail.BUILD_CONFIG) == 0;
    }

    public boolean isBuildNth() {
        return configType.compareToIgnoreCase(GraphConfigurationDetail.BUILDNTH_CONFIG) == 0;
    }

    public boolean isDate() {
        return configType.compareToIgnoreCase(GraphConfigurationDetail.DATE_CONFIG) == 0;
    }

    public boolean isDefaultDates() {
        return DEFAULT_DATE.compareTo(firstDayCount) == 0
                && DEFAULT_DATE.compareTo(lastDayCount) == 0;
    }

    static DateFormat format = new SimpleDateFormat(DEFAULT_DATE);

    public GraphConfigurationDetail(final AbstractProject<?, ?> project,
                                    final String pluginName, final StaplerRequest request) {

        String value = createCookieHandler(pluginName).getValue(
                request.getCookies());
        List<Integer> initializationListResult = initializeFrom(value);
        if (!initializationListResult.isEmpty()) {
            File defaultsFile = createDefaultsFile(project, pluginName);
            if (defaultsFile.exists()) {
                String defaultValue = readFromDefaultsFile(defaultsFile);
                initializationListResult = initializeFrom(defaultValue);
                if (!initializationListResult.isEmpty()) {
                    reset(initializationListResult);
                }
            } else {
                reset(initializationListResult);
            }
        }
    }

    /**
     * Saves the configured values. Subclasses need to implement the actual
     * persistence.
     *
     * @param request  Stapler request
     * @param response Stapler response
     */
    public void doSave(final StaplerRequest request,
                       final StaplerResponse response) {
        try {
            JSONObject formData = request.getSubmittedForm();
            String buildCountString = formData.getString("buildCountString");
            int buildCount = 0;
            if (StringUtils.isNotBlank(buildCountString)) {
                buildCount = formData.getInt("buildCountString");
            }
            String firstDayCountString = formData.getString("firstDayCountString");
            String firstDayCount = DEFAULT_DATE;
            if (StringUtils.isNotBlank(firstDayCountString)) {
                firstDayCount = formData.getString("firstDayCountString");
            }
            String lastDayCountString = formData.getString("lastDayCountString");
            String lastDayCount = DEFAULT_DATE;
            if (StringUtils.isNotBlank(lastDayCountString)) {
                lastDayCount = formData.getString("lastDayCountString");
            }
            String radioConfigType = formData.getString("radioConfigType");
            String configType = NONE_CONFIG;
            if (StringUtils.isNotBlank(radioConfigType)) {
                configType = formData.getString("radioConfigType");
            }

            int buildStep = DEFAULT_STEP;
            if (formData.has("buildStepString")) {
                String buildStepString = formData.getString("buildStepString");

                if (StringUtils.isNotBlank(buildStepString)) {
                    buildStep = formData.getInt("buildStepString");
                }
            }

            String value = serializeToString(configType, buildCount, firstDayCount,
                    lastDayCount, buildStep);
            persistValue(value, request, response);

        } catch (JSONException exception) {
            LOGGER.log(Level.SEVERE, "Can't parse the form data: " + request,
                    exception);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.SEVERE, "Can't parse the form data: " + request,
                    exception);
        } catch (ServletException exception) {
            LOGGER.log(Level.SEVERE, "Can't process the form data: " + request,
                    exception);
        } finally {
            try {
                response.sendRedirect("../");
            } catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "Can't redirect", exception);
            }
        }
    }

    public String getDisplayName() {
        return Messages.GraphConfigurationDetail_DisplayName();
    }

    /**
     * Creates a new cookie handler to convert the cookie to a string value.
     *
     * @param cookieName the suffix of the cookie name that is used to persist the
     *                   configuration per user
     * @return the new cookie handler
     */
    private static CookieHandler createCookieHandler(final String cookieName) {
        return new CookieHandler(cookieName);
    }

    protected void persistValue(final String value, final StaplerRequest request,
                                final StaplerResponse response) {

        // First check for URL values
        String buildCount = request.getParameter("buildCount");
        if (buildCount != null) {
            setBuildCount(Integer.parseInt(buildCount));
            setConfigType(GraphConfigurationDetail.BUILD_CONFIG);
            return;
        }

        String buildStep = request.getParameter("buildStep");
        if (buildStep != null) {
            setBuildStep(Integer.parseInt(buildStep));
            setConfigType(GraphConfigurationDetail.BUILDNTH_CONFIG);
            return;
        }

        // If not found, check cookie
        Cookie cookie = createCookieHandler("performance").create(
                request.getAncestors(), value);
        response.addCookie(cookie);
    }

    protected String serializeToString(final String configType,
                                       final int buildCount, final String firstDayCount,
                                       final String lastDayCount, final int buildStep) {
        return configType + SEPARATOR + buildCount + SEPARATOR + firstDayCount
                + SEPARATOR + lastDayCount + SEPARATOR + buildStep;
    }

    /**
     * Creates a file with for the default values.
     *
     * @param project    the project used as directory for the file
     * @param pluginName the name of the plug-in
     * @return the created file
     */
    protected static File createDefaultsFile(final AbstractProject<?, ?> project,
                                             final String pluginName) {
        return new File(project.getRootDir(), pluginName + ".txt");
    }

    /**
     * Parses the provided string and initializes the members. If the string is
     * not in the expected format, a list containing -1, 1, 2 and/or 3 is
     * returned. -1 is a global error, 1 is a dayCount error, 2 is a first date
     * error, 3 is a last date error. Return an empty list if all is good.
     *
     * @param value the initialization value stored in the format
     *              <code>configType;buildCount;firstDayCount;lastDayCount</code>
     * @return an empty list is the initialization was successful, a list
     * containing -1, 1, 2 or 3 otherwise
     */
    private List<Integer> initializeFrom(final String value) {
        List<Integer> listErrors = new ArrayList<Integer>(0);
        if (StringUtils.isBlank(value)) {
            listErrors.add(-1);
            return listErrors;
        }

        String[] values;
        if (value.contains(LEGACY_SEPARATOR))
            values = StringUtils.split(value, LEGACY_SEPARATOR);
        else
            values = StringUtils.split(value, SEPARATOR);

        if ((values.length != 4) && (values.length != 5)) {
            listErrors.add(-1);
            return listErrors;
        }
        configType = values[0];
        if (BUILD_CONFIG.compareToIgnoreCase(configType) != 0
                && BUILDNTH_CONFIG.compareToIgnoreCase(configType) != 0
                && DATE_CONFIG.compareToIgnoreCase(configType) != 0
                && NONE_CONFIG.compareToIgnoreCase(configType) != 0) {
            listErrors.add(-1);
        }
        try {
            buildCount = Integer.parseInt(values[1]);
        } catch (JSONException e) {
            listErrors.add(1);
            e.printStackTrace();
        }
        firstDayCount = values[2];
        lastDayCount = values[3];
        GregorianCalendar firstDate = null;
        GregorianCalendar lastDate = null;
        if (firstDayCount.compareTo(DEFAULT_DATE) == 0
                && DATE_CONFIG.compareToIgnoreCase(configType) == 0) {
            listErrors.add(2);
        }
        if (lastDayCount.compareTo(DEFAULT_DATE) == 0
                && DATE_CONFIG.compareToIgnoreCase(configType) == 0) {
            listErrors.add(3);
        }
        if (firstDayCount.compareTo(DEFAULT_DATE) != 0) {
            try {
                firstDate = getGregorianCalendarFromString(firstDayCount);
            } catch (IllegalArgumentException e) {
                listErrors.add(2);
                e.printStackTrace();
            } catch (ParseException e) {
                listErrors.add(2);
                e.printStackTrace();
            }
        }
        if (lastDayCount.compareTo(DEFAULT_DATE) != 0) {
            try {
                lastDate = getGregorianCalendarFromString(lastDayCount);
            } catch (IllegalArgumentException e) {
                listErrors.add(3);
                e.printStackTrace();
            } catch (ParseException e) {
                listErrors.add(3);
                e.printStackTrace();
            }
        }
        if (firstDate != null && lastDate != null && firstDate.after(lastDate)) {
            listErrors.add(2);
            listErrors.add(3);
        }

        try {
            if (values.length == 5) {
                buildStep = Integer.parseInt(values[4]);
            }
        } catch (JSONException e) {
            listErrors.add(4);
            e.printStackTrace();
        }

        // clean the error list
        if (!listErrors.isEmpty()) {
            Collections.sort(listErrors);
            if (listErrors.get(0) == -1) {
                listErrors = new ArrayList<Integer>(1);
                listErrors.add(-1);
            } else {
                int actualErrorType = 0;
                List<Integer> realListErrors = new ArrayList<Integer>(0);
                for (Integer typeError : listErrors) {
                    if (actualErrorType != typeError) {
                        actualErrorType = typeError;
                        realListErrors.add(typeError);
                    }
                }
                listErrors = realListErrors;
            }
        }
        return listErrors;
    }

    /**
     * <p>
     * Get a gregorian calendar from a String of type : DD/MM/YYYY
     * </p>
     *
     * @param dateString
     * @return GregorianCalendar
     * @throws ParseException
     */
    public static GregorianCalendar getGregorianCalendarFromString(
            String dateString) throws ParseException {
        Date date = format.parse(dateString);
        GregorianCalendar outCalendar = new GregorianCalendar();
        outCalendar.setTime(date);
        return outCalendar;
    }

    /**
     * Resets the graph configuration to the default values.
     */
    private void reset(List<Integer> initializationResult) {
        configType = NONE_CONFIG;
        for (Integer errorNumber : initializationResult) {
            if (errorNumber == -1) {
                buildCount = DEFAULT_COUNT;
                firstDayCount = DEFAULT_DATE;
                lastDayCount = DEFAULT_DATE;
                buildStep = DEFAULT_STEP;
            } else if (errorNumber == 1) {
                buildCount = DEFAULT_COUNT;
            } else if (errorNumber == 2) {
                firstDayCount = DEFAULT_DATE;
            } else if (errorNumber == 3) {
                lastDayCount = DEFAULT_DATE;
            } else if (errorNumber == 4) {
                buildStep = DEFAULT_STEP;
            }
        }
    }

    /**
     * Reads the default values from file.
     *
     * @param defaultsFile the file with the default values
     * @return the default values from file.
     */
    private String readFromDefaultsFile(final File defaultsFile) {
        String defaultValue = StringUtils.EMPTY;
        FileInputStream input = null;
        try {
            input = new FileInputStream(defaultsFile);
            defaultValue = IOUtils.toString(input);
        } catch (IOException exception) {
            // ignore
        } finally {
            IOUtils.closeQuietly(input);
        }
        return defaultValue;
    }

    public int getBuildCount() {
        return buildCount;
    }

    public void setBuildCount(int buildCount) {
        this.buildCount = buildCount;
    }

    public int getBuildStep() {
        return buildStep;
    }

    public void setBuildStep(int buildStep) {
        this.buildStep = buildStep;
    }

    public String getFirstDayCount() {
        return firstDayCount;
    }

    public void setFirstDayCount(String firstDayCount) {
        this.firstDayCount = firstDayCount;
    }

    public String getLastDayCount() {
        return lastDayCount;
    }

    public void setLastDayCount(String lastDayCount) {
        this.lastDayCount = lastDayCount;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }
}
