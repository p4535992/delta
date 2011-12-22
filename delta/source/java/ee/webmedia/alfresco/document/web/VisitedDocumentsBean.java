package ee.webmedia.alfresco.document.web;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;

/**
 * Maintains a list of documents which have been opened since last document search or quick-search.
 * That list is used by document search results and quick search results dialogs, that when user goes back to one of these dialogs, then all documents that are on the list are
 * refreshed (or deleted if they don't exist any more).
 * 
 * @author Alar Kvell
 */
public class VisitedDocumentsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "VisitedDocumentsBean";

    private Set<NodeRef> visitedDocuments;

    public Set<NodeRef> getVisitedDocuments() {
        if (visitedDocuments == null) {
            visitedDocuments = new HashSet<NodeRef>();
        }
        return visitedDocuments;
    }

    public void clearVisitedDocuments() {
        visitedDocuments = null;
    }

    public void resetVisitedDocuments(List<Document> documents) {
        for (NodeRef visitedDoc : getVisitedDocuments()) {
            boolean found = false;
            // Remove all matching entries
            for (Iterator<Document> i = documents.iterator(); i.hasNext();) {
                Document document = i.next();
                if (document != null && visitedDoc.equals(document.getNodeRef())) {
                    found = true;
                    i.remove();
                }
            }
            if (found && BeanHelper.getNodeService().exists(visitedDoc)) {
                QName resultType = BeanHelper.getNodeService().getType(visitedDoc);
                Document newDocument;
                if (!BeanHelper.getDictionaryService().isSubClass(resultType, DocumentCommonModel.Types.DOCUMENT)) {
                    newDocument = new FakeDocument(visitedDoc);
                } else {
                    newDocument = BeanHelper.getDocumentService().getDocumentByNodeRef(visitedDoc);
                }
                documents.add(newDocument);
            }
        }
        clearVisitedDocuments();
    }

}
