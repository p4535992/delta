package ee.webmedia.alfresco.document.search.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
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
        List<Document> docs = getDocumentSearchService().searchInProcessUserDocuments();
        Collections.sort(docs, new Comparator<Document>() {

            @Override
            public int compare(Document o1, Document o2) {
                // Created cannot be null for working documents
                return o2.getCreated().compareTo(o1.getCreated());
            }
        });
        documents = docs;
    }

    @Override
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/user-working-documents-list-dialog-columns.jsp";
    }
}