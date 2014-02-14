package ee.webmedia.alfresco.doclist.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

/**
 * Service to get consolidated list of documents and hierarchy.
 * 
 * @author Priit Pikk
 */

public interface DocumentListService {

    String BEAN_NAME = "DocumentListService";

    Map<QName, Serializable> exportCsv(NodeRef rootRef, NodeRef reportsSpaceRef);

    long createNewYearBasedVolumes();

    long closeAllOpenExpiredVolumes();

    /**
     * Update fields that contain documents count
     * 
     * @return number of documents in documentList
     */
    long updateDocCounters();

    long updateDocCounters(NodeRef functionsRoot);

    /**
     * Get all document nodeRefs and all case/volume/series/function nodeRefs in that particular order, so this list could be used to delete the nodes in that order.
     * 
     * @param functionsRoot
     * @return
     */
    Pair<List<NodeRef>, List<NodeRef>> getAllDocumentAndStructureRefs(NodeRef functionsRoot);

    String getDisplayPath(NodeRef nodeRef, boolean showLeaf);

}
