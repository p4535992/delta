package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.utils.RepoUtil.toQNameProperties;
import static ee.webmedia.alfresco.utils.RepoUtil.toStringProperties;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getTaskSearchableProps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Alar Kvell
 */
public class Task extends BaseWorkflowObject implements Comparable<Task>, CssStylable {
    private static final long serialVersionUID = 1L;

    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    public static FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    public static enum Action {
        NONE,
        FINISH,
        UNFINISH
    }

    public static final QName INITIATING_COMPOUND_WORKFLOW_REF = RepoUtil.createTransientProp("initiatingCompoundWorkflowRef");
    public static final QName INITIATING_COMPOUND_WORKFLOW_TITLE = RepoUtil.createTransientProp("initiatingCompoundWorkflowTitle");

    private static final QName PROP_RESOLUTION = RepoUtil.createTransientProp("resolution");
    private static final QName PROP_WORKFLOW_CATEGORY = RepoUtil.createTransientProp("category");
    private static final QName PROP_TEMP_FILES = RepoUtil.createTransientProp("files");
    private static final QName PROP_DUE_DATE_TIME_STR = RepoUtil.createTransientProp("dueDateTimeStr");
    private static final QName PROP_COMPOUNDWORKFLOW_NODEREF = RepoUtil.createTransientProp("compountWorkflowNodeRef");

    private final Workflow parent;
    private final int outcomes;
    private int outcomeIndex = -1;
    private Action action = Action.NONE;
    private List<DueDateHistoryRecord> dueDateHistoryRecords;
    private List<NodeRef> removedFiles;
    private boolean filesLoaded;
    /** If null, indicates that due date history data existence has not been checked and should not be updated in delta_task table */
    private Boolean hasDueDateHistory;
    private Boolean originalHasDueDateHistory;
    private Boolean originalHasFiles;
    private String groupDueDateVbString;
    private String workflowNodeRefId;
    private String storeRef;

    /**
     * Task's index in workflow during last save
     * (may not be current index if workflow is changed in memory).
     * At the moment used for secondary ordering in WorkflowBlock
     */
    private Integer taskIndexInWorkflow = null;

    private String cssStyleClass;

    public static <T extends Task> T create(Class<T> taskClass, WmNode taskNode, Workflow taskParent, int outcomes) {
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

        node.addPropertyResolver(PROP_RESOLUTION.toString(), resolutionPropertyResolver);
        node.addPropertyResolver(PROP_WORKFLOW_CATEGORY.toString(), categoryPropertyResolver);
        node.addPropertyResolver(PROP_DUE_DATE_TIME_STR.toString(), dueDateTimeStrPropertyResolver);
    }

    protected Task copy(Workflow copyParent) {
        Task copy = copyImpl(new Task(getNode().clone(), copyParent, outcomes));
        copy.setDueDateHistoryRecords(getDueDateHistoryRecords());
        copy.hasDueDateHistory = !getDueDateHistoryRecords().isEmpty();
        return copy;
    }

    protected Task copy() {
        return copy(parent);
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        Task task = (Task) super.copyImpl(copy);
        task.outcomeIndex = outcomeIndex;
        task.action = action;
        boolean hasNoParentNodeRef = parent == null || parent.getNodeRef() == null;
        task.workflowNodeRefId = hasNoParentNodeRef ? null : parent.getNodeRef().getId();
        task.storeRef = hasNoParentNodeRef ? null : parent.getNodeRef().getStoreRef().toString();
        task.taskIndexInWorkflow = taskIndexInWorkflow;
        @SuppressWarnings("unchecked")
        T result = (T) task;
        return result;
    }

    public void setCssStyleClass(String cssStyleClass) {
        this.cssStyleClass = cssStyleClass;
    }

    @Override
    public String getCssStyleClass() {
        return cssStyleClass;
    }

    public Workflow getParent() {
        return parent;
    }

