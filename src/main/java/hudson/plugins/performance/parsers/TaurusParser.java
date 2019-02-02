package hudson.plugins.performance.parsers;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hudson.Extension;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;

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

        Node urlNode = doc.getElementsByTagName("ReportURL").item(0);
        if (urlNode != null) {
            reportURL = urlNode.getTextContent();
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

        report.setBytes(Long.valueOf(getValueAttribute("bytes", group)));
        report.setFail(Integer.valueOf(getValueAttribute("fail", group)));
        report.setSucc(Integer.valueOf(getValueAttribute("succ", group)));
        if (group.getElementsByTagName("throughput").getLength() > 0) {
            report.setThroughput(Long.valueOf(getValueAttribute("throughput", group)));
        }
        report.setAverageResponseTime(Double.valueOf(getValueAttribute("avg_rt", group)) * 1000); // to ms

        NodeList perc = group.getElementsByTagName("perc");
        for (int i = 0; i < perc.getLength(); i++) {
            Node nNode = perc.item(i);
            String attributeParam = ((Element) nNode).getAttribute("param");
            Double valueInMs = Double.valueOf(((Element) nNode).getAttribute("value")) * 1000;
            
            if ("50.0".equals(attributeParam)) {
                report.setPerc50(valueInMs); // to ms
            } else if ("90.0".equals(attributeParam)) {
                report.setPerc90(valueInMs); // to ms
            } else if ("0.0".equals(attributeParam)) {
                report.setPerc0(valueInMs); // to ms
            } else if ("100.0".equals(attributeParam)) {
                report.setPerc100(valueInMs); // to ms
            }
        }
        return report;
    }

    private String getValueAttribute(String elementName, Element group) {
        return ((Element) group.getElementsByTagName(elementName).item(0)).getAttribute("value");
    }
}
