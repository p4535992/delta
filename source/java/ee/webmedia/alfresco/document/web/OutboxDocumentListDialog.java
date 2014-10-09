package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;

public class OutboxDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "OutboxDocumentListDialog";

    /** @param event from/menu */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getDocumentSearchService().searchDocumentsInOutbox());
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_outbox");
    }

}
