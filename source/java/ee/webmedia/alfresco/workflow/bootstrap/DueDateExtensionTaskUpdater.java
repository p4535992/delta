package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DueDateExtensionTaskUpdater extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef extensionTaskRef) throws Exception {
        String[] taskExistsError = TaskAssociatedDataTableInsertBootstrap.checkTaskExists(extensionTaskRef, log, nodeService, workflowService, workflowDbService);
        if (taskExistsError != null) {
            return taskExistsError;
        }
        QName type = nodeService.getType(extensionTaskRef);
        if (!WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(type)) {
            return new String[] { "notDueDateExtensionTask", type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        List<AssociationRef> initiatingTaskAssocs = nodeService.getSourceAssocs(extensionTaskRef, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION);
        if (initiatingTaskAssocs == null || initiatingTaskAssocs.isEmpty()) {
            return new String[] { "noInitiatingTaskAssocsExist", type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        AssociationRef assoc = initiatingTaskAssocs.get(0);
        Assert.isTrue(extensionTaskRef.equals(assoc.getTargetRef()) && !extensionTaskRef.equals(assoc.getSourceRef()));
        Map<QName, Serializable> initiatingTaskProps = nodeService.getProperties(assoc.getSourceRef());
        String creatorId = (String) initiatingTaskProps.get(WorkflowSpecificModel.Props.CREATOR_ID);
        Map<QName, Serializable> creatorProps = getUserService().getUserProperties(creatorId);
        Map<QName, Serializable> oldExtensionTaskProps = nodeService.getProperties(extensionTaskRef);
        Map<QName, Serializable> newExtensionTaskProps = new HashMap<QName, Serializable>();
        if (creatorProps != null) {
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_EMAIL, creatorProps.get(ContentModel.PROP_EMAIL));
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, creatorProps.get(ContentModel.PROP_JOBTITLE));
            Serializable orgName = (Serializable) getOrganizationStructureService().getOrganizationStructurePaths((String) creatorProps.get(ContentModel.PROP_ORGID));
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, orgName);
        } else {
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_EMAIL, null);
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, null);
            newExtensionTaskProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, null);
        }
        nodeService.addProperties(extensionTaskRef, newExtensionTaskProps);
        BeanHelper.getWorkflowDbService().updateTaskProperties(extensionTaskRef, newExtensionTaskProps);
        return new String[] {
                creatorProps != null ? "creatorFound" : "creatorNotFound",
                type.toPrefixString(serviceRegistry.getNamespaceService()),
                Integer.toString(initiatingTaskAssocs.size()),
                creatorId,
                (String) oldExtensionTaskProps.get(WorkflowCommonModel.Props.OWNER_EMAIL),
                (String) newExtensionTaskProps.get(WorkflowCommonModel.Props.OWNER_EMAIL) };
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
