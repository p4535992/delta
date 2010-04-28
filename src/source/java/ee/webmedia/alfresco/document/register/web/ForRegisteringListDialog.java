package ee.webmedia.alfresco.document.register.web;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

/**
 * List dialog for document available for registering.
 * 
 * @author Romet Aidla
 */
public class ForRegisteringListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 0L;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentSearchService().searchDocumentsForRegistering();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_register_docs");
    }
}
