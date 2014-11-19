package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;

/**
 * Maintains a list of documents which have been opened since last document search or quick-search.
 * That list is used by document search results and quick search results dialogs, that when user goes back to one of these dialogs, then all documents that are on the list are
 * refreshed (or deleted if they don't exist any more).
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
            boolean firstFind = false;
            Document newDocument = null;
            // Remove all matching entries
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                if (document != null && visitedDoc.equals(document.getNodeRef())) {
                    if (!firstFind) {
                        firstFind = true;
                        if (getNodeService().exists(visitedDoc)) {
                            QName resultType = getNodeService().getType(visitedDoc);
                            if (!getDictionaryService().isSubClass(resultType, DocumentCommonModel.Types.DOCUMENT)) {
                                newDocument = new FakeDocument(visitedDoc);
                            } else {
                                newDocument = getDocumentService().getDocumentByNodeRef(visitedDoc);
                            }
                        }
                    }
                    documents.set(i, newDocument);
                }
            }
        }
        documents.remove(null);
        clearVisitedDocuments();
    }
}
