package ee.webmedia.alfresco.adr.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Fix bug that for documents that were moved between different stores and having been sent to
 * adr from initial location, after relocating adr syncronisation didn't delete the document referring to old location.
 * So adr has two (or more) copies of the same document, the older ones not having corresponding
 * document in Delta.
 * Updater adds adrDeletedDocument nodes for all adrDeletedDocument.noderef.id combined with all existing active and archive stores,
 * excluding the store that is already referenced by adrDeletedDocument.noderef.storeRef, in case such document doesn't exist.
 * These adrDeletedDocuments refer to all possible nodeRefs for given node id, so if it has ever been moved between stores
 * and has obsolete copy of it in adr, the adr copy will be deleted during next update.
 * There is no need to check if referred document actually exists and if it is supposed to be sent to adr; this check is
 * done upon synchronization, so documents that actually exist are not deleted during synchronization.
 * See CL task 215716 for details.
 * 
 * @author Riina Tens
 */
public class AddAdrDeletedDocForNotExistingDoc extends AbstractNodeUpdater {

    NodeRef adrDeletedDocumentsRoot;
    Set<StoreRef> storeRefs;

    @Override
    protected void executeInternal() throws Throwable {
        if (isEnabled()) {
            adrDeletedDocumentsRoot = generalService.getNodeRef(AdrModel.Repo.ADR_DELETED_DOCUMENTS);
            storeRefs = generalService.getAllWithArchivalsStoreRefs();
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT);
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> adrDeletedDocProps = nodeService.getProperties(nodeRef);
        String docRefStr = (String) adrDeletedDocProps.get(AdrModel.Props.NODEREF);
        NodeRef docRef = null;
        try {
            if (docRefStr != null) {
                docRef = new NodeRef(docRefStr);
            }
        } catch (AlfrescoRuntimeException e) {
            // if docRef doesn't evaluate to nodeRef, return corresponding log message
        }
        if (docRef == null) {
            return new String[] { "Skipping, invalid document ref: " + docRefStr };
        }
        String docNodeId = docRef.getId();
        List<String> potentialNodeRefs = new ArrayList<String>();
        for (StoreRef storeRef : storeRefs) {
            if (storeRef.equals(docRef.getStoreRef())) {
                continue;
            }
            NodeRef potentialNodeRef = new NodeRef(storeRef, docNodeId);
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            String potentialNodeRefStr = potentialNodeRef.toString();
            props.put(AdrModel.Props.NODEREF, potentialNodeRefStr);
            props.put(AdrModel.Props.REG_NUMBER, adrDeletedDocProps.get(AdrModel.Props.REG_NUMBER));
            props.put(AdrModel.Props.REG_DATE_TIME, adrDeletedDocProps.get(AdrModel.Props.REG_DATE_TIME));
            // update deleted time to current time, so next synchronization will detect the deletion
            props.put(AdrModel.Props.DELETED_DATE_TIME, new Date());
            NodeRef deletedDocRef = nodeService.createNode(adrDeletedDocumentsRoot, AdrModel.Types.ADR_DELETED_DOCUMENT, AdrModel.Types.ADR_DELETED_DOCUMENT,
                    AdrModel.Types.ADR_DELETED_DOCUMENT, props).getChildRef();
            potentialNodeRefs.add(deletedDocRef.toString() + ": " + potentialNodeRefStr);
        }

        return new String[] { TextUtil.joinNonBlankStrings(potentialNodeRefs, ";") };

    }
}
