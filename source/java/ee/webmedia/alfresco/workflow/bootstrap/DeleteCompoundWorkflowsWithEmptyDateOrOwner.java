package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * Cl task 211790 - delete erroneous compound workflows containing
 * tasks with empty dueDate or ownerName property.
 * Should run only in environments where it has been verified that is is acceptable to delete such compound workflows.
 * 
 * @author Riina Tens
 */
public class DeleteCompoundWorkflowsWithEmptyDateOrOwner extends AbstractNodeUpdater {

    protected final Log log = LogFactory.getLog(getClass());

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        queryParts.add(DbSearchUtil.generateTaskFieldNotQuery(DbSearchUtil.TASK_TYPE_FIELD));

        String query = joinQueryPartsOr(joinQueryPartsAnd(
                DbSearchUtil.generateTaskPropertyNullQuery(WorkflowSpecificModel.Props.DUE_DATE), DbSearchUtil.generateTaskFieldNotQuery(DbSearchUtil.TASK_TYPE_FIELD)),
                DbSearchUtil.generateTaskPropertyNullQuery(WorkflowCommonModel.Props.OWNER_NAME));
        arguments.add(WorkflowSpecificModel.Types.INFORMATION_TASK.getLocalName());

        List<Task> tasks = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();

        Set<NodeRef> compoundWorkflowRefs = new HashSet<NodeRef>();
        for (Task task : tasks) {
            NodeRef workflowNodeRef = task.getWorkflowNodeRef();
            if (workflowNodeRef != null) {
                NodeRef compoundWorkflowRef = generalService.getAncestorNodeRefWithType(workflowNodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
                if (compoundWorkflowRef != null) {
                    compoundWorkflowRefs.add(compoundWorkflowRef);
                }
            }
        }
        return compoundWorkflowRefs;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // not used
        return null;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.deleteNode(nodeRef);
        return null;
    }
}
