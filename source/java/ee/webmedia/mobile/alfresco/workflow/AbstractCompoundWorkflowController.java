package ee.webmedia.mobile.alfresco.workflow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ui.Model;

import ee.webmedia.alfresco.workflow.service.DelegationHistoryUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.model.DelegationHistoryItem;

public abstract class AbstractCompoundWorkflowController extends AbstractBaseController {

    private static final long serialVersionUID = 1L;

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    @Resource(name = "WmWorkflowService")
    protected WorkflowService workflowService;

    protected void setupDelegationHistoryBlock(Model model, List<ee.webmedia.alfresco.workflow.service.Task> delegationTasks) {
        if (CollectionUtils.isEmpty(delegationTasks)) {
            return;
        }
        List<ee.webmedia.alfresco.workflow.service.Task> delegationHistory = new ArrayList<>();
        List<NodeRef> delegationTaskRefs = new ArrayList<>();
        delegationHistory.addAll(workflowService.getTasks4DelegationHistory(delegationTasks.get(0).getNode()));
        for (ee.webmedia.alfresco.workflow.service.Task task : delegationTasks) {
            delegationTaskRefs.add(task.getNodeRef());
        }
        List<Node> delegationHistoryNodes = DelegationHistoryUtil.getDelegationNodes(delegationTaskRefs, delegationHistory);
        List<DelegationHistoryItem> items = new ArrayList<>(delegationHistoryNodes.size());
        for (Node node : delegationHistoryNodes) {
            items.add(new DelegationHistoryItem(node.getProperties()));
        }
        model.addAttribute("delegationHistoryTaskCount", items.size());
        model.addAttribute("delegationHistoryBlockItems", items);
    }

    public static String redirectToCompoundWorkflow(String compoundWorkflowNodeId) {
        return "redirect:/m/" + CompundWorkflowDetailsController.COMPOUND_WORKFLOW_DETAILS_MAPPING + "/" + compoundWorkflowNodeId;
    }

    protected String redirectToTaskList() {
        return "redirect:/m/tasks/";
    }

    protected String redirectToTaskList(QName taskType) {
        return "redirect:/m/tasks/" + TASK_TYPE_TO_KEY_MAPPING.get(taskType);
    }

}
