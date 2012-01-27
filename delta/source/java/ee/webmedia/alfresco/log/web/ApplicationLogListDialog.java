package ee.webmedia.alfresco.log.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.log.model.SystemLog;

/**
 * Dialog for performing log entries search and displaying results.
 * 
 * @author Martti Tamm
 */
public class ApplicationLogListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "ApplicationLogListDialog";

    private List<SystemLog> logEntries;

    public void search(List<SystemLog> entries) {
        logEntries = entries;
    }

    public List<SystemLog> getLogEntries() {
        return logEntries;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        logEntries = null;
        return null;
    }
}
