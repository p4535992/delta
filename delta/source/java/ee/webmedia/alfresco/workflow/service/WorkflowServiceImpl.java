package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkCompoundWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkTask;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getExcludedNodeRefsOnFinishWorkflows;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isGeneratedByDelegation;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isInactiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatusAll;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatusAny;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.requireStatusUnchanged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types;
import ee.webmedia.alfresco.workflow.service.event.BaseWorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEvent;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListener;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventListenerWithModifications;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue.WorkflowQueueParameter;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventType;
import ee.webmedia.alfresco.workflow.service.event.WorkflowModifications;
import ee.webmedia.alfresco.workflow.service.type.AssignmentWorkflowType;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Alar Kvell
 */
public class WorkflowServiceImpl implements WorkflowService, WorkflowModifications {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowServiceImpl.class);
    private static final int SIGNATURE_TASK_OUTCOME_NOT_SIGNED = 0;
    private static final int REVIEW_TASK_OUTCOME_REJECTED = 2;

    /*
     * There are two ways to be notified of events:
     * 1) WorkflowEventListener which is registered via #registerEventListener, is called at the end of each service call.
     * Events are queued up during service call and are passed to event handler at the end, when previous actions were successful.
     * This is suitable for e-mail sending.
     * 2) If WorkflowType, which is registered via #registerWorkflowType, implements WorkflowEventListener or WorkflowEventListenerWithModifications,
     * then it is called on every event, right away. This is suitable for making additional modifications to workflow objects or repository,
     * for example registering document or finishing the task that was just started.
     */

    /*
     * handleEvents should be called as last step in changing workflow,
     * because handleEvents sends email notifications and in case of failure
     * in the middle of transaction some emails may be sent already
     * if handleEvents is called before all other workflow changes are done
     */

    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private GeneralService generalService;
    private UserService userService;
    private NamespaceService namespaceService;
    private OrganizationStructureService organizationStructureService;
    private DvkService dvkService;
    private ParametersService parametersService;

    private final Map<QName, WorkflowType> workflowTypesByWorkflow = new HashMap<QName, WorkflowType>();
    private final Map<QName, WorkflowType> workflowTypesByTask = new HashMap<QName, WorkflowType>();
    private final List<WorkflowEventListener> eventListeners = new ArrayList<WorkflowEventListener>();
    private final List<WorkflowEventListenerWithModifications> immediateEventListeners = new ArrayList<WorkflowEventListenerWithModifications>();

    /**
     * Seoses asutuseülese töövoo testimisega meie testis, kus asutus peab saama saata ülesandeid ka endale:
     * dokumendi vastuvõtmisel ja olemasoleva dokumendi otsimisele kontrollitakse
     * lisaks originalDvkId-le ka seda, et dokumendil oleks olemas aspekt notEditable, property notEditable=true.
     * Kui ei ole, siis tehakse uus dok. (Max peaks saama ühes süsteemis olla kaks dokumenti
     * sama originalDvkId-ga taskiga ja üks on alati notEditable sel juhul).
     * Ülesande teostamise vastuvõtmisel eelistatakse sellise dokumendi küljes olevat ülesannet,
     * millel ei ole notEditable aspekti. (Võib olla, et dokument on korduvalt edasi saadetud,
     * sel juhul ei ole ilma notEditable aspektita dokumenti olemas).
     * Testis tekib probleem sellise edasisaatmise korral, kui saata endale ja siis
     * edasisaadetud dokument uuesti endale, seda varianti ei saa testida.
     * Et asutus saaks tööülesannet saata iseendale, tuleb INTERNAL_TESTING väärtustada true,
     * sel juhul kuvatakse tööülesande täitja otsingus kontaktide nimekirjas ka
     * asutuse enda regitrikoodiga kontakt.
     * NB! Live keskkonnas PEAB INTERNAL_TESTING väärtus olema false!!!
     */
    private boolean INTERNAL_TESTING = false;

    @Override
    public void registerWorkflowType(WorkflowType workflowType) {
        Assert.notNull(workflowType);
        if (log.isDebugEnabled()) {
            log.debug("Registering workflowType:\n" + workflowType);
        }

        Assert.isTrue(!workflowTypesByWorkflow.containsKey(workflowType.getWorkflowType()));
        QName workflowTypeQName = workflowType.getWorkflowType();
        Assert.isTrue(dictionaryService.isSubClass(workflowTypeQName, WorkflowCommonModel.Types.WORKFLOW));
        workflowTypesByWorkflow.put(workflowTypeQName, workflowType);

        QName taskTypeQName = workflowType.getTaskType();
        if (taskTypeQName != null) {
            Assert.notNull(workflowType.getTaskClass());
            Assert.isTrue(!workflowTypesByTask.containsKey(taskTypeQName));
            Assert.isTrue(dictionaryService.isSubClass(taskTypeQName, WorkflowCommonModel.Types.TASK));
            workflowTypesByTask.put(taskTypeQName, workflowType);
        }
    }

    @Override
    public Map<QName, WorkflowType> getWorkflowTypes() {
        return Collections.unmodifiableMap(workflowTypesByWorkflow);
    }

    // ========================================================================
    // ========================== GET FROM REPOSITORY =========================
    // ========================================================================

    private NodeRef getRoot() {
        return generalService.getNodeRef(WorkflowCommonModel.Repo.WORKFLOWS_SPACE);
    }

    @Override
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions() {
        NodeRef root = getRoot();
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(root);
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            compoundWorkflowDefinitions.add(getCompoundWorkflowDefinition(nodeRef, root));
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(QName documentType, String documentStatus) {
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(documentStatus);
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = getCompoundWorkflowDefinitions();
        outer: //
        for (Iterator<CompoundWorkflowDefinition> i = compoundWorkflowDefinitions.iterator(); i.hasNext();) {
            CompoundWorkflowDefinition compoundWorkflowDefinition = i.next();
            if (!compoundWorkflowDefinition.getDocumentTypes().contains(documentType)) {
                i.remove();
                continue outer;
            }
            if (isFinished) {
                for (Workflow workflow : compoundWorkflowDefinition.getWorkflows()) {
                    QName workflowType = workflow.getNode().getType();
                    if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType) ||
                                WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(workflowType) ||
                                WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(workflowType)) {
                        i.remove();
                        continue outer;
                    }
                }
            }
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef) {
        return getCompoundWorkflowDefinition(nodeRef, nodeService.getPrimaryParent(nodeRef).getParentRef());
    }

    private CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef, NodeRef parent) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION, false, false);
        CompoundWorkflowDefinition compoundWorkflowDefinition = new CompoundWorkflowDefinition(node, parent);
        getAndAddWorkflows(nodeRef, compoundWorkflowDefinition, false);
        return compoundWorkflowDefinition;
    }

    @Override
    public List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent) {
        List<NodeRef> nodeRefs = getCompoundWorkflowNodeRefs(parent);
        List<CompoundWorkflow> compoundWorkflows = new ArrayList<CompoundWorkflow>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            compoundWorkflows.add(getCompoundWorkflow(nodeRef));
        }
        return compoundWorkflows;
    }

    private List<NodeRef> getCompoundWorkflowNodeRefs(NodeRef parent) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW);
        List<NodeRef> compoundWorkflows = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            compoundWorkflows.add(childAssoc.getChildRef());
        }
        return compoundWorkflows;
    }

    @Override
    public CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        getAndAddWorkflows(compoundWorkflow.getNode().getNodeRef(), compoundWorkflow, false);
        return compoundWorkflow;
    }

    @Override
    public void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow) {
        List<CompoundWorkflow> compoundWorkflows = getCompoundWorkflows(compoundWorkflow.getParent());
        // remove current compound workflow
        for (Iterator<CompoundWorkflow> it = compoundWorkflows.iterator(); it.hasNext();) {
            if (it.next().getNode().getNodeRef().equals(compoundWorkflow.getNode().getNodeRef())) {
                it.remove();
                break;
            }
        }
        compoundWorkflow.setOtherCompoundWorkflows(compoundWorkflows);
    }

    private void getAndAddWorkflows(NodeRef parent, CompoundWorkflow compoundWorkflow, boolean copy) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            Workflow workflow = getWorkflow(nodeRef, compoundWorkflow, copy);
            compoundWorkflow.addWorkflow(workflow);
            getAndAddTasks(nodeRef, workflow, copy);
        }
    }

    private Workflow getWorkflow(NodeRef nodeRef, CompoundWorkflow compoundWorkflow, boolean copy) {
        WmNode workflowNode = getNode(nodeRef, WorkflowCommonModel.Types.WORKFLOW, true, copy);
        return getWorkflow(compoundWorkflow, workflowNode);
    }

    private Workflow getWorkflow(CompoundWorkflow compoundWorkflow, WmNode workflowNode) {
        WorkflowType workflowType = workflowTypesByWorkflow.get(workflowNode.getType());
        if (workflowType == null) {
            throw new RuntimeException("Workflow type '" + workflowNode.getType() + "' not registered in service");
        }
        QName taskType = workflowType.getTaskType();
        WmNode taskNode = getTaskTemplateByType(taskType);

        Workflow workflow = Workflow.create(workflowType.getWorkflowClass(), workflowNode, compoundWorkflow, taskNode, workflowType.getTaskClass(),
                workflowType.getTaskOutcomes());
        return workflow;
    }

    @Override
    public WmNode getTaskTemplateByType(QName taskType) {
        if (taskType == null) {
            return null;
        }
        return new WmNode(null, taskType.getPrefixedQName(namespaceService), getDefaultAspects(taskType), getDefaultProperties(taskType));
    }

    private void getAndAddTasks(NodeRef parent, Workflow workflow, boolean copy) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            Task task = getTask(nodeRef, workflow, copy);
            workflow.addTask(task);
        }
    }

    @Override
    public Task getTask(NodeRef nodeRef, boolean fetchWorkflow) {
        Workflow workflow = null;
        if (fetchWorkflow) {
            NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
            workflow = getWorkflow(parent, null, false);
        }
        return getTask(nodeRef, workflow, false);
    }

    @Override
    public Set<Task> getTasks(NodeRef docRef, Predicate<Task> taskPredicate) {
        Set<Task> selectedTasks = new HashSet<Task>();
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    if (taskPredicate.evaluate(task)) {
                        selectedTasks.add(task);
                    }
                }
            }
        }
        return selectedTasks;
    }

    private Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        WmNode taskNode = getNode(nodeRef, WorkflowCommonModel.Types.TASK, true, copy);

        WorkflowType workflowType = workflowTypesByTask.get(taskNode.getType());
        if (workflowType == null) {
            throw new RuntimeException("Task type '" + taskNode.getType() + "' not registered in service, but existing node has it: " + nodeRef);
        }

        // If workflowType exists, then getTaskClass() cannot return null
        Task task = Task.create(workflowType.getTaskClass(), taskNode, workflow, workflowType.getTaskOutcomes());
        return task;
    }

    private WmNode getNode(NodeRef nodeRef, QName typeToCheck, boolean allowSubType, boolean copy) {
        QName type = nodeService.getType(nodeRef);
        if (allowSubType) {
            if (!dictionaryService.isSubClass(type, typeToCheck)) {
                throw new RuntimeException("Node type '" + type.toPrefixString(namespaceService) + "' is not a subclass of node type '"
                        + typeToCheck.toPrefixString(namespaceService) + "'");
            }
        } else {
            if (!typeToCheck.equals(type)) {
                throw new RuntimeException("Node type '" + type.toPrefixString(namespaceService) + "' is not equal to node type '"
                        + typeToCheck.toPrefixString(namespaceService) + "'");
            }
        }
        return new WmNode(copy ? null : nodeRef, type.getPrefixedQName(namespaceService), getNodeAspects(nodeRef), getNodeProperties(nodeRef));
    }

    // ========================================================================
    // =========================== OPERATE IN MEMORY ==========================
    // ========================================================================

    @Override
    public CompoundWorkflowDefinition getNewCompoundWorkflowDefinition() {
        QName type = WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION;
        WmNode compoundWorkflowNode = new WmNode(null, type.getPrefixedQName(namespaceService), getDefaultAspects(type), getDefaultProperties(type));
        CompoundWorkflowDefinition compoundWorkflowDefinition = new CompoundWorkflowDefinition(compoundWorkflowNode, getRoot());

        if (log.isDebugEnabled()) {
            log.debug("Creating new " + compoundWorkflowDefinition);
        }
        checkCompoundWorkflow(compoundWorkflowDefinition, Status.NEW);
        return compoundWorkflowDefinition;
    }

    @Override
    public CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent) {
        QName type = nodeService.getType(compoundWorkflowDefinition);
        if (!type.equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
            throw new RuntimeException("Node is not a compoundWorkflowDefinition, type '" + type.toPrefixString(namespaceService) + "', nodeRef="
                    + compoundWorkflowDefinition);
        }

        Map<QName, Serializable> props = getNodeProperties(compoundWorkflowDefinition);
        // Remove compoundWorkflowDefinition's properties, so we are left with only compoundWorkflow properties
        Map<QName, PropertyDefinition> propertyDefs = dictionaryService.getPropertyDefs(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        for (PropertyDefinition propDef : propertyDefs.values()) {
            if (propDef.getContainerClass().getName().equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
                props.remove(propDef.getName());
            }
        }
        Set<QName> aspects = getNodeAspects(compoundWorkflowDefinition);

        QName compoundWorkflowType = WorkflowCommonModel.Types.COMPOUND_WORKFLOW;
        WmNode compoundWorkflowNode = new WmNode(null, compoundWorkflowType.getPrefixedQName(namespaceService), aspects, props);
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(compoundWorkflowNode, parent);
        getAndAddWorkflows(compoundWorkflowDefinition, compoundWorkflow, true);

        // Set default owner to current user
        String userName = getUserNameToSave();
        compoundWorkflow.setOwnerId(userName);
        compoundWorkflow.setOwnerName(userService.getUserFullName(userName));

        if (log.isDebugEnabled()) {
            log.debug("Creating new " + compoundWorkflow);
        }
        checkCompoundWorkflow(compoundWorkflow, true, Status.NEW);
        return compoundWorkflow;
    }

    @Override
    public Workflow addNewWorkflow(CompoundWorkflow compoundWorkflow, QName workflowTypeQName, int index, boolean validateWorkflowIsNew) {
        WorkflowType workflowType = workflowTypesByWorkflow.get(workflowTypeQName);
        if (workflowType == null) {
            throw new RuntimeException("Workflow type '" + workflowTypeQName + "' not registered in service");
        }
        WmNode workflowNode = new WmNode(null, workflowTypeQName.getPrefixedQName(namespaceService), getDefaultAspects(workflowTypeQName),
                getDefaultProperties(workflowTypeQName));

        Workflow workflow = getWorkflow(compoundWorkflow, workflowNode);
        workflow.postCreate();

        // Set workflow creator to current user - not done here, but on saving; probably not needed in web client before that
        // workflow.setCreatorName(userService.getUserFullName());

        if (log.isDebugEnabled()) {
            log.debug("Adding new " + workflow);
        }
        if (validateWorkflowIsNew) {
            checkWorkflow(workflow, Status.NEW);
        }
        compoundWorkflow.addWorkflow(workflow, index);
        return workflow;
    }

    // ========================================================================
    // ========================== SAVE TO REPOSITORY ==========================
    // ========================================================================

    @Override
    public CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinitionOriginal) {
        CompoundWorkflowDefinition compoundWorkflowDefinition = compoundWorkflowDefinitionOriginal.copy();
        checkCompoundWorkflow(compoundWorkflowDefinition, true, Status.NEW); // XXX check at the beginning...
        requireStatusUnchanged(compoundWorkflowDefinition);

        WorkflowEventQueue queue = getNewEventQueue();
        boolean changed = saveCompoundWorkflow(queue, compoundWorkflowDefinition, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW_DEFINITION);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + compoundWorkflowDefinition);
        }

        // also check repo status
        CompoundWorkflowDefinition freshCompoundWorkflowDefinition = getCompoundWorkflowDefinition(compoundWorkflowDefinition.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflowDefinition, true, Status.NEW);
        // ignore events
        return freshCompoundWorkflowDefinition;
    }

    @Override
    public CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        return saveCompoundWorkflow(compoundWorkflowOriginal.copy(), getNewEventQueue());
    }

    /**
     * @param compoundWorkflow
     * @param queue if null, then queue is created and operations are performed on copy of <code>compoundWorkflow</code>,
     *            otherwise <code>queue</code> and <code>compoundWorkflow</code> are used to perform operations
     * @return fresh workflow based on <code>compoundWorkflow</code>(fetched from repo after saving)
     */
    private CompoundWorkflow saveCompoundWorkflow(CompoundWorkflow compoundWorkflow, WorkflowEventQueue queue) {
        saveCompoundWorkflow(queue, compoundWorkflow);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + compoundWorkflow);
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    public void saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        // checkCompoundWorkflow(compoundWorkflow, Status.NEW, Status.IN_PROGRESS, Status.STOPPED); // XXX NO check at the beginning...
        // XXX is it ok that this ^^ check is before we process task's finish/unfinish action?
        requireStatusUnchanged(compoundWorkflow);

        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.getAction() == Task.Action.FINISH) {
                    // Finish task
                    requireStatus(task, Status.IN_PROGRESS, Status.STOPPED, Status.UNFINISHED);
                    setTaskFinished(queue, task);

                } else if (task.getAction() == Task.Action.UNFINISH) {
                    // Unfinish task
                    requireStatus(task, Status.IN_PROGRESS, Status.STOPPED);
                    setTaskUnfinished(queue, task);
                }
            }
        }
        stepAndCheck(queue, compoundWorkflow);

        boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
    }

    private boolean saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, QName assocType) {
        if (assocType == null) {
            assocType = WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW;
        }
        boolean changed = createOrUpdate(queue, compoundWorkflow, compoundWorkflow.getParent(), assocType);

        // Remove workflows
        for (Workflow removedWorkflow : compoundWorkflow.getRemovedWorkflows()) {
            NodeRef removedWorkflowNodeRef = removedWorkflow.getNode().getNodeRef();
            if (removedWorkflowNodeRef != null) {
                checkWorkflow(getWorkflow(removedWorkflowNodeRef, compoundWorkflow, false), Status.NEW);
                nodeService.deleteNode(removedWorkflowNodeRef);
                changed = true;
            }
        }
        compoundWorkflow.getRemovedWorkflows().clear();

        int index = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            // Create or update workflow
            saveWorkflow(queue, workflow);

            // Update index
            ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(workflow.getNode().getNodeRef());
            if (childAssocRef.getNthSibling() != index) {
                nodeService.setChildAssociationIndex(childAssocRef, index);
                changed = true;
            }
            index++;
        }
        // changing workflow may have changed other workflows of the document too,
        // so we need to save them also
        for (CompoundWorkflow otherCompoundWorkflow : compoundWorkflow.getOtherCompoundWorkflows()) {
            saveCompoundWorkflow(queue, otherCompoundWorkflow, null);
            CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(otherCompoundWorkflow.getNode().getNodeRef());
            checkCompoundWorkflow(freshCompoundWorkflow);
        }
        return changed;
    }

    private boolean saveWorkflow(WorkflowEventQueue queue, Workflow workflow) {
        boolean changed = createOrUpdate(queue, workflow, workflow.getParent().getNode().getNodeRef(), WorkflowCommonModel.Assocs.WORKFLOW);

        // Remove tasks
        for (Task removedTask : workflow.getRemovedTasks()) {
            NodeRef removedTaskNodeRef = removedTask.getNode().getNodeRef();
            if (removedTaskNodeRef != null) {
                checkTask(getTask(removedTaskNodeRef, workflow, false), Status.NEW);
                nodeService.deleteNode(removedTaskNodeRef);
                changed = true;
            }
        }
        workflow.getRemovedTasks().clear();

        int index = 0;
        for (Task task : workflow.getTasks()) {
            // Create or update task
            saveTask(queue, task);

            // Update index
            ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(task.getNode().getNodeRef());
            if (childAssocRef.getNthSibling() != index) {
                nodeService.setChildAssociationIndex(childAssocRef, index);
                changed = true;
            }
            index++;
        }
        return changed;
    }

    @Override
    public MessageDataWrapper delegate(Task assignmentTaskOriginal) throws UnableToPerformMultiReasonException {
        MessageDataWrapper feedback = new MessageDataWrapper();
        Workflow workflowOriginal = assignmentTaskOriginal.getParent();
        CompoundWorkflow cWorkflowOriginal = workflowOriginal.getParent();
        // assuming that originalWFIndex doesn't change after removing or saving
        int originalWFIndex = cWorkflowOriginal.getWorkflows().indexOf(workflowOriginal);
        log.debug("originalCWorkflow=" + cWorkflowOriginal);
        CompoundWorkflow cWorkflowCopy = cWorkflowOriginal.copy();
        List<Workflow> cWorkflowWorkflowsCopy = cWorkflowCopy.getWorkflows();
        AssignmentWorkflow assignmentWorkflowCopy = (AssignmentWorkflow) cWorkflowWorkflowsCopy.get(originalWFIndex);

        int originalTaskIndexBeforeRemove = workflowOriginal.getTasks().indexOf(assignmentTaskOriginal);
        Task assignmentTaskCopy = assignmentWorkflowCopy.getTasks().get(originalTaskIndexBeforeRemove);
        assignmentTaskCopy.getNode().getProperties().put(AssignmentWorkflowType.TEMP_DELEGATED.toString(), true);
        if (StringUtils.isBlank(assignmentTaskCopy.getComment())) {
            assignmentTaskCopy.setComment(I18NUtil.getMessage("task_comment_delegated"));
            if (WorkflowUtil.isActiveResponsible(assignmentTaskCopy)) {
                assignmentTaskCopy.setActive(false);
            }
        }

        { // removeEmptyDelegationTasks
            WorkflowUtil.removeEmptyTasks(cWorkflowCopy);
            // also remove empty workflows that could be created when information or opinion tasks are added during delegating assignment task
            ArrayList<Integer> emptyWfIndexes = new ArrayList<Integer>();
            int wfIndex = 0;
            for (Workflow workflow1 : cWorkflowWorkflowsCopy) {
                if (WorkflowUtil.isGeneratedByDelegation(workflow1) && workflow1.getTasks().isEmpty()) {
                    emptyWfIndexes.add(wfIndex);
                }
                wfIndex++;
            }
            Collections.reverse(emptyWfIndexes);
            for (int emptyWfIndex : emptyWfIndexes) {
                cWorkflowCopy.removeWorkflow(emptyWfIndex);
            }
        }

        WorkflowEventQueue queue = getNewEventQueue();

        List<Integer> delegateAssignmentTaskIndexes = new ArrayList<Integer>();
        { // validate that at least one new equivalent task is created (and it contains minimal information)
            boolean searchResponsibleTask = WorkflowUtil.isActiveResponsible(assignmentTaskOriginal);
            Task newMandatoryTask = null;
            { // validate that tasks added during delegation have all mandatory fields filled and at least one task equivalent to delegatable task is also added
                for (Workflow workflow : cWorkflowWorkflowsCopy) {
                    int taskIndex = 0;
                    if (isGeneratedByDelegation(workflow)) {
                        setStatus(queue, workflow, Status.IN_PROGRESS);
                    }
                    for (Task task : workflow.getTasks()) {
                        if (isGeneratedByDelegation(task)) {
                            delegationTaskMandatoryFieldsFilled(task, feedback);
                            if (workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && newMandatoryTask == null) {
                                if (!searchResponsibleTask) {
                                    newMandatoryTask = task;
                                } else if (WorkflowUtil.isActiveResponsible(task)) {
                                    newMandatoryTask = task;
                                }
                            }
                            delegateAssignmentTaskIndexes.add(taskIndex);
                        }
                        taskIndex++;
                    }
                }
            }
            if (newMandatoryTask == null) {
                if (searchResponsibleTask) {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewResponsibleTask"));
                } else {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewTask"));
                }
            }
        }

        if (feedback.hasErrors()) {
            throw new UnableToPerformMultiReasonException(feedback);
        }
        CompoundWorkflow savedCompoundWorkflow = saveCompoundWorkflow(cWorkflowCopy, queue);

        savedCompoundWorkflow.getWorkflows().get(originalWFIndex);
        return feedback;
    }

    @Override
    public List<Task> getTasks4DelegationHistory(Node delegatableTask) {
        NodeRef docRef = generalService.getAncestorNodeRefWithType(delegatableTask.getNodeRef(), DocumentCommonModel.Types.DOCUMENT, true);
        List<Task> assignmentTasks = new ArrayList<Task>();
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                    for (Task task : workflow.getTasks()) {
                        if (task.isStatus(Status.FINISHED, Status.IN_PROGRESS)) {
                            assignmentTasks.add(task);
                        }
                    }
                }
            }
        }
        return assignmentTasks;
    }

    private static void delegationTaskMandatoryFieldsFilled(Task task, MessageDataWrapper feedback) {
        boolean noOwner = StringUtils.isBlank(task.getOwnerName());
        QName taskType = task.getType();
        String key = "delegate_error_taskMandatory_" + taskType.getLocalName();
        if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
            if (noOwner) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            }
        } else if (noOwner || task.getDueDate() == null) {
            if (taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            } else {
                if (task.isResponsible()) {
                    key += "_responsible";
                }
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, key));
            }

        }
    }

    private boolean saveTask(WorkflowEventQueue queue, Task task) {
        return createOrUpdate(queue, task, task.getParent().getNode().getNodeRef(), WorkflowCommonModel.Assocs.TASK);
    }

    @Override
    public void saveInProgressTask(Task taskOriginal) throws WorkflowChangedException {
        Task task = taskOriginal.copy();
        // check status==IN_PROGRESS, owner==currentUser
        requireInProgressCurrentUser(task);
        requireStatusUnchanged(task);

        WorkflowEventQueue queue = getNewEventQueue();
        boolean changed = saveTask(queue, task);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + task);
        }
        handleEvents(queue);
    }

    @Override
    public void finishInProgressTask(Task taskOriginal, int outcomeIndex) throws WorkflowChangedException {
        if (outcomeIndex < 0 || outcomeIndex >= taskOriginal.getOutcomes()) {
            throw new RuntimeException("outcomeIndex '" + outcomeIndex + "' out of bounds for " + taskOriginal);
        }
        // check status==IN_PROGRESS, owner==currentUser
        requireInProgressCurrentUser(taskOriginal);
        requireStatus(taskOriginal.getParent(), Status.IN_PROGRESS); // XXX this is not needed??
        requireStatusUnchanged(taskOriginal);

        // operate on compoundWorkflow that was fetched fresh from repo
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(taskOriginal.getParent().getParent().getNode().getNodeRef());
        // insert (possibly modified) task into compoundWorkflow from repo
        Task task = replaceTask(taskOriginal, compoundWorkflow);

        WorkflowEventQueue queue = getNewEventQueue();

        queue.setParameter(WorkflowQueueParameter.TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM,
                new Boolean(isRecievedExternalReviewTask(task)));
        setTaskFinished(queue, task, outcomeIndex);

        saveAndCheckCompoundWorkflow(queue, compoundWorkflow);
    }

    @Override
    public boolean isRecievedExternalReviewTask(Task task) {
        return task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                && (isInternalTesting() && !Boolean.TRUE.equals(nodeService.getProperty(task.getParent().getParent().getParent(),
                        DocumentSpecificModel.Props.NOT_EDITABLE)))
                || (isInternalTesting() && !isResponsibleCurrenInstitution(task));
    }

    private boolean isResponsibleCurrenInstitution(Task task) {
        return task.getInstitutionCode() != null && task.getInstitutionCode().equalsIgnoreCase(dvkService.getInstitutionCode());
    }

    // finishing initiated by external reviewer from another application instance
    @Override
    public void finishInProgressExternalReviewTask(Task taskOriginal, String comment, String outcome, Date dateCompleted, String dvkId)
            throws WorkflowChangedException {
        taskOriginal.setComment(comment);
        taskOriginal.setOutcome(outcome, -1);
        taskOriginal.setCompletedDateTime(dateCompleted);
        taskOriginal.setProp(WorkflowSpecificModel.Props.RECIEVED_DVK_ID, dvkId);

        requireStatus(taskOriginal, Status.IN_PROGRESS);
        requireStatus(taskOriginal.getParent(), Status.IN_PROGRESS);

        // operate on compoundWorkflow that was fetched fresh from repo
        NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(taskOriginal.getParent().getNode().getNodeRef()).getParentRef();
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(compoundWorkflowRef);

        Task task = replaceTask(taskOriginal, compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        queue.setParameter(WorkflowQueueParameter.EXTERNAL_REVIEWER_TRIGGERING_TASK, task.getNode().getNodeRef());

        setTaskFinishedOrUnfinished(queue, task, Status.FINISHED, null, -1, false);

        saveAndCheckCompoundWorkflow(queue, compoundWorkflow);
    }

    private void saveAndCheckCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) throws WorkflowChangedException {
        // check and save current task's compound workflow
        stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);

        boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + compoundWorkflow);
        }
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);

        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
    }

    private Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow) {
        Task newTask = null;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            List<Task> tasks = workflow.getModifiableTasks();
            if (tasks != null && tasks.size() > 0) {
                int i = 0;
                for (; i < tasks.size(); i++) {
                    if (tasks.get(i).getNode().getNodeRef().equals(replacementTask.getNode().getNodeRef())) {
                        newTask = replacementTask.copy(workflow);
                        break;
                    }
                }
                if (newTask != null) {
                    tasks.set(i, newTask);
                    break;
                }
            }
        }
        return newTask;
    }

    @Override
    public void deleteCompoundWorkflow(NodeRef nodeRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(compoundWorkflow, Status.NEW);
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + compoundWorkflow);
        }
        nodeService.deleteNode(nodeRef);
    }

    @Override
    public CompoundWorkflow saveAndStartCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        saveCompoundWorkflow(queue, compoundWorkflow);
        return startCompoundWorkflow(queue, compoundWorkflow.getNode().getNodeRef());
    }

    @Override
    public CompoundWorkflow startCompoundWorkflow(NodeRef nodeRef) {
        WorkflowEventQueue queue = getNewEventQueue();
        return startCompoundWorkflow(queue, nodeRef);
    }

    private CompoundWorkflow startCompoundWorkflow(WorkflowEventQueue queue, NodeRef nodeRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(compoundWorkflow, Status.NEW);

        setStatus(queue, compoundWorkflow, Status.IN_PROGRESS);
        stepAndCheck(queue, compoundWorkflow);
        boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
        if (log.isDebugEnabled()) {
            log.debug("Started " + compoundWorkflow);
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow saveAndFinishCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        saveCompoundWorkflow(queue, compoundWorkflow);
        return finishCompoundWorkflow(queue, compoundWorkflow.getNode().getNodeRef());
    }

    @Override
    public void finishCompoundWorkflowsOnRegisterDoc(NodeRef docRef, String comment) {
        List<CompoundWorkflow> compoundWorkflows = getCompoundWorkflows(docRef);
        if (compoundWorkflows != null && compoundWorkflows.size() > 0) {
            WorkflowEventQueue queue = getNewEventQueue();
            queue.getParameters().put(WorkflowQueueParameter.TRIGGERED_BY_DOC_REGISTRATION, Boolean.TRUE);
            for (Iterator<CompoundWorkflow> i = compoundWorkflows.iterator(); i.hasNext();) {
                CompoundWorkflow compoundWorkflow = i.next();
                if (compoundWorkflow.isStatus(Status.NEW)) {
                    deleteCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
                    i.remove();
                } else {
                    List<NodeRef> excludedNodeRefs = getExcludedNodeRefsOnFinishWorkflows(compoundWorkflow);
                    finishCompoundWorkflowInner(queue, compoundWorkflow.getNode().getNodeRef(), Status.UNFINISHED,
                            "task_outcome_unfinished_by_registering_reply_letter", comment, true, excludedNodeRefs);
                }
            }
            handleEvents(queue);
        }
    }

    private CompoundWorkflow finishCompoundWorkflow(WorkflowEventQueue queue, NodeRef cWorkflowRef) {
        return finishCompoundWorkflow(queue, cWorkflowRef, Status.FINISHED, "task_outcome_finished_manually", null, false, null);
    }

    private CompoundWorkflow finishCompoundWorkflow(WorkflowEventQueue queue, NodeRef cWorkflowRef, Status taskStatus
            , String taskOutcomeLabelId, String userTaskComment, boolean finishOnRegisterDocument, List<NodeRef> excludedNodeRefs) {
        CompoundWorkflow freshCompoundWorkflow = finishCompoundWorkflowInner(queue, cWorkflowRef, taskStatus, taskOutcomeLabelId, userTaskComment,
                finishOnRegisterDocument, excludedNodeRefs);
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private CompoundWorkflow finishCompoundWorkflowInner(WorkflowEventQueue queue, NodeRef cWorkflowRef, Status taskStatus, String taskOutcomeLabelId,
            String userTaskComment, boolean finishOnRegisterDocument, List<NodeRef> excludedNodeRefs) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(cWorkflowRef);
        // allow all statuses when finishing on registering reply document
        if ((finishOnRegisterDocument && compoundWorkflow.isStatus(Status.FINISHED))
                || (!finishOnRegisterDocument && checkCompoundWorkflow(compoundWorkflow, Status.IN_PROGRESS, Status.FINISHED) == Status.FINISHED)) {
            if (log.isDebugEnabled()) {
                log.debug("CompoundWorkflow is already finished, finishing is not performed, saved as is: " + compoundWorkflow);
            }
        } else {
            setWorkflowsAndTasksFinished(queue, compoundWorkflow, taskStatus, taskOutcomeLabelId, userTaskComment, finishOnRegisterDocument, excludedNodeRefs);
            if (finishOnRegisterDocument || excludedNodeRefs != null) {
                // don't check statuses when finishing on registering reply documents
                // or having excludedNodeRefs
                stepAndCheck(queue, compoundWorkflow);
            } else {
                stepAndCheck(queue, compoundWorkflow, Status.FINISHED);
            }
            boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Finished " + compoundWorkflow);
            }
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        if (!finishOnRegisterDocument && excludedNodeRefs == null) {
            checkCompoundWorkflow(freshCompoundWorkflow, Status.FINISHED);
        }
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        return freshCompoundWorkflow;
    }

    /**
     * param excludedWorkflows - cannot use only workflow type to define excluded workflows
     * because since version 2.2 we need to exclude also external review workflows sent by other institutions
     */
    @Override
    public void setWorkflowsAndTasksFinished(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, Status taskStatus, String taskOutcomeLabelId,
            String userTaskComment,
            boolean finishOnRegisterDocument,
            List<NodeRef> excludedWorkflows) {
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (excludedWorkflows != null && excludedWorkflows.contains(workflow.getNode().getNodeRef())) {
                continue;
            }
            if (isStatus(workflow, Status.NEW, Status.IN_PROGRESS)) {
                setStatus(queue, workflow, Status.FINISHED);

                for (Task task : workflow.getTasks()) {
                    if (finishOnRegisterDocument && isInProgressCurrentUserAssignmentTask(task)) {
                        setTaskFinishedOrUnfinished(queue, task, Status.FINISHED, "task_outcome_assignmentTask0", 0, true);
                        task.setComment(userTaskComment);
                    } else if (isStatus(task, Status.IN_PROGRESS, Status.NEW)) {
                        setTaskFinishedOrUnfinished(queue, task, taskStatus, taskOutcomeLabelId, -1, true);
                    }
                }
            }
        }
    }

    private boolean isInProgressCurrentUserAssignmentTask(Task task) {
        return task.getOwnerId() != null && task.getOwnerId().equals(AuthenticationUtil.getRunAsUser())
                && WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(task.getNode().getType())
                && isStatus(task, Status.IN_PROGRESS);
    }

    @Override
    public CompoundWorkflow saveAndStopCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        saveCompoundWorkflow(queue, compoundWorkflow);
        return stopCompoundWorkflow(queue, compoundWorkflow.getNode().getNodeRef());
    }

    @Override
    public CompoundWorkflow stopCompoundWorkflow(NodeRef nodeRef) {
        WorkflowEventQueue queue = getNewEventQueue();
        return stopCompoundWorkflow(queue, nodeRef);
    }

    private CompoundWorkflow stopCompoundWorkflow(WorkflowEventQueue queue, NodeRef nodeRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        if (checkCompoundWorkflow(compoundWorkflow, Status.IN_PROGRESS, Status.FINISHED) == Status.FINISHED) {
            if (log.isDebugEnabled()) {
                log.debug("CompoundWorkflow is already finished, stopping is not performed, saved as is: " + compoundWorkflow);
            }
        } else {
            setStatus(queue, compoundWorkflow, Status.STOPPED);
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (isStatus(workflow, Status.IN_PROGRESS)) {
                    setStatus(queue, workflow, Status.STOPPED);

                    for (Task task : workflow.getTasks()) {
                        if (isStatus(task, Status.IN_PROGRESS)) {
                            setStatus(queue, task, Status.STOPPED);
                        } else if (isStatus(task, Status.NEW)) {
                            task.setStoppedDateTime(queue.getNow());
                        }
                    }
                } else if (isStatus(workflow, Status.NEW)) {
                    workflow.setStoppedDateTime(queue.getNow());
                }
            }
            stepAndCheck(queue, compoundWorkflow, Status.STOPPED);
            boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Stopped " + compoundWorkflow);
            }
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow saveAndContinueCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        saveCompoundWorkflow(queue, compoundWorkflow);
        return continueCompoundWorkflow(queue, compoundWorkflow.getNode().getNodeRef());
    }

    @Override
    public CompoundWorkflow continueCompoundWorkflow(NodeRef nodeRef) {
        WorkflowEventQueue queue = getNewEventQueue();
        return continueCompoundWorkflow(queue, nodeRef);
    }

    private void ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow) {
        if (cWorkflow.getWorkflows().size() > 1) {
            addOtherCompundWorkflows(cWorkflow);
            for (CompoundWorkflow otherCWf : cWorkflow.getOtherCompoundWorkflows()) {
                if (otherCWf.isStatus(Status.IN_PROGRESS, Status.STOPPED) && otherCWf.getWorkflows().size() > 1) {
                    throw new UnableToPerformException(MessageSeverity.ERROR, "workflow_compound_start_error_twoInprogressOrStoppedWorkflows");
                }
            }
        }
    }

    private CompoundWorkflow continueCompoundWorkflow(WorkflowEventQueue queue, NodeRef nodeRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        if (checkCompoundWorkflow(compoundWorkflow, Status.STOPPED, Status.FINISHED) == Status.FINISHED) {
            if (log.isDebugEnabled()) {
                log.debug("CompoundWorkflow is already finished, continuing is not performed, saved as is: " + compoundWorkflow);
            }
        } else {
            setStatus(queue, compoundWorkflow, Status.IN_PROGRESS);
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (isStatus(workflow, Status.STOPPED)) {
                    setStatus(queue, workflow, Status.IN_PROGRESS);
                }
                workflow.setStoppedDateTime(null);
            }
            stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
            boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Continued " + compoundWorkflow);
            }
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private void continueTasks(WorkflowEventQueue queue, Workflow workflow) {
        for (Task task : workflow.getTasks()) {
            if (isStatus(task, Status.STOPPED)) {
                setStatus(queue, task, Status.IN_PROGRESS);
            }
            task.setStoppedDateTime(null);
        }
    }

    @Override
    public void stopAllCompoundWorkflows(NodeRef parent) {
        WorkflowEventQueue queue = getNewEventQueue();
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (Status.IN_PROGRESS.equals(getRepoStatus(compoundWorkflow))) {
                stopCompoundWorkflow(queue, compoundWorkflow);
            }
        }
        handleEvents(queue);
    }

    @Override
    public void continueAllCompoundWorkflows(NodeRef parent) {
        WorkflowEventQueue queue = getNewEventQueue();
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (Status.STOPPED.equals(getRepoStatus(compoundWorkflow))) {
                continueCompoundWorkflow(queue, compoundWorkflow);
            }
        }
        handleEvents(queue);
    }

    @Override
    public CompoundWorkflow saveAndCopyCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        if (!isStatus(compoundWorkflow, Status.FINISHED)) {
            saveCompoundWorkflow(queue, compoundWorkflow);
        }
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNode().getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        resetCompoundWorkflow(freshCompoundWorkflow);
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private void resetCompoundWorkflow(CompoundWorkflow compoundWorkflow) {
        reset(compoundWorkflow);
        // Set default owner to current user
        String userName = getUserNameToSave();
        compoundWorkflow.setOwnerId(userName);
        compoundWorkflow.setOwnerName(userService.getUserFullName(userName));

        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            reset(workflow);
            for (Iterator<Task> i = workflow.getModifiableTasks().iterator(); i.hasNext();) {
                Task task = i.next();
                if (isInactiveResponsible(task)) {
                    i.remove();
                } else {
                    reset(task);
                }
            }
            workflow.getRemovedTasks().clear();
        }
        compoundWorkflow.getRemovedWorkflows().clear();

        if (log.isDebugEnabled()) {
            log.debug("Resetted " + compoundWorkflow);
        }
        checkCompoundWorkflow(compoundWorkflow, Status.NEW);
    }

    private static void reset(BaseWorkflowObject object) {
        object.getNode().updateNodeRef(null);
        object.clearOriginalProperties();
        for (Entry<String, Object> prop : object.getNode().getProperties().entrySet()) {
            QName key = QName.createQName(prop.getKey());
            if (key.equals(WorkflowCommonModel.Props.STATUS)) {
                prop.setValue(Status.NEW.getName());
            } else if (key.equals(WorkflowCommonModel.Props.OWNER_ID) || key.equals(WorkflowCommonModel.Props.OWNER_NAME)
                    || key.equals(WorkflowCommonModel.Props.OWNER_EMAIL) || key.equals(WorkflowCommonModel.Props.PARALLEL_TASKS)
                    || key.equals(WorkflowCommonModel.Props.STOP_ON_FINISH) || key.equals(WorkflowSpecificModel.Props.DESCRIPTION)
                    || key.equals(WorkflowSpecificModel.Props.ACTIVE) || key.equals(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME)
                    || key.equals(WorkflowCommonModel.Props.OWNER_JOB_TITLE)) {
                // keep value
            } else {
                prop.setValue(null);
            }
        }
    }

    private void checkActiveResponsibleAssignmentTasks(NodeRef document) {
        int count = getActiveResponsibleAssignmentTasks(document);
        if (count > 1) {
            log.debug("Document has " + count + " active responsible tasks: " + document);
            throw new WorkflowActiveResponsibleTaskException();
        }
    }

    @Override
    public int getActiveResponsibleAssignmentTasks(NodeRef document) {
        int counter = 0;
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(document)) {
            if (!isStatus(compoundWorkflow, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!isStatus(workflow, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (!isStatus(task, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                        continue;
                    }
                    if (isActiveResponsible(task)) {
                        counter++;
                    }
                }
            }
        }
        return counter;
    }

    @Override
    public boolean hasUnfinishedReviewTasks(NodeRef docNode) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docNode)) {
            if (!compoundWorkflow.isStatus(Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    for (Task task : workflow.getTasks()) {
                        if (task.isStatus(Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasReviewTask(Node docNode) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docNode.getNodeRef())) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    continue;
                }
                if (!workflow.getTasks().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void finishUserActiveResponsibleInProgressTask(NodeRef docRef, String comment) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                for (Task task : workflow.getTasks()) {
                    if (!isStatus(task, Status.IN_PROGRESS)) {
                        continue;
                    }
                    if (isStatus(task, Status.IN_PROGRESS) && isActiveResponsible(task) && isOwner(task)) {
                        task.setComment(comment);
                        finishInProgressTask(task, 0);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void setTaskOwner(NodeRef task, String ownerId, boolean retainPreviousOwnerId) {
        if (!dictionaryService.isSubClass(nodeService.getType(task), WorkflowCommonModel.Types.TASK)) {
            throw new RuntimeException("Node is not a task: " + task);
        }
        String existingOwnerId = (String) nodeService.getProperty(task, WorkflowCommonModel.Props.OWNER_ID);
        if (ownerId.equals(existingOwnerId)) {
            if (log.isDebugEnabled()) {
                log.debug("Task owner is already set to " + ownerId + ", not overwriting properties");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting task owner from " + existingOwnerId + " to " + ownerId + " - " + task);
        }
        Map<QName, Serializable> personProps = userService.getUserProperties(ownerId);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
        props.put(WorkflowCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
        props.put(WorkflowCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, organizationStructureService.getOrganizationStructure(
                (String) personProps.get(ContentModel.PROP_ORGID)));
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
        String previousOwnerId = (retainPreviousOwnerId) ? existingOwnerId : null;
        props.put(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID, previousOwnerId);
        nodeService.addProperties(task, props);
    }

    // -------------
    // Checks that are required on both memory object and repository node

    private void requireInProgressCurrentUser(Task task) {
        requireStatus(task, Status.IN_PROGRESS);
        requireOwner(task);
    }

    private void requireOwner(Task task) {
        requireOwner(task, getUserNameToCheck());
    }

    private void requireOwner(Task task, String ownerId) {
        requireValue(task, task.getOwnerId(), WorkflowCommonModel.Props.OWNER_ID, ownerId);
    }

    private void requireStatus(BaseWorkflowObject object, Status... statuses) {
        String[] statusNames = new String[statuses.length];
        int i = 0;
        for (Status status : statuses) {
            statusNames[i++] = status.getName();
        }
        requireValue(object, object.getStatus(), WorkflowCommonModel.Props.STATUS, statusNames);
    }

    private void requireValue(BaseWorkflowObject object, String objectValue, QName repoPropertyName, String... requiredValues) {
        String repoValue = null;
        NodeRef nodeRef = object.getNode().getNodeRef();
        if (nodeRef != null) {
            repoValue = (String) nodeService.getProperty(nodeRef, repoPropertyName);
        }
        boolean matches = false;
        for (String requiredValue : requiredValues) {
            if (!requiredValue.equals(objectValue) || (nodeRef != null && !requiredValue.equals(repoValue))) {
                continue;
            }
            matches = true;
        }
        if (!matches) {
            throw new WorkflowChangedException("Illegal value\n  Property: " + repoPropertyName + "\n  Required values: "
                    + StringUtils.join(requiredValues, ", ")
                    + "\n  Object value: " + objectValue + "\n  Repo value: " + repoValue + "\n  Object = "
                    + StringUtils.replace(object.toString(), "\n", "\n  "));
        }
    }

    private String getRepoStatus(NodeRef nodeRef) {
        return (String) nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.STATUS);
    }

    // ========================================================================
    // ======================= FILTERING / PERMISSIONS ========================
    // ========================================================================

    @Override
    public List<Task> getMyTasksInProgress(List<CompoundWorkflow> compoundWorkflows) {
        return WorkflowUtil.getMyTasksInProgress(compoundWorkflows, getUserNameToCheck());
    }

    @Override
    public boolean isOwner(List<CompoundWorkflow> compoundWorkflows) {
        return WorkflowUtil.isOwner(compoundWorkflows, getUserNameToCheck());
    }

    @Override
    public boolean isOwner(CompoundWorkflow compoundWorkflow) {
        return WorkflowUtil.isOwner(compoundWorkflow, getUserNameToCheck());
    }

    @Override
    public boolean isOwner(Task task) {
        return WorkflowUtil.isOwner(task, getUserNameToCheck());
    }

    @Override
    public boolean isOwnerOfInProgressAssignmentTask(CompoundWorkflow compoundWorkflow) {
        return isOwnerOfInprogressTask(compoundWorkflow, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_TASK, false);
    }

    @Override
    public boolean isOwnerOfInProgressActiveResponsibleAssignmentTask(NodeRef docRef) {
        for (NodeRef cWfRef : getCompoundWorkflowNodeRefs(docRef)) {
            boolean result = isOwnerOfInprogressTask(getCompoundWorkflow(cWfRef)
                    , WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_TASK, true);
            if (result) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isOwnerOfInProgressExternalReviewTask(CompoundWorkflow cWorkflow) {
        return isOwnerOfInprogressTask(cWorkflow, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK, false);
    }

    private boolean isOwnerOfInprogressTask(CompoundWorkflow cWorkflow, QName workflowType, QName taskType, boolean requireActiveResponsible) {
        for (Workflow workflow : cWorkflow.getWorkflows()) {
            if (workflow.isType(workflowType)) {
                for (Task task : workflow.getTasks()) {
                    if (task.isType(taskType) && isOwner(task) && isStatus(task, Status.IN_PROGRESS) && (!requireActiveResponsible || WorkflowUtil.isActiveResponsible(task))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAllFinishedCompoundWorkflows(NodeRef parent) {
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        if (compoundWorkflows.isEmpty()) {
            return false;
        }
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (!Status.FINISHED.equals(getRepoStatus(compoundWorkflow))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasInprogressCompoundWorkflows(NodeRef parent) {
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        if (compoundWorkflows.isEmpty()) {
            return false;
        }
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (Status.IN_PROGRESS.equals(getRepoStatus(compoundWorkflow))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNoStoppedOrInprogressCompoundWorkflows(NodeRef parent) {
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        if (compoundWorkflows.isEmpty()) {
            return true;
        }
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            String wfStatus = getRepoStatus(compoundWorkflow);
            if (Status.IN_PROGRESS.equals(wfStatus) || Status.STOPPED.equals(wfStatus)) {
                return false;
            }
        }
        return true;
    }

    private String getUserNameToCheck() {
        return AuthenticationUtil.getRunAsUser();
    }

    private String getUserNameToSave() {
        return userService.getCurrentUserName();
    }

    // ========================================================================
    // ================================ EVENTS ================================
    // ========================================================================

    @Override
    public void registerEventListener(WorkflowEventListener listener) {
        Assert.notNull(listener);
        eventListeners.add(listener);
        if (log.isDebugEnabled()) {
            log.debug("Registered event listener: " + listener);
        }
    }

    @Override
    public void registerImmediateEventListener(WorkflowEventListenerWithModifications listener) {
        Assert.notNull(listener);
        immediateEventListeners.add(listener);
        if (log.isDebugEnabled()) {
            log.debug("Registered immediate event listener: " + listener);
        }
    }

    private void handleEvents(WorkflowEventQueue queue) {
        for (WorkflowEvent event : queue.getEvents()) {
            if (log.isDebugEnabled()) {
                BaseWorkflowObject object = event.getObject();
                log.debug("Handling event " + event.getType() + " for node type '" + object.getNode().getType().toPrefixString(namespaceService)
                        + "' and status '" + object.getStatus() + "'");
            }
            for (WorkflowEventListener listener : eventListeners) {
                listener.handle(event, queue);
            }
        }
        queue.getEvents().clear();
    }

    private void handleEventForWorkflowType(WorkflowEventQueue queue, WorkflowEvent event) {
        BaseWorkflowObject object = event.getObject();
        WorkflowType workflowType = null;
        if (object instanceof Workflow) {
            workflowType = workflowTypesByWorkflow.get(object.getNode().getType());
        } else if (object instanceof Task) {
            workflowType = workflowTypesByTask.get(object.getNode().getType());
        } else {
            return;
        }
        if (workflowType == null) {
            throw new RuntimeException("Workflow type not registered in service for node type: " + object.getNode().getType());
        }
        if (workflowType instanceof WorkflowEventListener) {
            ((WorkflowEventListener) workflowType).handle(event, queue);
        }
        if (workflowType instanceof WorkflowEventListenerWithModifications) {
            ((WorkflowEventListenerWithModifications) workflowType).handle(event, this, queue);
        }
    }

    private void handleEventImmediately(WorkflowEventQueue queue, WorkflowEvent event) {
        handleEventForWorkflowType(queue, event);

        for (WorkflowEventListenerWithModifications listener : immediateEventListeners) {
            listener.handle(event, this, queue);
        }
    }

    private void queueEvent(WorkflowEventQueue queue, WorkflowEventType type, BaseWorkflowObject object, Object... extras) {
        WorkflowEvent event = new BaseWorkflowEvent(type, object, extras);
        handleEventImmediately(queue, event);

        List<WorkflowEvent> events = queue.getEvents();
        for (WorkflowEvent existingEvent : events) {
            if (existingEvent.getType() == type && existingEvent.getObject().equals(object)) {
                if (log.isDebugEnabled()) {
                    log.debug("Event already exists in queue for this object: " + existingEvent);
                }
                return;
            }
        }
        events.add(event);
    }

    // ========================================================================
    // ================================ ENGINE ================================
    // ========================================================================

    // in the beginning of steps and at the end of each step workflow may not satisfy checks^^
    private void stepWorkflow(WorkflowEventQueue queue, Workflow workflow) {
        if (!isStatus(workflow, Status.IN_PROGRESS)) {
            return;
        }
        List<Task> tasks = workflow.getTasks();

        if (workflow.isParallelTasks()) {
            // Start all new tasks
            for (Task task : tasks) {
                if (isStatus(task, Status.NEW)) {
                    setStatus(queue, task, Status.IN_PROGRESS);
                }
            }
        } else {
            if (!isStatusAny(tasks, Status.IN_PROGRESS)) {
                // Start first new task
                for (Task task : tasks) {
                    if (isStatus(task, Status.NEW)) {
                        setStatus(queue, task, Status.IN_PROGRESS);
                        break;
                    }
                }
            }
        }

        // If all tasks are finished, then finish the workflow
        if (isStatusAll(tasks, Status.FINISHED, Status.UNFINISHED)) {
            setStatus(queue, workflow, Status.FINISHED);
            if (workflow.isStopOnFinish()) {
                setStatus(queue, workflow.getParent(), Status.STOPPED);
            }
        }
    }

    private void stepCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        List<Workflow> workflows = compoundWorkflow.getWorkflows();
        for (Workflow workflow : workflows) {
            stepWorkflow(queue, workflow);
        }

        if (!isStatus(compoundWorkflow, Status.IN_PROGRESS)) {
            return;
        }

        if (!isStatusAny(workflows, Status.IN_PROGRESS)) { // no in progress workflows
            // Start first new workflow
            for (Workflow workflow : workflows) {
                if (isStatus(workflow, Status.NEW)) {
                    setStatus(queue, workflow, Status.IN_PROGRESS);

                    // if we previously finished another workflow, then it means this workflow is started automatically
                    List<WorkflowEvent> events = queue.getEvents();
                    for (WorkflowEvent event : events) {
                        BaseWorkflowObject object = event.getObject();
                        if (event.getType() == WorkflowEventType.STATUS_CHANGED && object instanceof Workflow
                                && isStatus(object, Status.FINISHED)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Detected automatic start of workflow!\n  Finished workflow: " + object.getNode().getNodeRef()
                                        + "\n  Started workflow: " + workflow.getNode().getNodeRef());
                            }
                            queueEvent(queue, WorkflowEventType.WORKFLOW_STARTED_AUTOMATICALLY, workflow);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        // If all workflows are finished, then finish the compoundWorkflow
        if (isStatusAll(workflows, Status.FINISHED)) {
            setStatus(queue, compoundWorkflow, Status.FINISHED);
        }
    }

    // added sepparate setStatus methods to find places where the status of some concrete workflow object could be changed

    private void setStatus(WorkflowEventQueue queue, CompoundWorkflow cWorkflow, Status status) {
        setStatusInner(queue, cWorkflow, status);
    }

    private void setStatus(WorkflowEventQueue queue, Workflow workflow, Status status) {
        String formerWfStatus = workflow.getStatus();
        setStatusInner(queue, workflow, status);
        if (Status.IN_PROGRESS == statusChanged(workflow, formerWfStatus)) {
            if (Status.STOPPED.equals(formerWfStatus)) {
                continueTasks(queue, workflow);
            }
            unfinishTasksIfNeeded(queue, workflow);

            if (workflow.isType(WorkflowSpecificModel.CAN_START_PARALLEL)) {
                for (Workflow oWF : workflow.getParent().getWorkflows()) {
                    if (oWF.isType(WorkflowSpecificModel.CAN_START_PARALLEL) && !oWF.isStatus(Status.IN_PROGRESS, Status.FINISHED)) {
                        setStatus(queue, oWF, Status.IN_PROGRESS);
                        break;
                    } else if (oWF.isStatus(Status.NEW)) {
                        break; // workflows after new workflows shouldn't go to IN_PROGRESS status
                    }
                }
            }
        }
    }

    private Status statusChanged(BaseWorkflowObject wfObject, String formerStatus) {
        String newStatus = wfObject.getStatus();
        if (!newStatus.equals(formerStatus)) {
            return Status.of(newStatus);
        }
        return null;
    }

    private void unfinishTasksIfNeeded(WorkflowEventQueue queue, Workflow workflow) {
        // CL task 164814 raames välja kommenteeritud kood. Maiga: tõenäoline on, et lisatakse kunagi analoogne kontroll,
        // kuid kontrolli tulemusena kuvatakse lihtsalt hoiatusteade, mitte ei hakata automaatselt tööülesandeid katkestama
        //@formatter:off
        /*
        ReviewWorkflow sequentalReviewWorkflow = null;
        if (workflow instanceof ReviewWorkflow && !workflow.isParallelTasks()) {
            sequentalReviewWorkflow = (ReviewWorkflow) workflow;
        }
        if (sequentalReviewWorkflow != null || workflow.isType(Types.SIGNATURE_WORKFLOW)) {
            CompoundWorkflow cWf = workflow.getParent();
            List<Workflow> workflowsToFinish = new ArrayList<Workflow>();

            addOtherCompundWorkflows(cWf);
            List<CompoundWorkflow> cWorkflows = new ArrayList<CompoundWorkflow>(cWf.getOtherCompoundWorkflows());
            cWorkflows.add(cWf);

            for (CompoundWorkflow compoundWorkflow : cWorkflows) {
                if (compoundWorkflow.isStatus(Status.NEW)) {
                    continue;
                }
                for (Workflow workflow2 : compoundWorkflow.getWorkflows()) {
                    if (workflow2.isStatus(Status.IN_PROGRESS, Status.NEW)) {
                        boolean finishWF;
                        if (sequentalReviewWorkflow != null) {
                            finishWF = workflow2 instanceof ReviewWorkflow && workflow2.isParallelTasks();
                        } else {
                            finishWF = workflow2.isType(Types.REVIEW_WORKFLOW, Types.OPINION_WORKFLOW, Types.ASSIGNMENT_WORKFLOW, Types.DOC_REGISTRATION_WORKFLOW);
                        }
                        if (finishWF) {
                            workflowsToFinish.add(workflow2);
                        }
                    }
                }
            }
            log.debug("Unfinishing tasks of " + workflowsToFinish.size() + " workflows");
            for (Workflow workflowToFinish : workflowsToFinish) {
                for (Task taskToFinish : workflowToFinish.getTasks()) {
                    if (!taskToFinish.isStatus(Status.FINISHED, Status.UNFINISHED)) {
                        setTaskFinishedOrUnfinished(queue, taskToFinish, Status.UNFINISHED, "task_outcome_unfinished_movedOn", -1, true);
                    }
                }
                setStatus(queue, workflowToFinish, Status.FINISHED);
                saveWorkflow(queue, workflowToFinish);
            }
        }
        */
        // @formatter:on
    }

    private void setStatus(WorkflowEventQueue queue, Task task, Status status) {
        setStatusInner(queue, task, status);
    }

    private void setStatusInner(WorkflowEventQueue queue, BaseWorkflowObject object, Status status) {
        if (log.isDebugEnabled()) {
            log.debug("Setting status of node type '" + object.getNode().getType().toPrefixString() + "' from '" + object.getStatus() + "' to '"
                    + status.getName() + "'");
        }
        Status originalStatus = Status.of(object.getStatus());
        object.setStatus(status.getName());
        queueEvent(queue, WorkflowEventType.STATUS_CHANGED, object);
        addExternalReviewWorkflowData(queue, object, originalStatus);

        // status based property setting
        if (object.getStartedDateTime() == null && isStatus(object, Status.IN_PROGRESS)) {
            object.setStartedDateTime(queue.getNow());
        }
        if (isStatus(object, Status.STOPPED)) {
            object.setStoppedDateTime(queue.getNow());
        } else {
            object.setStoppedDateTime(null);
        }
    }

    // Also send to recipients who would have got updates before changing status,
    // excluding external reviewers who initiated this change
    private void addExternalReviewWorkflowData(WorkflowEventQueue queue, BaseWorkflowObject object, Status originalStatus) {
        if (object instanceof Task) {
            Task task = (Task) object;
            if (isExternalReviewAdditionalRecipientTask(queue, originalStatus, task)) {
                NodeRef docNodeRef = task.getParent().getParent().getParent();
                if (queue.getAdditionalExternalReviewRecipients().get(docNodeRef) == null) {
                    queue.getAdditionalExternalReviewRecipients().put(docNodeRef, new ArrayList<String>());
                }
                queue.getAdditionalExternalReviewRecipients().get(docNodeRef).add(task.getInstitutionCode());
            }
        }
    }

    public boolean isExternalReviewAdditionalRecipientTask(WorkflowEventQueue queue, Status originalStatus, Task task) {
        return task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                && task.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID) != null
                && !task.isStatus(Status.IN_PROGRESS, Status.STOPPED)
                && (originalStatus.equals(Status.IN_PROGRESS) || originalStatus.equals(Status.STOPPED))
                && (!task.getNode().getNodeRef().equals(queue.getParameter(WorkflowQueueParameter.EXTERNAL_REVIEWER_TRIGGERING_TASK)));
    }

    private boolean stepAndCheck(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, Status... requiredStatuses) {
        fireCreatedEvents(queue, compoundWorkflow);
        int before;
        do {
            before = queue.getEvents().size();
            stepCompoundWorkflow(queue, compoundWorkflow);
        } while (queue.getEvents().size() > before);
        checkCompoundWorkflow(compoundWorkflow, requiredStatuses);
        for (CompoundWorkflow otherCompoundWorkflow : compoundWorkflow.getOtherCompoundWorkflows()) {
            stepAndCheck(queue, otherCompoundWorkflow);
        }
        return !queue.getEvents().isEmpty();
    }

    private void fireCreatedEvents(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        fireCreatedEvent(queue, compoundWorkflow);
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            fireCreatedEvent(queue, workflow);
            for (Task task : workflow.getTasks()) {
                fireCreatedEvent(queue, task);
            }
        }
    }

    private void fireCreatedEvent(WorkflowEventQueue queue, BaseWorkflowObject object) {
        if (object.getNode().getNodeRef() == null) {
            queueEvent(queue, WorkflowEventType.CREATED, object);
        }
    }

    private void setTaskFinishedOrUnfinished(WorkflowEventQueue queue, Task task, Status newStatus, String outcomeLabelId, int outcomeIndex, boolean getOutcome) {
        if (!(newStatus == Status.FINISHED || newStatus == Status.UNFINISHED) || isStatus(task, Status.FINISHED, newStatus)) {
            throw new RuntimeException("New or old status is illegal, new='" + newStatus.getName() + "', old " + task);
        }

        setStatus(queue, task, newStatus);
        if (outcomeIndex == SIGNATURE_TASK_OUTCOME_NOT_SIGNED && WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(task.getNode().getType())) {
            stopIfNeeded(task, queue);
        } else if (task.isType(Types.REVIEW_TASK)) {
            // sometimes here value of TEMP_OUTCOME is Integer, sometimes String
            final Integer tempOutcome = DefaultTypeConverter.INSTANCE.convert(Integer.class, task.getProp(WorkflowSpecificModel.Props.TEMP_OUTCOME));
            if (tempOutcome != null && tempOutcome == REVIEW_TASK_OUTCOME_REJECTED) { // KAAREL: What is tempOutcome and why was it null?
                stopIfNeeded(task, queue);
            }
        }
        if (getOutcome) {
            task.setCompletedDateTime(queue.getNow());
            task.setOutcome(getOutcomeText(outcomeLabelId, outcomeIndex), outcomeIndex);
        }
    }

    private String getOutcomeText(String outcomeLabelId, int outcomeIndex) {
        String outcomeText = null;
        if (outcomeLabelId != null) {
            outcomeText = I18NUtil.getMessage(outcomeLabelId);
            if (outcomeText == null) {
                log.warn("Task outcome translation not found, key '" + outcomeLabelId + "'");
                outcomeText = Integer.toString(outcomeIndex);
            }
        }
        return outcomeText;
    }

    /**
     * Common logic when signature task is not signed or review task is not accepted(rejected)
     * 
     * @param task - signature task or review task that was rejected
     * @param queue
     */
    private void stopIfNeeded(Task task, WorkflowEventQueue queue) {
        final Workflow parentWorkFlow = task.getParent();
        final CompoundWorkflow compoundWorkflow = parentWorkFlow.getParent();
        final Date stoppedDateTime = queue.getNow();
        compoundWorkflow.setStoppedDateTime(stoppedDateTime);
        setStatus(queue, compoundWorkflow, Status.STOPPED);
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (isStatus(workflow, Status.NEW)) {
                workflow.setStoppedDateTime(stoppedDateTime);
            }
            boolean isParentWorkflow = parentWorkFlow.getNode().getNodeRef().equals(workflow.getNode().getNodeRef());
            boolean forceStopParentWorkflow = false;
            for (Task aTask : workflow.getTasks()) {
                boolean isReviewWorkflow = aTask.getParent().getNode().getType().equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
                boolean isParallelTasks = aTask.getParent().isParallelTasks();
                boolean isInProgress = isStatus(aTask, Status.IN_PROGRESS);
                boolean isNew = isStatus(aTask, Status.NEW);
                if (isNew || (isInProgress && isReviewWorkflow)) {
                    aTask.setStoppedDateTime(stoppedDateTime);
                    // We must change parallel task's statuses from in progress to stopped
                    if (isParallelTasks && isInProgress) {
                        setStatus(queue, aTask, Status.STOPPED);
                    }
                    // only stop parent if we have more than one task in the workflow
                    if (isParentWorkflow && !aTask.getNode().getNodeRef().equals(task.getNode().getNodeRef())) {
                        forceStopParentWorkflow = true;
                    }
                }
            }
            if (forceStopParentWorkflow) {
                setStatus(queue, parentWorkFlow, Status.STOPPED);
                parentWorkFlow.setStoppedDateTime(stoppedDateTime);
            }
        }
        saveCompoundWorkflow(queue, compoundWorkflow, null);
    }

    private void setTaskFinished(WorkflowEventQueue queue, Task task, int outcomeIndex) {
        String outcomeLabelId = "task_outcome_" + task.getNode().getType().getLocalName() + outcomeIndex;
        setTaskFinishedOrUnfinished(queue, task, Status.FINISHED, outcomeLabelId, outcomeIndex, true);
    }

    private void setTaskFinishedOrUnfinished(WorkflowEventQueue queue, Task task, Status status) {
        setTaskFinishedOrUnfinished(queue, task, status, "task_outcome_finished_manually", -1, true);
    }

    @Override
    public void setTaskFinished(WorkflowEventQueue queue, Task task) {
        if (task.getOutcomes() != 1) {
            throw new RuntimeException("Automatically finishing task is supported only when outcomes=1\n" + task);
        }
        setTaskFinished(queue, task, 0);
    }

    private void setTaskUnfinished(WorkflowEventQueue queue, Task task) {
        setTaskFinishedOrUnfinished(queue, task, Status.UNFINISHED);
    }

    // ========================================================================
    // ========================================================================

    private Set<QName> getDefaultAspects(QName className) {
        Set<QName> aspects = generalService.getDefaultAspects(className);
        removeSystemAspects(aspects);
        return aspects;
    }

    private Set<QName> getNodeAspects(NodeRef nodeRef) {
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        removeSystemAspects(aspects);
        return aspects;
    }

    private Map<QName, Serializable> getDefaultProperties(QName className) {
        return RepoUtil.getPropertiesIgnoringSystem(generalService.getDefaultProperties(className), dictionaryService);
    }

    private Map<QName, Serializable> getNodeProperties(NodeRef nodeRef) {
        return RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(nodeRef), dictionaryService);
    }

    private boolean createOrUpdate(WorkflowEventQueue queue, BaseWorkflowObject object, NodeRef parent, QName assocType) {
        boolean changed = false;
        WmNode node = object.getNode();
        if (node.getNodeRef() == null) {
            // If saving a new node, then set creator to current user
            object.setCreatorName(userService.getUserFullName());
            if (object.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                object.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, dvkService.getInstitutionCode());
            }
        }

        object.preSave();

        Map<QName, Serializable> props = getSaveProperties(object.getChangedProperties());
        if (node.getNodeRef() == null) {
            // Create workflow
            if (log.isDebugEnabled()) {
                log.debug("Creating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                        + WmNode.toString(props.entrySet()));
            }
            NodeRef nodeRef = nodeService.createNode(parent, assocType, assocType, node.getType(), props).getChildRef();
            node.updateNodeRef(nodeRef);
            changed = true;

            // Add additional aspects
            Set<QName> aspects = new HashSet<QName>(node.getAspects());
            aspects.removeAll(nodeService.getAspects(nodeRef));
            if (aspects.size() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding aspects to node (type '" + node.getType().toPrefixString(namespaceService) + "') " //
                            + WmNode.toString(aspects));
                }
                for (QName aspect : aspects) {
                    nodeService.addAspect(nodeRef, aspect, null);
                }
            }

            // removing aspects is not implemented - not needed for now
        } else {
            // Update workflow
            if (!props.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                            + WmNode.toString(props.entrySet()));
                }

                if (props.containsKey(WorkflowCommonModel.Props.OWNER_ID) || props.containsKey(WorkflowCommonModel.Props.OWNER_NAME)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL) || props.containsKey(WorkflowCommonModel.Props.PARALLEL_TASKS)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME) || props.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE)) {
                    requireStatus(object, Status.NEW);
                }

                nodeService.addProperties(node.getNodeRef(), props); // do not replace non-changed properties
                changed = true;

                queueEvent(queue, WorkflowEventType.UPDATED, object, props);

                // adding/removing aspects is not implemented - not needed for now
            }
        }
        object.setChangedProperties(props);
        return changed;
    }

    // ---

    private void removeSystemAspects(Set<QName> aspects) {
        for (Iterator<QName> i = aspects.iterator(); i.hasNext();) {
            QName aspect = i.next();
            if (RepoUtil.isSystemAspect(aspect)) {
                i.remove();
            }
        }
    }

    private Map<QName, Serializable> getSaveProperties(Map<QName, Serializable> props) {
        Map<QName, Serializable> filteredProps = RepoUtil.getPropertiesIgnoringSystem(props, dictionaryService);
        generalService.savePropertiesFiles(filteredProps);
        return filteredProps;
    }

    private static WorkflowEventQueue getNewEventQueue() {
        return new WorkflowEventQueue();
    }

    @Override
    public boolean isSendableExternalWorkflowDoc(NodeRef docNodeRef) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docNodeRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)
                        && workflow.isStatus(Status.IN_PROGRESS, Status.FINISHED)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean externalReviewWorkflowEnabled() {
        return Boolean.parseBoolean(parametersService.getStringParameter(Parameters.EXTERNAL_REVIEW_WORKFLOW_ENABLED));
    }

    // START: getters / setters

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @Override
    public boolean isInternalTesting() {
        return INTERNAL_TESTING;
    }

    public void setInternalTesting(boolean internalTesting) {
        INTERNAL_TESTING = internalTesting;
    }

    // END: getters / setters

}
