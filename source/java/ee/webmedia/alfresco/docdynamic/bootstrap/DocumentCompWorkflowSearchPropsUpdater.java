package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Updates documents' searchableHasAllFinishedCompoundWorkflows and searchableHasStartedCompoundWorkflows property (calculation of the properties has changed).
 * NB! Updating searchableHasAllFinishedCompoundWorkflows is not needed, when it is quaranteed that DocumentUpdater also runs
 * (for example when migrating from 2.* to 3.*), because DocumentUpdater calls the same (changed) functionality.
 * During 2.* -> 3.* migration this updater should be merged with DocumentUpdater.
 */
public class DocumentCompWorkflowSearchPropsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(docRef);
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();

        String hasAllFinishedCompoundWorkflowsUpdaterLog = DocumentUpdater.updateHasAllFinishedCompoundWorkflows(docRef, origProps, updatedProps, BeanHelper.getWorkflowService());
        String hasStartedCompoundWorkflowsUpdaterLog = updateHasStartedCompoundWorkflows(docRef, origProps, updatedProps);

        if (!updatedProps.isEmpty()) {
            nodeService.addProperties(docRef, updatedProps);
        }
        return new String[] { hasAllFinishedCompoundWorkflowsUpdaterLog, hasStartedCompoundWorkflowsUpdaterLog };
    }

    private String updateHasStartedCompoundWorkflows(NodeRef docRef, Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps) {
        Serializable origValueReal = origProps.get(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS);
        boolean origValue = Boolean.TRUE.equals(origValueReal);
        boolean newValue = BeanHelper.getWorkflowService().hasStartedCompoundWorkflows(docRef);
        if (origValue != newValue) {
            updatedProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, newValue);
            return ObjectUtils.toString(origValueReal, "null") + ", " + newValue;
        }
        return "";
    }

}
