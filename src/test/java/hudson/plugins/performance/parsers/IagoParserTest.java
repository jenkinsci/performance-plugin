package hudson.plugins.performance.parsers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.model.FreeStyleBuild;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.parsers.IagoParser.Stats;
import hudson.plugins.performance.reports.PerformanceReport;
import junit.framework.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

public class IagoParserTest extends HudsonTestCase {

    @Test
    public void testParseValidLine() throws Exception {
        IagoParser parser = new IagoParser(null);

        //Line to parse
        String line = "INF [20140611-21:34:01.224] stats: {\"400\":84,\"client\\/request_latency_ms_average\":3,\"client\\/request_latency_ms_count\":85,\"client\\/request_latency_ms_maximum\":105,\"client\\/request_latency_ms_minimum\":1,\"client\\/request_latency_ms_p50\":2,\"client\\/request_latency_ms_p90\":4,\"client\\/request_latency_ms_p95\":4,\"client\\/request_latency_ms_p99\":105,\"client\\/request_latency_ms_p999\":105,\"client\\/request_latency_ms_p9999\":105,\"client\\/request_latency_ms_sum\":276,\"client\\/requests\":85,\"client\\/sent_bytes\":11919,\"client\\/socket_unwritable_ms\":0,\"client\\/socket_writable_ms\":207,\"client\\/success\":84}";
        String key = "TEST";

        HttpSample sample = parser.getSample(line, key);
        Assert.assertEquals(105, sample.getSummarizerMax());
        Assert.assertEquals(1, sample.getSummarizerMin());
        Assert.assertEquals(85, sample.getSummarizerSamples());
        Assert.assertEquals((float) 1.0, sample.getSummarizerErrors());
        Assert.assertEquals(3, sample.getDuration());
        Assert.assertEquals(key, sample.getUri());
    }

