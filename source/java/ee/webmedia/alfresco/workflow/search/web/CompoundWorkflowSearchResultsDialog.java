<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.document.web.BaseLimitedListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Compount workflow search results dialog bean.
 * 
 * @author Keit Tehvan
 */
public class CompoundWorkflowSearchResultsDialog extends BaseLimitedListDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowSearchResultsDialog";

    private List<CompoundWorkflow> workflows;
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
        workflows = null;
        return super.cancel();
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
    }

    public void doInitialSearch() {
        workflows = setLimited(getDocumentSearchService().searchCompoundWorkflows(filter, getLimit()));
        clearRichList();
    }

    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    public UIRichList getRichList() {
        return richList;
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public void exportAsCsv(@SuppressWarnings("unused") ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("workflowList");

        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    public void setup(Node filter) {
        this.filter = filter;
        resetLimit(true);
        doInitialSearch();
    }

    public List<CompoundWorkflow> getWorkflows() {
        return workflows;
    }

}
=======
package ee.webmedia.alfresco.workflow.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.lucene.search.BooleanQuery;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.web.BaseLimitedListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

/**
 * Compount workflow search results dialog bean.
 */
public class CompoundWorkflowSearchResultsDialog extends BaseLimitedListDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowSearchResultsDialog";
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowSearchResultsDialog.class);

    private List<CompoundWorkflow> workflows;
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
        workflows = null;
        return super.cancel();
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
    }

    public void doInitialSearch() {
        try {
            workflows = setLimited(getDocumentSearchService().queryCompoundWorkflows(filter, getLimit()));
        } catch (BooleanQuery.TooManyClauses e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(filter.getProperties()));
            LOG.error("Compound workflow search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, BeanHelper.getNamespaceService()));
            workflows = setLimitedEmpty();
            MessageUtil.addErrorMessage("cw_search_toomanyclauses");
        }
        clearRichList();
    }

    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    public UIRichList getRichList() {
        return richList;
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public void exportAsCsv(@SuppressWarnings("unused") ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("workflowList");

        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    public void setup(Node filter) {
        this.filter = filter;
        resetLimit(true);
        doInitialSearch();
    }

    public List<CompoundWorkflow> getWorkflows() {
        return workflows;
    }

}
>>>>>>> develop-5.1
