package hudson.plugins.performance.details;

import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.plugins.performance.Messages;
import org.jfree.data.category.CategoryDataset;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Configures the trend graph of this plug-in.
 */
public class TrendReportDetail implements ModelObject,
        Iterable<TrendReportDetail.Row> {

    private AbstractProject<?, ?> project;
    private String filename;
    private CategoryDataset dataSet;

    public TrendReportDetail(final AbstractProject<?, ?> project,
                             final String pluginName, final StaplerRequest request, String filename,
                             CategoryDataset dataSet) {
        this.project = project;
        this.filename = filename;
        this.dataSet = dataSet;
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public String getFilename() {
        return filename;
    }

    public String getDisplayName() {
        return Messages.TrendReportDetail_DisplayName();
    }

    public Iterator<Row> iterator() {
        return new RowIterator();
    }

    public Iterator<Row> getIterator() {
        return iterator();
    }

    public List getColumnLabels() {
        return dataSet.getRowKeys();
    }

    public class RowIterator implements Iterator<Row> {

        private int entry = 0;

        public boolean hasNext() {
            return (entry < dataSet.getColumnCount());
        }

        public Row next() {
            return new Row(entry++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public class Row {

        int entry;

        public Row(int entry) {
            this.entry = entry;
        }

        public Object getLabel() {
            return dataSet.getColumnKey(entry);
        }

        public List getLabels() {
            return dataSet.getRowKeys();
        }

        public List<Number> getValues() {
            int count = dataSet.getRowCount();
            List<Number> list = new ArrayList<Number>(count);
            for (int i = 0; i < count; i++) {
                list.add(dataSet.getValue(dataSet.getRowKey(i),
                        dataSet.getColumnKey(entry)));
            }
            return list;
        }
    }
}