    @Test
    public void testParseInvalidLine() throws Exception {
        IagoParser parser = new IagoParser("");

        //Valid line to parse
        String line = "[20140611-21:34:01.224] stats: {\"400\":84,\"client\\/available\":1,\"client\\/cancelled_connects\":0,\"client\\/closechans\":85,\"client\\/closed\":85,\"client\\/closes\":84,\"client\\/codec_connection_preparation_latency_ms_average\":3,\"client\\/codec_connection_preparation_latency_ms_count\":85,\"client\\/codec_connection_preparation_latency_ms_maximum\":142,\"client\\/codec_connection_preparation_latency_ms_minimum\":1,\"client\\/codec_connection_preparation_latency_ms_p50\":2,\"client\\/codec_connection_preparation_latency_ms_p90\":4,\"client\\/codec_connection_preparation_latency_ms_p95\":4,\"client\\/codec_connection_preparation_latency_ms_p99\":142,\"client\\/codec_connection_preparation_latency_ms_p999\":142,\"client\\/codec_connection_preparation_latency_ms_p9999\":142,\"client\\/codec_connection_preparation_latency_ms_sum\":316,\"client\\/connect_latency_ms_average\":2,\"client\\/connect_latency_ms_count\":85,\"client\\/connect_latency_ms_maximum\":142,\"client\\/connect_latency_ms_minimum\":0,\"client\\/connect_latency_ms_p50\":1,\"client\\/connect_latency_ms_p90\":2,\"client\\/connect_latency_ms_p95\":4,\"client\\/connect_latency_ms_p99\":142,\"client\\/connect_latency_ms_p999\":142,\"client\\/connect_latency_ms_p9999\":142,\"client\\/connect_latency_ms_sum\":238,\"client\\/connection_duration_average\":5,\"client\\/connection_duration_count\":85,\"client\\/connection_duration_maximum\":173,\"client\\/connection_duration_minimum\":2,\"client\\/connection_duration_p50\":3,\"client\\/connection_duration_p90\":6,\"client\\/connection_duration_p95\":7,\"client\\/connection_duration_p99\":173,\"client\\/connection_duration_p999\":173,\"client\\/connection_duration_p9999\":173,\"client\\/connection_duration_sum\":477,\"client\\/connection_received_bytes_average\":604,\"client\\/connection_received_bytes_count\":85,\"client\\/connection_received_bytes_maximum\":576,\"client\\/connection_received_bytes_minimum\":576,\"client\\/connection_received_bytes_p50\":576,\"client\\/connection_received_bytes_p90\":576,\"client\\/connection_received_bytes_p95\":576,\"client\\/connection_received_bytes_p99\":576,\"client\\/connection_received_bytes_p999\":576,\"client\\/connection_received_bytes_p9999\":576,\"client\\/connection_received_bytes_sum\":51340,\"client\\/connection_requests_average\":1,\"client\\/connection_requests_count\":85,\"client\\/connection_requests_maximum\":1,\"client\\/connection_requests_minimum\":1,\"client\\/connection_requests_p50\":1,\"client\\/connection_requests_p90\":1,\"client\\/connection_requests_p95\":1,\"client\\/connection_requests_p99\":1,\"client\\/connection_requests_p999\":1,\"client\\/connection_requests_p9999\":1,\"client\\/connection_requests_sum\":85,\"client\\/connection_sent_bytes_average\":140,\"client\\/connection_sent_bytes_count\":85,\"client\\/connection_sent_bytes_maximum\":142,\"client\\/connection_sent_bytes_minimum\":142,\"client\\/connection_sent_bytes_p50\":142,\"client\\/connection_sent_bytes_p90\":142,\"client\\/connection_sent_bytes_p95\":142,\"client\\/connection_sent_bytes_p99\":142,\"client\\/connection_sent_bytes_p999\":142,\"client\\/connection_sent_bytes_p9999\":142,\"client\\/connection_sent_bytes_sum\":11919,\"client\\/connections\":0,\"client\\/connects\":85,\"client\\/failed_connect_latency_ms_count\":0,\"client\\/failfast\":0,\"client\\/failfast\\/unhealthy_for_ms\":0,\"client\\/failfast\\/unhealthy_num_tries\":0,\"client\\/failures\":1,\"client\\/failures\\/com.twitter.finagle.ChannelClosedException\":1,\"client\\/idle\":0,\"client\\/jonatstr-dt-otc_80\\/available\":1,\"client\\/jonatstr-dt-otc_80\\/cancelled_connects\":0,\"client\\/jonatstr-dt-otc_80\\/closechans\":85,\"client\\/jonatstr-dt-otc_80\\/closed\":85,\"client\\/jonatstr-dt-otc_80\\/closes\":84,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_average\":2,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_count\":85,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_maximum\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_minimum\":0,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p50\":1,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p90\":2,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p95\":4,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p99\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p999\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p9999\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_sum\":238,\"client\\/jonatstr-dt-otc_80\\/connection_duration_average\":5,\"client\\/jonatstr-dt-otc_80\\/connection_duration_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_duration_maximum\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_minimum\":2,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p50\":3,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p90\":6,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p95\":7,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p99\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p999\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p9999\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_sum\":477,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_average\":604,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_maximum\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_minimum\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p50\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p90\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p95\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p99\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p999\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p9999\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_sum\":51340,\"client\\/jonatstr-dt-otc_80\\/connection_requests_average\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_requests_maximum\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_minimum\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p50\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p90\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p95\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p99\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p999\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p9999\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_sum\":85,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_average\":140,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_maximum\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_minimum\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p50\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p90\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p95\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p99\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p999\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p9999\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_sum\":11919,\"client\\/jonatstr-dt-otc_80\\/connections\":0,\"client\\/jonatstr-dt-otc_80\\/connects\":85,\"client\\/jonatstr-dt-otc_80\\/failed_connect_latency_ms_count\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\\/unhealthy_for_ms\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\\/unhealthy_num_tries\":0,\"client\\/jonatstr-dt-otc_80\\/failures\":1,\"client\\/jonatstr-dt-otc_80\\/failures\\/com.twitter.finagle.ChannelClosedException\":1,\"client\\/jonatstr-dt-otc_80\\/idle\":0,\"client\\/jonatstr-dt-otc_80\\/lifetime\":0,\"client\\/jonatstr-dt-otc_80\\/load\":0,\"client\\/jonatstr-dt-otc_80\\/pending\":0,\"client\\/jonatstr-dt-otc_80\\/pool_cached\":0,\"client\\/jonatstr-dt-otc_80\\/pool_num_waited\":0,\"client\\/jonatstr-dt-otc_80\\/pool_size\":0,\"client\\/jonatstr-dt-otc_80\\/pool_waiters\":0,\"client\\/jonatstr-dt-otc_80\\/received_bytes\":51340,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_average\":3,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_count\":85,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_maximum\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_minimum\":1,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p50\":2,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p90\":4,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p95\":4,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p99\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p999\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p9999\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_sum\":276,\"client\\/jonatstr-dt-otc_80\\/requests\":85,\"client\\/jonatstr-dt-otc_80\\/sent_bytes\":11919,\"client\\/jonatstr-dt-otc_80\\/socket_unwritable_ms\":0,\"client\\/jonatstr-dt-otc_80\\/socket_writable_ms\":207,\"client\\/jonatstr-dt-otc_80\\/success\":84,\"client\\/lifetime\":0,\"client\\/load\":0,\"client\\/loadbalancer\\/adds\":0,\"client\\/loadbalancer\\/available\":1,\"client\\/loadbalancer\\/load\":0,\"client\\/loadbalancer\\/removes\":0,\"client\\/loadbalancer\\/size\":1,\"client\\/pending\":0,\"client\\/pool_cached\":0,\"client\\/pool_num_waited\":0,\"client\\/pool_size\":0,\"client\\/pool_waiters\":0,\"client\\/received_bytes\":51340,\"client\\/request_latency_ms_average\":3,\"client\\/request_latency_ms_count\":85,\"client\\/request_latency_ms_maximum\":105,\"client\\/request_latency_ms_minimum\":1,\"client\\/request_latency_ms_p50\":2,\"client\\/request_latency_ms_p90\":4,\"client\\/request_latency_ms_p95\":4,\"client\\/request_latency_ms_p99\":105,\"client\\/request_latency_ms_p999\":105,\"client\\/request_latency_ms_p9999\":105,\"client\\/request_latency_ms_sum\":276,\"client\\/requests\":85,\"client\\/sent_bytes\":11919,\"client\\/socket_unwritable_ms\":0,\"client\\/socket_writable_ms\":207,\"client\\/success\":84,\"clock_error\":0,\"jvm_buffer_direct_count\":4,\"jvm_buffer_direct_max\":133120,\"jvm_buffer_direct_used\":133120,\"jvm_buffer_mapped_count\":0,\"jvm_buffer_mapped_max\":0,\"jvm_buffer_mapped_used\":0,\"jvm_current_mem_CMS_Old_Gen_max\":3657433088,\"jvm_current_mem_CMS_Old_Gen_used\":12173984,\"jvm_current_mem_CMS_Perm_Gen_max\":85983232,\"jvm_current_mem_CMS_Perm_Gen_used\":44355376,\"jvm_current_mem_Code_Cache_max\":50331648,\"jvm_current_mem_Code_Cache_used\":2425792,\"jvm_current_mem_Eden_Space_max\":429522944,\"jvm_current_mem_Eden_Space_used\":169518376,\"jvm_current_mem_Survivor_Space_max\":53673984,\"jvm_current_mem_Survivor_Space_used\":53673984,\"jvm_current_mem_used\":282147512,\"jvm_fd_count\":142,\"jvm_fd_limit\":4096,\"jvm_gc_ConcurrentMarkSweep_cycles\":0,\"jvm_gc_ConcurrentMarkSweep_msec\":0,\"jvm_gc_Copy_cycles\":0,\"jvm_gc_Copy_msec\":0,\"jvm_gc_cycles\":0,\"jvm_gc_msec\":0,\"jvm_heap_committed\":4140630016,\"jvm_heap_max\":4140630016,\"jvm_heap_used\":235366344,\"jvm_nonheap_committed\":47120384,\"jvm_nonheap_max\":136314880,\"jvm_nonheap_used\":46777704,\"jvm_num_cpus\":1,\"jvm_post_gc_CMS_Old_Gen_max\":3657433088,\"jvm_post_gc_CMS_Old_Gen_used\":0,\"jvm_post_gc_CMS_Perm_Gen_max\":85983232,\"jvm_post_gc_CMS_Perm_Gen_used\":0,\"jvm_post_gc_Eden_Space_max\":429522944,\"jvm_post_gc_Eden_Space_used\":0,\"jvm_post_gc_Survivor_Space_max\":53673984,\"jvm_post_gc_Survivor_Space_used\":53673984,\"jvm_post_gc_used\":53673984,\"jvm_start_time\":1402536778818,\"jvm_thread_count\":18,\"jvm_thread_daemon_count\":12,\"jvm_thread_peak_count\":18,\"jvm_uptime\":62216,\"queue_depth\":41,\"records-read\":126,\"requests_sent\":85,\"service\":\"parrot_web\",\"source\":\"jonatstr-dt-oneconnector\",\"timestamp\":1402536841,\"unexpected_error\":1,\"unexpected_error\\/com.twitter.finagle.ChannelClosedException\":1}";
        String key = "TEST";

        boolean exceptionFound = false;
        try {
            parser.getSample(line, key);
        } catch (ParseException e) {
            exceptionFound = true;
        }

        Assert.assertTrue(exceptionFound);

    }

