package ee.webmedia.alfresco.doclist.service;

import java.io.OutputStream;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;

/**
 * Service to get consolidated list of documents and hierarchy.
 * 
 * @author Priit Pikk
 */

public interface DocumentListService {

    String BEAN_NAME = "DocumentListService";

    void getExportCsv(OutputStream outputStream, NodeRef rootRef);

    long createNewYearBasedVolumes();

    long closeAllOpenExpiredVolumes();

    /**
     * Update fields that contain documents count
     * 
     * @return number of documents in documentList
     */
    long updateDocCounters();

    long updateDocCounters(NodeRef functionsRoot);

    Pair<List<NodeRef>, Long> getAllDocumentAndCaseRefs();

    String getDisplayPath(NodeRef nodeRef, boolean showLeaf);

}
