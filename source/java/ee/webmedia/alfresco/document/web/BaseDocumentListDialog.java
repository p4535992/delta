package ee.webmedia.alfresco.document.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * @author Alar Kvell
 */
public abstract class BaseDocumentListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private transient DocumentSearchService documentSearchService;

    protected List<Document> documents;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
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
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null; // since we are using actions, but not action context, we don't need instance of NavigationBean that is used in the overloadable method
    }

    public abstract String getListTitle();

    public String getInfoMessage() {
        return ""; // Subclasses can override if necessary
    }

    public boolean isInfoMessageVisible() {
        return getInfoMessage().length() > 0;
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

    // END: getters / setters

}