    @Test
    public void testParseInvalidDate() throws Exception {
        IagoParser parser = new IagoParser("");

        //Line to parse
        String line = "INF [20140611+21:34:01.224] stats: {\"400\":84,\"client\\/available\":1,\"client\\/cancelled_connects\":0,\"client\\/closechans\":85,\"client\\/closed\":85,\"client\\/closes\":84,\"client\\/codec_connection_preparation_latency_ms_average\":3,\"client\\/codec_connection_preparation_latency_ms_count\":85,\"client\\/codec_connection_preparation_latency_ms_maximum\":142,\"client\\/codec_connection_preparation_latency_ms_minimum\":1,\"client\\/codec_connection_preparation_latency_ms_p50\":2,\"client\\/codec_connection_preparation_latency_ms_p90\":4,\"client\\/codec_connection_preparation_latency_ms_p95\":4,\"client\\/codec_connection_preparation_latency_ms_p99\":142,\"client\\/codec_connection_preparation_latency_ms_p999\":142,\"client\\/codec_connection_preparation_latency_ms_p9999\":142,\"client\\/codec_connection_preparation_latency_ms_sum\":316,\"client\\/connect_latency_ms_average\":2,\"client\\/connect_latency_ms_count\":85,\"client\\/connect_latency_ms_maximum\":142,\"client\\/connect_latency_ms_minimum\":0,\"client\\/connect_latency_ms_p50\":1,\"client\\/connect_latency_ms_p90\":2,\"client\\/connect_latency_ms_p95\":4,\"client\\/connect_latency_ms_p99\":142,\"client\\/connect_latency_ms_p999\":142,\"client\\/connect_latency_ms_p9999\":142,\"client\\/connect_latency_ms_sum\":238,\"client\\/connection_duration_average\":5,\"client\\/connection_duration_count\":85,\"client\\/connection_duration_maximum\":173,\"client\\/connection_duration_minimum\":2,\"client\\/connection_duration_p50\":3,\"client\\/connection_duration_p90\":6,\"client\\/connection_duration_p95\":7,\"client\\/connection_duration_p99\":173,\"client\\/connection_duration_p999\":173,\"client\\/connection_duration_p9999\":173,\"client\\/connection_duration_sum\":477,\"client\\/connection_received_bytes_average\":604,\"client\\/connection_received_bytes_count\":85,\"client\\/connection_received_bytes_maximum\":576,\"client\\/connection_received_bytes_minimum\":576,\"client\\/connection_received_bytes_p50\":576,\"client\\/connection_received_bytes_p90\":576,\"client\\/connection_received_bytes_p95\":576,\"client\\/connection_received_bytes_p99\":576,\"client\\/connection_received_bytes_p999\":576,\"client\\/connection_received_bytes_p9999\":576,\"client\\/connection_received_bytes_sum\":51340,\"client\\/connection_requests_average\":1,\"client\\/connection_requests_count\":85,\"client\\/connection_requests_maximum\":1,\"client\\/connection_requests_minimum\":1,\"client\\/connection_requests_p50\":1,\"client\\/connection_requests_p90\":1,\"client\\/connection_requests_p95\":1,\"client\\/connection_requests_p99\":1,\"client\\/connection_requests_p999\":1,\"client\\/connection_requests_p9999\":1,\"client\\/connection_requests_sum\":85,\"client\\/connection_sent_bytes_average\":140,\"client\\/connection_sent_bytes_count\":85,\"client\\/connection_sent_bytes_maximum\":142,\"client\\/connection_sent_bytes_minimum\":142,\"client\\/connection_sent_bytes_p50\":142,\"client\\/connection_sent_bytes_p90\":142,\"client\\/connection_sent_bytes_p95\":142,\"client\\/connection_sent_bytes_p99\":142,\"client\\/connection_sent_bytes_p999\":142,\"client\\/connection_sent_bytes_p9999\":142,\"client\\/connection_sent_bytes_sum\":11919,\"client\\/connections\":0,\"client\\/connects\":85,\"client\\/failed_connect_latency_ms_count\":0,\"client\\/failfast\":0,\"client\\/failfast\\/unhealthy_for_ms\":0,\"client\\/failfast\\/unhealthy_num_tries\":0,\"client\\/failures\":1,\"client\\/failures\\/com.twitter.finagle.ChannelClosedException\":1,\"client\\/idle\":0,\"client\\/jonatstr-dt-otc_80\\/available\":1,\"client\\/jonatstr-dt-otc_80\\/cancelled_connects\":0,\"client\\/jonatstr-dt-otc_80\\/closechans\":85,\"client\\/jonatstr-dt-otc_80\\/closed\":85,\"client\\/jonatstr-dt-otc_80\\/closes\":84,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_average\":2,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_count\":85,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_maximum\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_minimum\":0,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p50\":1,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p90\":2,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p95\":4,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p99\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p999\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_p9999\":142,\"client\\/jonatstr-dt-otc_80\\/connect_latency_ms_sum\":238,\"client\\/jonatstr-dt-otc_80\\/connection_duration_average\":5,\"client\\/jonatstr-dt-otc_80\\/connection_duration_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_duration_maximum\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_minimum\":2,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p50\":3,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p90\":6,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p95\":7,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p99\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p999\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_p9999\":173,\"client\\/jonatstr-dt-otc_80\\/connection_duration_sum\":477,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_average\":604,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_maximum\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_minimum\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p50\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p90\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p95\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p99\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p999\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_p9999\":576,\"client\\/jonatstr-dt-otc_80\\/connection_received_bytes_sum\":51340,\"client\\/jonatstr-dt-otc_80\\/connection_requests_average\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_requests_maximum\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_minimum\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p50\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p90\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p95\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p99\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p999\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_p9999\":1,\"client\\/jonatstr-dt-otc_80\\/connection_requests_sum\":85,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_average\":140,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_count\":85,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_maximum\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_minimum\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p50\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p90\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p95\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p99\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p999\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_p9999\":142,\"client\\/jonatstr-dt-otc_80\\/connection_sent_bytes_sum\":11919,\"client\\/jonatstr-dt-otc_80\\/connections\":0,\"client\\/jonatstr-dt-otc_80\\/connects\":85,\"client\\/jonatstr-dt-otc_80\\/failed_connect_latency_ms_count\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\\/unhealthy_for_ms\":0,\"client\\/jonatstr-dt-otc_80\\/failfast\\/unhealthy_num_tries\":0,\"client\\/jonatstr-dt-otc_80\\/failures\":1,\"client\\/jonatstr-dt-otc_80\\/failures\\/com.twitter.finagle.ChannelClosedException\":1,\"client\\/jonatstr-dt-otc_80\\/idle\":0,\"client\\/jonatstr-dt-otc_80\\/lifetime\":0,\"client\\/jonatstr-dt-otc_80\\/load\":0,\"client\\/jonatstr-dt-otc_80\\/pending\":0,\"client\\/jonatstr-dt-otc_80\\/pool_cached\":0,\"client\\/jonatstr-dt-otc_80\\/pool_num_waited\":0,\"client\\/jonatstr-dt-otc_80\\/pool_size\":0,\"client\\/jonatstr-dt-otc_80\\/pool_waiters\":0,\"client\\/jonatstr-dt-otc_80\\/received_bytes\":51340,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_average\":3,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_count\":85,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_maximum\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_minimum\":1,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p50\":2,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p90\":4,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p95\":4,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p99\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p999\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_p9999\":105,\"client\\/jonatstr-dt-otc_80\\/request_latency_ms_sum\":276,\"client\\/jonatstr-dt-otc_80\\/requests\":85,\"client\\/jonatstr-dt-otc_80\\/sent_bytes\":11919,\"client\\/jonatstr-dt-otc_80\\/socket_unwritable_ms\":0,\"client\\/jonatstr-dt-otc_80\\/socket_writable_ms\":207,\"client\\/jonatstr-dt-otc_80\\/success\":84,\"client\\/lifetime\":0,\"client\\/load\":0,\"client\\/loadbalancer\\/adds\":0,\"client\\/loadbalancer\\/available\":1,\"client\\/loadbalancer\\/load\":0,\"client\\/loadbalancer\\/removes\":0,\"client\\/loadbalancer\\/size\":1,\"client\\/pending\":0,\"client\\/pool_cached\":0,\"client\\/pool_num_waited\":0,\"client\\/pool_size\":0,\"client\\/pool_waiters\":0,\"client\\/received_bytes\":51340,\"client\\/request_latency_ms_average\":3,\"client\\/request_latency_ms_count\":85,\"client\\/request_latency_ms_maximum\":105,\"client\\/request_latency_ms_minimum\":1,\"client\\/request_latency_ms_p50\":2,\"client\\/request_latency_ms_p90\":4,\"client\\/request_latency_ms_p95\":4,\"client\\/request_latency_ms_p99\":105,\"client\\/request_latency_ms_p999\":105,\"client\\/request_latency_ms_p9999\":105,\"client\\/request_latency_ms_sum\":276,\"client\\/requests\":85,\"client\\/sent_bytes\":11919,\"client\\/socket_unwritable_ms\":0,\"client\\/socket_writable_ms\":207,\"client\\/success\":84,\"clock_error\":0,\"jvm_buffer_direct_count\":4,\"jvm_buffer_direct_max\":133120,\"jvm_buffer_direct_used\":133120,\"jvm_buffer_mapped_count\":0,\"jvm_buffer_mapped_max\":0,\"jvm_buffer_mapped_used\":0,\"jvm_current_mem_CMS_Old_Gen_max\":3657433088,\"jvm_current_mem_CMS_Old_Gen_used\":12173984,\"jvm_current_mem_CMS_Perm_Gen_max\":85983232,\"jvm_current_mem_CMS_Perm_Gen_used\":44355376,\"jvm_current_mem_Code_Cache_max\":50331648,\"jvm_current_mem_Code_Cache_used\":2425792,\"jvm_current_mem_Eden_Space_max\":429522944,\"jvm_current_mem_Eden_Space_used\":169518376,\"jvm_current_mem_Survivor_Space_max\":53673984,\"jvm_current_mem_Survivor_Space_used\":53673984,\"jvm_current_mem_used\":282147512,\"jvm_fd_count\":142,\"jvm_fd_limit\":4096,\"jvm_gc_ConcurrentMarkSweep_cycles\":0,\"jvm_gc_ConcurrentMarkSweep_msec\":0,\"jvm_gc_Copy_cycles\":0,\"jvm_gc_Copy_msec\":0,\"jvm_gc_cycles\":0,\"jvm_gc_msec\":0,\"jvm_heap_committed\":4140630016,\"jvm_heap_max\":4140630016,\"jvm_heap_used\":235366344,\"jvm_nonheap_committed\":47120384,\"jvm_nonheap_max\":136314880,\"jvm_nonheap_used\":46777704,\"jvm_num_cpus\":1,\"jvm_post_gc_CMS_Old_Gen_max\":3657433088,\"jvm_post_gc_CMS_Old_Gen_used\":0,\"jvm_post_gc_CMS_Perm_Gen_max\":85983232,\"jvm_post_gc_CMS_Perm_Gen_used\":0,\"jvm_post_gc_Eden_Space_max\":429522944,\"jvm_post_gc_Eden_Space_used\":0,\"jvm_post_gc_Survivor_Space_max\":53673984,\"jvm_post_gc_Survivor_Space_used\":53673984,\"jvm_post_gc_used\":53673984,\"jvm_start_time\":1402536778818,\"jvm_thread_count\":18,\"jvm_thread_daemon_count\":12,\"jvm_thread_peak_count\":18,\"jvm_uptime\":62216,\"queue_depth\":41,\"records-read\":126,\"requests_sent\":85,\"service\":\"parrot_web\",\"source\":\"jonatstr-dt-oneconnector\",\"timestamp\":1402536841,\"unexpected_error\":1,\"unexpected_error\\/com.twitter.finagle.ChannelClosedException\":1}";
        String key = "TEST";

        boolean exceptionFound = false;
        try {
            parser.getSample(line, key);
        } catch (ParseException e) {
            exceptionFound = true;
        }

        Assert.assertTrue(exceptionFound);

    }

