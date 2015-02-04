package ee.webmedia.alfresco.workflow.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.BaseLimitedListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.workflow.web.TaskInfoListDataProvider;

/**
 * Task search results dialog bean.
 */
public class TaskSearchResultsDialog extends BaseLimitedListDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "TaskSearchResultsDialog";

    private TaskInfoListDataProvider tasks;
    private Node filter;

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
        BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
                doInitialSearch();
                return null;
            }
        }, false);

    }

    @Override
    public void clean() {
        tasks = null;
        filter = null;
    }

    private void doInitialSearch() {
        tasks = new TaskInfoListDataProvider(setLimited(getDocumentSearchService().searchTaskRefs(filter, AuthenticationUtil.getRunAsUser(), getLimit())));
        clearRichList();
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

    public TaskInfoListDataProvider getTasks() {
        return tasks;
    }

    public void setRichList(UIRichList richList) {
        getJsfBindingHelper().addBinding(getRichListBindingName(), richList);
    }

    public UIRichList getRichList() {
        return (UIRichList) getJsfBindingHelper().getComponentBinding(getRichListBindingName());
    }

}
