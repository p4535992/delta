package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * List dialog for documents where user is participant in discussions.
 */
public class DiscussionDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DiscussionDocumentListDialog";

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getDocumentSearchService().searchDiscussionDocuments(), true, DOC_PROPS_TO_LOAD);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_discussion_docs");
    }
}
