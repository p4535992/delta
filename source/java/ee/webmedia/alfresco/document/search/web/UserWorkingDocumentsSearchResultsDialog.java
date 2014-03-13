package ee.webmedia.alfresco.document.search.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

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
}
