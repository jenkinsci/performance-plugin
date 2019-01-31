package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.data.TaurusFinalStats;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Parser for Taurus
 */
public class TaurusParser extends AbstractParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "Taurus";
        }
    }

    public TaurusParser(String glob, String percentiles) {
        super(glob, percentiles, PerformanceReport.INCLUDE_ALL);
    }
    
    @DataBoundConstructor
    public TaurusParser(String glob, String percentiles, String filterRegex) {
        super(glob, percentiles, filterRegex);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.xml";
    }

    @Override
    protected PerformanceReport parse(File reportFile) throws Exception {
        return readFromXML(reportFile);
    }

    private PerformanceReport readFromXML(File reportFile) throws Exception {
        final PerformanceReport report = createPerformanceReport();
        report.setExcludeResponseTime(excludeResponseTime);
        report.setReportFileName(reportFile.getName());

        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(reportFile);
        doc.getDocumentElement().normalize();

        Node URLNode = doc.getElementsByTagName("ReportURL").item(0);
        if (URLNode != null) {
            reportURL = URLNode.getTextContent();
        }

        Node testDurationNode = doc.getElementsByTagName("TestDuration").item(0);
        Double testDuration = null;
        if (testDurationNode != null) {
            testDuration = Double.parseDouble(testDurationNode.getTextContent()) * 1000; // to ms
        }

        NodeList nList = doc.getElementsByTagName("Group");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            TaurusFinalStats statusReport = getTaurusFinalStats((Element) nNode);
            statusReport.setTestDuration(testDuration);
            if (!((Element) nNode).getAttribute("label").isEmpty()) {
                statusReport.setLabel(((Element) nNode).getAttribute("label"));
                report.addSample(statusReport, false);
            } else {
                statusReport.setLabel(TaurusFinalStats.DEFAULT_TAURUS_LABEL);
                report.addSample(statusReport, true);
            }
        }

        return report;
    }

    private TaurusFinalStats getTaurusFinalStats(Element group) {
        final TaurusFinalStats report = new TaurusFinalStats();

        report.setBytes(Long.valueOf(((Element) group.getElementsByTagName("bytes").item(0)).getAttribute("value")));
        report.setFail(Integer.valueOf(((Element) group.getElementsByTagName("fail").item(0)).getAttribute("value")));
        report.setSucc(Integer.valueOf(((Element) group.getElementsByTagName("succ").item(0)).getAttribute("value")));
        report.setThroughput(Long.valueOf(((Element) group.getElementsByTagName("throughput").item(0)).getAttribute("value")));
        if (group.getElementsByTagName("throughput").getLength() > 0) {
            report.setThroughput(Long.valueOf(((Element) group.getElementsByTagName("throughput").item(0)).getAttribute("value")));
        }
        report.setAverageResponseTime(Double.valueOf(((Element) group.getElementsByTagName("avg_rt").item(0)).getAttribute("value")) * 1000); // to ms

        NodeList perc = group.getElementsByTagName("perc");
        for (int i = 0; i < perc.getLength(); i++) {
            Node nNode = perc.item(i);
            if (((Element) nNode).getAttribute("param").equals("50.0")) {
                report.setPerc50(Double.valueOf(((Element) nNode).getAttribute("value")) * 1000); // to ms
            } else if (((Element) nNode).getAttribute("param").equals("90.0")) {
                report.setPerc90(Double.valueOf(((Element) nNode).getAttribute("value")) * 1000); // to ms
            } else if (((Element) nNode).getAttribute("param").equals("0.0")) {
                report.setPerc0(Double.valueOf(((Element) nNode).getAttribute("value")) * 1000); // to ms
            } else if (((Element) nNode).getAttribute("param").equals("100.0")) {
                report.setPerc100(Double.valueOf(((Element) nNode).getAttribute("value")) * 1000); // to ms
            }
        }


        return report;
    }
}
