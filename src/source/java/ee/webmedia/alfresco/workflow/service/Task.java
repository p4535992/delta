package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Date;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Alar Kvell
 */
public class Task extends BaseWorkflowObject implements Serializable, Comparable<Task> {
    private static final long serialVersionUID = 1L;

    public static enum Action {
        NONE,
        FINISH,
        UNFINISH
    }

    private static String PROP_RESOLUTION = "{temp}resolution";

    private Workflow parent;
    private int outcomes;
    private int outcomeIndex = -1;
    private Action action = Action.NONE;

    protected static <T extends Task> T create(Class<T> taskClass, WmNode taskNode, Workflow taskParent, int outcomes) {
        try {
            return taskClass.getDeclaredConstructor(WmNode.class, Workflow.class, Integer.class).newInstance(taskNode, taskParent, outcomes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // If you change constructor arguments, update create method above accordingly
    protected Task(WmNode node, Workflow parent, Integer outcomes) {
        super(node);
        // parent can be null, WorkflowService#getTask(NodeRef) does not fetch parent
        Assert.notNull(outcomes);
        this.parent = parent;
        this.outcomes = outcomes;

        node.addPropertyResolver(PROP_RESOLUTION, resolutionPropertyResolver);
    }

    public Workflow getParent() {
        return parent;
    }

    public int getOutcomes() {
        return outcomes;
    }

    public String getOwnerId() {
        return getProp(WorkflowCommonModel.Props.OWNER_ID);
    }

    public void setOwnerId(String ownerId) {
        setProp(WorkflowCommonModel.Props.OWNER_ID, ownerId);
    }

    public String getOwnerName() {
        return getProp(WorkflowCommonModel.Props.OWNER_NAME);
    }

    public void setOwnerName(String ownerName) {
        setProp(WorkflowCommonModel.Props.OWNER_NAME, ownerName);
    }

    public String getOwnerEmail() {
        return getProp(WorkflowCommonModel.Props.OWNER_EMAIL);
    }

    public void setOwnerEmail(String ownerEmail) {
        setProp(WorkflowCommonModel.Props.OWNER_EMAIL, ownerEmail);
    }

    public Date getCompletedDateTime() {
        return getProp(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
    }

    protected void setCompletedDateTime(Date completedDateTime) {
        setProp(WorkflowCommonModel.Props.COMPLETED_DATE_TIME, completedDateTime);
    }

    public String getOutcome() {
        return getProp(WorkflowCommonModel.Props.OUTCOME);
    }

    protected void setOutcome(String outcome, int outcomeIndex) {
        setProp(WorkflowCommonModel.Props.OUTCOME, outcome);
        this.outcomeIndex = outcomeIndex;
    }

    /**
     * Not stored in repository, only provided during finishTaskInProgress service call.
     */
    public int getOutcomeIndex() {
        return outcomeIndex;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public Date getDueDate() {
        return getProp(WorkflowSpecificModel.Props.DUE_DATE);
    }

    public String getResolution() {
        // Cannot use getProp(QName) because we need to use resolutionPropertyResolver
        Object resolution = getNode().getProperties().get(PROP_RESOLUTION);
        return (resolution != null) ? resolution.toString() : "";
    }
    
    public String getShortResolution() {
        String resolution = getResolution();
        if(resolution.length() > 150) {
            return resolution.substring(0, 150).concat("...");
        }
        return resolution;
    }
    
    public int getResolutionLength() {
        return getResolution().length();
    }
    
    public void setComment(String comment) {
        if (getNode().hasAspect(WorkflowSpecificModel.Aspects.COMMENT)) {
            setProp(WorkflowSpecificModel.Props.COMMENT, comment);
        }
        else {
            throw new RuntimeException("Can not set COMMENT value, task does not have COMMENT aspect.");
        }
    }
    
    public String getComment() {
        if(getNode().hasAspect(WorkflowSpecificModel.Aspects.COMMENT) && getProp(WorkflowSpecificModel.Props.COMMENT) != null) {
            return getProp(WorkflowSpecificModel.Props.COMMENT).toString();
        }
        return "";
    }
    
    public String getShortComment() {
        String comment = getComment();
        if(comment.length() > 150) {
            return comment.substring(0, 150).concat("...");
        }
        return comment;
    }
    
    public int getCommentLength() {
        return getComment().length();
    }

    // Can only be called from web layer
    public String getFileDownloadUrl() {
        ContentData contentData = getProp(WorkflowSpecificModel.Props.FILE);
        if (contentData == null) {
            return null;
        }
        MimetypeService mimetypeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMimetypeService();
        String extension = mimetypeService.getExtension(contentData.getMimetype());
        return DownloadContentServlet.generateDownloadURL(getNode().getNodeRef(), "fail." + extension) + "?property=" + WorkflowSpecificModel.Props.FILE;
    }

    @Override
    protected String additionalToString() {
        return "\n  parent=" + WmNode.toString(getParent()) + "\n  outcomes=" + outcomes;
    }

    @Override
    public int compareTo(Task task) {
        Date dueDate = getDueDate();
        if (dueDate != null) {
            if (task.getDueDate() == null) {
                return -1;
            }
            return dueDate.compareTo(task.getDueDate());
        }
        return 0;
    }

    // returns task's resolution if present or workflow's resolution otherwise
    private NodePropertyResolver resolutionPropertyResolver = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            if (node.hasAspect(WorkflowSpecificModel.Props.RESOLUTION)) {
                return node.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
            }
            if (getParent() == null) {
                return null;
            }
            return getParent().getNode().getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        }

    };

    @Override
    protected void preSave() {
        super.preSave();

        // Set completedOverdue value which is used in task search
        boolean completedOverdue = false;
        if (getCompletedDateTime() != null && getDueDate() != null) {
            completedOverdue = getCompletedDateTime().after(getDueDate());
        }
        setProp(WorkflowSpecificModel.Props.COMPLETED_OVERDUE, completedOverdue);
        
        // Set workflowResolution value which is used in task search
        if (getNode().getNodeRef() == null) {
            setProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, parent.getProp(WorkflowSpecificModel.Props.RESOLUTION));
        }
        
        // Check if the new task is under CompoundWorkflow (not Definition) then add Searchable aspect
        if (getNode().getNodeRef() == null && !(getParent().getParent() instanceof CompoundWorkflowDefinition)) {
            getNode().getAspects().add(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
    };
    
}
