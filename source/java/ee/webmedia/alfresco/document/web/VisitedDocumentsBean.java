package ee.webmedia.alfresco.document.web;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;

/**
 * Maintains a list of documents which have been opened since last document search or quick-search.
 * That list is used by document search results and quick search results dialogs, that when user goes back to one of these dialogs, then all documents that are on the list are
 * refreshed (or deleted if they don't exist any more).
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

    public void resetVisitedDocuments(DocumentListDataProvider documentsProvider) {
        documentsProvider.reloadRows(getVisitedDocuments());
        clearVisitedDocuments();
    }
}
