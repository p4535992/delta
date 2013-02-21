package ee.webmedia.alfresco.document.bootstrap;

import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;

/**
 * Read nodeRefs that have been marked by ConvertToDynamicDocumentsUpdater for deleting and delete them.
 * BatchSize should always be 1 to minimize errors on deleting.
 * This is not critical updater, nodes that cannot be deleted are skipped, their existence shouldn't affect application's behavior.
 * 
 * @author Riina Tens
 */
public class DeleteNodesAfterConvertToDynamicDocumentUpdater extends AbstractNodeUpdater {

    private ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("getNodeLoadingResultSet not used in CleanupAfterConvertToDynamicDocumentUpdater!");
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return loadNodesFromFile(convertToDynamicDocumentsUpdater.loadNodesToDeleteFile(), false);
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.deleteNode(nodeRef);
        return null;
    }

    public void setConvertToDynamicDocumentsUpdater(ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater) {
        this.convertToDynamicDocumentsUpdater = convertToDynamicDocumentsUpdater;
    }

}
