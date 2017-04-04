package hudson.plugins.performance.parsers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import javax.xml.bind.ValidationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Iago results as dumped by the server.
 *
 * @author jwstric2
 */
public class IagoParser extends AbstractParser {

    public String statsDateFormat;

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "Iago";
        }
    }

    @DataBoundConstructor
    public IagoParser(String glob) {
        super(glob);
        this.statsDateFormat = getStatsDateFormat();
    }

    @Override
    public String getDefaultGlobPattern() {
        //Normally just a parrot server result file; if using multiple
        //user will need to add their own recognizable glob extensions
        return "parrot-server-stats.log";
    }

    protected String getStatsDateFormat() {
        //Example default date from log file iago 0.6.14
        //20140611-21:46:01.013
        return "yyyymmdd-HH:mm:ss.SSS";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        final PerformanceReport report = new PerformanceReport();
        report.setReportFileName(reportFile.getName());

        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        try {
            String line = reader.readLine();
            while (line != null) {
                final HttpSample sample = this.getSample(line, reportFile.getName());
                String nextLine = reader.readLine();
                if (sample != null) {
                    try {
                        report.addSample(sample);
                    } catch (SAXException e) {
                        throw new RuntimeException("Error parsing file '" + reportFile + "': Unable to add sample for line " + line, e);
                    }
                }
                line = nextLine;
            }
        } finally {
            if (reader != null)
                reader.close();
        }

        return report;
    }


    /**
     * Parses a line and return a HttpSample
     */
    //INF [20140611-21:34:01.224] stats: {"400":84,"client\/available":1,"client\/cancelled_connects":0,"client\/closechans":85,"client\/closed":85,"client\/closes":84,"client\/codec_connection_preparation_latency_ms_average":3,"client\/codec_connection_preparation_latency_ms_count":85,"client\/codec_connection_preparation_latency_ms_maximum":142,"client\/codec_connection_preparation_latency_ms_minimum":1,"client\/codec_connection_preparation_latency_ms_p50":2,"client\/codec_connection_preparation_latency_ms_p90":4,"client\/codec_connection_preparation_latency_ms_p95":4,"client\/codec_connection_preparation_latency_ms_p99":142,"client\/codec_connection_preparation_latency_ms_p999":142,"client\/codec_connection_preparation_latency_ms_p9999":142,"client\/codec_connection_preparation_latency_ms_sum":316,"client\/connect_latency_ms_average":2,"client\/connect_latency_ms_count":85,"client\/connect_latency_ms_maximum":142,"client\/connect_latency_ms_minimum":0,"client\/connect_latency_ms_p50":1,"client\/connect_latency_ms_p90":2,"client\/connect_latency_ms_p95":4,"client\/connect_latency_ms_p99":142,"client\/connect_latency_ms_p999":142,"client\/connect_latency_ms_p9999":142,"client\/connect_latency_ms_sum":238,"client\/connection_duration_average":5,"client\/connection_duration_count":85,"client\/connection_duration_maximum":173,"client\/connection_duration_minimum":2,"client\/connection_duration_p50":3,"client\/connection_duration_p90":6,"client\/connection_duration_p95":7,"client\/connection_duration_p99":173,"client\/connection_duration_p999":173,"client\/connection_duration_p9999":173,"client\/connection_duration_sum":477,"client\/connection_received_bytes_average":604,"client\/connection_received_bytes_count":85,"client\/connection_received_bytes_maximum":576,"client\/connection_received_bytes_minimum":576,"client\/connection_received_bytes_p50":576,"client\/connection_received_bytes_p90":576,"client\/connection_received_bytes_p95":576,"client\/connection_received_bytes_p99":576,"client\/connection_received_bytes_p999":576,"client\/connection_received_bytes_p9999":576,"client\/connection_received_bytes_sum":51340,"client\/connection_requests_average":1,"client\/connection_requests_count":85,"client\/connection_requests_maximum":1,"client\/connection_requests_minimum":1,"client\/connection_requests_p50":1,"client\/connection_requests_p90":1,"client\/connection_requests_p95":1,"client\/connection_requests_p99":1,"client\/connection_requests_p999":1,"client\/connection_requests_p9999":1,"client\/connection_requests_sum":85,"client\/connection_sent_bytes_average":140,"client\/connection_sent_bytes_count":85,"client\/connection_sent_bytes_maximum":142,"client\/connection_sent_bytes_minimum":142,"client\/connection_sent_bytes_p50":142,"client\/connection_sent_bytes_p90":142,"client\/connection_sent_bytes_p95":142,"client\/connection_sent_bytes_p99":142,"client\/connection_sent_bytes_p999":142,"client\/connection_sent_bytes_p9999":142,"client\/connection_sent_bytes_sum":11919,"client\/connections":0,"client\/connects":85,"client\/failed_connect_latency_ms_count":0,"client\/failfast":0,"client\/failfast\/unhealthy_for_ms":0,"client\/failfast\/unhealthy_num_tries":0,"client\/failures":1,"client\/failures\/com.twitter.finagle.ChannelClosedException":1,"client\/idle":0,"client\/jonatstr-dt-otc_80\/available":1,"client\/jonatstr-dt-otc_80\/cancelled_connects":0,"client\/jonatstr-dt-otc_80\/closechans":85,"client\/jonatstr-dt-otc_80\/closed":85,"client\/jonatstr-dt-otc_80\/closes":84,"client\/jonatstr-dt-otc_80\/connect_latency_ms_average":2,"client\/jonatstr-dt-otc_80\/connect_latency_ms_count":85,"client\/jonatstr-dt-otc_80\/connect_latency_ms_maximum":142,"client\/jonatstr-dt-otc_80\/connect_latency_ms_minimum":0,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p50":1,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p90":2,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p95":4,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p99":142,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p999":142,"client\/jonatstr-dt-otc_80\/connect_latency_ms_p9999":142,"client\/jonatstr-dt-otc_80\/connect_latency_ms_sum":238,"client\/jonatstr-dt-otc_80\/connection_duration_average":5,"client\/jonatstr-dt-otc_80\/connection_duration_count":85,"client\/jonatstr-dt-otc_80\/connection_duration_maximum":173,"client\/jonatstr-dt-otc_80\/connection_duration_minimum":2,"client\/jonatstr-dt-otc_80\/connection_duration_p50":3,"client\/jonatstr-dt-otc_80\/connection_duration_p90":6,"client\/jonatstr-dt-otc_80\/connection_duration_p95":7,"client\/jonatstr-dt-otc_80\/connection_duration_p99":173,"client\/jonatstr-dt-otc_80\/connection_duration_p999":173,"client\/jonatstr-dt-otc_80\/connection_duration_p9999":173,"client\/jonatstr-dt-otc_80\/connection_duration_sum":477,"client\/jonatstr-dt-otc_80\/connection_received_bytes_average":604,"client\/jonatstr-dt-otc_80\/connection_received_bytes_count":85,"client\/jonatstr-dt-otc_80\/connection_received_bytes_maximum":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_minimum":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p50":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p90":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p95":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p99":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p999":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_p9999":576,"client\/jonatstr-dt-otc_80\/connection_received_bytes_sum":51340,"client\/jonatstr-dt-otc_80\/connection_requests_average":1,"client\/jonatstr-dt-otc_80\/connection_requests_count":85,"client\/jonatstr-dt-otc_80\/connection_requests_maximum":1,"client\/jonatstr-dt-otc_80\/connection_requests_minimum":1,"client\/jonatstr-dt-otc_80\/connection_requests_p50":1,"client\/jonatstr-dt-otc_80\/connection_requests_p90":1,"client\/jonatstr-dt-otc_80\/connection_requests_p95":1,"client\/jonatstr-dt-otc_80\/connection_requests_p99":1,"client\/jonatstr-dt-otc_80\/connection_requests_p999":1,"client\/jonatstr-dt-otc_80\/connection_requests_p9999":1,"client\/jonatstr-dt-otc_80\/connection_requests_sum":85,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_average":140,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_count":85,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_maximum":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_minimum":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p50":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p90":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p95":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p99":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p999":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_p9999":142,"client\/jonatstr-dt-otc_80\/connection_sent_bytes_sum":11919,"client\/jonatstr-dt-otc_80\/connections":0,"client\/jonatstr-dt-otc_80\/connects":85,"client\/jonatstr-dt-otc_80\/failed_connect_latency_ms_count":0,"client\/jonatstr-dt-otc_80\/failfast":0,"client\/jonatstr-dt-otc_80\/failfast\/unhealthy_for_ms":0,"client\/jonatstr-dt-otc_80\/failfast\/unhealthy_num_tries":0,"client\/jonatstr-dt-otc_80\/failures":1,"client\/jonatstr-dt-otc_80\/failures\/com.twitter.finagle.ChannelClosedException":1,"client\/jonatstr-dt-otc_80\/idle":0,"client\/jonatstr-dt-otc_80\/lifetime":0,"client\/jonatstr-dt-otc_80\/load":0,"client\/jonatstr-dt-otc_80\/pending":0,"client\/jonatstr-dt-otc_80\/pool_cached":0,"client\/jonatstr-dt-otc_80\/pool_num_waited":0,"client\/jonatstr-dt-otc_80\/pool_size":0,"client\/jonatstr-dt-otc_80\/pool_waiters":0,"client\/jonatstr-dt-otc_80\/received_bytes":51340,"client\/jonatstr-dt-otc_80\/request_latency_ms_average":3,"client\/jonatstr-dt-otc_80\/request_latency_ms_count":85,"client\/jonatstr-dt-otc_80\/request_latency_ms_maximum":105,"client\/jonatstr-dt-otc_80\/request_latency_ms_minimum":1,"client\/jonatstr-dt-otc_80\/request_latency_ms_p50":2,"client\/jonatstr-dt-otc_80\/request_latency_ms_p90":4,"client\/jonatstr-dt-otc_80\/request_latency_ms_p95":4,"client\/jonatstr-dt-otc_80\/request_latency_ms_p99":105,"client\/jonatstr-dt-otc_80\/request_latency_ms_p999":105,"client\/jonatstr-dt-otc_80\/request_latency_ms_p9999":105,"client\/jonatstr-dt-otc_80\/request_latency_ms_sum":276,"client\/jonatstr-dt-otc_80\/requests":85,"client\/jonatstr-dt-otc_80\/sent_bytes":11919,"client\/jonatstr-dt-otc_80\/socket_unwritable_ms":0,"client\/jonatstr-dt-otc_80\/socket_writable_ms":207,"client\/jonatstr-dt-otc_80\/success":84,"client\/lifetime":0,"client\/load":0,"client\/loadbalancer\/adds":0,"client\/loadbalancer\/available":1,"client\/loadbalancer\/load":0,"client\/loadbalancer\/removes":0,"client\/loadbalancer\/size":1,"client\/pending":0,"client\/pool_cached":0,"client\/pool_num_waited":0,"client\/pool_size":0,"client\/pool_waiters":0,"client\/received_bytes":51340,"client\/request_latency_ms_average":3,"client\/request_latency_ms_count":85,"client\/request_latency_ms_maximum":105,"client\/request_latency_ms_minimum":1,"client\/request_latency_ms_p50":2,"client\/request_latency_ms_p90":4,"client\/request_latency_ms_p95":4,"client\/request_latency_ms_p99":105,"client\/request_latency_ms_p999":105,"client\/request_latency_ms_p9999":105,"client\/request_latency_ms_sum":276,"client\/requests":85,"client\/sent_bytes":11919,"client\/socket_unwritable_ms":0,"client\/socket_writable_ms":207,"client\/success":84,"clock_error":0,"jvm_buffer_direct_count":4,"jvm_buffer_direct_max":133120,"jvm_buffer_direct_used":133120,"jvm_buffer_mapped_count":0,"jvm_buffer_mapped_max":0,"jvm_buffer_mapped_used":0,"jvm_current_mem_CMS_Old_Gen_max":3657433088,"jvm_current_mem_CMS_Old_Gen_used":12173984,"jvm_current_mem_CMS_Perm_Gen_max":85983232,"jvm_current_mem_CMS_Perm_Gen_used":44355376,"jvm_current_mem_Code_Cache_max":50331648,"jvm_current_mem_Code_Cache_used":2425792,"jvm_current_mem_Eden_Space_max":429522944,"jvm_current_mem_Eden_Space_used":169518376,"jvm_current_mem_Survivor_Space_max":53673984,"jvm_current_mem_Survivor_Space_used":53673984,"jvm_current_mem_used":282147512,"jvm_fd_count":142,"jvm_fd_limit":4096,"jvm_gc_ConcurrentMarkSweep_cycles":0,"jvm_gc_ConcurrentMarkSweep_msec":0,"jvm_gc_Copy_cycles":0,"jvm_gc_Copy_msec":0,"jvm_gc_cycles":0,"jvm_gc_msec":0,"jvm_heap_committed":4140630016,"jvm_heap_max":4140630016,"jvm_heap_used":235366344,"jvm_nonheap_committed":47120384,"jvm_nonheap_max":136314880,"jvm_nonheap_used":46777704,"jvm_num_cpus":1,"jvm_post_gc_CMS_Old_Gen_max":3657433088,"jvm_post_gc_CMS_Old_Gen_used":0,"jvm_post_gc_CMS_Perm_Gen_max":85983232,"jvm_post_gc_CMS_Perm_Gen_used":0,"jvm_post_gc_Eden_Space_max":429522944,"jvm_post_gc_Eden_Space_used":0,"jvm_post_gc_Survivor_Space_max":53673984,"jvm_post_gc_Survivor_Space_used":53673984,"jvm_post_gc_used":53673984,"jvm_start_time":1402536778818,"jvm_thread_count":18,"jvm_thread_daemon_count":12,"jvm_thread_peak_count":18,"jvm_uptime":62216,"queue_depth":41,"records-read":126,"requests_sent":85,"service":"parrot_web","source":"jonatstr-dt-oneconnector","timestamp":1402536841,"unexpected_error":1,"unexpected_error\/com.twitter.finagle.ChannelClosedException":1}
    protected HttpSample getSample(String line, String key) throws ParseException, ValidationException {

        HttpSample sample = new HttpSample();
        Pattern pattern = Pattern.compile("^INF \\[(.+)\\] stats: (\\{.+\\})$");
        Matcher matcher = pattern.matcher(line);

        //Should have group count of 2, the date and the stats json
        if (!matcher.find()) {
            throw new ParseException("Invalid line " + line, 0);
        }

        String dateString = matcher.group(1);
        String statsString = matcher.group(2);

        //Get date object
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.statsDateFormat);
        Date dateObject = dateFormat.parse(dateString);

        //Now we need to parse the stats json
        GsonBuilder gsonBuilder = new GsonBuilder();
        StatsDeserializer deserializer = new StatsDeserializer();
        gsonBuilder.registerTypeAdapter(Stats.class, deserializer);
        Gson gson = gsonBuilder.create();

        Stats statsObject = null;
        try {
            statsObject = gson.fromJson(statsString, Stats.class);
        } catch (JsonParseException e) {
            throw new ValidationException("Invalid stat data " + statsString + ":" + e.getLocalizedMessage());
        }

        //Set the sample data
        sample.setDate(dateObject);
        sample.setSummarizerSamples(statsObject.getClientRequests()); // set SamplesCount
        sample.setDuration(statsObject.getClientRequestLatencyMsAverage());
        sample.setSuccessful(true);
        sample.setSummarizerMin(statsObject.getClientRequestLatencyMsMinimum());
        sample.setSummarizerMax(statsObject.getClientRequestLatencyMsMaximum());
        sample.setSummarizerErrors((statsObject.getClientRequests() - statsObject.getClientSuccess()) + statsObject.getSumValidationErrors());
        sample.setUri(key);

        return sample;
    }

    protected static class Stats {

        @SerializedName("client/request_latency_ms_minimum")
        private long clientRequestLatencyMsMinimum = 0;
        @SerializedName("client/request_latency_ms_maximum")
        private long clientRequestLatencyMsMaximum = 0;
        @SerializedName("client/request_latency_ms_average")
        private long clientRequestLatencyMsAverage = 0;
        @SerializedName("client/sent_bytes")
        private long clientSendBytes = 0;
        @SerializedName("client/requests")
        private long clientRequests = 0;
        @SerializedName("client/success")
        private long clientSuccess = 0;

        //User defined validation errors
        private transient Dictionary<String, Long> validationErrors = new Hashtable<String, Long>();

        public Stats() {

        }

        public long getClientRequestLatencyMsMinimum() {
            return clientRequestLatencyMsMinimum;
        }

        public void setClientRequestLatencyMsMinimum(
                long clientRequestLatencyMsMinimum) {
            this.clientRequestLatencyMsMinimum = clientRequestLatencyMsMinimum;
        }

        public long getClientRequestLatencyMsMaximum() {
            return clientRequestLatencyMsMaximum;
        }

        public void setClientRequestLatencyMsMaximum(
                long clientRequestLatencyMsMaximum) {
            this.clientRequestLatencyMsMaximum = clientRequestLatencyMsMaximum;
        }

        public long getClientRequestLatencyMsAverage() {
            return clientRequestLatencyMsAverage;
        }

        public void setClientRequestLatencyMsAverage(
                long clientRequestLatencyMsAverage) {
            this.clientRequestLatencyMsAverage = clientRequestLatencyMsAverage;
        }

        public long getClientSendBytes() {
            return clientSendBytes;
        }

        public void setClientSendBytes(long clientSendBytes) {
            this.clientSendBytes = clientSendBytes;
        }

        public long getClientRequests() {
            return clientRequests;
        }

        public void setClientRequests(long clientRequests) {
            this.clientRequests = clientRequests;
        }

        public long getClientSuccess() {
            return clientSuccess;
        }

        public void setClientSuccess(long clientSuccess) {
            this.clientSuccess = clientSuccess;
        }

        public void addValidationError(String name, long value) {
            synchronized (validationErrors) {
                this.validationErrors.put(name, new Long(value));
            }
        }

        public long getSumValidationErrors() {
            long sumValidationErrors = 0;
            synchronized (validationErrors) {
                Enumeration<String> keys = validationErrors.keys();
                while (keys.hasMoreElements()) {
                    sumValidationErrors += validationErrors.get(keys.nextElement());
                }
            }
            return sumValidationErrors;
        }

    }


    /**
     * A Stats Deserializer to verify during deserialization that
     * all needed stats are available for the IagoParser and handle
     * any special error fields that a user specified
     *
     * @author jwstric2
     */
    private static class StatsDeserializer implements JsonDeserializer<Stats> {

        private static final String[] requiredFields = new String[]{
                "client/request_latency_ms_minimum",
                "client/request_latency_ms_maximum",
                "client/request_latency_ms_average",
                "client/sent_bytes",
                "client/requests",
                "client/success"};

        public Stats deserialize(JsonElement json, Type typeOfT,
                                 JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonObject = (JsonObject) json;
            Stats statsObj = null;

            //First, check we have all our required fields ..
            for (String fieldName : requiredFields) {
                if (jsonObject.get(fieldName) == null) {
                    throw new JsonParseException("Required Field Not Found: " + fieldName);
                }
            }

            return new Gson().fromJson(json, Stats.class);
        }

    }
}