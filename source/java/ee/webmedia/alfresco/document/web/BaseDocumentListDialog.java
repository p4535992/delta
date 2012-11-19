package ee.webmedia.alfresco.document.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * @author Alar Kvell
 */
public abstract class BaseDocumentListDialog extends BaseLimitedListDialog {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private transient DocumentSearchService documentSearchService;

    private transient UIRichList richList;
    private transient UIPanel panel;

    protected List<Document> documents;
    private Map<NodeRef, Boolean> listCheckboxes = new HashMap<NodeRef, Boolean>();

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        clearRichList();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
                     // then silently ignore it and stay on the same page
    }

    @Override
    public String cancel() {
        documents = null;
        clearRichList();
        listCheckboxes = new HashMap<NodeRef, Boolean>();
        return super.cancel();
    }

    public boolean isShowCheckboxes() {
        return false;
    }

    @Override
    public Object getActionsContext() {
        return null; // since we are using actions, but not action context, we don't need instance of NavigationBean that is used in the overloadable method
    }

    public abstract String getListTitle();

    public void getAllDocsWithoutLimit(ActionEvent event) {
        restored();
    }

    public String getInfoMessage() {
        return ""; // Subclasses can override if necessary
    }

    public boolean isInfoMessageVisible() {
        return getInfoMessage().length() > 0;
    }

    public boolean isShowOrgStructColumn() {
        return false;
    }

    public boolean isShowComplienceDateColumn() {
        return true;
    }

    /**
     * Returns the file name to import as document list columns
     * Subclasses can override if necessary.
     * 
     * @return String path to JSP file
     */
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp";
    }

    public String getInitialSortColumn() {
        return null;
    }

    // START: getters / setters

    public List<Document> getDocuments() {
        return documents;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    public UIRichList getRichList() {
        return richList;
    }

    protected void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
    }

    public Map<NodeRef, Boolean> getListCheckboxes() {
        return listCheckboxes;
    }

    public void setListCheckboxes(Map<NodeRef, Boolean> listCheckboxes) {
        this.listCheckboxes = listCheckboxes;
    }

    public void setPanel(UIPanel panel) {
        this.panel = panel;
    }

    public UIPanel getPanel() {
        if (panel == null) {
            panel = new UIPanel();
        }
        return panel;
    }

    public boolean isContainsCases() {
        return false;
    }

    // END: getters / setters

}
