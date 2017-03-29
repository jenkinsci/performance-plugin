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

    @DataBoundConstructor
    public TaurusParser(String glob) {
        super(glob);
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
        final PerformanceReport report = new PerformanceReport();
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

        NodeList nList = doc.getElementsByTagName("Group");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (!((Element) nNode).getAttribute("label").isEmpty()) {
                TaurusFinalStats statusReport = getTaurusFinalStats((Element) nNode);
                statusReport.setLabel(((Element) nNode).getAttribute("label"));
                report.addSample(statusReport, false);
            } else {
                TaurusFinalStats statusReport = getTaurusFinalStats((Element) nNode);
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
