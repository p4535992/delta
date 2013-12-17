package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Add docType prop to all tasks, needed for searching tasks by docType
 * 
 * @author Keit Tehvan
 */
public class AddDocTypeToTaskNodeUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AddDocTypeToTaskNodeUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String docType = getDocType(nodeRef);
        nodeService.setProperty(nodeRef, WorkflowCommonModel.Props.DOCUMENT_TYPE, docType);
        return new String[] { String.valueOf(docType) };
    }

    private String getDocType(NodeRef parent) {
        NodeRef doc = parent;
        do {
            if (nodeService.getType(doc).equals(DocumentCommonModel.Types.DOCUMENT)) {
                return (String) nodeService.getProperty(doc, DocumentAdminModel.Props.OBJECT_TYPE_ID);
            }
            doc = nodeService.getPrimaryParent(doc).getParentRef();
        } while (doc != null);
        return null;
    }

}
