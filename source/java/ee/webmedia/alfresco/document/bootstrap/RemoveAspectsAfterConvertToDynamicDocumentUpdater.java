<<<<<<< HEAD
package ee.webmedia.alfresco.document.bootstrap;

import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;

/**
 * Read nodeRefs that have been marked by ConvertToDynamicDocumentsUpdater and remove aspects with child nodes on them.
 * BatchSize should always be 1 to minimize errors on deleting child nodes (although child nodes should have been deleted
 * by DeleteNodesAfterConvertToDynamicDocumentUpdater before this updater runs).
 * This is not critical updater, nodes where aspects cannot be removed are skipped, existence of these aspects on nodes shouldn't affect application's behavior.
 * 
 * @author Riina Tens
 */
public class RemoveAspectsAfterConvertToDynamicDocumentUpdater extends AbstractNodeUpdater {

    private ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("getNodeLoadingResultSet not used in RemoveAspectsAfterConvertToDynamicDocumentUpdater!");
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return loadNodesFromFile(convertToDynamicDocumentsUpdater.loadNodesToRemoveAspectsFile(), false);
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        for (QName aspectToRemove : ConvertToDynamicDocumentsUpdater.ASPECTS_WITH_CHILD_ASSOCS) {
            nodeService.removeAspect(nodeRef, aspectToRemove);
        }
        return null;
    }

    public void setConvertToDynamicDocumentsUpdater(ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater) {
        this.convertToDynamicDocumentsUpdater = convertToDynamicDocumentsUpdater;
    }

}
=======
package ee.webmedia.alfresco.document.bootstrap;

import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;

/**
 * Read nodeRefs that have been marked by ConvertToDynamicDocumentsUpdater and remove aspects with child nodes on them.
 * BatchSize should always be 1 to minimize errors on deleting child nodes (although child nodes should have been deleted
 * by DeleteNodesAfterConvertToDynamicDocumentUpdater before this updater runs).
 * This is not critical updater, nodes where aspects cannot be removed are skipped, existence of these aspects on nodes shouldn't affect application's behavior.
 */
public class RemoveAspectsAfterConvertToDynamicDocumentUpdater extends AbstractNodeUpdater {

    private ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("getNodeLoadingResultSet not used in RemoveAspectsAfterConvertToDynamicDocumentUpdater!");
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        return loadNodesFromFile(convertToDynamicDocumentsUpdater.loadNodesToRemoveAspectsFile(), false);
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        for (QName aspectToRemove : ConvertToDynamicDocumentsUpdater.ASPECTS_WITH_CHILD_ASSOCS) {
            nodeService.removeAspect(nodeRef, aspectToRemove);
        }
        return null;
    }

    public void setConvertToDynamicDocumentsUpdater(ConvertToDynamicDocumentsUpdater convertToDynamicDocumentsUpdater) {
        this.convertToDynamicDocumentsUpdater = convertToDynamicDocumentsUpdater;
    }

}
>>>>>>> develop-5.1
