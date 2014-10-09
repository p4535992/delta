package ee.webmedia.alfresco.workflow.bootstrap;

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
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
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

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAndNotQuery(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION));
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> compoundWorkflowProps = nodeService.getProperties(nodeRef);
        String ownerId = (String) compoundWorkflowProps.get(WorkflowCommonModel.Props.OWNER_ID);
        Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
        if (StringUtils.isBlank((String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE))) {
            propsToAdd.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
        }
        if (!compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE) ||
                !compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME)) {
            Map<QName, Serializable> userProps = BeanHelper.getUserService().getUserProperties(ownerId);
            if (userProps != null) {
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                List<String> ownerOrgPath = userIdToUserOrgPath.get(ownerId);
                if (ownerOrgPath == null && !userIdToUserOrgPath.containsKey(ownerId)) {
                    ownerOrgPath = userService.getUserOrgPathOrOrgName(userProps);
                    userIdToUserOrgPath.put(ownerId, ownerOrgPath);
                }
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) ownerOrgPath);
            }
        }
        if (!propsToAdd.isEmpty()) {
            nodeService.addProperties(nodeRef, propsToAdd);
            return new String[] { "updated" };
        }
        return new String[] { "user properties present, no update needed" };
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
