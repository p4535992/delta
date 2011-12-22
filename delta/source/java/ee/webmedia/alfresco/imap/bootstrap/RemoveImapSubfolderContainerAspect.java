package ee.webmedia.alfresco.imap.bootstrap;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Remove imapSubfolderContainer aspect from all nodes that have it
 * 
 * @author Riina Tens
 */
public class RemoveImapSubfolderContainerAspect extends AbstractNodeUpdater {

    private static final QName IMAP_SUBFOLDER_CONTAINER_ASPECT = QName.createQName(ImapModel.URI, "imapSubfolderContainer");

    @Override
    protected boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAspectQuery(IMAP_SUBFOLDER_CONTAINER_ASPECT);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.removeAspect(nodeRef, IMAP_SUBFOLDER_CONTAINER_ASPECT);
        return null;
    }

}
