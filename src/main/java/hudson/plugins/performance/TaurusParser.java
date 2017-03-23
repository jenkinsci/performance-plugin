package hudson.plugins.performance;

import hudson.Extension;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

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
    PerformanceReport parse(File reportFile) throws Exception {
        return isXMLFileType(reportFile) ? readFromXML(reportFile) : readFromCSV(reportFile);
    }

    private boolean isXMLFileType(File reportFile) {
        return FilenameUtils.getExtension(reportFile.getName()).toLowerCase().contains("xml");
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
                TaurusStatusReport statusReport = getTaurusStatusReport((Element) nNode);
                statusReport.setLabel(((Element) nNode).getAttribute("label"));
                report.addSample(statusReport, false);
            } else {
                TaurusStatusReport statusReport = getTaurusStatusReport((Element) nNode);
                statusReport.setLabel(TaurusStatusReport.DEFAULT_TAURUS_LABEL);
                report.addSample(statusReport, true);
            }
        }

        return report;
    }

    private TaurusStatusReport getTaurusStatusReport(Element group) {
        final TaurusStatusReport report = new TaurusStatusReport();

        report.setBytes(Long.valueOf(((Element) group.getElementsByTagName("bytes").item(0)).getAttribute("value")));
        report.setFail(Integer.valueOf(((Element) group.getElementsByTagName("fail").item(0)).getAttribute("value")));
        report.setSucc(Integer.valueOf(((Element) group.getElementsByTagName("succ").item(0)).getAttribute("value")));
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

    private PerformanceReport readFromCSV(File reportFile) throws Exception {
        final PerformanceReport report = new PerformanceReport();
        report.setReportFileName(reportFile.getName());

        final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
        try {
            final List<String> header = readHeader(reader.readLine(), ",");
            String line = reader.readLine();
            if (line != null) {
                report.addSample(getTaurusStatusReport(header, line), true);
                line = reader.readLine();
            }
            while (line != null) {
                report.addSample(getTaurusStatusReport(header, line), false);
                line = reader.readLine();
            }
            return report;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private List<String> readHeader(String headerLine, String separator) {
        final List<String> header = new ArrayList<String>();
        for (String v : headerLine.split(separator)) {
            header.add(v.toLowerCase());
        }
        return header;
    }

    private TaurusStatusReport getTaurusStatusReport(List<String> header, String line) {
        final TaurusStatusReport report = new TaurusStatusReport();

        String[] values = line.split(",");

        int labelIndex = header.indexOf("label");
        if (values.length < (labelIndex - 1) && !values[labelIndex].isEmpty()) {
            report.setLabel(values[labelIndex]);
        } else {
            report.setLabel(TaurusStatusReport.DEFAULT_TAURUS_LABEL);
        }

        report.setBytes(Long.valueOf(values[header.indexOf("bytes")]));
        report.setFail(Integer.valueOf(values[header.indexOf("fail")]));
        report.setSucc(Integer.valueOf(values[header.indexOf("succ")]));
        report.setAverageResponseTime(Double.valueOf(values[header.indexOf("avg_rt")]) * 1000); // to ms

        report.setPerc0(Double.valueOf(values[header.indexOf("perc_0.0")]) * 1000); // to ms
        report.setPerc50(Double.valueOf(values[header.indexOf("perc_50.0")]) * 1000); // to ms
        report.setPerc90(Double.valueOf(values[header.indexOf("perc_90.0")]) * 1000); // to ms
        report.setPerc100(Double.valueOf(values[header.indexOf("perc_100.0")]) * 1000); // to ms

        return report;
    }
}
