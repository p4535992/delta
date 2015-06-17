package ee.webmedia.alfresco.document.search.bootstrap;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Update recipient unsent document properties to trigger reindexing.
 */
public class RecipientFinishedDocumentsReindexBootstrap extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("Not implemented, use loadNodesFromRepo() method!");
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() {
        return new HashSet<>(BeanHelper.getDocumentSearchService().searchRecipientFinishedDocuments());
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        nodeService.addProperties(nodeRef, props);
        return null;
    }

}
