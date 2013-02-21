package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.workflow.bootstrap.TaskAssociatedDataTableInsertBootstrap.checkTaskExists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Updater to add and reorganize properties and aspects of tasks.
 * 
 * @author Riina Tens
 */
public class TaskUpdater extends AbstractNodeUpdater {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK);
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> result = new ArrayList<String>();
        String[] taskExistsError = checkTaskExists(nodeRef, log, nodeService, workflowService, workflowDbService);
        if (taskExistsError != null) {
            return taskExistsError;
        }
        boolean searchablePropsUpdated = false;
        QName taskType = nodeService.getType(nodeRef);
        boolean isExternalReviewTask = WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType);
        boolean isLinkedReviewTask = WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.equals(taskType);
        boolean isSearchable = nodeService.hasAspect(nodeRef, WorkflowSpecificModel.Aspects.SEARCHABLE);
        if (isExternalReviewTask || WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            if (isExternalReviewTask) {
                nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.CREATOR_INSTITUTION_CODE, null);
                nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.RECIEVED_DVK_ID, null);
                result.add(WorkflowSpecificModel.Aspects.CREATOR_INSTITUTION_CODE.toPrefixString(serviceRegistry.getNamespaceService()));
                result.add(WorkflowSpecificModel.Aspects.RECIEVED_DVK_ID.toPrefixString(serviceRegistry.getNamespaceService()));
            } else {
                nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.CREATOR_INSTITUTION, null);
                result.add(WorkflowSpecificModel.Aspects.CREATOR_INSTITUTION.toPrefixString(serviceRegistry.getNamespaceService()));
            }
            nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.INSTITUTION, null);
            nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.SENT_DVK_DATA, null);
            result.add(WorkflowSpecificModel.Aspects.INSTITUTION.toPrefixString(serviceRegistry.getNamespaceService()));
            result.add(WorkflowSpecificModel.Aspects.SENT_DVK_DATA.toPrefixString(serviceRegistry.getNamespaceService()));
        } else {
            if (isLinkedReviewTask && !isSearchable) {
                nodeService.addAspect(nodeRef, WorkflowSpecificModel.Aspects.SEARCHABLE_COMPOUND_WORKFLOW_TITLE_AND_COMMENT, null);
                result.add(WorkflowSpecificModel.Aspects.SEARCHABLE_COMPOUND_WORKFLOW_TITLE_AND_COMMENT.toPrefixString(serviceRegistry.getNamespaceService()));
            }
        }
        if (isSearchable && !isLinkedReviewTask) {
            Map<QName, PropertyDefinition> propertyDefinitions = BeanHelper.getDictionaryService().getPropertyDefs(WorkflowSpecificModel.Aspects.SEARCHABLE);
            // if some searchable properties are missing, update all searchable properties
            Map<QName, Serializable> taskProps = nodeService.getProperties(nodeRef);
            for (QName propertyDef : propertyDefinitions.keySet()) {
                if (!taskProps.containsKey(propertyDef)) {
                    BeanHelper.getWorkflowService().updateTaskSearchableProperties(nodeRef);
                    searchablePropsUpdated = true;
                    break;
                }
            }

        }
        return new String[] { StringUtils.join(result, ","), Boolean.toString(searchablePropsUpdated) };
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

}
