package ee.webmedia.alfresco.docadmin.bootstrap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class DocTypesDocSigningForOwnerEnabledUpdater extends AbstractNodeUpdater {

    private Map<NodeRef, Node> docTypeActiveStatus;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.DOCUMENT_TYPE);
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Node docType = docTypeActiveStatus.get(nodeRef);
        if (docType == null || docType.getProperties() == null) {
            log.warn("Did not find node: " + nodeRef);
            return new String[] { "Node not found" };
        }
        boolean docTypeUsed = Boolean.TRUE.equals(docType.getProperties().get(DocumentAdminModel.Props.USED));
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.DOC_SIGNING_FOR_OWNER_ENABLED, docTypeUsed);
        return new String[] { Boolean.toString(docTypeUsed) };
    }

    @Override
    protected void doBeforeBatchUpdate(List<NodeRef> batchList) {
        docTypeActiveStatus = BeanHelper.getBulkLoadNodeService().loadNodes(batchList, Collections.singleton(DocumentAdminModel.Props.USED));
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        docTypeActiveStatus = null;
    }

}