    @Test
    public void testParseValidFile() throws Exception {

        Stats stats1 = new IagoParser.Stats();
        stats1.setClientRequestLatencyMsAverage((long) 4);
        stats1.setClientRequestLatencyMsMaximum((long) 5);
        stats1.setClientRequestLatencyMsMinimum((long) 1);
        stats1.setClientSuccess((long) 10);
        stats1.setClientRequests((long) 10);
        stats1.setClientSendBytes((long) 9000);

        Stats stats2 = new IagoParser.Stats();
        stats2.setClientRequestLatencyMsAverage((long) 3);
        stats2.setClientRequestLatencyMsMaximum((long) 4);
        stats2.setClientRequestLatencyMsMinimum((long) 2);
        stats2.setClientSuccess((long) 12);
        stats2.setClientRequests((long) 13);
        stats2.setClientSendBytes((long) 12000);


        // Create a temp file.
        // We can move this to resources, but allow flexibility in
        // unit test to create on the fly
        File temp = File.createTempFile("parrot-server-stats", ".log");
        // Delete the file when program exits.
        temp.deleteOnExit();

        GsonBuilder objGsonBuilder = new GsonBuilder();
        Gson gson = objGsonBuilder.create();
        boolean exceptionOccured = false;

        // Write to temp file
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(temp));