    public int getOutcomes() {
        return outcomes;
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

    public String getOwnerGroup() {
        return getProp(WorkflowCommonModel.Props.OWNER_GROUP);
    }

    public void setOwnerGroup(String ownerGroup) {
        setProp(WorkflowCommonModel.Props.OWNER_GROUP, ownerGroup);
    }

    @SuppressWarnings("unchecked")
    public String getOwnerOrgStructUnit() {
        return UserUtil.getDisplayUnit((List<String>) getNode().getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
    }

    @SuppressWarnings("unchecked")
    public List<String> getOwnerOrgStructUnitProp() {
        return (List<String>) getNode().getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
    }

    public void setOwnerOrgStructUnitProp(List<String> ownerOrgStructUnitProp) {
        getNode().getProperties().put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), ownerOrgStructUnitProp);
    }

    public String getOwnerJobTitle() {
        return getProp(WorkflowCommonModel.Props.OWNER_JOB_TITLE);
    }

    public void setOwnerJobTitle(String ownerJobTitle) {
        setProp(WorkflowCommonModel.Props.OWNER_JOB_TITLE, ownerJobTitle);
    }

    public String getPreviousOwnerId() {
        return getProp(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID);
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

    public String getOutcomeAndComments() {
        final String outcome = getOutcome();
        final String comment = getComment();
        if (StringUtils.isNotBlank(comment)) {
            String outcomeWithSeparator = (StringUtils.isNotBlank(outcome)) ? (outcome + ": ") : "";
            return outcomeWithSeparator + comment;
        }
        return outcome;
    }

    protected void setOutcome(String outcome, int outcomeIndex) {
        setProp(WorkflowCommonModel.Props.OUTCOME, outcome);
        this.outcomeIndex = outcomeIndex;
    }

    public String getInstitutionName() {
        return getProp(WorkflowSpecificModel.Props.INSTITUTION_NAME);
    }

    public void setInstitutionName(String institutionName) {
        setProp(WorkflowSpecificModel.Props.INSTITUTION_NAME, institutionName);
    }

    public String getInstitutionCode() {
        return getProp(WorkflowSpecificModel.Props.INSTITUTION_CODE);
    }

    public void setInstitutionCode(String institutionCode) {
        setProp(WorkflowSpecificModel.Props.INSTITUTION_CODE, institutionCode);
    }

    public String getCreatorInstitutionCode() {
        return getProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE);
    }

