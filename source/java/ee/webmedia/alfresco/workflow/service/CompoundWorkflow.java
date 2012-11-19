package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public class CompoundWorkflow extends BaseWorkflowObject implements Serializable, Comparable<CompoundWorkflow> {
    private static final long serialVersionUID = 1L;

    private final NodeRef parent;
    private final List<Workflow> workflows = new ArrayList<Workflow>();
    private List<CompoundWorkflow> otherCompoundWorkflows = new ArrayList<CompoundWorkflow>();
    private final List<Workflow> removedWorkflows = new ArrayList<Workflow>();
    private Integer numberOfDocuments;
    private final List<NodeRef> newAssocs = new ArrayList<NodeRef>();
    private final List<RelatedUrl> newRelatedUrls = new ArrayList<RelatedUrl>();

    private List<Pair<String, Object[]>> reviewTaskDvkInfoMessages;

    protected CompoundWorkflow(WmNode node, NodeRef parent) {
        super(node);
        Assert.notNull(parent);
        this.parent = parent;
    }

    protected CompoundWorkflow copy() {
        return copyImpl(new CompoundWorkflow(getNode().clone(), parent));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) super.copyImpl(copy);
        for (Workflow workflow : workflows) {
            compoundWorkflow.workflows.add(workflow.copy(compoundWorkflow));
        }
        for (Workflow removedWorkflow : removedWorkflows) {
            compoundWorkflow.removedWorkflows.add(removedWorkflow.copy(compoundWorkflow));
        }
        for (NodeRef docRef : newAssocs) {
            compoundWorkflow.newAssocs.add(docRef);
        }
        for (RelatedUrl relatedUrl : newRelatedUrls) {
            compoundWorkflow.newRelatedUrls.add(relatedUrl);
        }
        @SuppressWarnings("unchecked")
        T result = (T) compoundWorkflow;
        return result;
    }

    public NodeRef getParent() {
        return parent;
    }

    public List<Workflow> getWorkflows() {
        return Collections.unmodifiableList(workflows);
    }

    /**
     * NB! At moment it is not quaranteed that this property contains updated info from repo,
     * it is used only to pass (possibly changed) compound workflows to and from AssignmentWorkflowType.
     * Returned list doesn't (and MUST NOT) contain this compound workflow.
     * 
     * @return
     */
    public List<CompoundWorkflow> getOtherCompoundWorkflows() {
        return otherCompoundWorkflows;
    }

    public void setOtherCompoundWorkflows(List<CompoundWorkflow> otherCompoundWorkflows) {
        Assert.notNull(otherCompoundWorkflows);
        this.otherCompoundWorkflows = otherCompoundWorkflows;
    }

    protected List<Workflow> getRemovedWorkflows() {
        return removedWorkflows;
    }

    public void removeWorkflow(int index) {
        removedWorkflows.add(workflows.remove(index));
    }

    protected void addWorkflow(Workflow workflow) {
        workflows.add(workflow);
    }

    protected void addWorkflow(Workflow workflow, int index) {
        workflows.add(index, workflow);
    }

    @Override
    public String getOwnerId() {
        return getProp(WorkflowCommonModel.Props.OWNER_ID);
    }

    @Override
    public void setOwnerId(String ownerId) {
        setProp(WorkflowCommonModel.Props.OWNER_ID, ownerId);
    }

    public String getOwnerName() {
        return getProp(WorkflowCommonModel.Props.OWNER_NAME);
    }

    public void setOwnerName(String ownerName) {
        setProp(WorkflowCommonModel.Props.OWNER_NAME, ownerName);
    }

    @SuppressWarnings("unchecked")
    public String getOwnerStructUnit() {
        return UserUtil.getDisplayUnit((List<String>) getNode().getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
    }

    @SuppressWarnings("unchecked")
    public List<String> getOwnerStructUnitProp() {
        return (List<String>) getProp(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
    }

    public void setOwnerStructUnit(List<String> structUnit) {
        setProp(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) structUnit);
    }

    public String getOwnerJobTitle() {
        return getProp(WorkflowCommonModel.Props.OWNER_JOB_TITLE);
    }

    public void setOwnerJobTitle(String ownerJobTitle) {
        setProp(WorkflowCommonModel.Props.OWNER_JOB_TITLE, ownerJobTitle);
    }

    public CompoundWorkflowType getTypeEnum() {
        String typeStr = getProp(WorkflowCommonModel.Props.TYPE);
        return StringUtils.isBlank(typeStr) ? null : CompoundWorkflowType.valueOf(typeStr);
    }

    public void setTypeEnum(CompoundWorkflowType type) {
        setProp(WorkflowCommonModel.Props.TYPE, type.name());
    }

    public String getCreatedDateStr() {
        Date created = getCreatedDateTime();
        return created != null ? Task.dateTimeFormat.format(created) : "";
    }

    public Date getCreatedDateTime() {
        return getProp(WorkflowCommonModel.Props.CREATED_DATE_TIME);
    }

    public String getStartedDateStr() {
        Date started = getStartedDateTime();
        return started != null ? Task.dateFormat.format(started) : "";
    }

    public String getStoppedDateStr() {
        Date stopped = getStoppedDateTime();
        return stopped != null ? Task.dateFormat.format(stopped) : "";
    }

    public String getEndedDateStr() {
        Date ended = getEndedDateTime();
        return ended != null ? Task.dateFormat.format(ended) : "";
    }

    public Date getEndedDateTime() {
        return getProp(WorkflowCommonModel.Props.FINISHED_DATE_TIME);
    }

    public String getComment() {
        return getProp(WorkflowCommonModel.Props.COMMENT);
    }

    public String getWorkflowTypeString() {
        return MessageUtil.getMessage(getTypeEnum());
    }

    public String getTitle() {
        return getProp(WorkflowCommonModel.Props.TITLE);
    }

    public void setTitle(String title) {
        setProp(WorkflowCommonModel.Props.TITLE, title);
    }

    @Override
    protected String additionalToString() {
        return "\n  parent=" + getParent() + "\n  workflows=" + WmNode.toString(getWorkflows()) + "\n  removedWorkflows="
                + WmNode.toString(getRemovedWorkflows());
    }

    public boolean isDocumentWorkflow() {
        return getTypeEnum() == null || isType(CompoundWorkflowType.DOCUMENT_WORKFLOW);
    }

    public boolean isIndependentWorkflow() {
        return isType(CompoundWorkflowType.INDEPENDENT_WORKFLOW);
    }

    public boolean isCaseFileWorkflow() {
        return isType(CompoundWorkflowType.CASE_FILE_WORKFLOW);
    }

    public String action() {
        if (isIndependentWorkflow()) {
            return "dialog:compoundWorkflowDialog";
        }

        // DocumentDynamicDialog and CaseFileDialog handle the navigation with actionListeners
        return null;
    }

    public void actionListener(ActionEvent event) {
        if (isDocumentWorkflow()) {
            BeanHelper.getDocumentDynamicDialog().openFromChildList(event);
        } else if (isIndependentWorkflow()) {
            BeanHelper.getCompoundWorkflowDialog().setupWorkflowFromList(event);
        } else if (isCaseFileWorkflow()) {
            BeanHelper.getCaseFileDialog().openFromChildList(event);
        }
    }

    private boolean isType(CompoundWorkflowType compoundWorkflowType) {
        Assert.notNull(compoundWorkflowType);
        return getTypeEnum() == compoundWorkflowType;
    }

    public void setMainDocument(NodeRef nodeRef) {
        setProp(WorkflowCommonModel.Props.MAIN_DOCUMENT, nodeRef);
    }

    public NodeRef getMainDocument() {
        return getProp(WorkflowCommonModel.Props.MAIN_DOCUMENT);
    }

    public List<NodeRef> getDocumentsToSign() {
        List<NodeRef> documentsToSign = getPropList(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN);
        if (documentsToSign == null) {
            documentsToSign = new ArrayList<NodeRef>();
            setPropList(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, documentsToSign);
        }
        return documentsToSign;
    }

    public List<String> getDocumentsToSignNodeRefIds() {
        return RepoUtil.getNodeRefIds(getDocumentsToSign());
    }

    public void setNumberOfDocuments(int numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public int getNumberOfDocuments() {
        if (numberOfDocuments == null) {
            if (isDocumentWorkflow()) {
                numberOfDocuments = 1;
            } else {
                numberOfDocuments = BeanHelper.getWorkflowService().getCompoundWorkflowDocumentCount(getNodeRef());
            }
        }
        return numberOfDocuments;
    }

    public String getNumberOfDocumentsStr() {
        return new Integer(getNumberOfDocuments()).toString();
    }

    @Override
    public int compareTo(CompoundWorkflow wf) {
        String ownerName = getOwnerName();
        if (StringUtils.isNotBlank(ownerName)) {
            if (StringUtils.isBlank(wf.getOwnerName())) {
                return -1;
            }
            return AppConstants.DEFAULT_COLLATOR.compare(ownerName, wf.getOwnerName());
        }
        return 0;
    }

    public List<NodeRef> getNewAssocs() {
        return newAssocs;
    }

    public List<RelatedUrl> getNewRelatedUrls() {
        return newRelatedUrls;
    }

    public List<Pair<String, Object[]>> getReviewTaskDvkInfoMessages() {
        if (reviewTaskDvkInfoMessages == null) {
            reviewTaskDvkInfoMessages = new ArrayList<Pair<String, Object[]>>();
        }
        return reviewTaskDvkInfoMessages;
    }

    public void setReviewTaskDvkInfoMessages(List<Pair<String, Object[]>> reviewTaskDvkInfoMessages) {
        this.reviewTaskDvkInfoMessages = reviewTaskDvkInfoMessages;
    }

}