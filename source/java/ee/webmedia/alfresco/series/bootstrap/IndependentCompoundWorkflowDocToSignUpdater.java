package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * This updater fixes docToSign refs for Independent compound workflows with broken docToSign ref.
 */
public class IndependentCompoundWorkflowDocToSignUpdater extends AbstractNodeUpdater {
    

    private boolean executeInBackground;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
    	 List<String> queryParts = new ArrayList<String>(2);
         queryParts.add(generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW));
         queryParts.add(SearchUtil.generatePropertyExactQuery(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.INDEPENDENT_WORKFLOW.name()));
         queryParts.add(SearchUtil.generatePropertyNotNullQuery(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN));
         String query = joinQueryPartsAnd(queryParts);

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> logInfo = new ArrayList<String>();

        fixDocumentsToSignRef(nodeRef, logInfo);

        return logInfo.toArray(new String[logInfo.size()]);
    }

    private void fixDocumentsToSignRef(NodeRef nodeRef, List<String> logInfo) {
    	List<NodeRef> documentsToSign = (List<NodeRef>) nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN);
        if (documentsToSign != null) {
            boolean needToUpdate = false;
            List<NodeRef> documentsToSignUpdate = new ArrayList<NodeRef>();
        	for (NodeRef docRef: documentsToSign) {
            	if (!nodeService.exists(docRef)) {
            		needToUpdate = true;
		            logInfo.add("docToSignRefIsBroken");
		            
		            NodeRef fixedNodeRef = new NodeRef(generalService.getArchivalsStoreRef(), docRef.getId());
		            if (nodeService.exists(fixedNodeRef)) {
		            	documentsToSignUpdate.add(fixedNodeRef);
		            }
	        	} else {
	        		documentsToSignUpdate.add(docRef);
	        	}
            }
        	if (needToUpdate) {
        		nodeService.setProperty(nodeRef, WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, (Serializable)documentsToSignUpdate);
        	}
        }
    }


    @Override
    protected void executeInternal() throws Throwable {
        if (!isEnabled()) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        if (executeInBackground) {
            super.executeUpdaterInBackground();
        } else {
            super.executeUpdater();
        }
    }

    public void setExecuteInBackground(boolean executeInBackground) {
        this.executeInBackground = executeInBackground;
    }

}
