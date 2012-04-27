package ee.webmedia.alfresco.workflow.search.web;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.CreatedOrRegistratedDateComparator;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;

/**
 * Task search results dialog bean.
 * 
 * @author Erko Hansar
 */
public class TaskSearchResultsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<TaskInfo> tasks;
    private Node filter;
    protected boolean documentListLimited = false;
    protected boolean temporarilyDisableLimiting = false;
    private UIRichList richList;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    public String getLimitedMessage() {
        return MessageUtil.getMessage("task_list_limited", BeanHelper.getDocumentSearchService().getResultsLimit());
    }

    public boolean isTaskListLimited() {
        return documentListLimited;
    }

    public void getAllTasksWithoutLimit(@SuppressWarnings("unused") ActionEvent event) {
        temporarilyDisableLimiting = true;
        restored();
        temporarilyDisableLimiting = false;
    }

    private void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
    }

    @Override
    public String cancel() {
        documentListLimited = false;
        temporarilyDisableLimiting = false;
        tasks = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        Pair<List<TaskInfo>, Boolean> searchTasks = BeanHelper.getDocumentSearchService().searchTasks(filter, !temporarilyDisableLimiting);
        tasks = searchTasks.getFirst();
        clearRichList();
        documentListLimited = searchTasks.getSecond();
        Collections.sort(tasks, CreatedOrRegistratedDateComparator.getComparator());
    }

    public void exportAsCsv(@SuppressWarnings("unused") ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("taskList");

        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    public void setup(Node filter) {
        this.filter = filter;
        restored();
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
