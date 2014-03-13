package ee.webmedia.alfresco.workflow.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.document.model.CreatedOrRegistratedDateComparator;
import ee.webmedia.alfresco.document.web.BaseLimitedListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;

/**
 * Task search results dialog bean.
 */
public class TaskSearchResultsDialog extends BaseLimitedListDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "TaskSearchResultsDialog";

    private List<TaskInfo> tasks;
    private Node filter;
    private UIRichList richList;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    private void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
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

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
    }

    private void doInitialSearch() {
        tasks = setLimited(getDocumentSearchService().searchTasks(filter, getLimit()));
        clearRichList();
        Collections.sort(tasks, CreatedOrRegistratedDateComparator.getComparator());
    }

    public void exportAsCsv(@SuppressWarnings("unused") ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("taskList");

        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    public void setup(Node filter) {
        this.filter = filter;
        resetLimit(true);
        doInitialSearch();
    }

    public List<TaskInfo> getTasks() {
        return tasks;
    }

    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    public UIRichList getRichList() {
        return richList;
    }

}
