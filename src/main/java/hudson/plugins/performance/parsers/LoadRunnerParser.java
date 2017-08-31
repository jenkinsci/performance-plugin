package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/** Parser for LoadRunner Analysis results stored in an MS Access database (*.mdb file).
 * 
 * Reference https://community.saas.hpe.com/t5/Performance-Center-Practitioners/Regarding-Event-meter-table-in-LoadRunner-session-MS-ACCESS/td-p/566738
 */
public class LoadRunnerParser extends AbstractParser {

    private String resultQuery = 
        "select "+
        "    cast(([Start Time] - [Time Zone] + e.[End Time] - e.Value)*1000 as decimal) as timeStamp, "+
        "    cast(e.Value*1000 as decimal) as elapsed, "+
        "    [Event Name] as label, "+
        "    case [Transaction End Status] when 'Pass' then 'true' end as success "+
        "from Event_meter e "+
        "join Event_map on Event_map.[Event ID] = e.[Event ID] "+
        "join TransactionEndStatus on TransactionEndStatus.Status1 = e.Status1 "+
        "join Result on Result.[Result ID] = e.[Result ID]"+
        "where [Event Type] = 'Transaction'";

    @DataBoundConstructor
    public LoadRunnerParser(String glob) {
        super(glob);
    }

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "LoadRunner";
        }
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.mdb";
    }

    protected String jdbcUrlForFile(File reportFile) throws ClassNotFoundException {
        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        return String.format("jdbc:ucanaccess://%s;mirrorFolder=java.io.tmpdir;immediatelyReleaseResources=true", reportFile.getAbsolutePath());
    }

    protected HttpSample getSample(ResultSet res) throws SQLException {
        HttpSample sample = new HttpSample();
        sample.setDate(new Date(res.getLong(1)));
        sample.setDuration(res.getLong(2));
        sample.setUri(res.getString(3));
        sample.setSuccessful(Boolean.parseBoolean(res.getString(4)));
        return sample;
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        final PerformanceReport report = new PerformanceReport();
        report.setExcludeResponseTime(excludeResponseTime);
        report.setReportFileName(reportFile.getName());

        try (Connection con = DriverManager.getConnection(jdbcUrlForFile(reportFile));
            Statement stmt = con.createStatement(); 
            ResultSet res = stmt.executeQuery(getResultQuery())) {

            while (res.next()) {
                report.addSample(getSample(res));
            }
        }
        return report;
    }

    protected String getResultQuery() {
        return resultQuery;
    }

    protected void setResultQuery(String resultQuery) {
        this.resultQuery = resultQuery;
    }
}
