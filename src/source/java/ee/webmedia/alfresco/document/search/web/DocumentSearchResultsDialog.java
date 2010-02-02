package ee.webmedia.alfresco.document.search.web;

import java.util.List;

import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public void setup(List<Document> documents) {
        this.documents = documents;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_results");
    }

}