            System.out.println(gson.toJson(stats1));

            out.write("INF [20140611-21:34:01.224] stats: " + gson.toJson(stats1));
            out.write("INF [20140611-21:35:01.111] stats: " + gson.toJson(stats2));
        } catch (IOException e) {
            exceptionOccured = true;
        } finally {
            if (out != null) {
                out.close();
            }
        }
        Assert.assertFalse(exceptionOccured);


        IagoParser parser = new IagoParser("");
        Collection<PerformanceReport> reports = parser.parse(new FreeStyleBuild(createFreeStyleProject()), Arrays.asList(temp), createTaskListener());
        Assert.assertEquals(1, reports.size());

        PerformanceReport report = (PerformanceReport) reports.toArray()[0];

        Assert.assertEquals((stats1.getClientRequestLatencyMsAverage() + stats2.getClientRequestLatencyMsAverage()) / 2, report.getAverage());
    }





    public void testUserRegexNoValidationErrors() throws Exception {

        IagoParser parser = new IagoParser(null);//, "4[0-9]+,5[0-9]+", ",");

        //Line to parse
        String line = "INF [20140611-21:34:01.224] stats: {\"client\\/request_latency_ms_average\":3,\"client\\/request_latency_ms_count\":85,\"client\\/request_latency_ms_maximum\":105,\"client\\/request_latency_ms_minimum\":1,\"client\\/request_latency_ms_p50\":2,\"client\\/request_latency_ms_p90\":4,\"client\\/request_latency_ms_p95\":4,\"client\\/request_latency_ms_p99\":105,\"client\\/request_latency_ms_p999\":105,\"client\\/request_latency_ms_p9999\":105,\"client\\/request_latency_ms_sum\":276,\"client\\/requests\":85,\"client\\/sent_bytes\":11919,\"client\\/socket_unwritable_ms\":0,\"client\\/socket_writable_ms\":207,\"client\\/success\":84}";
        String key = "TEST";

        HttpSample sample = parser.getSample(line, key);
        Assert.assertEquals(105, sample.getSummarizerMax());
        Assert.assertEquals(1, sample.getSummarizerMin());
        Assert.assertEquals(85, sample.getSummarizerSamples());
        Assert.assertEquals((float) 1.0, sample.getSummarizerErrors());
        Assert.assertEquals(3, sample.getDuration());
        Assert.assertEquals(key, sample.getUri());

    }



    public void testNoErrors() throws Exception {
        IagoParser parser = new IagoParser(null);//, "4[0-9]+,5[0-9]+", ",");

        //Line to parse
        String line = "INF [20140611-21:34:01.224] stats: {\"client\\/request_latency_ms_average\":3,\"client\\/request_latency_ms_count\":85,\"client\\/request_latency_ms_maximum\":105,\"client\\/request_latency_ms_minimum\":1,\"client\\/request_latency_ms_p50\":2,\"client\\/request_latency_ms_p90\":4,\"client\\/request_latency_ms_p95\":4,\"client\\/request_latency_ms_p99\":105,\"client\\/request_latency_ms_p999\":105,\"client\\/request_latency_ms_p9999\":105,\"client\\/request_latency_ms_sum\":276,\"client\\/requests\":85,\"client\\/sent_bytes\":11919,\"client\\/socket_unwritable_ms\":0,\"client\\/socket_writable_ms\":207,\"client\\/success\":85}";
        String key = "TEST";

        HttpSample sample = parser.getSample(line, key);
        Assert.assertEquals(105, sample.getSummarizerMax());
        Assert.assertEquals(1, sample.getSummarizerMin());
        Assert.assertEquals(85, sample.getSummarizerSamples());
        Assert.assertEquals((float) 0.0, sample.getSummarizerErrors());
        Assert.assertEquals(3, sample.getDuration());
        Assert.assertEquals(key, sample.getUri());
    }




}
