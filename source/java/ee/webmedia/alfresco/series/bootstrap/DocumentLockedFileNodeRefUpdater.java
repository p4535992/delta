package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * This updater removes lockedFile ref for documents with broken lockedFile ref.
 */
public class DocumentLockedFileNodeRefUpdater extends AbstractNodeUpdater {
    

    private boolean executeInBackground;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
    	 List<String> queryParts = new ArrayList<String>(2);
         queryParts.add(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
         queryParts.add(SearchUtil.generatePropertyNotNullQuery(FileModel.Props.LOCKED_FILE_NODEREF));
         String query = joinQueryPartsAnd(queryParts);

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> logInfo = new ArrayList<String>();

        fixLockFileRef(nodeRef, logInfo);

        return logInfo.toArray(new String[logInfo.size()]);
    }

    private void fixLockFileRef(NodeRef nodeRef, List<String> logInfo) {
    	NodeRef lockedFileNodeRef = (NodeRef)nodeService.getProperty(nodeRef, FileModel.Props.LOCKED_FILE_NODEREF);
        if (!nodeService.exists(lockedFileNodeRef)) {
            logInfo.add("lockedFileRefIsBroken");
            nodeService.removeProperty(nodeRef, FileModel.Props.LOCKED_FILE_NODEREF);
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
