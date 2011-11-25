package ee.webmedia.alfresco.document.search.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class UserWorkingDocumentsSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_myWorkingDocuments");
    }

    @Override
    public void restored() {
        documents = getDocumentSearchService().searchInProcessUserDocuments();
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/user-working-documents-list-dialog-columns.jsp";
    }
}
