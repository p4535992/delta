package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkCompoundWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkTask;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isGeneratedByDelegation;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isInactiveResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatusAll;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatusAny;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.requireStatusUnchanged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.service.VersionsService;
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
import ee.webmedia.alfresco.workflow.service.event.WorkflowMultiEventListener;
import ee.webmedia.alfresco.workflow.service.type.AssignmentWorkflowType;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Alar Kvell
 */
public class WorkflowServiceImpl implements WorkflowService, WorkflowModifications {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowServiceImpl.class);
    private static final int SIGNATURE_TASK_OUTCOME_NOT_SIGNED = 0;
    private static final int REVIEW_TASK_OUTCOME_ACCEPTED = 0;
    private static final int REVIEW_TASK_OUTCOME_ACCEPTED_WITH_COMMENT = 1;
    private static final int REVIEW_TASK_OUTCOME_REJECTED = 2;
    private static final int CONFIRMATION_TASK_OUTCOME_REJECTED = 1;

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
    private PrivilegeService privilegeService;
    private NamespaceService namespaceService;
    private OrganizationStructureService organizationStructureService;
    private DvkService dvkService;
    private ParametersService parametersService;
    private FileService fileService;
    private VersionsService versionsService;
    private BehaviourFilter behaviourFilter;
    private LogService logService;
    private WorkflowDbService workflowDbService;

    private final Map<QName, WorkflowType> workflowTypesByWorkflow = new HashMap<QName, WorkflowType>();
    private final Map<QName, WorkflowType> workflowTypesByTask = new HashMap<QName, WorkflowType>();
    private final Map<QName, Collection<QName>> taskDataTypeDefaultAspects = new HashMap<QName, Collection<QName>>();
    private final Map<QName, List<QName>> taskDataTypeDefaultProps = new HashMap<QName, List<QName>>();
    private final Map<QName, QName> taskPrefixedQNames = new HashMap<QName, QName>();
    private final List<WorkflowEventListener> eventListeners = new ArrayList<WorkflowEventListener>();
    private final List<WorkflowMultiEventListener> multiEventListeners = new ArrayList<WorkflowMultiEventListener>();
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
    private boolean INTERNAL_TESTING;
    private boolean orderAssignmentCategoryEnabled;
    private boolean orderAssignmentWorkflowEnabled;
    private boolean confirmationWorkflowEnabled;

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
            Collection<QName> aspects = RepoUtil.getAspectsIgnoringSystem(generalService.getDefaultAspects(taskTypeQName));
            taskDataTypeDefaultAspects.put(workflowTypeQName, aspects);
            List<QName> taskDefaultProps = new ArrayList<QName>();
            taskDataTypeDefaultProps.put(workflowTypeQName, taskDefaultProps);
            for (QName aspect : aspects) {
                addPropertyDefs(taskDefaultProps, dictionaryService.getPropertyDefs(aspect));
            }
            addPropertyDefs(taskDefaultProps, dictionaryService.getPropertyDefs(taskTypeQName));
            taskPrefixedQNames.put(taskTypeQName, taskTypeQName.getPrefixedQName(namespaceService));
        }
    }

    private void addPropertyDefs(List<QName> taskDefaultProps, Map<QName, PropertyDefinition> propertyDefs) {
        for (Map.Entry<QName, PropertyDefinition> entry : propertyDefs.entrySet()) {
            PropertyDefinition propDef = entry.getValue();
            QName prop = propDef.getName();
            if ((WorkflowCommonModel.URI.equals(prop.getNamespaceURI()) || WorkflowSpecificModel.URI.equals(prop.getNamespaceURI()))) {
                taskDefaultProps.add(entry.getKey());
            }
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
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(boolean getUserFullName) {
        NodeRef root = getRoot();
        List<ChildAssociationRef> childAssocs = getWorkflows(root);
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            compoundWorkflowDefinitions.add(getCompoundWorkflowDefinition(nodeRef, root, getUserFullName));
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef) {
        return getCompoundWorkflowDefinition(nodeRef, nodeService.getPrimaryParent(nodeRef).getParentRef(), true);
    }

    private CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef, NodeRef parent, boolean getUserFullName) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION, false, false);
        CompoundWorkflowDefinition compoundWorkflowDefinition = new CompoundWorkflowDefinition(node, parent);
        String userId = compoundWorkflowDefinition.getUserId();
        if (StringUtils.isNotBlank(userId) && getUserFullName) {
            compoundWorkflowDefinition.setUserFullName(userService.getUserFullName(userId));
        }
        getAndAddWorkflows(nodeRef, compoundWorkflowDefinition, false, true);
        return compoundWorkflowDefinition;
    }

    @Override
    public NodeRef getCompoundWorkflowDefinitionByName(String newCompWorkflowDefinitionName, String userId, boolean checkGlobalDefinitions) {
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false)) {
            String existingUserId = compWorkflowDefinition.getUserId();
            if (StringUtils.equalsIgnoreCase(newCompWorkflowDefinitionName, compWorkflowDefinition.getName())
                    && ((checkGlobalDefinitions && StringUtils.isBlank(existingUserId)) || (StringUtils.isNotBlank(existingUserId) && StringUtils.equals(userId, existingUserId)))) {
                return compWorkflowDefinition.getNodeRef();
            }
        }
        return null;
    }

    @Override
    public List<CompoundWorkflowDefinition> getUserCompoundWorkflowDefinitions(String userId) {
        Assert.isTrue(StringUtils.isNotBlank(userId));
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>();
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false)) {
            if (StringUtils.equals(userId, compWorkflowDefinition.getUserId())) {
                compoundWorkflowDefinitions.add(compWorkflowDefinition);
            }
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public NodeRef createCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String newCompWorkflowDefinitionName) {
        CompoundWorkflowDefinition compWorkflowDefinition = getNewCompoundWorkflowDefinition();
        compWorkflowDefinition.setUserId(userId);
        compWorkflowDefinition.setName(newCompWorkflowDefinitionName);
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef != null) {
            String docTypeId = (String) nodeService.getProperty(docRef, DocumentAdminModel.Props.OBJECT_TYPE_ID);
            if (docTypeId != null) {
                compWorkflowDefinition.setDocumentTypes(Arrays.asList(docTypeId));
            }
        }
        copyDefinitionDataFromCompoundWorkflow(compWorkflowDefinition, compoundWorkflow);
        compWorkflowDefinition = saveCompoundWorkflowDefinition(compWorkflowDefinition);
        return compWorkflowDefinition.getNodeRef();
    }

    private void copyDefinitionDataFromCompoundWorkflow(CompoundWorkflowDefinition compWorkflowDefinition, CompoundWorkflow compoundWorkflow) {
        String statusNew = Status.NEW.getName();
        for (int wfIndex = 0; wfIndex < compoundWorkflow.getWorkflows().size(); wfIndex++) {
            Workflow existingWorkflow = compoundWorkflow.getWorkflows().get(wfIndex);
            Workflow newWorkflow = addNewWorkflow(compWorkflowDefinition, existingWorkflow.getType(), wfIndex, true);
            newWorkflow.setStatus(statusNew);
            newWorkflow.setParallelTasks(existingWorkflow.getParallelTasks());
            newWorkflow.setDescription(existingWorkflow.getDescription());
            newWorkflow.setStopOnFinish(existingWorkflow.getStopOnFinish());
            newWorkflow.setResolution(existingWorkflow.getResolution());
            if (existingWorkflow.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                newWorkflow.setCategory(existingWorkflow.getCategory());
            }
            newWorkflow.setMandatory(existingWorkflow.getMandatory());
            for (int taskIndex = 0; taskIndex < existingWorkflow.getTasks().size(); taskIndex++) {
                Task existingTask = existingWorkflow.getTasks().get(taskIndex);
                Task newTask = null;
                if (existingTask.isResponsible()) {
                    if (existingTask.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                        if (!WorkflowUtil.isActiveResponsible(existingTask)) {
                            continue;
                        }
                        newTask = ((AssignmentWorkflow) newWorkflow).addResponsibleTask();
                    } else {
                        newTask = ((OrderAssignmentWorkflow) newWorkflow).addResponsibleTask();
                    }

                } else {
                    newTask = newWorkflow.addTask();
                }
                newTask.setOwnerId(existingTask.getOwnerId());
                newTask.setOwnerEmail(existingTask.getOwnerEmail());
                newTask.setOwnerName(existingTask.getOwnerName());
                newTask.setOwnerJobTitle(existingTask.getOwnerJobTitle());
                newTask.setOwnerOrgStructUnitProp(existingTask.getOwnerOrgStructUnitProp());
                if (existingTask.isType(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                    newTask.setResolution(existingTask.getResolution());
                }
            }
        }
    }

    @Override
    public void deleteCompoundWorkflowDefinition(String existingCompWorkflowDefinitionName, String userId) {
        NodeRef nodeRef = getCompoundWorkflowDefinitionByName(existingCompWorkflowDefinitionName, userId, false);
        if (nodeRef != null) {
            nodeService.deleteNode(nodeRef);
        }
    }

    @Override
    public NodeRef overwriteExistingCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String existingCompWorkflowDefinitionName) {
        NodeRef existingCompWorkflowRef = getCompoundWorkflowDefinitionByName(existingCompWorkflowDefinitionName, userId, false);
        if (existingCompWorkflowRef != null) {
            nodeService.deleteNode(existingCompWorkflowRef);
        }
        return createCompoundWorkflowDefinition(compoundWorkflow, userId, existingCompWorkflowDefinitionName);
    }

    @Override
    public List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent) {
        return getCompoundWorkflows(parent, null, true);
    }

    private List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent, NodeRef nodeRefToSkip, boolean loadTasks) {
        List<NodeRef> nodeRefs = getCompoundWorkflowNodeRefs(parent);
        List<CompoundWorkflow> compoundWorkflows = new ArrayList<CompoundWorkflow>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            if (!nodeRef.equals(nodeRefToSkip)) {
                compoundWorkflows.add(getCompoundWorkflow(nodeRef, loadTasks));
            }
        }
        return compoundWorkflows;
    }

    private List<CompoundWorkflow> getCompoundWorkflowsOfType(NodeRef parent, List<QName> types) {
        List<NodeRef> nodeRefs = getCompoundWorkflowNodeRefs(parent);
        List<CompoundWorkflow> compoundWorkflows = new ArrayList<CompoundWorkflow>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs) {
            compoundWorkflows.add(getCompoundWorkflowOfType(nodeRef, types));
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
    public List<NodeRef> getCompoundWorkflowAndTaskNodeRefs(NodeRef parentRef) {
        List<NodeRef> compoundWorkflowNodeRefs = getCompoundWorkflowNodeRefs(parentRef);
        List<NodeRef> compoundWorkflowAndTaskNodeRefs = new ArrayList<NodeRef>();
        compoundWorkflowAndTaskNodeRefs.addAll(compoundWorkflowNodeRefs);
        for (NodeRef compoundWorkflowNodeRef : compoundWorkflowNodeRefs) {
            List<ChildAssociationRef> workflowAssocRefs = getWorkflows(compoundWorkflowNodeRef);
            for (ChildAssociationRef workflowAssocRef : workflowAssocRefs) {
                compoundWorkflowAndTaskNodeRefs.addAll(workflowDbService.getWorkflowTaskNodeRefs(workflowAssocRef.getChildRef()));
            }
        }
        return compoundWorkflowAndTaskNodeRefs;
    }

    @Override
    public CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef) {
        return getCompoundWorkflow(nodeRef, true);
    }

    private CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, boolean loadTasks) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        getAndAddWorkflows(compoundWorkflow.getNodeRef(), compoundWorkflow, false, true);
        return compoundWorkflow;
    }

    private CompoundWorkflow getCompoundWorkflowOfType(NodeRef nodeRef, List<QName> types) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        List<ChildAssociationRef> childAssocs = getWorkflows(nodeRef);
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            if (types.contains(nodeService.getType(childAssoc.getChildRef()))) {
                workflowIndex = addWorkflow(compoundWorkflow, false, workflowIndex, childAssoc, true);
            }
        }
        return compoundWorkflow;
    }

    @Override
    public void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow) {
        compoundWorkflow.getOtherCompoundWorkflows().clear();
        compoundWorkflow.setOtherCompoundWorkflows(getOtherCompoundWorkflows(compoundWorkflow, false));
    }

    @Override
    public List<CompoundWorkflow> getOtherCompoundWorkflows(CompoundWorkflow compoundWorkflow) {
        return getOtherCompoundWorkflows(compoundWorkflow, true);
    }

    private List<CompoundWorkflow> getOtherCompoundWorkflows(CompoundWorkflow compoundWorkflow, boolean loadTasks) {
        return getCompoundWorkflows(compoundWorkflow.getParent(), compoundWorkflow.getNodeRef(), loadTasks);
    }

    private void getAndAddWorkflows(NodeRef parent, CompoundWorkflow compoundWorkflow, boolean copy, boolean addTasks) {
        List<ChildAssociationRef> childAssocs = getWorkflows(parent);
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            workflowIndex = addWorkflow(compoundWorkflow, copy, workflowIndex, childAssoc, addTasks);
        }
    }

    private List<ChildAssociationRef> getWorkflows(NodeRef parent) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        return childAssocs;
    }

    private int addWorkflow(CompoundWorkflow compoundWorkflow, boolean copy, int workflowIndex, ChildAssociationRef childAssoc, boolean addTasks) {
        NodeRef nodeRef = childAssoc.getChildRef();
        Workflow workflow = getWorkflow(nodeRef, compoundWorkflow, copy);
        workflow.setIndexInCompoundWorkflow(workflowIndex);
        compoundWorkflow.addWorkflow(workflow);
        if (addTasks) {
            getAndAddTasks(nodeRef, workflow, copy);
        }
        workflowIndex++;
        return workflowIndex;
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
        QName workflowType = workflow.getType();
        workflow.addTasks(workflowDbService.getWorkflowTasks(parent, taskDataTypeDefaultAspects.get(workflowType), taskDataTypeDefaultProps.get(workflowType),
                taskPrefixedQNames, workflowTypesByWorkflow.get(workflowType), workflow, copy));
        for (Task task : workflow.getTasks()) {
            loadDueDateHistory(task);
        }
    }

    @Override
    public Task getTask(NodeRef nodeRef, boolean fetchWorkflow) {
        Workflow workflow = null;
        if (fetchWorkflow) {
            NodeRef parent = workflowDbService.getTaskParentNodeRef(nodeRef);
            workflow = getWorkflow(parent, null, false);
        }
        return getTask(nodeRef, workflow, false);
    }

    @Override
    public List<Task> getWorkflowTasks(NodeRef workflowRef) {
        QName workflowType = nodeService.getType(workflowRef);
        return workflowDbService.getWorkflowTasks(workflowRef, taskDataTypeDefaultAspects.get(workflowType), taskDataTypeDefaultProps.get(workflowType),
                taskPrefixedQNames, workflowTypesByWorkflow.get(workflowType), null, false);
    }

    @Override
    public Set<Task> getTasks(NodeRef docRef, Predicate<Task> taskPredicate) {
        return WorkflowUtil.getTasks(new HashSet<Task>(), getCompoundWorkflows(docRef), taskPredicate);
    }

    private Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        Task task = getTaskWithoutParentAndChildren(nodeRef, workflow, copy);
        loadDueDateHistory(task);
        return task;
    }

    private void loadDueDateHistory(Task task) {
        if (Boolean.FALSE.equals(task.getHasDueDateHistory()) || !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.TASK_DUE_DATE_EXTENSION_CONTAINER)) {
            task.setHasDueDateHistory(Boolean.FALSE);
            return;
        }
        List<Pair<String, Date>> historyRecords = task.getDueDateHistoryRecords();
        historyRecords.addAll(workflowDbService.getDueDateHistoryRecords(task.getNodeRef()));
        task.setHasDueDateHistory(!historyRecords.isEmpty());
    }

    @Override
    public Task getTaskWithoutParentAndChildren(NodeRef nodeRef, Workflow workflow, boolean copy) {
        return workflowDbService.getTask(nodeRef, taskPrefixedQNames, workflow, copy);
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

    @Override
    public void retrieveTaskFiles(Task task, List<NodeRef> taskFiles) {
        if (taskFiles != null && !taskFiles.isEmpty()) {
            task.loadFiles(fileService.getFiles(taskFiles));
        }
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
    public CompoundWorkflow getNewCompoundWorkflow(Node compoundWorkflowDefinition, NodeRef parent) {
        QName type = compoundWorkflowDefinition.getType();
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(compoundWorkflowDefinition.getProperties());
        Set<QName> aspects = compoundWorkflowDefinition.getAspects();
        return getNewCompoundWorkflow(compoundWorkflowDefinition.getNodeRef(), parent, type, props, aspects);
    }

    @Override
    public CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent) {
        QName type = nodeService.getType(compoundWorkflowDefinition);
        Map<QName, Serializable> props = getNodeProperties(compoundWorkflowDefinition);
        Set<QName> aspects = getNodeAspects(compoundWorkflowDefinition);
        return getNewCompoundWorkflow(compoundWorkflowDefinition, parent, type, props, aspects);
    }

    private CompoundWorkflow getNewCompoundWorkflow(NodeRef compoundWorkflowDefinition, NodeRef parent, QName type, Map<QName, Serializable> props, Set<QName> aspects) {
        if (!type.equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
            throw new RuntimeException("Node is not a compoundWorkflowDefinition, type '" + type.toPrefixString(namespaceService) + "', nodeRef="
                    + compoundWorkflowDefinition);
        }

        // Remove compoundWorkflowDefinition's properties, so we are left with only compoundWorkflow properties
        Map<QName, PropertyDefinition> propertyDefs = dictionaryService.getPropertyDefs(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        for (PropertyDefinition propDef : propertyDefs.values()) {
            if (propDef.getContainerClass().getName().equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
                props.remove(propDef.getName());
            }
        }

        QName compoundWorkflowType = WorkflowCommonModel.Types.COMPOUND_WORKFLOW;
        WmNode compoundWorkflowNode = new WmNode(null, compoundWorkflowType.getPrefixedQName(namespaceService), aspects, props);
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(compoundWorkflowNode, parent);
        if (RepoUtil.isSaved(compoundWorkflowDefinition) && nodeService.exists(compoundWorkflowDefinition)) {
            getAndAddWorkflows(compoundWorkflowDefinition, compoundWorkflow, true, true);
        }

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
        saveCompoundWorkflow(queue, compoundWorkflowDefinition, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW_DEFINITION);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + compoundWorkflowDefinition);
        }

        // also check repo status
        CompoundWorkflowDefinition freshCompoundWorkflowDefinition = getCompoundWorkflowDefinition(compoundWorkflowDefinition.getNodeRef());
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
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    public void saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        proccessPreSave(queue, compoundWorkflow);
        saveCompoundWorkflow(queue, compoundWorkflow, null);
    }

    private void proccessPreSave(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
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
                    queue.setParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY, Boolean.TRUE);
                    setTaskUnfinished(queue, task);
                }
            }
        }
        stepAndCheck(queue, compoundWorkflow);
    }

    private boolean saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, QName assocType) {
        return saveCompoundWorkflow(queue, compoundWorkflow, assocType, new HashSet<NodeRef>());
    }

    private boolean saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, QName assocType, Set<NodeRef> savedCompoundWorkflows) {
        if (assocType == null) {
            assocType = WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW;
        }
        String previousOwnerId = null;
        if (compoundWorkflow.isSaved()) {
            previousOwnerId = (String) nodeService.getProperty(compoundWorkflow.getNodeRef(), WorkflowCommonModel.Props.OWNER_ID);
        }

        // If this node is not saved, then all children are newly created, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and no children are being added or removed, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and at least one child is being added or removed, then childAssociationIndexes have to be set on all children
        // (because maybe all children have assocIndex=-1).
        boolean setChildAssocIndexes = false;
        boolean wasSaved = compoundWorkflow.isSaved();
        boolean changed = createOrUpdate(queue, compoundWorkflow, compoundWorkflow.getParent(), assocType);

        // Remove workflows
        for (Workflow removedWorkflow : compoundWorkflow.getRemovedWorkflows()) {
            if (removedWorkflow.isSaved()) {
                NodeRef removedWorkflowNodeRef = removedWorkflow.getNodeRef();
                checkWorkflow(getWorkflow(removedWorkflowNodeRef, compoundWorkflow, false), Status.NEW);
                nodeService.deleteNode(removedWorkflowNodeRef);
                changed = true;
                setChildAssocIndexes = true;
            }
        }
        compoundWorkflow.getRemovedWorkflows().clear();

        if (!setChildAssocIndexes && wasSaved) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isSaved()) {
                    setChildAssocIndexes = true;
                    break;
                }
            }
        }

        int index = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            // Create or update workflow
            saveWorkflow(queue, workflow);

            if (setChildAssocIndexes) {
                ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(workflow.getNodeRef());
                if (childAssocRef.getNthSibling() != index) {
                    nodeService.setChildAssociationIndex(childAssocRef, index);
                    changed = true;
                }
            }
            index++;
        }

        savedCompoundWorkflows.add(compoundWorkflow.getNodeRef());
        // changing workflow may have changed other workflows of the document too,
        // so we need to save them also
        for (CompoundWorkflow otherCompoundWorkflow : compoundWorkflow.getOtherCompoundWorkflows()) {
            if (!savedCompoundWorkflows.contains(otherCompoundWorkflow.getNodeRef())) {
                if (log.isDebugEnabled()) {
                    log.debug("Saving other compoundWorkflow attached to compoundWorkflow " + compoundWorkflow.getNode().getNodeRef() + ": " + otherCompoundWorkflow);
                }
                saveCompoundWorkflow(queue, otherCompoundWorkflow, null, savedCompoundWorkflows);
                CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(otherCompoundWorkflow.getNodeRef());
                checkCompoundWorkflow(freshCompoundWorkflow);
            }
        }
        if (CompoundWorkflow.class.equals(compoundWorkflow.getClass())) { // compound worflow tied to document (not compoundWorkflowDef)
            if (!StringUtils.equals(compoundWorkflow.getOwnerId(), previousOwnerId)) {
                // doesn't matter what status cWF has, just add the privileges
                NodeRef docRef = nodeService.getPrimaryParent(compoundWorkflow.getNodeRef()).getParentRef();
                privilegeService.setPermissions(docRef, compoundWorkflow.getOwnerId(), Privileges.VIEW_DOCUMENT_FILES);
            }
        }
        return changed;
    }

    private boolean saveWorkflow(WorkflowEventQueue queue, Workflow workflow) {
        // If this node is not saved, then all children are newly created, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and no children are being added or removed, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and at least one child is being added or removed, then childAssociationIndexes have to be set on all children
        // (because maybe all children have assocIndex=-1).
        boolean setChildAssocIndexes = false;
        boolean wasSaved = workflow.isSaved();
        boolean changed = createOrUpdate(queue, workflow, workflow.getParent().getNodeRef(), WorkflowCommonModel.Assocs.WORKFLOW);

        // Remove tasks
        for (Task removedTask : workflow.getRemovedTasks()) {
            NodeRef removedTaskNodeRef = removedTask.getNodeRef();
            if (removedTask.isSaved()) {
                checkTask(getTask(removedTaskNodeRef, workflow, false), Status.NEW);
                workflowDbService.deleteTask(removedTaskNodeRef);
                changed = true;
                setChildAssocIndexes = true;
            }
        }
        workflow.getRemovedTasks().clear();

        if (!setChildAssocIndexes && wasSaved) {
            for (Task task : workflow.getTasks()) {
                if (!task.isSaved()) {
                    setChildAssocIndexes = true;
                    break;
                }
            }
        }

        int index = 0;
        log.debug("Starting to save " + workflow.getTasks().size() + " tasks");
        for (Task task : workflow.getTasks()) {
            // Create or update task
            task.setTaskIndexInWorkflow(index);
            saveTask(queue, task);
            // TODO: is it necessary to determine if task's index in workflow changed and according to that change value returned by the function?
            // At the moment the returned value is not used anywhere
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
        Workflow assignmentWorkflowCopy = cWorkflowWorkflowsCopy.get(originalWFIndex);

        int originalTaskIndexBeforeRemove = workflowOriginal.getTasks().indexOf(assignmentTaskOriginal);
        if (assignmentTaskOriginal.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            Task assignmentTaskCopy = assignmentWorkflowCopy.getTasks().get(originalTaskIndexBeforeRemove);
            assignmentTaskCopy.getNode().getProperties().put(AssignmentWorkflowType.TEMP_DELEGATED.toString(), true);
            if (StringUtils.isBlank(assignmentTaskCopy.getComment())) {
                assignmentTaskCopy.setComment(I18NUtil.getMessage("task_comment_delegated"));
                if (WorkflowUtil.isActiveResponsible(assignmentTaskCopy)) {
                    assignmentTaskCopy.setActive(false);
                }
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

        { // validate that at least one new equivalent task is created (and it contains minimal information)
            boolean searchResponsibleTask = WorkflowUtil.isActiveResponsible(assignmentTaskOriginal);
            boolean isAssignmentWorkflow = assignmentTaskOriginal.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
            Task newMandatoryTask = null;
            boolean hasAtLeastOneDelegationTask = false;
            { // validate that tasks added during delegation have all mandatory fields filled and at least one task equivalent to delegatable task is also added
                for (Workflow workflow : cWorkflowWorkflowsCopy) {
                    if (isGeneratedByDelegation(workflow)) {
                        setStatus(queue, workflow, Status.IN_PROGRESS);
                    }
                    for (Task task : workflow.getTasks()) {
                        if (isGeneratedByDelegation(task)) {
                            hasAtLeastOneDelegationTask = true;
                            delegationTaskMandatoryFieldsFilled(task, feedback);
                            Date dueDate = task.getDueDate();
                            if (dueDate != null) {
                                dueDate.setHours(23);
                                dueDate.setMinutes(59);
                            }
                            if (isAssignmentWorkflow && workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && newMandatoryTask == null) {
                                if (!searchResponsibleTask) {
                                    newMandatoryTask = task;
                                } else if (WorkflowUtil.isActiveResponsible(task)) {
                                    newMandatoryTask = task;
                                }
                            }
                        }
                    }
                }
            }
            if (isAssignmentWorkflow) {
                if (newMandatoryTask == null) {
                    if (searchResponsibleTask) {
                        feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewResponsibleTask"));
                    } else {
                        feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noNewTask"));
                    }
                }
            } else if (!hasAtLeastOneDelegationTask) {
                feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "delegate_error_noDelegationTask"));
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
    public boolean getOrderAssignmentCategoryEnabled() {
        return orderAssignmentCategoryEnabled;
    }

    public void setOrderAssignmentCategoryEnabled(boolean orderAssignmentCategoryEnabled) {
        this.orderAssignmentCategoryEnabled = orderAssignmentCategoryEnabled;
    }

    @Override
    public boolean isOrderAssignmentWorkflowEnabled() {
        return orderAssignmentWorkflowEnabled;
    }

    public void setOrderAssignmentWorkflowEnabled(boolean enabled) {
        orderAssignmentWorkflowEnabled = enabled;
    }

    public void setConfirmationWorkflowEnabled(boolean enabled) {
        confirmationWorkflowEnabled = enabled;
    }

    @Override
    public boolean isConfirmationWorkflowEnabled() {
        return confirmationWorkflowEnabled;
    }

    @Override
    public List<Task> getTasks4DelegationHistory(Node delegatableTask) {
        NodeRef docRef = generalService.getAncestorNodeRefWithType(workflowDbService.getTaskParentNodeRef(delegatableTask.getNodeRef()), DocumentCommonModel.Types.DOCUMENT, true);
        NodeRef workflowRef = null;
        boolean orderAssignmentTask = delegatableTask.getType().equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        if (orderAssignmentTask) {
            workflowRef = workflowDbService.getTaskParentNodeRef(delegatableTask.getNodeRef());
        }
        List<Task> assignmentTasks = new ArrayList<Task>();
        cWfLoop: for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(docRef,
                Arrays.asList(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW))) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if ((!orderAssignmentTask && workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW))
                        || (orderAssignmentTask && workflow.getNodeRef().equals(workflowRef))) {
                    for (Task task : workflow.getTasks()) {
                        if (task.isStatus(Status.FINISHED, Status.IN_PROGRESS)) {
                            assignmentTasks.add(task);
                        }
                    }
                    if (orderAssignmentTask) {
                        break cWfLoop;
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
        Workflow parent = task.getParent();
        @SuppressWarnings("unchecked")
        boolean changed = createOrUpdate(queue, task, parent.getNodeRef(), null);
        List<NodeRef> removedFiles = task.getRemovedFiles();
        for (NodeRef removedFileRef : removedFiles) {
            nodeService.deleteNode(removedFileRef);
        }
        NodeRef taskRef = task.getNodeRef();
        workflowDbService.removeTaskFiles(taskRef, removedFiles);
        removedFiles.clear();
        List<String> existingDisplayNames = new ArrayList<String>();
        List<NodeRef> newFileRefs = new ArrayList<NodeRef>();
        for (Object fileObj : task.getFiles()) {
            if (!(fileObj instanceof FileWithContentType)) {
                // existing file, no update needed
                continue;
            }
            FileWithContentType file = (FileWithContentType) fileObj;
            String originalDisplayName = FilenameUtil.getDiplayNameFromName(file.fileName);
            // files are actually saved unde workflow
            NodeRef workflowNodeRef = parent.getNodeRef();
            Pair<String, String> filenames = FilenameUtil.getTaskFilenameFromDisplayname(task, existingDisplayNames, originalDisplayName, generalService,
                    workflowDbService);
            String fileDisplayName = filenames.getSecond();
            NodeRef fileRef = fileService.addFile(filenames.getFirst(), fileDisplayName, workflowNodeRef, file.file, file.contentType);
            newFileRefs.add(fileRef);
            existingDisplayNames.add(fileDisplayName);
        }
        workflowDbService.createTaskFileEntriesFromNodeRefs(taskRef, newFileRefs);
        return changed;
    }

    @Override
    public void saveInProgressTask(Task taskOriginal) throws WorkflowChangedException {
        Task task = taskOriginal.copy();
        task.getRemovedFiles().addAll(taskOriginal.getRemovedFiles());
        // check status==IN_PROGRESS, owner==currentUser
        requireInProgressCurrentUser(task);
        requireStatusUnchanged(task);

        WorkflowEventQueue queue = getNewEventQueue();
        saveTask(queue, task);
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
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(taskOriginal.getParent().getParent().getNodeRef());
        // insert (possibly modified) task into compoundWorkflow from repo
        Task task = replaceTask(taskOriginal, compoundWorkflow);

        WorkflowEventQueue queue = getNewEventQueue();

        queue.setParameter(WorkflowQueueParameter.TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM,
                new Boolean(isRecievedExternalReviewTask(task)));
        setTaskFinished(queue, task, outcomeIndex);

        // this finishing logic is executed only when task is set finished from user interface button
        if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && task.isResponsible()) {
            queue.setParameter(WorkflowQueueParameter.ORDER_ASSIGNMENT_FINISH_TRIGGERING_TASK, task);
            for (Task workflowTask : task.getParent().getTasks()) {
                if (workflowTask.isStatus(Status.IN_PROGRESS) && !task.getNodeRef().equals(workflowTask.getNodeRef())) {
                    workflowTask.setComment(I18NUtil.getMessage("task_comment_orderAssignmentTask_finished_automatically"));
                    setTaskFinished(queue, workflowTask, outcomeIndex);
                }
            }
        }
        saveAndCheckCompoundWorkflow(queue, compoundWorkflow);
    }

    @Override
    public boolean isRecievedExternalReviewTask(Task task) {
        return task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                && ((isInternalTesting() && !Boolean.TRUE.equals(nodeService.getProperty(task.getParent().getParent().getParent(),
                        DocumentSpecificModel.Props.NOT_EDITABLE)))
                || (!isInternalTesting() && !isResponsibleCurrenInstitution(task)));
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
        NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(taskOriginal.getParent().getNodeRef()).getParentRef();
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(compoundWorkflowRef);

        Task task = replaceTask(taskOriginal, compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        queue.setParameter(WorkflowQueueParameter.EXTERNAL_REVIEWER_TRIGGERING_TASK, task.getNodeRef());

        setTaskFinishedOrUnfinished(queue, task, Status.FINISHED, null, -1, false);

        saveAndCheckCompoundWorkflow(queue, compoundWorkflow);
    }

    private void saveAndCheckCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) throws WorkflowChangedException {
        // check and save current task's compound workflow
        stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);

        saveCompoundWorkflow(queue, compoundWorkflow, null);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + compoundWorkflow);
        }
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNodeRef());
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
                    if (tasks.get(i).getNodeRef().equals(replacementTask.getNodeRef())) {
                        newTask = replacementTask.copy(workflow);
                        // this is not general desired behaviour during copy, don't move this to Task.copy method
                        newTask.getRemovedFiles().addAll(replacementTask.getRemovedFiles());
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
        checkCompoundWorkflow(compoundWorkflow, Status.NEW, Status.FINISHED);
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + compoundWorkflow);
        }
        nodeService.deleteNode(nodeRef);

        final NodeRef docRef = compoundWorkflow.getParent();
        final boolean hasAllFinishedCompoundWorkflows = hasAllFinishedCompoundWorkflows(docRef);

        // Ignore document locking, because we are not changing a property that is user-editable or related to one
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, hasAllFinishedCompoundWorkflows);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public CompoundWorkflow startCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        proccessPreSave(queue, compoundWorkflow);
        checkCompoundWorkflow(compoundWorkflow, Status.NEW);

        setStatus(queue, compoundWorkflow, Status.IN_PROGRESS);
        stepAndCheck(queue, compoundWorkflow);
        saveCompoundWorkflow(queue, compoundWorkflow, null);
        if (log.isDebugEnabled()) {
            log.debug("Started " + compoundWorkflow);
        }

        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow finishCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        queue.setParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY, Boolean.TRUE);
        proccessPreSave(queue, compoundWorkflow);
        CompoundWorkflow freshCompoundWorkflow = finishCompoundWorkflow(queue, compoundWorkflow, "task_outcome_finished_manually");
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    @Override
    public void finishTasksByRegisteringReplyLetter(NodeRef docRef, String comment) {
        boolean unfinishAllTasks = !hasTaskOfType(docRef, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW);
        WorkflowEventQueue queue = getNewEventQueue();
        queue.getParameters().put(WorkflowQueueParameter.TRIGGERED_BY_DOC_REGISTRATION, Boolean.TRUE);

        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            if (compoundWorkflow.isStatus(Status.NEW)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW) || workflow.isStatus(Status.FINISHED)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (task.isStatus(Status.IN_PROGRESS)
                            && task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
                            && getUserNameToCheck().equals(task.getOwnerId())) {
                        task.setComment(comment);
                        setTaskFinishedOrUnfinished(queue, task, Status.FINISHED, "task_outcome_assignmentTask0", 0, true);
                    } else if (unfinishAllTasks && task.isStatus(Status.NEW, Status.IN_PROGRESS)) {
                        // this is different from usual workflow handling where task status change NEW -> UNFINISHED is not allowed
                        setTaskFinishedOrUnfinished(queue, task, Status.UNFINISHED, "task_outcome_unfinished_by_registering_reply_letter", -1, true);
                    }
                }
            }
            finishNewWorkflows(compoundWorkflow, queue);
            stepAndCheck(queue, compoundWorkflow);
            saveCompoundWorkflow(queue, compoundWorkflow, null);
        }

        for (NodeRef compoundWorkflowRef : getCompoundWorkflowNodeRefs(docRef)) {
            CompoundWorkflow compoundWorkflow = getCompoundWorkflow(compoundWorkflowRef);
            if (compoundWorkflow.isStatus(Status.NEW)) {
                deleteCompoundWorkflow(compoundWorkflowRef);
            } else {
                checkCompoundWorkflow(compoundWorkflow);
                checkActiveResponsibleAssignmentTasks(compoundWorkflow.getParent());
            }
        }
        handleEvents(queue);
    }

    @Override
    public void unfinishTasksByFinishingLetterResponsibleTask(Task thisTask, WorkflowEventQueue queue) {
        CompoundWorkflow thisCompoundWorkflow = thisTask.getParent().getParent();
        // add oher compound worfklows so they are involved in the same saving process
        addOtherCompundWorkflows(thisCompoundWorkflow);
        List<CompoundWorkflow> compoundWorkflows = thisCompoundWorkflow.getOtherCompoundWorkflows();
        // pass this compound workflow with other compound workflows
        compoundWorkflows.add(thisCompoundWorkflow);

        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (compoundWorkflow.isStatus(Status.NEW)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isType(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW) || workflow.isStatus(Status.FINISHED)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (task.isStatus(Status.NEW, Status.IN_PROGRESS) && Task.Action.FINISH != task.getAction() && Task.Action.UNFINISH != task.getAction()) {
                        // this is different from usual workflow handling where task status change NEW -> UNFINISHED is not allowed
                        setTaskFinishedOrUnfinished(queue, task, Status.UNFINISHED, "task_outcome_unfinished_by_finishing_responsible_task", -1, true);
                    }
                }
            }
            finishNewWorkflows(compoundWorkflow, queue);
            stepAndCheck(queue, compoundWorkflow);
        }

        // Assume that after task statuses are set and compound workflow/workflow statuses are recalculated,
        // the status state of given objects in this process is final, so it is safe to check compound workflows for NEW status and delete corresponding
        // compound workflows, even if other objects' data is saved later in this process.
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (compoundWorkflow.isStatus(Status.NEW)) {
                deleteCompoundWorkflow(compoundWorkflow.getNodeRef());
                thisCompoundWorkflow.getOtherCompoundWorkflows().remove(compoundWorkflow);
            }
        }
    }

    private void finishNewWorkflows(CompoundWorkflow compoundWorkflow, WorkflowEventQueue queue) {
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isStatus(Status.NEW) && isStatusAll(workflow.getTasks(), Status.FINISHED, Status.UNFINISHED)) {
                setStatus(queue, workflow, Status.FINISHED);
            }
        }
    }

    private CompoundWorkflow finishCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, String taskOutcomeLabelId) {
        // allow all statuses when finishing on registering reply document
        if (checkCompoundWorkflow(compoundWorkflow, Status.IN_PROGRESS, Status.FINISHED) == Status.FINISHED) {
            if (log.isDebugEnabled()) {
                log.debug("CompoundWorkflow is already finished, finishing is not performed, saved as is: " + compoundWorkflow);
            }
        } else {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (isStatus(workflow, Status.NEW, Status.IN_PROGRESS)) {
                    setStatus(queue, workflow, Status.FINISHED);

                    for (Task task : workflow.getTasks()) {
                        if (isStatus(task, Status.IN_PROGRESS, Status.NEW)) {
                            setTaskFinishedOrUnfinished(queue, task, Status.UNFINISHED, taskOutcomeLabelId, -1, true);
                        }
                    }
                }
            }
            stepAndCheck(queue, compoundWorkflow, Status.FINISHED);
            saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Finished " + compoundWorkflow);
            }
        }

        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow stopCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        proccessPreSave(queue, compoundWorkflow);

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
            saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Stopped " + compoundWorkflow);
            }
        }

        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow continueCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        WorkflowEventQueue queue = getNewEventQueue();
        proccessPreSave(queue, compoundWorkflow);

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
            saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Continued " + compoundWorkflow);
            }
        }

        // also check repo status
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
        return freshCompoundWorkflow;
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

    private void continueTasks(WorkflowEventQueue queue, Workflow workflow) {
        for (Task task : workflow.getTasks()) {
            if (isStatus(task, Status.STOPPED)) {
                setStatus(queue, task, Status.IN_PROGRESS);
            }
            task.setStoppedDateTime(null);
        }
    }

    @Override
    public CompoundWorkflow copyAndResetCompoundWorkflow(NodeRef compoundWorkflowRef) {
        CompoundWorkflow newCompoundWorkflow = getCompoundWorkflow(compoundWorkflowRef);
        resetCompoundWorkflow(newCompoundWorkflow);
        return newCompoundWorkflow;
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
        int count = getActiveResponsibleTasks(document, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);
        if (count > 1) {
            log.debug("Document has " + count + " active responsible tasks: " + document);
            throw new WorkflowActiveResponsibleTaskException();
        }
    }

    @Override
    public int getActiveResponsibleTasks(NodeRef document, QName workflowType) {
        return getActiveResponsibleTasks(document, workflowType, false);
    }

    @Override
    public int getActiveResponsibleTasks(NodeRef document, QName workflowType, boolean allowFinished) {
        Status[] allowedStatuses = allowFinished ? new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED } :
                new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED };
        int counter = 0;
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(document, Collections.singletonList(workflowType))) {
            if (!compoundWorkflow.isStatus(allowedStatuses)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isType(workflowType) || !workflow.isStatus(allowedStatuses)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (!task.isStatus(allowedStatuses)) {
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
    public boolean hasInProgressActiveResponsibleTasks(NodeRef document) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(document, WorkflowSpecificModel.RESPONSIBLE_TASK_WORKFLOW_TYPES)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                for (Task task : workflow.getTasks()) {
                    if (isStatus(task, Status.NEW, Status.UNFINISHED, Status.FINISHED)) {
                        continue;
                    }
                    if (isActiveResponsible(task)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasInProgressOtherUserOrderAssignmentTasks(NodeRef docRef) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(docRef, Arrays.asList(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW))) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                for (Task task : workflow.getTasks()) {
                    if (task.isStatus(Status.IN_PROGRESS) && getUserNameToCheck().equals(task.getOwnerId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasUnfinishedReviewTasks(NodeRef docNode) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(docNode,
                Arrays.asList(WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW))) {
            if (!compoundWorkflow.isStatus(Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                for (Task task : workflow.getTasks()) {
                    if (task.isStatus(Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasTaskOfType(NodeRef docRef, QName... workflowTypes) {
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isType(workflowTypes)) {
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
                if (!workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                    continue;
                }
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
    public void setTaskOwner(NodeRef taskRef, String ownerId, boolean retainPreviousOwnerId) {
        if (!dictionaryService.isSubClass(workflowDbService.getTaskType(taskRef), WorkflowCommonModel.Types.TASK)) {
            throw new RuntimeException("Node is not a task: " + taskRef);
        }
        Task task = workflowDbService.getTask(taskRef, taskPrefixedQNames, null, false);
        String existingOwnerId = task.getOwnerId();
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

        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, taskRef, "applog_task_assigned",
                UserUtil.getPersonFullName1(personProps), MessageUtil.getTypeName(getNodeRefType(taskRef))));

        props.put(WorkflowCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
        props.put(WorkflowCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
        props.put(WorkflowCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) organizationStructureService.getOrganizationStructurePaths(
                (String) personProps.get(ContentModel.PROP_ORGID)));
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
        String previousOwnerId = (retainPreviousOwnerId) ? existingOwnerId : null;
        props.put(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID, previousOwnerId);
        workflowDbService.updateTaskProperties(taskRef, props);
    }

    @Override
    public QName getNodeRefType(final NodeRef nodeRef) {
        if (nodeService.exists(nodeRef)) {
            return nodeService.getType(nodeRef);
        }
        return workflowDbService.getTaskType(nodeRef);
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
        boolean saved = object.isSaved();
        if (saved) {
            NodeRef nodeRef = object.getNodeRef();
            if (object instanceof Task) {
                repoValue = (String) workflowDbService.getTaskProperty(nodeRef, repoPropertyName);
            } else {
                repoValue = (String) nodeService.getProperty(nodeRef, repoPropertyName);
            }
        }
        boolean matches = false;
        for (String requiredValue : requiredValues) {
            if (!requiredValue.equals(objectValue) || (saved && !requiredValue.equals(repoValue))) {
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

    @Override
    public void registerMultiEventListener(WorkflowMultiEventListener listener) {
        Assert.notNull(listener);
        multiEventListeners.add(listener);
        if (log.isDebugEnabled()) {
            log.debug("Registered multi-event listener: " + listener);
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
        for (WorkflowMultiEventListener listener : multiEventListeners) {
            listener.handleMultipleEvents(queue);
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
        boolean parallelTasks = workflow.isParallelTasks();
        boolean isReviewWorkFlow = workflow.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
        for (Task task : tasks) {
            if (isReviewWorkFlow) {
                setStopOnFinishByOutcome(workflow, task);
            }
            // Start all new tasks
            if (parallelTasks && isStatus(task, Status.NEW)) {
                setStatus(queue, task, Status.IN_PROGRESS);
            }
        }
        if (!parallelTasks && !isStatusAny(tasks, Status.IN_PROGRESS)) {
            // Start first new task
            for (Task task : tasks) {
                if (isStatus(task, Status.NEW)) {
                    setStatus(queue, task, Status.IN_PROGRESS);
                    break;
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

    private void setStopOnFinishByOutcome(Workflow workflow, Task task) {
        if (isStatus(task, Status.FINISHED) && task.getOutcome().equals(MessageUtil.getMessage("task_outcome_reviewTask1"))) {
            workflow.setProp(WorkflowCommonModel.Props.STOP_ON_FINISH, true);
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
                                log.debug("Detected automatic start of workflow!\n  Finished workflow: " + object.getNodeRef()
                                        + "\n  Started workflow: " + workflow.getNodeRef());
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

        List<Object> extras = new ArrayList<Object>();
        if (Boolean.TRUE.equals(queue.getParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY))) {
            extras.add(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY);
        }
        extras.add(originalStatus);

        queueEvent(queue, WorkflowEventType.STATUS_CHANGED, object, extras.toArray());
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
                && (!task.getNodeRef().equals(queue.getParameter(WorkflowQueueParameter.EXTERNAL_REVIEWER_TRIGGERING_TASK)));
    }

    private boolean stepAndCheck(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, Status... requiredStatuses) {
        return stepAndCheck(queue, compoundWorkflow, new HashSet<NodeRef>(), requiredStatuses);
    }

    private boolean stepAndCheck(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, Set<NodeRef> checkedNodeRefs, Status... requiredStatuses) {
        fireCreatedEvents(queue, compoundWorkflow);
        int before;
        do {
            before = queue.getEvents().size();
            stepCompoundWorkflow(queue, compoundWorkflow);
        } while (queue.getEvents().size() > before);
        checkCompoundWorkflow(compoundWorkflow, requiredStatuses);
        checkedNodeRefs.add(compoundWorkflow.getNodeRef());
        for (CompoundWorkflow otherCompoundWorkflow : compoundWorkflow.getOtherCompoundWorkflows()) {
            if (!checkedNodeRefs.contains(otherCompoundWorkflow.getNodeRef())) {
                stepAndCheck(queue, otherCompoundWorkflow, checkedNodeRefs);
            }
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
        if (object.isUnsaved()) {
            queueEvent(queue, WorkflowEventType.CREATED, object);
        }
    }

    private void setTaskFinishedOrUnfinished(WorkflowEventQueue queue, Task task, Status newStatus, String outcomeLabelId, int outcomeIndex, boolean getOutcome) {
        if (!(newStatus == Status.FINISHED || newStatus == Status.UNFINISHED) || isStatus(task, Status.FINISHED, newStatus)) {
            throw new RuntimeException("New or old status is illegal, new='" + newStatus.getName() + "', old " + task);
        }
        // task outcome is used during event handling in workflow type,
        // so this needs to be performed before calling setStatus
        if (getOutcome) {
            task.setCompletedDateTime(queue.getNow());
            task.setOutcome(getOutcomeText(outcomeLabelId, outcomeIndex), outcomeIndex);
        }
        setStatus(queue, task, newStatus);
        if (isStoppingNeeded(task, outcomeIndex)) {
            stopIfNeeded(task, queue);
        }
        if (task.isType(WorkflowSpecificModel.Types.REVIEW_TASK) && (outcomeIndex == REVIEW_TASK_OUTCOME_ACCEPTED || outcomeIndex == REVIEW_TASK_OUTCOME_ACCEPTED_WITH_COMMENT)) {
            List<File> files = fileService.getAllFilesExcludingDigidocSubitems(task.getParent().getParent().getParent());
            List<String> filesWithVersions = new ArrayList<String>();
            behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
            for (File file : files) {
                final NodeRef fileRef = file.getNodeRef();
                String nextVersionLabel = versionsService.calculateNextVersionLabel(fileRef);
                filesWithVersions.add(file.getDisplayName() + " " + nextVersionLabel);
                AuthenticationUtil.runAs(new RunAsWork<Void>() {
                    @Override
                    public Void doWork() throws Exception {
                        nodeService.setProperty(fileRef, FileModel.Props.NEW_VERSION_ON_NEXT_SAVE, Boolean.TRUE);
                        return null;
                    }
                }, AuthenticationUtil.getSystemUserName());
            }
            behaviourFilter.enableBehaviour(ContentModel.ASPECT_AUDITABLE);
            task.setProp(WorkflowSpecificModel.Props.FILE_VERSIONS, StringUtils.join(filesWithVersions, ", "));
        }

        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, task.getNodeRef(), "applog_task_done",
                MessageUtil.getTypeName(task.getType()), task.getOutcome()));
    }

    private boolean isStoppingNeeded(Task task, int outcomeIndex) {
        if (outcomeIndex == SIGNATURE_TASK_OUTCOME_NOT_SIGNED && Types.SIGNATURE_TASK.equals(task.getNode().getType())) {
            return true;
        } else if (task.isType(Types.REVIEW_TASK)) {
            // sometimes here value of TEMP_OUTCOME is Integer, sometimes String
            final Integer tempOutcome = DefaultTypeConverter.INSTANCE.convert(Integer.class, task.getProp(WorkflowSpecificModel.Props.TEMP_OUTCOME));
            if (tempOutcome != null && tempOutcome == REVIEW_TASK_OUTCOME_REJECTED) { // KAAREL: What is tempOutcome and why was it null?
                return true;
            }
        } else if (outcomeIndex == CONFIRMATION_TASK_OUTCOME_REJECTED && task.isType(Types.CONFIRMATION_TASK)) {
            return true;
        }
        return false;
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
     * Common logic when signature task is not signed or review task or confirmation task is not accepted(rejected)
     * 
     * @param task - signature task, review task or confirmation task that was rejected
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
            boolean isParentWorkflow = parentWorkFlow.getNodeRef().equals(workflow.getNodeRef());
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
                    if (isParentWorkflow && !aTask.getNodeRef().equals(task.getNodeRef())) {
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
        return RepoUtil.getAspectsIgnoringSystem(aspects);
    }

    private Set<QName> getNodeAspects(NodeRef nodeRef) {
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        return RepoUtil.getAspectsIgnoringSystem(aspects);
    }

    private Map<QName, Serializable> getDefaultProperties(QName className) {
        return RepoUtil.getPropertiesIgnoringSystem(generalService.getDefaultProperties(className), dictionaryService);
    }

    private Map<QName, Serializable> getNodeProperties(NodeRef nodeRef) {
        return RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(nodeRef), dictionaryService);
    }

    private boolean createOrUpdate(WorkflowEventQueue queue, BaseWorkflowObject object, NodeRef parent, QName assocType) {
        boolean isTask = object instanceof Task;
        Assert.isTrue(!isTask || assocType == null, "tasks cannot be written to repo!");
        boolean changed = false;
        WmNode node = object.getNode();
        if (object.isUnsaved()) {
            // If saving a new node, then set creator to current user
            object.setCreatorName(userService.getUserFullName());
            if (isTask) {
                String username = userService.getCurrentUserName();
                ((Task) object).setCreatorId(username);
                ((Task) object).setCreatorEmail(userService.getUserEmail(username));
                ((Task) object).setDocumentType(getDocumentTypeFromTaskParent(parent));
            }
            if (object.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                object.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, dvkService.getInstitutionCode());
            }
        }

        object.preSave();

        Map<QName, Serializable> props = getSaveProperties(object.getChangedProperties());
        if (object.isUnsaved()) {
            // Create workflow
            if (log.isDebugEnabled()) {
                log.debug("Creating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                        + WmNode.toString(props.entrySet()));
            }
            NodeRef nodeRef;
            if (!isTask) {
                nodeRef = nodeService.createNode(parent, assocType, assocType, node.getType(), props).getChildRef();
            } else {
                nodeRef = new NodeRef(parent.getStoreRef(), GUID.generate());
            }
            node.updateNodeRef(nodeRef);
            changed = true;

            if (!isTask) {
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
            } else {
                workflowDbService.createTaskEntry((Task) object);
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
                    // if original status in this process was not "uus" trigger standard functionality to throw workflow exception
                    requireValue(object, (String) object.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS), WorkflowCommonModel.Props.STATUS, Status.NEW.getName());
                }

                if (!isTask) {
                    nodeService.addProperties(node.getNodeRef(), props); // do not replace non-changed properties
                }
                changed = true;
                // task update event is not queued as currently no action is required on task update
                // and for large workflows it can impose performance issues (OutOfMemory: java heap space)
                if (!isTask) {
                    queueEvent(queue, WorkflowEventType.UPDATED, object, props);
                }
                // adding/removing aspects is not implemented - not needed for now
            }
            if (isTask) {
                workflowDbService.updateTaskEntry((Task) object, props);
            }
        }
        object.setChangedProperties(props);
        return changed;
    }

    private String getDocumentTypeFromTaskParent(NodeRef parent) {
        NodeRef doc = parent;
        do {
            if (nodeService.getType(doc).equals(DocumentCommonModel.Types.DOCUMENT)) {
                return (String) nodeService.getProperty(doc, DocumentAdminModel.Props.OBJECT_TYPE_ID);
            }
            doc = nodeService.getPrimaryParent(doc).getParentRef();
        } while (doc != null);
        return null;
    }

    @Override
    public void createDueDateExtension(String reason, Date newDate, Date dueDate, Task initiatingTask, NodeRef containerRef) {
        CompoundWorkflow extensionCompoundWorkflow = getNewCompoundWorkflow(getNewCompoundWorkflowDefinition().getNode(), containerRef);
        Workflow workflow = getWorkflowService().addNewWorkflow(extensionCompoundWorkflow, WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW,
                extensionCompoundWorkflow.getWorkflows().size(), true);
        Task extensionTask = workflow.addTask();
        String creatorName = initiatingTask.getCreatorName();
        extensionTask.setOwnerName(creatorName);
        extensionTask.setOwnerId(initiatingTask.getCreatorId());
        extensionTask.setOwnerEmail(initiatingTask.getCreatorEmail()); // updater
        Map<QName, Serializable> creatorProps = userService.getUserProperties(initiatingTask.getCreatorId());
        if (creatorProps != null) {
            extensionTask.setOwnerJobTitle((String) creatorProps.get(ContentModel.PROP_JOBTITLE));
            List<String> orgName = organizationStructureService.getOrganizationStructurePaths((String) creatorProps.get(ContentModel.PROP_ORGID));
            extensionTask.setOwnerOrgStructUnitProp(orgName);
        }
        workflow.setProp(WorkflowSpecificModel.Props.RESOLUTION, reason);
        extensionTask.setProposedDueDate(newDate);
        dueDate.setHours(23);
        dueDate.setMinutes(59);
        extensionTask.setDueDate(dueDate);
        extensionCompoundWorkflow = saveCompoundWorkflow(extensionCompoundWorkflow);
        NodeRef dueDateExtensionTask = extensionCompoundWorkflow.getWorkflows().get(0).getTasks().get(0).getNodeRef();
        workflowDbService.createTaskDueDateExtensionAssocEntry(initiatingTask.getNodeRef(), dueDateExtensionTask);

        BeanHelper.getDocumentLogService().addDocumentLog(extensionCompoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
        extensionCompoundWorkflow = startCompoundWorkflow(extensionCompoundWorkflow);
    }

    @Override
    public void changeInitiatingTaskDueDate(Task task, WorkflowEventQueue queue) {
        List<Task> initiatingTasks = workflowDbService.getDueDateExtensionInitiatingTask(task.getNodeRef(), taskPrefixedQNames);
        if (initiatingTasks.size() != 1) {
            throw new RuntimeException("dueDateExtension task must have exactly one initiating task; current task has " + initiatingTasks.size() + " initiating tasks.");
        }
        Task initiatingTask = getTask(initiatingTasks.get(0).getNodeRef(), true);
        addDueDateHistoryRecord(initiatingTask, task);
        initiatingTask.setDueDate(task.getConfirmedDueDate());
        initiatingTask.setHasDueDateHistory(true);
        saveTask(queue, initiatingTask);
    }

    @SuppressWarnings("unchecked")
    private void addDueDateHistoryRecord(Task initiatingTask, Task task) {
        String comment = task.getComment();
        Date previousDueDate = initiatingTask.getDueDate();
        String changeReason = StringUtils.isNotBlank(comment) ? comment : task.getResolution();
        workflowDbService.createTaskDueDateHistoryEntries(initiatingTask.getNodeRef(), Arrays.asList(new Pair<String, Date>(changeReason, previousDueDate)));
        NodeRef initatingTaskRef = initiatingTask.getNodeRef();
        String previousDueDateStr = previousDueDate != null ? Task.dateFormat.format(previousDueDate) : null;
        String confirmedDueDateStr = task.getConfirmedDueDate() != null ? Task.dateFormat.format(task.getConfirmedDueDate()) : null;
        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, initatingTaskRef, "applog_task_deadline",
                initiatingTask.getOwnerName(), MessageUtil.getTypeName(task.getType()), previousDueDateStr, confirmedDueDateStr, changeReason));
    }

    // ---

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
    public Task createTaskInMemory(NodeRef wfRef, WorkflowType workflowType, Map<QName, Serializable> props) {
        HashSet<QName> taskDefaultAspects = new HashSet<QName>(taskDataTypeDefaultAspects.get(workflowType.getWorkflowType()));
        NodeRef taskRef = new NodeRef(wfRef.getStoreRef(), GUID.generate());
        WmNode taskNode = new WmNode(taskRef, workflowType.getTaskType(), taskDefaultAspects, props);
        return Task.create(workflowType.getTaskClass(), taskNode, null, workflowType.getTaskOutcomes());
    }

    @Override
    public Map<QName, WorkflowType> getWorkflowTypesByTask() {
        return workflowTypesByTask;
    }

    @Override
    public boolean externalReviewWorkflowEnabled() {
        return Boolean.parseBoolean(parametersService.getStringParameter(Parameters.EXTERNAL_REVIEW_WORKFLOW_ENABLED));
    }

    @Override
    public Map<QName, Collection<QName>> getTaskDataTypeDefaultAspects() {
        return taskDataTypeDefaultAspects;
    }

    @Override
    public Map<QName, List<QName>> getTaskDataTypeDefaultProps() {
        return taskDataTypeDefaultProps;
    }

    @Override
    public Map<QName, QName> getTaskPrefixedQNames() {
        return taskPrefixedQNames;
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

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
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

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    @Override
    public boolean isInternalTesting() {
        return INTERNAL_TESTING;
    }

    public void setInternalTesting(boolean internalTesting) {
        INTERNAL_TESTING = internalTesting;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    // END: getters / setters

    /**
     * The type of action performs (after getting confirmation)
     * 
     * @author Vladimir Drozdik
     */
    public static enum DialogAction {
        SAVING, STARTING, CONTINUING;
    }

}
