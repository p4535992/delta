package ee.webmedia.alfresco.log.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.EscapingCSVExporter;
import ee.webmedia.alfresco.simdhs.RichListDataReader;

/**
 * Dialog for performing log entries search and displaying results.
 */
public class ApplicationLogListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "ApplicationLogListDialog";

    private LogEntryDataProvider logEntries;

    public void search(LogFilter filter) {
        logEntries = new LogEntryDataProvider(filter);
    }

    public LogEntryDataProvider getLogEntries() {
        return logEntries;
    }

    public void exportCsv(@SuppressWarnings("unused") ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new EscapingCSVExporter(dataReader);
        exporter.export("applogList");
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        logEntries = null;
        return null;
    }

    @Override
    public void clean() {
        logEntries = null;
    }
}
