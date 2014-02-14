package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Wrapper class for displaying compound workflow info in list
 * 
 * @author Riina Tens
 */
public class CompoundWorkflowWithObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private final CompoundWorkflow compoundWorkflow;
    private String objectTitle;

    public CompoundWorkflowWithObject(CompoundWorkflow compoundWorkflow) {
        Assert.notNull(compoundWorkflow);
        this.compoundWorkflow = compoundWorkflow;
    }

    public String getTitle() {
        return compoundWorkflow.getTitle();
    }

    public void setObjectTitle(String objectTitle) {
        this.objectTitle = objectTitle;
    }

    public String getObjectTitle() {
        if (compoundWorkflow.isDocumentWorkflow() || compoundWorkflow.isCaseFileWorkflow()) {
            return objectTitle;
        }
        return WorkflowUtil.getCompoundWorkflowDocMsg(compoundWorkflow.getNumberOfDocuments(), MessageUtil.getMessage("compound_workflow_independent_no_documents"));
    }

    public String getStatus() {
        return compoundWorkflow.getStatus();
    }

    public CompoundWorkflow getCompoundWorkflow() {
        return compoundWorkflow;
    }

    public Date getStartedDate() {
        return compoundWorkflow.getStartedDateTime();
    }

    public String getStartedDateStr() {
        return compoundWorkflow.getStartedDateStr();
    }

    public Date getCreatedDateTime() {
        return compoundWorkflow.getCreatedDateTime();
    }

    public String getState() {
        Map<Workflow, List<String>> workflowStates = new HashMap<Workflow, List<String>>();
        StringBuilder state = null;
        for (Workflow wf : compoundWorkflow.getWorkflows()) {
            if (!WorkflowUtil.isStatus(wf, Status.IN_PROGRESS)) {
                continue;
            }
            List<String> taskOwners = new ArrayList<String>();
            for (Task task : wf.getTasks()) {
                if (!WorkflowUtil.isStatus(task, Status.IN_PROGRESS)) {
                    continue;
                }
                taskOwners.add(task.getOwnerName());
            }
            if (!taskOwners.isEmpty()) {
                workflowStates.put(wf, taskOwners);
            }
            state = new StringBuilder();
            for (Entry<Workflow, List<String>> entry : workflowStates.entrySet()) {
                if (state.length() > 0) {
                    state.append("; ");
                }
                state.append(MessageUtil.getMessage(entry.getKey().getType().getLocalName()))
                        .append(" (")
                        .append(StringUtils.join(entry.getValue(), ", "))
                        .append(")");
            }
        }
        return state != null ? state.toString() : "";
    }
}
