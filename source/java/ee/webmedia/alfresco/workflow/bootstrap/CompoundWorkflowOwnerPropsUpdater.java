package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Add ownerOrganizationName and ownerJobTitle properties used in compound workflow search.
 */
public class CompoundWorkflowOwnerPropsUpdater extends AbstractNodeUpdater {

    private UserService userService;
    private final Map<String, List<String>> userIdToUserOrgPath = new HashMap<>();
    private final Map<String, String> userIdToUserJobTitle = new HashMap<>();
    private BulkLoadNodeService bulkLoadNodeService;
    private final Map<Long, QName> propertyTypes = new HashMap<>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAndNotQuery(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION));
        List<ResultSet> result = new ArrayList<>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected List<String[]> processNodes(final List<NodeRef> batchList, File failedNodesFile) throws Exception, InterruptedException {
        final List<String[]> batchInfos = new ArrayList<>(batchList.size());
        Map<NodeRef, Node> compoundWorkflows = bulkLoadNodeService.loadNodes(batchList, null, propertyTypes);
        Map<NodeRef, Map<QName, Serializable>> compoundWorkflowsNewProps = new HashMap<>();
        for (Map.Entry<NodeRef, Node> entry : compoundWorkflows.entrySet()) {
            Map<String, Object> compoundWorkflowProps = entry.getValue().getProperties();
            Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
            if (StringUtils.isBlank((String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE.toString()))) {
                propsToAdd.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
            }
            if (!compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE.toString()) ||
                    !compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString())) {
                String ownerId = (String) compoundWorkflowProps.get(WorkflowCommonModel.Props.OWNER_ID.toString());
                List<String> ownerOrgPath = userIdToUserOrgPath.get(ownerId);
                String userJobTitle = userIdToUserJobTitle.get(ownerId);
                if (ownerOrgPath == null && !userIdToUserOrgPath.containsKey(ownerId)) {
                    Map<QName, Serializable> userProps = BeanHelper.getUserService().getUserProperties(ownerId);
                    if (userProps != null) {
                        ownerOrgPath = userService.getUserOrgPathOrOrgName(userProps);
                        userIdToUserOrgPath.put(ownerId, ownerOrgPath);
                        userJobTitle = (String) userProps.get(ContentModel.PROP_JOBTITLE);
                        userIdToUserJobTitle.put(ownerId, userJobTitle);
                    }
                }
                if (ownerOrgPath != null && !ownerOrgPath.isEmpty()) {
                    propsToAdd.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) ownerOrgPath);
                }
                if (StringUtils.isNotBlank(userJobTitle)) {
                    propsToAdd.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, userJobTitle);
                }
            }
            compoundWorkflowsNewProps.put(entry.getKey(), propsToAdd);
        }
        for (NodeRef nodeRef : batchList) {
            if (!compoundWorkflowsNewProps.containsKey(nodeRef)) {
                batchInfos.add(new String[] { "not found " });
                continue;
            }
            Map<QName, Serializable> propsToAdd = compoundWorkflowsNewProps.get(nodeRef);
            if (propsToAdd.isEmpty()) {
                batchInfos.add(new String[] { "user properties present, no update needed" });
                continue;
            }
            nodeService.addProperties(nodeRef, propsToAdd);
            batchInfos.add(new String[] { "updated" });

        }

        return batchInfos;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        throw new RuntimeException("Method not implemented!");
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }
}
