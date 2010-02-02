package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.utils.MessageUtil;


public class UnsentDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    
    public void setup(ActionEvent event) {
        documents = getDocumentSearchService().searchRecipientFinishedDocuments();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_unsent_list");
    }
}
