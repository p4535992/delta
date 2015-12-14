package ee.webmedia.mobile.alfresco.workflow;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ee.webmedia.alfresco.workflow.service.DelegationHistoryUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.mobile.alfresco.common.AbstractBaseController;
import ee.webmedia.mobile.alfresco.workflow.model.DelegationHistoryItem;
import ee.webmedia.mobile.alfresco.workflow.model.LockMessage;

public abstract class AbstractCompoundWorkflowController extends AbstractBaseController {

    private static final long serialVersionUID = 1L;

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    @Resource(name = "WmWorkflowService")
    protected WorkflowService workflowService;
    
    @Resource(name = "retryingTransactionHelper")
    protected RetryingTransactionHelper txnHelper;

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
    
    /**
     * Sets compound workflow lock
     * @return
     */
    protected boolean setLock(final NodeRef compoundWfNodeRef, final String lockMsgKey, final RedirectAttributes redirectAttributes) {
        RetryingTransactionCallback<Boolean> callback = new RetryingTransactionCallback<Boolean>()
        {
           public Boolean execute() throws Throwable
           {
        	   
        	   LockStatus lockStatus = getDocLockService().setLockIfFree(compoundWfNodeRef);
               boolean result;
               
	           	if (lockStatus == LockStatus.LOCK_OWNER) {
	           		result = true;
	            } else {
	            	String lockOwner = StringUtils.substringBefore(getDocLockService().getLockOwnerIfLocked(compoundWfNodeRef), "_");
	                String lockOwnerName = getUserService().getUserFullNameAndId(lockOwner);
	               	addRedirectErrorMsg(redirectAttributes, lockMsgKey, lockOwnerName);
	               	result = false;
                }
                return result;
           }
        };
        
        return txnHelper.doInTransaction(callback, false, true);
    	
    }
    
}
