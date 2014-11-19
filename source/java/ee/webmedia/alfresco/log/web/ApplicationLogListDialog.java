package ee.webmedia.alfresco.log.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.EscapingCSVExporter;
import ee.webmedia.alfresco.simdhs.RichListDataReader;

/**
 * Dialog for performing log entries search and displaying results.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class ApplicationLogListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "ApplicationLogListDialog";

    private List<LogEntry> logEntries;

    public void search(List<LogEntry> entries) {
        logEntries = entries;
    }

    public List<LogEntry> getLogEntries() {
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
}
