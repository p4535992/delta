package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Ats Uiboupin
 */
public class OutboxDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    /** @param event from/menu */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentSearchService().searchDocumentsInOutbox();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_outbox");
    }

}
