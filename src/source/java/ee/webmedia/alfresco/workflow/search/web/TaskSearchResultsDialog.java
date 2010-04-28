package ee.webmedia.alfresco.workflow.search.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;

/**
 * Task search results dialog bean.
 * 
 * @author Erko Hansar
 */
public class TaskSearchResultsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<TaskInfo> tasks;
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    @Override
    public String cancel() {
        tasks = null;
        return super.cancel();
    }
    
    @Override
    public Object getActionsContext() {
        return null; 
    }
    
    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("taskList");
    }

    public void setup(List<TaskInfo> tasks) {
        this.tasks = tasks;
    }

    public List<TaskInfo> getTasks() {
        return tasks;
    }

}