    public void setCreatorInstitutionCode(String creatorInstitutionCode) {
        setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, creatorInstitutionCode);
    }

    public String getCreatorInstitutionName() {
        return getProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_NAME);
    }

    public void setCreatorInstitutionName(String creatorInstitutionName) {
        setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_NAME, creatorInstitutionName);
    }

    public String getCreatorId() {
        return getProp(WorkflowSpecificModel.Props.CREATOR_ID);
    }

    protected void setCreatorId(String creatorId) {
        setProp(WorkflowSpecificModel.Props.CREATOR_ID, creatorId);
    }

    public void setDocumentType(String documentType) {
        setProp(WorkflowCommonModel.Props.DOCUMENT_TYPE, documentType);
    }

    public String getCreatorEmail() {
        return getProp(WorkflowSpecificModel.Props.CREATOR_EMAIL);
    }

    protected void setCreatorEmail(String email) {
        setProp(WorkflowSpecificModel.Props.CREATOR_EMAIL, email);
    }

    public String getOriginalDvkId() {
        return getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID);
    }

    public void setOriginalDvkId(String originalDvkId) {
        setProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID, originalDvkId);
    }

    public String getSendStatus() {
        return getProp(WorkflowSpecificModel.Props.SEND_STATUS);
    }

    public void setSendStatus(String status) {
        setProp(WorkflowSpecificModel.Props.SEND_STATUS, status);
    }

    public void setParallel(boolean parallel) {
        setProp(WorkflowCommonModel.Props.PARALLEL_TASKS, parallel);
    }

    public void setActive(boolean active) {
        setProp(WorkflowSpecificModel.Props.ACTIVE, active);
    }

    public boolean isResponsible() {
        return getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE);
    }

    public void setResponsible(boolean responsible) {
        if (responsible) {
            getNode().getAspects().add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
        } else {
            getNode().getAspects().remove(WorkflowSpecificModel.Aspects.RESPONSIBLE);
        }
    }

    public String getFileVersions() {
        return getProp(WorkflowSpecificModel.Props.FILE_VERSIONS);
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

    public void setDueDate(Date dueDate) {
        setProp(WorkflowSpecificModel.Props.DUE_DATE, dueDate);
    }

    public Integer getDueDateDays() {
        return getProp(WorkflowSpecificModel.Props.DUE_DATE_DAYS);
    }

    public void setDueDateDays(Integer dueDateDays) {
        setProp(WorkflowSpecificModel.Props.DUE_DATE_DAYS, dueDateDays);
    }

    public String getDueDateStr() {
        return getDueDate() != null ? dateFormat.format(getDueDate()) : "";
    }

    public Date getProposedDueDate() {
        return getProp(WorkflowSpecificModel.Props.PROPOSED_DUE_DATE);
    }

    public String getProposedDueDateStr() {
        return getProposedDueDate() != null ? dateFormat.format(getProposedDueDate()) : "";
    }

    public void setProposedDueDate(Date proposedDueDate) {
        setProp(WorkflowSpecificModel.Props.PROPOSED_DUE_DATE, proposedDueDate);
    }

    public Date getConfirmedDueDate() {
        return getProp(WorkflowSpecificModel.Props.CONFIRMED_DUE_DATE);
    }

    public void setConfirmedDueDate(Date confirmedDueDate) {
        setProp(WorkflowSpecificModel.Props.CONFIRMED_DUE_DATE, confirmedDueDate);
    }

    /**
     * @return resolution of the task or workflow
     */
    public String getResolution() {
        // Cannot use getProp(QName) because we need to use resolutionPropertyResolver
        Object resolution = getNode().getProperties().get(PROP_RESOLUTION.toString());
        return (resolution != null) ? resolution.toString() : "";
    }

    /**
     * Unlike {@link #getResolution()} method it doesn't search resolution from workflow if task doesn't have resolution.
     * 
     * @return value of task property WorkflowSpecificModel.Props.RESOLUTION
     */
    public String getResolutionOfTask() {
        return getProp(WorkflowSpecificModel.Props.RESOLUTION);
    }

    public void setResolution(String resolution) {
        setProp(WorkflowSpecificModel.Props.RESOLUTION, resolution);
    }

    /**
     * @return parent workflow category property value, if parent is present, null otherwise
     */
    public String getCategory() {
        // Cannot use getProp(QName) because we need to use categoryPropertyResolver
        return (String) getNode().getProperties().get(PROP_WORKFLOW_CATEGORY.toString());
    }

    public void setComment(String comment) {
        if (getNode().hasAspect(WorkflowSpecificModel.Aspects.COMMENT)) {
            setProp(WorkflowSpecificModel.Props.COMMENT, comment);
        } else {
            throw new RuntimeException("Can not set COMMENT value, task does not have COMMENT aspect.");
        }
    }

    public String getComment() {
        if (getNode().hasAspect(WorkflowSpecificModel.Aspects.COMMENT) && getProp(WorkflowSpecificModel.Props.COMMENT) != null) {
            return getProp(WorkflowSpecificModel.Props.COMMENT).toString();
        }
        return "";
    }

    public String getCommentAndLinks() {
        return WebUtil.escapeHtmlExceptLinks(WebUtil.processLinks(getComment()));
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
    private final NodePropertyResolver resolutionPropertyResolver = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            if (node.hasAspect(WorkflowSpecificModel.Props.RESOLUTION)) {
                return node.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
            }
            if (getParent() == null) {
                // if it occurs that task workflowResolution property is not valid value here (although it should be),
                // don't load entire workflow, but query workflow resolution property from nodeService
                return node.getProperties().get(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION);
            }
            return getParent().getNode().getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        }

    };

    private final NodePropertyResolver categoryPropertyResolver = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            if (getParent() == null) {
                return null;
            }
            return getParent().getNode().getProperties().get(WorkflowSpecificModel.Props.CATEGORY);
        }

    };

    private final NodePropertyResolver dueDateTimeStrPropertyResolver = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            Date dueDate = (Date) node.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
            return dueDate != null ? dateTimeFormat.format(dueDate) : "";
        }

    };

    @Override
    protected void preSave() {
        super.preSave();

        calculateOverdue();

        // Set workflowResolution value which is used in task search
        if (isUnsaved()) {
            setProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, parent.getProp(WorkflowSpecificModel.Props.RESOLUTION));
        }

        // Check if the new task is under CompoundWorkflow (not Definition) then add Searchable aspect
        if (isUnsaved() && !(getParent().getParent() instanceof CompoundWorkflowDefinition)) {
            getNode().getAspects().add(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
        if (node.getAspects().contains(WorkflowSpecificModel.Aspects.SEARCHABLE) && parent != null && parent.getParent() != null) {
            CompoundWorkflow compoundWorkflow = parent.getParent();
            node.getProperties().putAll(
                    toStringProperties(getTaskSearchableProps(toQNameProperties(compoundWorkflow.getNode().getProperties()))));
            setCompoundWorkflowId(compoundWorkflow.getNodeRef().getId());
            setCompoundWorkflowTitle(compoundWorkflow.getTitle());
        }

    }

    protected void calculateOverdue() {
        // Set completedOverdue value which is used in task search
        boolean completedOverdue = false;
        if (getCompletedDateTime() != null && getDueDate() != null) {
            Date completedDay = DateUtils.truncate(getCompletedDateTime(), Calendar.DATE);
            Date dueDay = DateUtils.truncate(getDueDate(), Calendar.DATE);
            completedOverdue = completedDay.after(dueDay);
        }
        setProp(WorkflowSpecificModel.Props.COMPLETED_OVERDUE, completedOverdue);
    }

    public Integer getTaskIndexInWorkflow() {
        return taskIndexInWorkflow;
    }

    public void setTaskIndexInWorkflow(Integer taskIndexInWorkflow) {
        this.taskIndexInWorkflow = taskIndexInWorkflow;
    }

    public int getWorkflowIndex() {
        return parent.getIndexInCompoundWorkflow();
    }

    public void setDueDateHistoryRecords(List<DueDateHistoryRecord> dueDateHistoryRecords) {
        this.dueDateHistoryRecords = dueDateHistoryRecords;
    }

    public List<DueDateHistoryRecord> getDueDateHistoryRecords() {
        if (dueDateHistoryRecords == null) {
            dueDateHistoryRecords = new ArrayList<DueDateHistoryRecord>();
        }
        return dueDateHistoryRecords;
    }

    /** Return list of FileWithContentType or File objects */
    public List<Object> getFiles() {
        return Collections.unmodifiableList(getFilesList());
    }

    @SuppressWarnings("unchecked")
    private List<Object> getFilesList() {
        if (getNode().getProperties().get(PROP_TEMP_FILES.toString()) == null) {
            getNode().getProperties().put(PROP_TEMP_FILES.toString(), new ArrayList<File>());
        }
        return (ArrayList<Object>) getNode().getProperties().get(PROP_TEMP_FILES.toString());
    }

    /** Load existing files */
    public void loadFiles(List<File> list) {
        getFilesList().addAll(list);
        filesLoaded = true;
    }

    /** Copy files */
    public void copyFiles(List<Object> files) {
        List<Object> filesList = getFilesList();
        filesList.clear();
        filesList.addAll(files);
        filesLoaded = true;
    }

    public void clearFiles() {
        getFilesList().clear();
        filesLoaded = false;
    }

    public List<NodeRef> getRemovedFiles() {
        if (removedFiles == null) {
            removedFiles = new ArrayList<NodeRef>();
        }
        return removedFiles;
    }

    public boolean filesLoaded() {
        return filesLoaded;
    }

    public void setHasDueDateHistory(boolean hasDueDateHistory) {
        this.hasDueDateHistory = hasDueDateHistory;
    }

    public Boolean getHasDueDateHistory() {
        return hasDueDateHistory;
    }

    public Boolean getOriginalHasDueDateHistory() {
        return originalHasDueDateHistory;
    }

    public void setOriginalHasDueDateHistory(Boolean originalHasDueDateHistory) {
        this.originalHasDueDateHistory = originalHasDueDateHistory;
    }

    public Boolean getOriginalHasFiles() {
        return originalHasFiles;
    }

    public void setOriginalHasFiles(Boolean originalHasFiles) {
        this.originalHasFiles = originalHasFiles;
    }

    public String getGroupDueDateVbString() {
        return groupDueDateVbString;
    }

    public void setGroupDueDateVbString(String groupDueDateVbString) {
        this.groupDueDateVbString = groupDueDateVbString;
    }

    public void setCompoundWorkflowTitle(String compoundWorkflowTitle) {
        setProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE, compoundWorkflowTitle);
    }

    public String getCompoundWorkflowTitle() {
        return getProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE);
    }

    public void setCompoundWorkflowComment(String compoundWorkflowComment) {
        setProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_COMMENT, compoundWorkflowComment);
    }

    public String getCompoundWorkflowComment() {
        return getProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_COMMENT);
    }

    public void setOriginalNoderefId(String originalNoderefId) {
        setProp(WorkflowSpecificModel.Props.ORIGINAL_NODEREF_ID, originalNoderefId);
    }

    public void setOriginalTaskObjectUrl(String originalTaskObjectUrl) {
        setProp(WorkflowSpecificModel.Props.ORIGINAL_TASK_OBJECT_URL, originalTaskObjectUrl);
    }

    public String getOriginalTaskObjectUrl() {
        return getProp(WorkflowSpecificModel.Props.ORIGINAL_TASK_OBJECT_URL);
    }

    public void setWorkflowResolution(String workflowResolution) {
        setProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, workflowResolution);
    }

    public String getWorkflowResolution() {
        return getProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION);
    }

    public void setReceivedDvkId(String receivedDvkId) {
        setProp(WorkflowSpecificModel.Props.RECIEVED_DVK_ID, receivedDvkId);
    }

    public String getReceivedDvkId() {
        return getProp(WorkflowSpecificModel.Props.RECIEVED_DVK_ID);
    }

    public void setWorkflowNodeRefId(String workflowNodeRefId) {
        this.workflowNodeRefId = workflowNodeRefId;
    }

    public String getStoreRef() {
        return storeRef;
    }

    public void setStoreRef(String storeRef) {
        this.storeRef = storeRef;
    }

    public NodeRef getWorkflowNodeRef() {
        if (workflowNodeRefId == null) {
            return parent == null ? null : parent.getNodeRef();
        }
        return new NodeRef(new StoreRef(storeRef), workflowNodeRefId);
    }

    public void setCompoundWorkflowId(String compoundWorkflowId) {
        setProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_ID, compoundWorkflowId);
    }

    public NodeRef getInitiatingCompoundWorkflowRef() {
        return getProp(INITIATING_COMPOUND_WORKFLOW_REF);
    }

    public String getInitiatingCompoundWorkflowTitle() {
        return getProp(INITIATING_COMPOUND_WORKFLOW_TITLE);
    }

    public void setInitiatingCompoundWorkflowTitle(String title) {
        setProp(INITIATING_COMPOUND_WORKFLOW_TITLE, title);
    }

    public String getCompoundWorkflowId() {
        return getProp(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_ID);
    }

}
