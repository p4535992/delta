package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Update task ownerOrganizationName property to multivalued property (String -> List<String>)
 * 
 * @author Riina Tens
 */
public class OwnerOrganizationNameToMultivaluedUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Object ownerOrganizationName = nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
        if (ownerOrganizationName == null || ownerOrganizationName instanceof List) {
            return new String[] { "Not updated, has valid value." };
        }
        if (ownerOrganizationName != null && ownerOrganizationName instanceof String) {
            nodeService.setProperty(nodeRef, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) Collections.singleton(ownerOrganizationName));
            return new String[] { "Updated." };
        }
        // should never actually happen
        return new String[] { "Not updated, no conversion to multivalue available." };
    }
}
