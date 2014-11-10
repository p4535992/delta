package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.log.PropDiffHelper.value;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkCompoundWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkTask;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.checkWorkflow;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isActiveResponsible;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Props;
import ee.webmedia.alfresco.casefile.log.service.CaseFileLogService;
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException.ErrorCause;
import ee.webmedia.alfresco.workflow.generated.DeleteLinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.LinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
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

public class WorkflowServiceImpl implements WorkflowService, WorkflowModifications, BeanFactoryAware {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowServiceImpl.class);
    private static final org.apache.commons.logging.Log log2 = org.apache.commons.logging.LogFactory.getLog(WorkflowServiceImpl.class.getName() + ".log2");
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
    private DocumentLogService documentLogService;
    private CaseFileLogService caseFileLogService;
    private WorkflowConstantsBean workflowConstantsBean;
    private ApplicationConstantsBean applicationConstantsBean;
    // START: properties that would cause dependency cycle when trying to inject them
    private DocumentAssociationsService _documentAssociationsService;
    // END: properties that would cause dependency cycle when trying to inject them
    protected BeanFactory beanFactory;
    private WorkflowDbService workflowDbService;
    private BulkLoadNodeService bulkLoadNodeService;
    private SimpleCache<NodeRef, CompoundWorkflowDefinition> compoundWorkflowDefinitionsCache;

    private final List<WorkflowEventListener> eventListeners = new ArrayList<WorkflowEventListener>();
    private final List<WorkflowMultiEventListener> multiEventListeners = new ArrayList<WorkflowMultiEventListener>();
    private final List<WorkflowEventListenerWithModifications> immediateEventListeners = new ArrayList<WorkflowEventListenerWithModifications>();
    private final Set<QName> ownerRelatedKeys = new HashSet<QName>(Arrays.asList(WorkflowCommonModel.Props.OWNER_ID, WorkflowCommonModel.Props.OWNER_NAME,
            WorkflowCommonModel.Props.PARALLEL_TASKS, WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, WorkflowCommonModel.Props.OWNER_JOB_TITLE));
    private NodeRef compoundWorkflowDefinitionsRoot;

    // ========================================================================
    // ========================== GET FROM REPOSITORY =========================
    // ========================================================================

    private NodeRef getRoot() {
        if (compoundWorkflowDefinitionsRoot == null) {
            compoundWorkflowDefinitionsRoot = generalService.getNodeRef(WorkflowCommonModel.Repo.WORKFLOWS_SPACE);
        }
        return compoundWorkflowDefinitionsRoot;
    }

    @Override
    public NodeRef getIndependentWorkflowsRoot() {
        return generalService.getNodeRef(WorkflowCommonModel.Repo.INDEPENDENT_WORKFLOWS_SPACE);
    }

    @Override
    public List<CompoundWorkflowDefinition> getActiveCompoundWorkflowDefinitions(boolean getUserFullName) {
        return getCompoundWorkflowDefinitions(getUserFullName, applicationConstantsBean.isCaseVolumeEnabled(), workflowConstantsBean.isIndependentWorkflowEnabled(),
                workflowConstantsBean.isDocumentWorkflowEnabled());
    }

    private List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(boolean getUserFullName, boolean getCaseFileType, boolean getIndependentType, boolean getDocumentType) {
        List<ChildAssociationRef> childAssocs = getAllCompoundWorkflowDefinitionRefs();
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            CompoundWorkflowType type = getCompoundWorkflowType(nodeRef);
            if (!getCaseFileType && CompoundWorkflowType.CASE_FILE_WORKFLOW == type) {
                continue;
            } else if (!getIndependentType && CompoundWorkflowType.INDEPENDENT_WORKFLOW == type) {
                continue;
            } else if (!getDocumentType && CompoundWorkflowType.DOCUMENT_WORKFLOW == type) {
                continue;
            }
            compoundWorkflowDefinitions.add(getCompoundWorkflowDefinition(nodeRef, getRoot(), getUserFullName));
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public List<CompoundWorkflowDefinition> getIndependentCompoundWorkflowDefinitions(String userId) {
        return getCompoundWorkflowDefinitionsByType(userId, CompoundWorkflowType.INDEPENDENT_WORKFLOW, true);
    }

    @Override
    public CompoundWorkflowDefinition getCompoundWorkflowDefinition(NodeRef nodeRef, NodeRef parentRef) {
        CompoundWorkflowDefinition definition = compoundWorkflowDefinitionsCache.get(nodeRef);
        if (definition == null) {
            WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION, false, false);
            definition = new CompoundWorkflowDefinition(node, parentRef);
            compoundWorkflowDefinitionsCache.put(nodeRef, definition);
        }
        return definition;
    }

    @Override
    public void removeDeletedCompoundWorkflowDefinitionFromCache() {
        Set<NodeRef> allDefinitionRefs = bulkLoadNodeService.loadChildRefs(getRoot(), WorkflowCommonModel.Props.TYPE, null,
                WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        Collection<NodeRef> removed = CollectionUtils.subtract(compoundWorkflowDefinitionsCache.getKeys(), allDefinitionRefs);
        for (NodeRef ref : removed) {
            compoundWorkflowDefinitionsCache.remove(ref);
        }
    }

    @Override
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitionsByType(String userId, CompoundWorkflowType workflowType, boolean compoundWorkflowsWithoutOwner) {
        NodeRef root = getRoot();
        Set<NodeRef> definitionRefs = bulkLoadNodeService.loadChildRefs(root, WorkflowCommonModel.Props.TYPE, workflowType.name(),
                WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(definitionRefs.size());
        for (NodeRef ref : definitionRefs) {
            CompoundWorkflowDefinition compoundWorkflowDefinition = getCompoundWorkflowDefinition(ref, root);
            if (compoundWorkflowDefinition == null) {
                log.warn("Unknown compoundWorkflowDefinition: " + ref);
                continue;
            }
            String compWorkflowUserId = (String) compoundWorkflowDefinition.getProp(WorkflowCommonModel.Props.USER_ID);
            if ((compoundWorkflowsWithoutOwner && StringUtils.isBlank(compWorkflowUserId)) || StringUtils.equals(userId, compWorkflowUserId)) {
                compoundWorkflowDefinitions.add(compoundWorkflowDefinition);
            }
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public List<ChildAssociationRef> getAllCompoundWorkflowDefinitionRefs() {
        return nodeService.getChildAssocs(getRoot());
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
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false, true, true, true)) {
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
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<>();
        if (applicationConstantsBean.isCaseVolumeEnabled()) {
            compoundWorkflowDefinitions.addAll(getCompoundWorkflowDefinitionsByType(userId, CompoundWorkflowType.CASE_FILE_WORKFLOW, false));
        }
        if (workflowConstantsBean.isDocumentWorkflowEnabled()) {
            compoundWorkflowDefinitions.addAll(getCompoundWorkflowDefinitionsByType(userId, CompoundWorkflowType.DOCUMENT_WORKFLOW, false));
        }
        if (workflowConstantsBean.isIndependentWorkflowEnabled()) {
            compoundWorkflowDefinitions.addAll(getCompoundWorkflowDefinitionsByType(userId, CompoundWorkflowType.INDEPENDENT_WORKFLOW, false));
        }
        return compoundWorkflowDefinitions;
    }

    @Override
    public NodeRef createCompoundWorkflowDefinition(CompoundWorkflow compoundWorkflow, String userId, String newCompWorkflowDefinitionName) {
        CompoundWorkflowDefinition compWorkflowDefinition = getNewCompoundWorkflowDefinition();
        compWorkflowDefinition.setUserId(userId);
        compWorkflowDefinition.setName(newCompWorkflowDefinitionName);
        compWorkflowDefinition.setTypeEnum(compoundWorkflow.getTypeEnum());
        if (compoundWorkflow.isDocumentWorkflow()) {
            NodeRef docRef = compoundWorkflow.getParent();
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

    @Override
    public List<CompoundWorkflow> getCompoundWorkflows(NodeRef parent, NodeRef nodeRefToSkip) {
        return getCompoundWorkflows(parent, nodeRefToSkip, true);
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

    @Override
    public List<NodeRef> getCompoundWorkflowNodeRefs(NodeRef parent) {
        Assert.isTrue(parent != null, "Compound workflow container reference must not be null!");
        if (!nodeService.exists(parent)) {
            log.info("Tried to query compound workflow nodeRefs with nonexistent parent reference!");
            return Collections.emptyList();
        }
        QName parentType = nodeService.getType(parent);
        Assert.isTrue(!WorkflowCommonModel.Types.INDEPENDENT_COMPOUND_WORKFLOWS_ROOT.equals(parentType), "Querying all independent compound workflows is not supported!");
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
        return getCompoundWorkflow(nodeRef, loadTasks, true);
    }

    @Override
    public CompoundWorkflow getCompoundWorkflow(NodeRef compoundWorkflownodeRef, boolean loadTasks, boolean loadWorkflows) {
        WmNode node = getNode(compoundWorkflownodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(compoundWorkflownodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        if (loadWorkflows) {
            getAndAddWorkflows(compoundWorkflow.getNodeRef(), compoundWorkflow, false, loadTasks);
        }
        return compoundWorkflow;
    }

    private CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, List<Pair<String, Object[]>> reviewTaskDvkInfoMessages) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        compoundWorkflow.getReviewTaskDvkInfoMessages().clear();
        compoundWorkflow.getReviewTaskDvkInfoMessages().addAll(reviewTaskDvkInfoMessages);
        return compoundWorkflow;
    }

    @Override
    public void addOtherCompundWorkflows(CompoundWorkflow compoundWorkflow) {
        compoundWorkflow.getOtherCompoundWorkflows().clear();
        compoundWorkflow.setOtherCompoundWorkflows(getOtherCompoundWorkflows(compoundWorkflow, true));
    }

    private List<CompoundWorkflow> getOtherCompoundWorkflows(CompoundWorkflow compoundWorkflow, boolean loadTasks) {
        return compoundWorkflow.isDocumentWorkflow() ? getCompoundWorkflows(compoundWorkflow.getParent(), compoundWorkflow.getNodeRef(), loadTasks)
                : new ArrayList<CompoundWorkflow>();
    }

    @Override
    public List<NodeRef> getChildWorkflowNodeRefs(List<NodeRef> compoundWorkflows) {
        List<NodeRef> workflowNodeRefs = new ArrayList<>();
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (!RepoUtil.isSaved(compoundWorkflow)) {
                continue;
            }
            // FIXME KAAREL: getChildAssocs in a loop. woop-woop.
            List<ChildAssociationRef> workflowAssocs = nodeService.getChildAssocs(compoundWorkflow, RegexQNamePattern.MATCH_ALL, WorkflowCommonModel.Assocs.WORKFLOW);
            for (ChildAssociationRef workflowAssoc : workflowAssocs) {
                workflowNodeRefs.add(workflowAssoc.getChildRef());
            }
        }
        return workflowNodeRefs;
    }

    @Override
    public Map<NodeRef, List<NodeRef>> getChildWorkflowNodeRefsByCompoundWorkflow(List<NodeRef> compoundWorkflows) {
        Map<NodeRef, List<NodeRef>> workflowNodeRefsByCompoundRef = new HashMap<>();
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (!RepoUtil.isSaved(compoundWorkflow)) {
                continue;
            }
            // FIXME KAAREL: getChildAssocs in a loop. woop-woop.
            List<ChildAssociationRef> workflowAssocs = nodeService.getChildAssocs(compoundWorkflow, RegexQNamePattern.MATCH_ALL, WorkflowCommonModel.Assocs.WORKFLOW);
            List<NodeRef> workflowRefs = new ArrayList<>(workflowAssocs.size());
            for (ChildAssociationRef workflowAssoc : workflowAssocs) {
                workflowRefs.add(workflowAssoc.getChildRef());
            }
            workflowNodeRefsByCompoundRef.put(compoundWorkflow, workflowRefs);
        }
        return workflowNodeRefsByCompoundRef;
    }

    @Override
    public CompoundWorkflow getCompoundWorkflowOfType(NodeRef nodeRef, List<QName> types) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        List<ChildAssociationRef> childAssocs = getWorkflows(nodeRef);
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            if (types.contains((nodeService.getType(childAssoc.getChildRef())))) {
                workflowIndex = addWorkflow(compoundWorkflow, false, workflowIndex, childAssoc, true);
            }
        }
        return compoundWorkflow;
    }

    @Override
    public List<CompoundWorkflow> getOtherCompoundWorkflows(CompoundWorkflow compoundWorkflow) {
        return getOtherCompoundWorkflows(compoundWorkflow, true);
    }

    private void getAndAddWorkflows(NodeRef parent, CompoundWorkflow compoundWorkflow, boolean copy, boolean addTasks) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, workflowConstantsBean.getAllWorkflowTypes());
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
        NodeRef workflowNodeRef = childAssoc.getChildRef();
        Workflow workflow = getWorkflow(workflowNodeRef, compoundWorkflow, copy);
        workflow.setIndexInCompoundWorkflow(workflowIndex);
        compoundWorkflow.addWorkflow(workflow);
        if (addTasks) {
            getAndAddTasks(workflowNodeRef, workflow, copy);
        }
        workflowIndex++;
        return workflowIndex;
    }

    private Workflow getWorkflow(NodeRef workflowNodeRef, CompoundWorkflow compoundWorkflow, boolean copy) {
        WmNode workflowNode = getNode(workflowNodeRef, WorkflowCommonModel.Types.WORKFLOW, true, copy);
        return getWorkflow(compoundWorkflow, workflowNode);
    }

    private Workflow getWorkflow(CompoundWorkflow compoundWorkflow, WmNode workflowNode) {
        WorkflowType workflowType = workflowConstantsBean.getWorkflowTypes().get(workflowNode.getType());
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

    @Override
    public CompoundWorkflowType getWorkflowCompoundWorkflowType(NodeRef workflowRef) {
        NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflowRef)).getParentRef();
        return getCompoundWorkflowType(compoundWorkflowRef);
    }

    private void getAndAddTasks(NodeRef parent, Workflow workflow, boolean copy) {
        WorkflowType workflowType = workflowConstantsBean.getWorkflowTypes().get(workflow.getType());
        QName workflowTaskType = workflowType.getTaskType();
        workflow.addTasks(workflowDbService.getWorkflowTasks(parent, workflowConstantsBean.getTaskDataTypeDefaultAspects().get(workflowTaskType),
                workflowConstantsBean.getTaskDataTypeDefaultProps().get(workflowTaskType),
                workflowConstantsBean.getTaskPrefixedQNames(), workflowType, workflow, copy));
        for (Task task : workflow.getTasks()) {
            loadDueDateData(task);
        }
    }

    @Override
    public Task getTask(NodeRef nodeRef, boolean fetchWorkflow) {
        return getTask(nodeRef, fetchWorkflow, false);
    }

    private Task getTask(NodeRef nodeRef, boolean fetchWorkflow, boolean fetchCompoundWorkflow) {
        Workflow workflow = null;
        if (fetchWorkflow) {
            NodeRef parent = workflowDbService.getTaskParentNodeRef(nodeRef);
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(parent).getParentRef();
            CompoundWorkflow compoundWorkflow = fetchCompoundWorkflow ? getCompoundWorkflow(compoundWorkflowRef) : null;
            workflow = getWorkflow(parent, compoundWorkflow, false);
        }
        return getTask(nodeRef, workflow, false);
    }

    @Override
    public Task getTaskWithParents(NodeRef taskNodeRef) {
        NodeRef workflowNodeRef = workflowDbService.getTaskParentNodeRef(taskNodeRef);
        CompoundWorkflow compoundWorkflow = getParentCompoundWorkflow(workflowNodeRef);
        return WorkflowUtil.findTask(compoundWorkflow, taskNodeRef);
    }

    @Override
    public Set<Task> getTasks(NodeRef docRef, Predicate<Task> taskPredicate) {
        return WorkflowUtil.getTasks(new HashSet<Task>(), getCompoundWorkflows(docRef), taskPredicate);
    }

    @Override
    public Set<Task> getTasksInProgress(NodeRef docRef) {
        return WorkflowUtil.getTasks(new HashSet<Task>(), getCompoundWorkflows(docRef, null, true), new Predicate<Task>() {
            @Override
            public boolean eval(Task task) {
                return task.isStatus(Status.IN_PROGRESS);
            }
        });
    }

    private Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        Task task = getTaskWithoutParentAndChildren(nodeRef, workflow, copy);
        loadDueDateData(task);
        return task;
    }

    private void loadDueDateData(Task task) {
        if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK)) {
            // due date extension task
            NodeRef initiatingCompoundWorkflowRef = task.getInitiatingCompoundWorkflowRef();
            if (initiatingCompoundWorkflowRef != null && StringUtils.isBlank(task.getInitiatingCompoundWorkflowTitle())) {
                task.setInitiatingCompoundWorkflowTitle((String) nodeService.getProperty(initiatingCompoundWorkflowRef, WorkflowCommonModel.Props.TITLE));
            }
            return;
        }
        if (!task.isSaved() || !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.TASK_DUE_DATE_EXTENSION_CONTAINER)) {
            return;
        }
        // due date initiating task (assignment or order assignment task)
        List<DueDateHistoryRecord> historyRecords = task.getDueDateHistoryRecords();
        historyRecords.addAll(workflowDbService.getDueDateHistoryRecords(task.getNodeRef()));
    }

    @Override
    public Task getTaskWithoutParentAndChildren(NodeRef nodeRef, Workflow workflow, boolean copy) {
        return workflowDbService.getTask(nodeRef, workflow, copy);
    }

    @Override
    public Map<NodeRef, Task> getTasksWithCompoundWorkflowRef(List<NodeRef> taskRefs) {
        return workflowDbService.getTasksWithCompoundWorkflowRef(taskRefs);
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
    public List<RelatedUrl> getRelatedUrls(NodeRef compoundWorkflowRef) {
        List<RelatedUrl> relatedUrls = new ArrayList<RelatedUrl>();
        List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(compoundWorkflowRef, Collections.singleton(WorkflowCommonModel.Types.RELATED_URL));
        for (ChildAssociationRef childAssocRef : childAssocRefs) {
            relatedUrls.add(getRelatedUrl(childAssocRef.getChildRef()));
        }
        return relatedUrls;
    }

    @Override
    public void saveRelatedUrl(RelatedUrl relatedUrl, NodeRef compoundWorkflowRef) {
        Map<String, Object> strProperties = relatedUrl.getNode().getProperties();
        String userFullName = userService.getUserFullName();
        Date now = new Date();
        strProperties.put(WorkflowCommonModel.Props.URL_MODIFIER_NAME.toString(), userFullName);
        strProperties.put(WorkflowCommonModel.Props.MODIFIED.toString(), now);
        Map<QName, Serializable> properties = RepoUtil.toQNameProperties(strProperties);
        if (RepoUtil.isUnsaved(relatedUrl.getNode())) {
            properties.put(WorkflowCommonModel.Props.URL_CREATOR_NAME, userFullName);
            properties.put(WorkflowCommonModel.Props.CREATED, now);
            nodeService.createNode(compoundWorkflowRef, WorkflowCommonModel.Assocs.RELATED_URL, WorkflowCommonModel.Assocs.RELATED_URL, WorkflowCommonModel.Types.RELATED_URL,
                    properties);
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflowRef, "applog_compoundWorkflow_relatedUrl_added", relatedUrl.getUrl(),
                    relatedUrl.getUrlComment()));
        } else {
            NodeRef relatedUrlRef = relatedUrl.getNodeRef();
            nodeService.addProperties(relatedUrlRef, properties);
            Map<String, Object> originalProps = relatedUrl.getOriginalProps();
            Node compoundWorkflow = generalService.getPrimaryParent(relatedUrlRef);
            for (Map.Entry<String, Object> entry : strProperties.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object originalValue = originalProps.get(key);
                if (!EqualsHelper.nullSafeEquals(value, originalValue)) {
                    logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_relatedUrl_changed",
                            MessageUtil.getPropertyName(QName.createQName(key)), originalValue, value));
                }
            }
        }
    }

    @Override
    public RelatedUrl getRelatedUrl(NodeRef relatedUrlNodeRef) {
        WmNode wmNode = new WmNode(relatedUrlNodeRef, WorkflowCommonModel.Types.RELATED_URL);
        wmNode.getProperties();
        return new RelatedUrl(wmNode);
    }

    @Override
    public void deleteRelatedUrl(NodeRef relatedUrlRef) {
        RelatedUrl relatedUrl = getRelatedUrl(relatedUrlRef);
        Node compoundWorkflow = generalService.getPrimaryParent(relatedUrlRef);
        nodeService.deleteNode(relatedUrl.getNodeRef());
        logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_relatedUrl_deleted",
                relatedUrl.getUrl(),
                relatedUrl.getUrlComment()));

    }

    @Override
    public void loadTaskFilesFromCompoundWorkflows(List<Task> tasks, List<NodeRef> compoundWorkflows) {
        Map<NodeRef, List<NodeRef>> taskFiles = workflowDbService.getCompoundWorkflowsTaskFiles(compoundWorkflows);
        for (Task task : tasks) {
            if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
                loadTaskFiles(task, taskFiles.get(task.getNodeRef()));
            }
        }
    }

    @Override
    public void loadTaskFiles(Task task, List<NodeRef> taskFiles) {
        if (taskFiles != null && !taskFiles.isEmpty()) {
            task.loadFiles(fileService.getFiles(taskFiles));
        }
    }

    @Override
    public List<Comment> getComments(NodeRef compoundWorkflowRef) {
        return workflowDbService.getCompoundWorkflowComments(compoundWorkflowRef.getId());
    }

    @Override
    public void addCompoundWorkflowComment(Comment comment) {
        workflowDbService.addCompoundWorkfowComment(comment);
    }

    @Override
    public void editCompoundWorkflowComment(Long commentId, String commentText) {
        workflowDbService.editCompoundWorkflowComment(commentId, commentText);
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
        WorkflowUtil.setCompoundWorkflowOwnerProperties(userService, getUserNameToSave(), compoundWorkflow);

        if (log.isDebugEnabled()) {
            log.debug("Creating new " + compoundWorkflow);
        }
        checkCompoundWorkflow(compoundWorkflow, true, Status.NEW);
        return compoundWorkflow;
    }

    @Override
    public Workflow addNewWorkflow(CompoundWorkflow compoundWorkflow, QName workflowTypeQName, int index, boolean validateWorkflowIsNew) {
        WorkflowType workflowType = workflowConstantsBean.getWorkflowTypesByWorkflow().get(workflowTypeQName);
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

    @Override
    public void injectWorkflows(CompoundWorkflow compoundWorkflow, int index, List<Workflow> workflowsToInsert) {
        for (Workflow workflow : workflowsToInsert) {
            compoundWorkflow.addWorkflow(workflow, index++);
        }
    }

    @Override
    public void injectTasks(Workflow workflow, int index, List<Task> tasksToInsert) {
        for (Task task : tasksToInsert) {
            workflow.addTask(task, index++);
        }
    }

    // ========================================================================
    // ========================== SAVE TO REPOSITORY ==========================
    // ========================================================================

    @Override
    public CompoundWorkflowDefinition saveCompoundWorkflowDefinition(CompoundWorkflowDefinition compoundWorkflowDefinitionOriginal) {
        CompoundWorkflowDefinition compoundWorkflowDefinition = compoundWorkflowDefinitionOriginal.copy();
        compoundWorkflowDefinitionsCache.remove(compoundWorkflowDefinition.getNodeRef());
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
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        copyInfoMessages(originalReviewTaskDvkInfoMessages, freshCompoundWorkflow);
        return freshCompoundWorkflow;
    }

    public void saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        proccessPreSave(queue, compoundWorkflow);

        boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
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
        if (!wasSaved) {
            compoundWorkflow.setCreatedDateTime(queue.getNow());
        }
        boolean changed = createOrUpdate(queue, compoundWorkflow, compoundWorkflow.getParent(), assocType);

        // Remove workflows
        List<String> removedWorkflowsStr = new ArrayList<String>();
        List<Task> removedTasks = new ArrayList<Task>();
        for (Workflow removedWorkflow : compoundWorkflow.getRemovedWorkflows()) {
            if (removedWorkflow.isSaved()) {
                NodeRef removedWorkflowNodeRef = removedWorkflow.getNodeRef();
                checkWorkflow(getWorkflow(removedWorkflowNodeRef, compoundWorkflow, false), Status.NEW);
                nodeService.deleteNode(removedWorkflowNodeRef);
                changed = true;
                setChildAssocIndexes = true;
                removedWorkflowsStr.add(MessageUtil.getMessage(removedWorkflow.getType().getLocalName()));
                removedTasks.addAll(removedWorkflow.getTasks());
            }
        }
        if (!removedWorkflowsStr.isEmpty()) {
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_workflow_deleted",
                    StringUtils.join(removedWorkflowsStr, ", ")));
            logRemovedTasks(compoundWorkflow.getNodeRef(), removedTasks);
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
        List<String> addedWorkflows = new ArrayList<String>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (!workflow.isSaved()) {
                addedWorkflows.add(MessageUtil.getMessage(workflow.getType().getLocalName()));
            }
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
        if (!addedWorkflows.isEmpty()) {
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_workflow_added",
                    StringUtils.join(addedWorkflows, ", ")));
        }

        if (!wasSaved) {
            if (compoundWorkflow.getNewAssocs() != null) {
                NodeRef compoundWorkflowRef = compoundWorkflow.getNodeRef();
                for (NodeRef docRef : compoundWorkflow.getNewAssocs()) {
                    getDocumentAssociationsService().createWorkflowAssoc(docRef, compoundWorkflowRef, false, true);
                }
                compoundWorkflow.getNewAssocs().clear();
            }
            if (compoundWorkflow.getNewRelatedUrls() != null) {
                NodeRef compoundWorkflowRef = compoundWorkflow.getNodeRef();
                for (RelatedUrl relatedUrl : compoundWorkflow.getNewRelatedUrls()) {
                    saveRelatedUrl(relatedUrl, compoundWorkflowRef);
                }
                compoundWorkflow.getNewRelatedUrls().clear();
            }
            if (compoundWorkflow.getNewComments() != null) {
                String compoundWorkflowId = compoundWorkflow.getNodeRef().getId();
                for (Comment comment : compoundWorkflow.getNewComments()) {
                    comment.setCompoundWorkflowId(compoundWorkflowId);
                    addCompoundWorkflowComment(comment);
                }
                compoundWorkflow.getNewComments().clear();
            }
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
        if (compoundWorkflow.isDocumentWorkflow() && CompoundWorkflow.class.equals(compoundWorkflow.getClass())) { // compound worflow tied to document (not compoundWorkflowDef)
            if (!StringUtils.equals(compoundWorkflow.getOwnerId(), previousOwnerId)) {
                // doesn't matter what status cWF has, just add the privileges
                NodeRef docRef = nodeService.getPrimaryParent(compoundWorkflow.getNodeRef()).getParentRef();
                privilegeService.setPermissions(docRef, compoundWorkflow.getOwnerId(), Privilege.VIEW_DOCUMENT_FILES);
            }
        }
        if (isCompoundWorkflow(compoundWorkflow) && !wasSaved) {
            if (compoundWorkflow.isDocumentWorkflow()) {
                documentLogService.addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
            }
            if (compoundWorkflow.isCaseFileWorkflow()) {
                caseFileLogService.addCaseFileLog(compoundWorkflow.getParent(), "casefile_log_status_workflow");
            }
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_created"));
        }
        return changed;
    }

    private boolean saveWorkflow(WorkflowEventQueue queue, Workflow workflow) {
        boolean changed = createOrUpdate(queue, workflow, workflow.getParent().getNodeRef(), WorkflowCommonModel.Assocs.WORKFLOW);

        List<Task> removedTasks = workflow.getRemovedTasks();
        List<NodeRef> savedTasksToDelete = new ArrayList<>(removedTasks.size());
        for (Task removedTask : removedTasks) {
            NodeRef removedTaskNodeRef = removedTask.getNodeRef();
            if (removedTask.isSaved()) {
                checkTask(getTask(removedTaskNodeRef, workflow, false), Status.NEW);
                savedTasksToDelete.add(removedTaskNodeRef);
                changed = true;
            }
        }
        workflowDbService.deleteTasks(savedTasksToDelete);
        logRemovedTasks(workflow.getParent().getNodeRef(), removedTasks);
        workflow.getRemovedTasks().clear();
        saveTasks(workflow);
        return changed;
    }

    private void saveTasks(Workflow workflow) {
        int batchSize = 100;

        NodeRef parentRef = workflow.getNodeRef();
        log.debug("Starting to save " + workflow.getTasks().size() + " tasks");
        NodeRef linkedReviewTaskSpace = getLinkedReviewTaskSpace();
        List<TaskUpdateInfo> tasksToCreate = new ArrayList<>();
        List<TaskUpdateInfo> taskToUpdate = new ArrayList<>();
        Set<String> createTaskUsedFieldNames = new LinkedHashSet<>();
        Set<String> updateTaskUsedFieldNames = new LinkedHashSet<>();

        int index = 0;
        for (Task task : workflow.getTasks()) {
            task.setTaskIndexInWorkflow(index);
            Map<QName, Serializable> propsToSave = prepareTaskForSaving(task, parentRef);

            if (task.isUnsaved()) {
                NodeRef nodeRef = new NodeRef(parentRef.getStoreRef(), GUID.generate());
                task.getNode().updateNodeRef(nodeRef);
                TaskUpdateInfo info = workflowDbService.verifyTaskAndGetUpdateInfoOnCreate(task, (linkedReviewTaskSpace.equals(parentRef) ? null : parentRef));
                info.setPostSaveProperties(propsToSave);

                tasksToCreate.add(info);
                createTaskUsedFieldNames.addAll(info.getUnmodifiableFieldNames());

                if (tasksToCreate.size() >= batchSize) {
                    processCreateTaskBatch(parentRef, tasksToCreate, createTaskUsedFieldNames);
                }
            } else {
                verifyRequiredStatusOnUpdate(task, propsToSave);
                TaskUpdateInfo info = workflowDbService.verifyTaskAndGetUpdateInfoOnUpdate(task, (linkedReviewTaskSpace.equals(parentRef) ? null : parentRef), propsToSave);
                info.setPostSaveProperties(propsToSave);

                taskToUpdate.add(info);
                updateTaskUsedFieldNames.addAll(info.getUnmodifiableFieldNames());

                if (taskToUpdate.size() >= batchSize) {
                    processUpdateTaskBatch(parentRef, taskToUpdate, updateTaskUsedFieldNames);
                }
            }
            index++;
        }

        // Eat the leftovers
        processUpdateTaskBatch(parentRef, taskToUpdate, updateTaskUsedFieldNames);
        processCreateTaskBatch(parentRef, tasksToCreate, createTaskUsedFieldNames);
    }

    private void processCreateTaskBatch(NodeRef parentRef, List<TaskUpdateInfo> tasksToCreate, Set<String> createTaskUsedFieldNames) {
        if (tasksToCreate == null || tasksToCreate.isEmpty()) {
            return;
        }
        workflowDbService.createTaskEntries(tasksToCreate, createTaskUsedFieldNames);
        processTaskBatchPostDb(tasksToCreate, parentRef);
        tasksToCreate.clear();
        createTaskUsedFieldNames.clear();
    }

    private void processUpdateTaskBatch(NodeRef parentRef, List<TaskUpdateInfo> tasksToUpdate, Set<String> updateTaskUsedFieldNames) {
        if (tasksToUpdate == null || tasksToUpdate.isEmpty()) {
            return;
        }
        workflowDbService.updateTaskEntries(tasksToUpdate, updateTaskUsedFieldNames);
        processTaskBatchPostDb(tasksToUpdate, parentRef);
        tasksToUpdate.clear();
        updateTaskUsedFieldNames.clear();
    }

    private void processTaskBatchPostDb(List<TaskUpdateInfo> updateInfos, NodeRef parentNodeRef) {
        for (TaskUpdateInfo info : updateInfos) {
            info.applyPostSaveProperties();
            Task task = info.getTask();
            NodeRef taskRef = task.getNodeRef();
            List<NodeRef> removedFiles = task.getRemovedFiles();
            if (removedFiles != null && !removedFiles.isEmpty()) {
                for (NodeRef removedFileRef : removedFiles) {
                    nodeService.deleteNode(removedFileRef);
                }
                workflowDbService.removeTaskFiles(taskRef, removedFiles);
                removedFiles.clear();
            }
            List<String> existingDisplayNames = new ArrayList<>();
            List<NodeRef> newFileRefs = new ArrayList<>();
            final List<Object> files = task.getFiles();

            for (Object fileObj : files) {
                if (!(fileObj instanceof FileWithContentType)) {
                    // existing file, no update needed
                    continue;
                }
                FileWithContentType file = (FileWithContentType) fileObj;
                String originalDisplayName = FilenameUtil.getDiplayNameFromName(file.fileName);
                Pair<String, String> filenames = FilenameUtil.getTaskFilenameFromDisplayname(task, existingDisplayNames, originalDisplayName, generalService,
                        workflowDbService);
                String fileDisplayName = filenames.getSecond();
                // Files are actually saved under workflow.
                NodeRef fileRef = fileService.addFileToTask(filenames.getFirst(), fileDisplayName, parentNodeRef, file.file, file.contentType);
                newFileRefs.add(fileRef);
                existingDisplayNames.add(fileDisplayName);
            }
            workflowDbService.createTaskFileEntriesFromNodeRefs(taskRef, newFileRefs);
        }
    }

    private void logRemovedTasks(NodeRef compoundWorkflowNodeRef, List<Task> removedTasks) {
        List<String> removedTasksStr = new ArrayList<String>();
        for (Task removedTask : removedTasks) {
            if (removedTask.isSaved()) {
                removedTasksStr.add(MessageUtil.getTypeName(removedTask.getType()) + ", " + removedTask.getOwnerName() + ", " + removedTask.getDueDateStr());
            }
        }
        if (!removedTasksStr.isEmpty()) {
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflowNodeRef, "applog_compoundWorkflow_task_deleted",
                    StringUtils.join(removedTasksStr, "; ")));
        }
    }

    @Override
    public CompoundWorkflow delegate(Task assignmentTaskOriginal) {
        CompoundWorkflow compoundWorkflow = preprocessAndCopyCompoundWorkflow(assignmentTaskOriginal);
        WorkflowEventQueue queue = getNewEventQueue();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (WorkflowUtil.isGeneratedByDelegation(workflow)) {
                setStatus(queue, workflow, Status.IN_PROGRESS);
            }
        }
        return saveCompoundWorkflow(compoundWorkflow, queue);
    }

    private CompoundWorkflow preprocessAndCopyCompoundWorkflow(Task assignmentTaskOriginal) {
        Workflow workflowOriginal = assignmentTaskOriginal.getParent();
        CompoundWorkflow cWorkflowOriginal = workflowOriginal.getParent();
        // assuming that originalWFIndex doesn't change after removing or saving
        int originalWFIndex = cWorkflowOriginal.getWorkflows().indexOf(workflowOriginal);
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

        WorkflowUtil.removeEmptyTasks(cWorkflowCopy);
        // also remove empty workflows that could be created when information or opinion tasks are added during delegating assignment task
        WorkflowUtil.removeEmptyWorkflowsGeneratedByDelegation(cWorkflowCopy);

        for (Workflow workflow : cWorkflowWorkflowsCopy) {
            for (Task task : workflow.getTasks()) {
                if (WorkflowUtil.isGeneratedByDelegation(task)) {
                    Date dueDate = task.getDueDate();
                    if (dueDate != null) {
                        dueDate.setHours(23);
                        dueDate.setMinutes(59);
                    }
                }
            }
        }

        return cWorkflowCopy;
    }

    @Override
    public NodeRef importLinkedReviewTask(LinkedReviewTaskType taskToImport, String dvkId) {
        if (taskToImport == null) {
            return null;
        }
        String originalNoderefId = taskToImport.getOriginalNoderefId();
        NodeRef existingTaskRef = BeanHelper.getDocumentSearchService().searchLinkedReviewTaskByOriginalNoderefId(originalNoderefId);
        Task task;
        if (existingTaskRef == null) {
            WorkflowType workflowType = workflowConstantsBean.getWorkflowTypesByTask().get(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK);
            WmNode taskNode = getTaskTemplateByType(workflowType.getTaskType());
            task = Task.create(workflowType.getTaskClass(), taskNode, null, workflowType.getTaskOutcomes());
        } else {
            task = getTask(existingTaskRef, false);
            int existingDvkId = Integer.MIN_VALUE;
            try {
                existingDvkId = Integer.parseInt(task.getReceivedDvkId());
            } catch (NumberFormatException e) {
                // use Integer.MIN_VALUE
            }
            int newDvkId = Integer.MIN_VALUE;
            try {
                newDvkId = Integer.parseInt(dvkId);
            } catch (NumberFormatException e) {
                // use Integer.MIN_VALUE
            }
            if (existingDvkId > newDvkId) {
                // Existing version is newer than the one being imported.
                // Return nodeRef to indicate that import succeeded,
                // although no data is imported.
                return task.getNodeRef();
            }
        }

        task.setCreatorName(taskToImport.getCreatorName());
        task.setCreatorId(taskToImport.getCreatorId());
        task.setStartedDateTime(XmlUtil.getDate(taskToImport.getStartedDateTime()));
        task.setOwnerId(taskToImport.getOwnerId());
        task.setOwnerName(taskToImport.getOwnerName());
        task.setDueDate(XmlUtil.getDate(taskToImport.getDueDate()));
        task.setStatus(taskToImport.getStatus());
        task.setCompoundWorkflowTitle(taskToImport.getCompoundWorkflowTitle());
        task.setResolution(taskToImport.getTaskResolution());
        task.setCreatorInstitutionName(taskToImport.getCreatorInstitutionName());
        task.setCreatorInstitutionCode(taskToImport.getCreatorInstitutionCode());
        task.setReceivedDvkId(dvkId);
        task.setOriginalNoderefId(originalNoderefId);
        task.setOriginalTaskObjectUrl(taskToImport.getOriginalTaskObjectUrl());
        task.setOutcome(taskToImport.getOutcome(), 0);
        task.setComment(taskToImport.getComment());
        task.setCompletedDateTime(XmlUtil.getDate(taskToImport.getCompletedDateTime()));
        task.setStoppedDateTime(XmlUtil.getDate(taskToImport.getStoppedDateTime()));

        boolean isUnsaved = task.isUnsaved();
        createOrUpdateTask(task, getLinkedReviewTaskSpace());
        if (isUnsaved) {
            logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, task.getNodeRef(), "applog_task_assigned", task.getOwnerName(),
                    MessageUtil.getTypeName(task.getType())));
        } else if (task.isStatus(Status.FINISHED, Status.UNFINISHED)) {
            logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, task.getNodeRef(), "applog_task_done",
                    MessageUtil.getTypeName(task.getType()), task.getOutcome()));
        }
        return task.getNodeRef();
    }

    protected NodeRef getLinkedReviewTaskSpace() {
        return generalService.getNodeRef(WorkflowSpecificModel.Repo.LINKED_REVIEW_TASKS_SPACE);
    }

    @Override
    public NodeRef markLinkedReviewTaskDeleted(DeleteLinkedReviewTaskType deletedTask) {
        NodeRef existingTaskRef = BeanHelper.getDocumentSearchService().searchLinkedReviewTaskByOriginalNoderefId(deletedTask.getOriginalNoderefId());
        if (existingTaskRef != null) {
            Task task = getTask(existingTaskRef, false);
            task.setStatus(Status.DELETED.getName());
            createOrUpdateTask(task, null);
            BeanHelper.getLogService().addLogEntry(
                    LogEntry.create(LogObject.TASK, BeanHelper.getUserService(), existingTaskRef, "applog_task_review_delete_received", task.getOwnerName(),
                            MessageUtil.getTypeName(task.getType())));
        }
        return existingTaskRef;
    }

    @Override
    public int getCompoundWorkflowDocumentCount(NodeRef compoundWorkflowRef) {
        return getCompoundWorkflowDocumentRefs(compoundWorkflowRef).size();
    }

    @Override
    public List<Document> getCompoundWorkflowDocuments(NodeRef compoundWorkflowRef) {
        List<NodeRef> docRefs = getCompoundWorkflowDocumentRefs(compoundWorkflowRef);
        List<Document> documents = new ArrayList<Document>(docRefs.size());
        for (NodeRef docRef : docRefs) {
            documents.add(new Document(docRef));
        }
        return documents;

    }

    @Override
    public List<NodeRef> getCompoundWorkflowDocumentRefs(NodeRef compoundWorkflowRef) {
        List<AssociationRef> assocRefs = nodeService.getSourceAssocs(compoundWorkflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        List<NodeRef> docRefs = new ArrayList<NodeRef>(assocRefs.size());
        for (AssociationRef assocRef : assocRefs) {
            NodeRef docRef = assocRef.getSourceRef();
            if (nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                docRefs.add(docRef);
            }
        }
        return docRefs;
    }

    @Override
    public List<NodeRef> getCompoundWorkflowSigningDocumentRefs(NodeRef compoundWorkflowRef) {
        List<NodeRef> compoundWorkflowDocumentRefs = getCompoundWorkflowDocumentRefs(compoundWorkflowRef);
        List<NodeRef> compoundWorkflowSigningDocumentRefs = new ArrayList<NodeRef>(compoundWorkflowDocumentRefs.size());
        @SuppressWarnings("unchecked")
        List<String> documentsToSign = RepoUtil.getNodeRefIds((List<NodeRef>) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN));
        for (NodeRef docRef : compoundWorkflowDocumentRefs) {
            if (documentsToSign.contains(docRef.getId())) {
                compoundWorkflowSigningDocumentRefs.add(docRef);
            }
        }
        return compoundWorkflowSigningDocumentRefs;
    }

    @Override
    public Map<NodeRef, List<File>> getCompoundWorkflowSigningFiles(NodeRef compoundWorkflowRef) {
        Map<NodeRef, List<File>> activeFiles = new HashMap<NodeRef, List<File>>();
        List<String> documentsToSign = RepoUtil.getNodeRefIds(getCompoundWorkflowSigningDocumentRefs(compoundWorkflowRef));
        for (NodeRef docRef : getCompoundWorkflowDocumentRefs(compoundWorkflowRef)) {
            if (documentsToSign.contains(docRef.getId())) {
                List<File> documentActiveFiles = getFileService().getAllActiveFiles(docRef);
                activeFiles.put(docRef, documentActiveFiles);
            }
        }
        return activeFiles;
    }

    @Override
    public NodeRef getCompoundWorkflowMainDocumentRef(NodeRef compoundWorkflowRef) {
        return (NodeRef) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.MAIN_DOCUMENT);
    }

    @Override
    public void removeDeletedDocumentFromCompoundWorkflows(NodeRef docRef) {
        List<AssociationRef> assocRefs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        String documentToDeleteRefId = docRef.getId();
        for (AssociationRef assocRef : assocRefs) {
            NodeRef compoundWorkflowRef = assocRef.getTargetRef();
            Map<QName, Serializable> compoundWorkflowProps = nodeService.getProperties(compoundWorkflowRef);
            Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
            NodeRef mainDocRef = (NodeRef) compoundWorkflowProps.get(WorkflowCommonModel.Props.MAIN_DOCUMENT);
            if (mainDocRef != null && mainDocRef.getId().equals(documentToDeleteRefId)) {
                propsToAdd.put(WorkflowCommonModel.Props.MAIN_DOCUMENT, null);
            }
            @SuppressWarnings("unchecked")
            List<NodeRef> documentsToSign = (List<NodeRef>) compoundWorkflowProps.get(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN);
            if (documentsToSign != null) {
                for (Iterator<NodeRef> i = documentsToSign.iterator(); i.hasNext();) {
                    NodeRef docToSign = i.next();
                    if (documentToDeleteRefId.equals(docToSign.getId())) {
                        i.remove();
                        propsToAdd.put(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, (Serializable) documentsToSign);
                    }
                }
            }
            if (!propsToAdd.isEmpty()) {
                nodeService.addProperties(compoundWorkflowRef, propsToAdd);
            }
        }
    }

    @Override
    public List<Task> getTasks4DelegationHistory(Node delegatableTask) {
        NodeRef taskParentNodeRef = workflowDbService.getTaskParentNodeRef(delegatableTask.getNodeRef());
        NodeRef docRef = generalService.getAncestorNodeRefWithType(taskParentNodeRef, DocumentCommonModel.Types.DOCUMENT, true);
        NodeRef workflowRef = null;
        boolean orderAssignmentTask = delegatableTask.getType().equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        if (orderAssignmentTask) {
            workflowRef = taskParentNodeRef;
        }
        List<Task> assignmentTasks = new ArrayList<Task>();
        List<CompoundWorkflow> compoundWorkflows;
        List<QName> requiredWorkflowTypes = Arrays.asList(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW);
        if (docRef != null) {
            compoundWorkflows = getCompoundWorkflowsOfType(docRef, requiredWorkflowTypes);
        } else {
            NodeRef compoundWorkflowRef = generalService.getAncestorNodeRefWithType(taskParentNodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false);
            compoundWorkflows = Arrays.asList(getCompoundWorkflowOfType(compoundWorkflowRef, requiredWorkflowTypes));
        }
        cWfLoop: for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
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

    private boolean saveTask(Task task) {
        NodeRef parentNodeRef = task.getWorkflowNodeRef();
        boolean changed = createOrUpdateTask(task, parentNodeRef);
        NodeRef taskRef = task.getNodeRef();
        List<NodeRef> removedFiles = task.getRemovedFiles();
        if (removedFiles != null && !removedFiles.isEmpty()) {
            for (NodeRef removedFileRef : removedFiles) {
                nodeService.deleteNode(removedFileRef);
            }
            workflowDbService.removeTaskFiles(taskRef, removedFiles);
            removedFiles.clear();
        }
        List<String> existingDisplayNames = new ArrayList<>();
        List<NodeRef> newFileRefs = new ArrayList<>();
        final List<Object> files = task.getFiles();

        for (Object fileObj : files) {
            if (!(fileObj instanceof FileWithContentType)) {
                // existing file, no update needed
                continue;
            }
            FileWithContentType file = (FileWithContentType) fileObj;
            String originalDisplayName = FilenameUtil.getDiplayNameFromName(file.fileName);
            Pair<String, String> filenames = FilenameUtil.getTaskFilenameFromDisplayname(task, existingDisplayNames, originalDisplayName, generalService,
                    workflowDbService);
            String fileDisplayName = filenames.getSecond();
            // Files are actually saved under workflow.
            NodeRef fileRef = fileService.addFileToTask(filenames.getFirst(), fileDisplayName, parentNodeRef, file.file, file.contentType);
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

        saveTask(task);
        if (log.isDebugEnabled()) {
            log.debug("Saved " + task);
        }
    }

    @Override
    public void updateTaskSearchableProperties(NodeRef taskRef) {
        if (!nodeService.hasAspect(taskRef, WorkflowSpecificModel.Aspects.SEARCHABLE)) {
            return;
        }
        NodeRef compoundWorkflowRef = generalService.getAncestorNodeRefWithType(taskRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        if (compoundWorkflowRef == null) {
            return;
        }
        Map<QName, Serializable> taskSearchableProps = WorkflowUtil.getTaskSearchableProps(nodeService.getProperties(compoundWorkflowRef));
        workflowDbService.updateTaskProperties(taskRef, taskSearchableProps);
    }

    private CompoundWorkflow getParentCompoundWorkflow(NodeRef workflowNodeRef) {
        NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflowNodeRef).getParentRef();
        return getCompoundWorkflow(compoundWorkflowRef);
    }

    private void finishInProgressTask(Task taskOriginal, int outcomeIndex, boolean requireCurrentUser) throws WorkflowChangedException {
        if (outcomeIndex < 0 || outcomeIndex >= taskOriginal.getOutcomes()) {
            throw new RuntimeException("outcomeIndex '" + outcomeIndex + "' out of bounds for " + taskOriginal);
        }
        // check status==IN_PROGRESS, owner==currentUser
        if (requireCurrentUser) {
            requireInProgressCurrentUser(taskOriginal);
        }
        // requireStatus(taskOriginal.getParent(), Status.IN_PROGRESS); // XXX this is not needed??
        requireStatusUnchanged(taskOriginal);

        // operate on compoundWorkflow that was fetched fresh from repo
        CompoundWorkflow compoundWorkflow = getParentCompoundWorkflow(taskOriginal.getWorkflowNodeRef());
        // insert (possibly modified) task into compoundWorkflow from repo
        Task task = replaceTask(taskOriginal, compoundWorkflow);

        WorkflowEventQueue queue = getNewEventQueue();

        queue.setParameter(WorkflowQueueParameter.TRIGGERED_BY_FINISHING_EXTERNAL_REVIEW_TASK_ON_CURRENT_SYSTEM,
                new Boolean(isRecievedExternalReviewTask(task)));
        queue.setParameter(WorkflowQueueParameter.INITIATING_GROUP_TASK, task.isType(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK) ? task.getNodeRef() : null);
        if (requireCurrentUser && BeanHelper.getSubstitutionBean().getSubstitutionInfo().isSubstituting()) {
            String user = BeanHelper.getUserService().getUserFullName();
            task.setOwnerSubstituteName(user);
        }
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
    public void finishInProgressTask(Task taskOriginal, int outcomeIndex) throws WorkflowChangedException {
        finishInProgressTask(taskOriginal, outcomeIndex, true);
    }

    @Override
    public boolean isRecievedExternalReviewTask(Task task) {
        boolean internalTesting = applicationConstantsBean.isInternalTesting();
        return task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                && ((internalTesting && !Boolean.TRUE.equals(nodeService.getProperty(task.getParent().getParent().getParent(),
                        DocumentCommonModel.Props.NOT_EDITABLE)))
                || (!internalTesting && !isResponsibleCurrenInstitution(task)));
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

    @Override
    public boolean isDocumentWorkflow(NodeRef compoundWorkflowRef) {
        return CompoundWorkflowType.DOCUMENT_WORKFLOW.equals(getCompoundWorkflowType(compoundWorkflowRef));
    }

    @Override
    public boolean isIndependentWorkflow(NodeRef compoundWorkflowRef) {
        return CompoundWorkflowType.INDEPENDENT_WORKFLOW.equals(getCompoundWorkflowType(compoundWorkflowRef));
    }

    @Override
    public CompoundWorkflowType getCompoundWorkflowType(NodeRef compoundWorkflowRef) {
        String typeStr = (String) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.TYPE);
        CompoundWorkflowType type = StringUtils.isNotBlank(typeStr) ? CompoundWorkflowType.valueOf(typeStr) : null;
        return type;
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

        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
    }

    @Override
    public Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow) {
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
    public void deleteCompoundWorkflow(NodeRef nodeRef, boolean validateStatuses) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        if (validateStatuses) {
            checkCompoundWorkflow(compoundWorkflow, Status.NEW, Status.FINISHED);
        }
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + compoundWorkflow);
        }
        if (workflowConstantsBean.isReviewToOtherOrgEnabled()) {
            for (Task task : getNotNewOtherOrgReviewTasks(compoundWorkflow)) {
                dvkService.sendReviewTaskDeletingNotification(task);
            }
        }
        List<NodeRef> docRefs = null;
        if (validateStatuses) {
            docRefs = getCompoundWorkflowDocsSearchPropsRefs(compoundWorkflow);
        }
        nodeService.deleteNode(nodeRef);

        NodeRef docRef = compoundWorkflow.getParent();
        if (validateStatuses) {
            updateCompWorkflowDocsSearchProps(docRefs);
        }
        if (compoundWorkflow.isIndependentWorkflow()) {
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, nodeRef,
                    "applog_compoundWorkflow_deleted", compoundWorkflow.getTitle()));
        }
        if (compoundWorkflow.isCaseFileWorkflow()) {
            BeanHelper.getLogService().addLogEntry(
                    LogEntry.createLoc(LogObject.CASE_FILE, BeanHelper.getUserService().getCurrentUserName(), BeanHelper.getUserService().getUserFullName(),
                            compoundWorkflow.getParent(),
                            MessageUtil.getMessage("casefile_log_status_workflow_deleted", compoundWorkflow.getTitle())));
        }
        if (compoundWorkflow.isDocumentWorkflow()) {
            documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_workflow_deleted", compoundWorkflow.getTitle()));
        }
    }

    private List<Task> getNotNewOtherOrgReviewTasks(CompoundWorkflow compoundWorkflow) {
        List<Task> tasks = new ArrayList<Task>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.isType(WorkflowSpecificModel.Types.REVIEW_TASK) && !task.isStatus(Status.NEW)) {
                    String institutionCode = task.getInstitutionCode();
                    String creatorInstitutionCode = task.getCreatorInstitutionCode();
                    if (StringUtils.isNotBlank(institutionCode) && StringUtils.isNotBlank(creatorInstitutionCode) && !institutionCode.equals(creatorInstitutionCode)) {
                        tasks.add(task);
                    }
                }
            }
        }
        return tasks;
    }

    @Override
    public CompoundWorkflow startCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        if (compoundWorkflow.isDocumentWorkflow()) {
            ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        }
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
        List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        copyInfoMessages(originalReviewTaskDvkInfoMessages, freshCompoundWorkflow);
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
        copyInfoMessages(compoundWorkflow.getReviewTaskDvkInfoMessages(), freshCompoundWorkflow);
        return freshCompoundWorkflow;
    }

    private void copyInfoMessages(List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages, CompoundWorkflow freshCompoundWorkflow) {
        List<Pair<String, Object[]>> reviewTaskDvkInfoMessages = freshCompoundWorkflow.getReviewTaskDvkInfoMessages();
        reviewTaskDvkInfoMessages.clear();
        reviewTaskDvkInfoMessages.addAll(originalReviewTaskDvkInfoMessages);
    }

    @Override
    public CompoundWorkflow reopenCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        checkCompoundWorkflow(compoundWorkflow, Status.FINISHED);
        WorkflowEventQueue queue = getNewEventQueue();
        setStatus(queue, compoundWorkflow, Status.STOPPED);
        saveCompoundWorkflow(queue, compoundWorkflow, null);

        List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.STOPPED);
        handleEvents(queue);
        copyInfoMessages(originalReviewTaskDvkInfoMessages, freshCompoundWorkflow);
        return freshCompoundWorkflow;
    }

    @Override
    public void finishTasksByRegisteringReplyLetter(NodeRef docRef, String comment) {
        boolean unfinishAllTasks = !hasTaskOfType(docRef, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        WorkflowEventQueue queue = getNewEventQueue();
        queue.getParameters().put(WorkflowQueueParameter.TRIGGERED_BY_DOC_REGISTRATION, Boolean.TRUE);
        String userNameToCheck = getUserNameToCheck();

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
                            && userNameToCheck.equals(task.getOwnerId())) {
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
                deleteCompoundWorkflow(compoundWorkflowRef, true);
            } else {
                checkCompoundWorkflow(compoundWorkflow);
                checkActiveResponsibleAssignmentTasks(compoundWorkflow);
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
                    if (task.isStatus(Status.NEW, Status.IN_PROGRESS) && Task.Action.FINISH != task.getAction()) {
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
                deleteCompoundWorkflow(compoundWorkflow.getNodeRef(), true);
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
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        return freshCompoundWorkflow;
    }

    @Override
    public CompoundWorkflow stopCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
        WorkflowEventQueue queue = getNewEventQueue();
        queue.setParameter(WorkflowQueueParameter.WORKFLOW_STOPPED_MANUALLY, Boolean.TRUE);
        proccessPreSave(queue, compoundWorkflow);

        stopCompoundWorkflow(queue, compoundWorkflow, true);
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        List<Pair<String, Object[]>> reviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef, reviewTaskDvkInfoMessages);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private void stopCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow, boolean save) {
        // if not saving, this method is called from finishing task process,
        // which has modified compound workflow so id doesn't have any IN_PROGRESS workflows
        // and we cannot perform usual status check
        if ((!save && compoundWorkflow.isStatus(Status.FINISHED)) || (save && checkCompoundWorkflow(compoundWorkflow, Status.IN_PROGRESS, Status.FINISHED) == Status.FINISHED)) {
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
            if (save) {
                saveCompoundWorkflow(queue, compoundWorkflow, null);
                if (log.isDebugEnabled()) {
                    log.debug("Stopped " + compoundWorkflow);
                }
            }
        }
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
            queue.setParameter(WorkflowQueueParameter.WORKFLOW_CONTINUED, true);
            stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
            boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
            if (log.isDebugEnabled()) {
                log.debug("Continued " + compoundWorkflow);
            }
        }

        // also check repo status
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        List<Pair<String, Object[]>> reviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef, reviewTaskDvkInfoMessages);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private void ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow) {
        if (hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(cWorkflow, true)) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "workflow_compound_start_error_twoInprogressOrStoppedWorkflows");
        }
    }

    @Override
    public boolean hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow, boolean checkCurrentWorkflow) {
        boolean hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows = false;
        if (!checkCurrentWorkflow || cWorkflow.getWorkflows().size() > 1) {
            for (CompoundWorkflow otherCWf : getOtherCompoundWorkflows(cWorkflow)) {
                if (otherCWf.isStatus(Status.IN_PROGRESS, Status.STOPPED) && otherCWf.getWorkflows().size() > 1) {
                    hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows = true;
                    break;
                }
            }
        }
        return hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows;
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

    @Override
    public CompoundWorkflow copyCompoundWorkflowInMemory(CompoundWorkflow compoundWorkflowOriginal) {
        return compoundWorkflowOriginal.copy();
    }

    @Override
    public Map<String, Object> getTaskChangedProperties(Task task) {
        return RepoUtil.toStringProperties(RepoUtil.getPropertiesIgnoringSystem(task.getChangedProperties(),
                BeanHelper.getDictionaryService()));
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

    private void reset(BaseWorkflowObject object) {
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
                    || key.equals(WorkflowCommonModel.Props.OWNER_JOB_TITLE) || key.equals(WorkflowCommonModel.Props.TYPE)
                    || key.equals(WorkflowSpecificModel.Props.CATEGORY) || key.equals(WorkflowSpecificModel.Props.SIGNING_TYPE)
                    || (key.equals(WorkflowCommonModel.Props.TITLE) && workflowConstantsBean.isWorkflowTitleEnabled())) {
                // keep value
            } else {
                prop.setValue(null);
            }
        }
    }

    private void checkActiveResponsibleAssignmentTasks(CompoundWorkflow compoundWorkflow) {
        int count = getConnectedActiveResponsibleTasksCount(compoundWorkflow, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);
        if (count > 1) {
            log.debug("Compound workflow and connected workflows have " + count + " active responsible tasks.");
            throw new WorkflowActiveResponsibleTaskException();
        }
    }

    @Override
    public int getConnectedActiveResponsibleTasksCount(CompoundWorkflow compoundWorkflow, QName workflowType) {
        return getConnectedActiveResponsibleTasksCount(compoundWorkflow, workflowType, false, null);
    }

    @Override
    public int getConnectedActiveResponsibleTasksCount(CompoundWorkflow compoundWorkflow, QName workflowType, boolean allowFinished, NodeRef compoundWorkflowToSkip) {
        List<CompoundWorkflow> compoundWorkflows;
        if (compoundWorkflow.isDocumentWorkflow()) {
            compoundWorkflows = getCompoundWorkflowsOfType(compoundWorkflow.getParent(), Arrays.asList(workflowType));
        } else {
            if (compoundWorkflow.isSaved()) {
                // TODO: could be optimized
                compoundWorkflows = Arrays.asList(getCompoundWorkflowOfType(compoundWorkflow.getNodeRef(), Arrays.asList(workflowType)));
            } else {
                compoundWorkflows = new ArrayList<CompoundWorkflow>();
            }
        }
        return getActiveresponsibleTasks(compoundWorkflows, allowFinished, compoundWorkflowToSkip);
    }

    private int getActiveresponsibleTasks(List<CompoundWorkflow> compoundWorkflows, boolean allowFinished, NodeRef compoundWorkflowToSkip) {
        return getConnectedActiveResponsibleTasksCount(compoundWorkflows, allowFinished, compoundWorkflowToSkip);
    }

    @Override
    public int getConnectedActiveResponsibleTasksCount(List<CompoundWorkflow> compoundWorkflows, boolean allowFinished, NodeRef compoundWorkflowToSkip) {
        Status[] allowedStatuses = allowFinished ? new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED } :
                new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED };
        int counter = 0;
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (!compoundWorkflow.isStatus(allowedStatuses) || compoundWorkflow.getNodeRef().equals(compoundWorkflowToSkip)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isStatus(allowedStatuses)) {
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
    public boolean hasInProgressOtherUserOrderAssignmentTasks(NodeRef parentRef) {
        List<NodeRef> compoundWorkflowNodeRefs = getCompoundWorkflowNodeRefs(parentRef);
        if (compoundWorkflowNodeRefs.isEmpty()) {
            return false;
        }
        return workflowDbService.hasInProgressOtherUserOrderAssignmentTasks(AuthenticationUtil.getRunAsUser(), compoundWorkflowNodeRefs);
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
    public boolean hasTaskOfType(NodeRef docRef, QName... taskTypes) {
        List<NodeRef> compoundWorkflowRefs = getCompoundWorkflowNodeRefs(docRef);
        return workflowDbService.containsTaskOfType(compoundWorkflowRefs, taskTypes);
    }

    @Override
    public void finishUserActiveResponsibleInProgressTask(NodeRef docRef, String comment) {
        String currentUser = AuthenticationUtil.getRunAsUser();
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflows(docRef)) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (!isStatus(task, Status.IN_PROGRESS)) {
                        continue;
                    }
                    if (isStatus(task, Status.IN_PROGRESS) && isActiveResponsible(task) && WorkflowUtil.isOwner(task, currentUser)) {
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
        Task task = workflowDbService.getTask(taskRef, null, false);
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
    public boolean containsDocumentsWithLimitedActivities(NodeRef compoundWorkflowRef) {
        List<Document> documents = getCompoundWorkflowDocuments(compoundWorkflowRef);
        if (!documents.isEmpty()) {
            Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
            for (Document doc : documents) {
                if (BeanHelper.getFunctionsService().getUnmodifiableFunction(doc.getNodeRef(), propertyTypes).isDocumentActivitiesAreLimited()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setCompoundWorkflowOwner(NodeRef compoundWorkflowRef, String ownerId, boolean retainPreviousOwnerId) {
        if (!dictionaryService.isSubClass(nodeService.getType(compoundWorkflowRef), WorkflowCommonModel.Types.COMPOUND_WORKFLOW)) {
            throw new RuntimeException("Node is not a compoundWorkflow: " + compoundWorkflowRef);
        }
        String existingOwnerId = (String) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.OWNER_ID);
        if (ownerId.equals(existingOwnerId)) {
            if (log.isDebugEnabled()) {
                log.debug("CW owner is already set to " + ownerId + ", not overwriting properties");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting CW owner from " + existingOwnerId + " to " + ownerId + " - " + compoundWorkflowRef);
        }

        Map<QName, Serializable> personProps = userService.getUserProperties(ownerId);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
        props.put(WorkflowCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
        props.put(WorkflowCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) userService.getUserOrgPathOrOrgName(personProps));
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
        String previousOwnerId = (retainPreviousOwnerId) ? existingOwnerId : null;
        props.put(WorkflowCommonModel.Props.PREVIOUS_OWNER_ID, previousOwnerId);
        nodeService.addProperties(compoundWorkflowRef, props);
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
        requireStatus(object, object.getStatus(), statuses);
    }

    private void requireStatus(BaseWorkflowObject object, String status, Status... statuses) {
        String[] statusNames = new String[statuses.length];
        int i = 0;
        for (Status stat : statuses) {
            statusNames[i++] = stat.getName();
        }
        requireValue(object, status, WorkflowCommonModel.Props.STATUS, statusNames);
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
                    + StringUtils.replace(object.toString(), "\n", "\n  "), null);
        }
    }

    private String getRepoStatus(NodeRef nodeRef) {
        return (String) nodeService.getProperty(nodeRef, WorkflowCommonModel.Props.STATUS);
    }

    // ========================================================================
    // ======================= FILTERING / PERMISSIONS ========================
    // ========================================================================

    @Override
    public List<Task> getMyTasksInProgress(List<NodeRef> compoundWorkflows) {
        if (!compoundWorkflows.isEmpty()) {
            return workflowDbService.getInProgressTasks(compoundWorkflows, getUserNameToCheck());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isCompoundWorkflowOwner(List<NodeRef> compoundWorkflows) {
        Set<QName> props = new HashSet<>(1);
        props.add(WorkflowCommonModel.Props.OWNER_ID);
        Map<NodeRef, Node> ownerIds = bulkLoadNodeService.loadNodes(compoundWorkflows, props);
        String userName = getUserNameToCheck();
        for (Node node : ownerIds.values()) {
            if (userName.equals(node.getProperties().get(WorkflowCommonModel.Props.OWNER_ID))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isOwner(NodeRef compoundWorkflowNodeRef) {
        return StringUtils.equals((String) nodeService.getProperty(compoundWorkflowNodeRef, WorkflowCommonModel.Props.OWNER_ID), getUserNameToCheck());
    }

    @Override
    public boolean isOwnerOfInProgressActiveResponsibleAssignmentTask(NodeRef docRef) {
        return workflowDbService.isOwnerOfInProgressTask(getCompoundWorkflowNodeRefs(docRef), WorkflowSpecificModel.Types.ASSIGNMENT_TASK, true);
    }

    @Override
    public void updateCompWorkflowDocsSearchProps(CompoundWorkflow cWorkflow) {
        if (cWorkflow.isCaseFileWorkflow()) {
            return;
        }
        updateCompWorkflowDocsSearchProps(getCompoundWorkflowDocsSearchPropsRefs(cWorkflow));
    }

    private List<NodeRef> getCompoundWorkflowDocsSearchPropsRefs(CompoundWorkflow cWorkflow) {
        if (cWorkflow.isCaseFileWorkflow()) {
            return null;
        }
        List<NodeRef> docRefs;
        if (cWorkflow.isDocumentWorkflow()) {
            docRefs = Collections.singletonList(cWorkflow.getParent());
        } else {
            docRefs = getCompoundWorkflowDocumentRefs(cWorkflow.getNodeRef());
        }
        return docRefs;
    }

    private void updateCompWorkflowDocsSearchProps(List<NodeRef> docRefs) {
        if (docRefs == null) {
            return;
        }
        for (NodeRef docRef : docRefs) {
            updateDocumentCompWorkflowSearchProps(docRef);
        }
    }

    @Override
    public void updateDocumentCompWorkflowSearchProps(final NodeRef docRef) {
        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        boolean hasAllFinishedCompoundWorkflows = hasAllFinishedCompoundWorkflows(docRef, null);
        props.put(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, hasAllFinishedCompoundWorkflows);
        boolean hasStartedWorkflows = hasStartedCompoundWorkflows(docRef);
        props.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, hasStartedWorkflows);

        // Ignore document locking, because we are not changing a property that is user-editable or related to one
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                nodeService.addProperties(docRef, props);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Override
    public boolean hasAllFinishedCompoundWorkflows(NodeRef docRef, Map<Long, QName> propertyTypes) {
        Map<NodeRef, List<Node>> docCompoundWorkflowsMap = bulkLoadNodeService.loadChildNodes(Arrays.asList(docRef), null, WorkflowCommonModel.Types.COMPOUND_WORKFLOW,
                propertyTypes,
                new CreateObjectCallback<Node>() {

            @Override
            public Node create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                return new WmNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, null, properties);
            }
        });
        List<Node> docCompoundWorkflows = docCompoundWorkflowsMap.get(docRef);
        boolean docCWFnotEmpty = CollectionUtils.isNotEmpty(docCompoundWorkflows);
        if (docCWFnotEmpty) {
            for (Node compoundWorkflow : docCompoundWorkflows) {
                if (!Status.FINISHED.equals((String) compoundWorkflow.getProperties().get(WorkflowCommonModel.Props.STATUS))) {
                    return false;
                }
            }
        }
        if (!BeanHelper.getWorkflowConstantsBean().isIndependentWorkflowEnabled()) {
            return true;
        }
        List<AssociationRef> independentCompWorkflowAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        if (docCWFnotEmpty && independentCompWorkflowAssocs.isEmpty()) {
            return false;
        }
        List<NodeRef> independentCompWorkflows = new ArrayList<NodeRef>(independentCompWorkflowAssocs.size());
        for (AssociationRef assocRef : independentCompWorkflowAssocs) {
            independentCompWorkflows.add(assocRef.getTargetRef());
        }
        return checkAllFinishedCompoundWorkflows(independentCompWorkflows);
    }

    private boolean checkAllFinishedCompoundWorkflows(List<NodeRef> compoundWorkflows) {
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (!Status.FINISHED.equals(getRepoStatus(compoundWorkflow))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasStartedCompoundWorkflows(NodeRef docRef) {
        List<NodeRef> docCompoundWorkflows = getCompoundWorkflowNodeRefs(docRef);
        if (checkStartedCompoundWorkflows(docCompoundWorkflows)) {
            return true;
        }
        List<AssociationRef> independentCompWorkflowAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        if (docCompoundWorkflows.isEmpty() && independentCompWorkflowAssocs.isEmpty()) {
            return false;
        }
        List<NodeRef> independentCompWorkflows = new ArrayList<NodeRef>(independentCompWorkflowAssocs.size());
        for (AssociationRef assocRef : independentCompWorkflowAssocs) {
            independentCompWorkflows.add(assocRef.getTargetRef());
        }
        return checkStartedCompoundWorkflows(independentCompWorkflows);
    }

    private boolean checkStartedCompoundWorkflows(List<NodeRef> compoundWorkflows) {
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (nodeService.getProperty(compoundWorkflow, WorkflowCommonModel.Props.STARTED_DATE_TIME) != null) {
                return true;
            }
        }
        return false;
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
            workflowType = workflowConstantsBean.getWorkflowTypesByWorkflow().get(object.getNode().getType());
        } else if (object instanceof Task) {
            workflowType = workflowConstantsBean.getWorkflowTypesByTask().get(object.getNode().getType());
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
        boolean isAssignmentWorkflow = WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.getLocalName().equals(workflow.getType().getLocalName());
        for (Task task : tasks) {
            if (isReviewWorkFlow) {
                setStopOnFinishByOutcome(workflow, task);
            }
            // Start all new tasks
            if (parallelTasks && isStatus(task, Status.NEW)) {
                setStatus(queue, task, Status.IN_PROGRESS);
                setNewIndepententWorkflowOwner(isAssignmentWorkflow, workflow, task, queue);
            }
        }
        if (!parallelTasks && !isStatusAny(tasks, Status.IN_PROGRESS)) {
            // Start first new task
            for (Task task : tasks) {
                if (isStatus(task, Status.NEW)) {
                    setStatus(queue, task, Status.IN_PROGRESS);
                    setNewIndepententWorkflowOwner(isAssignmentWorkflow, workflow, task, queue);
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

    private void setNewIndepententWorkflowOwner(boolean isAssignmentWorkflow, Workflow workflow, Task task, WorkflowEventQueue queue) {
        if (isAssignmentWorkflow && task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && workflow.getParent().isIndependentWorkflow()) {
            String newOwner = task.getOwnerId();
            if (newOwner != null) {
                queue.setParameter(WorkflowQueueParameter.ASSIGNEMNT_TASK_STARTED_WITH_RESPONSIBLE_ASPECT, newOwner);
                // update display
                workflow.getParent().setOwnerName(userService.getUserFullName(newOwner));
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
            boolean isFirstWorkflow = true;
            for (Workflow workflow : workflows) {
                if (isStatus(workflow, Status.NEW)) {
                    // if we previously finished another workflow, then it means this workflow is started automatically
                    ErrorCause errorCause = checkCanStartIndependentWorkflow(compoundWorkflow, workflow);
                    if (errorCause == null) {
                        setStatus(queue, workflow, Status.IN_PROGRESS);
                        for (WorkflowEvent event : queue.getEvents()) {
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
                    } else if (!queue.getBooleanParameter(WorkflowQueueParameter.WORKFLOW_CONTINUED) && !isFirstWorkflow) {
                        stopCompoundWorkflow(queue, compoundWorkflow, false);
                        queueEvent(queue, WorkflowEventType.WORKFLOW_STOPPED_AUTOMATICALLY, workflow);
                    } else {
                        throw new WorkflowChangedException("workflow_compound_no_documents", compoundWorkflow, errorCause);
                    }
                    break;
                }
                isFirstWorkflow = false;
            }
        }

        // If all workflows are finished, then finish the compoundWorkflow
        if (isStatusAll(workflows, Status.FINISHED)) {
            setStatus(queue, compoundWorkflow, Status.FINISHED);
        }
    }

    // Registration and signature workflows need associated documents to be started in independent workflow
    private ErrorCause checkCanStartIndependentWorkflow(CompoundWorkflow compoundWorkflow, Workflow workflow) {
        if (!compoundWorkflow.isIndependentWorkflow()) {
            return null;
        }
        int compoundWorkflowDocumentCount = compoundWorkflow.isSaved() ? getCompoundWorkflowDocumentCount(compoundWorkflow.getNodeRef()) : compoundWorkflow.getNewAssocs().size();
        if (workflow.isType(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW) && compoundWorkflowDocumentCount == 0) {
            return ErrorCause.INDEPENDENT_WORKFLOW_REGISTRATION_NO_DOCUMENTS;
        }
        if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) && !hasSigningDocuments(compoundWorkflow)) {
            return ErrorCause.INDEPENDENT_WORKFLOW_SIGNATURE_NO_DOCUMENTS;
        }
        return null;
    }

    private boolean hasSigningDocuments(CompoundWorkflow compoundWorkflow) {
        List<String> compoundWorkflowSigningDocs = compoundWorkflow.getDocumentsToSignNodeRefIds();
        List<NodeRef> documentRefs;
        if (compoundWorkflow.isSaved()) {
            documentRefs = getCompoundWorkflowDocumentRefs(compoundWorkflow.getNodeRef());
        } else {
            documentRefs = compoundWorkflow.getNewAssocs();
        }
        for (NodeRef documentRef : documentRefs) {
            if (compoundWorkflowSigningDocs.contains(documentRef.getId())) {
                return true;
            }
        }
        return false;
    }

    // added separate setStatus methods to find places where the status of some concrete workflow object could be changed

    private void setStatus(WorkflowEventQueue queue, CompoundWorkflow cWorkflow, Status status) {
        Status originalStatus = Status.of(cWorkflow.getStatus());
        if (status != originalStatus) {
            if (originalStatus == Status.NEW && status == Status.IN_PROGRESS) {
                logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, cWorkflow.getNodeRef(), "applog_compoundWorkflow_started"));
            } else if (originalStatus == Status.STOPPED && status == Status.IN_PROGRESS) {
                logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, cWorkflow.getNodeRef(), "applog_compoundWorkflow_continued"));
            } else if (status == Status.STOPPED) {
                if (Boolean.TRUE.equals(queue.getParameter(WorkflowQueueParameter.WORKFLOW_STOPPED_MANUALLY))) {
                    logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, cWorkflow.getNodeRef(), "applog_compoundWorkflow_stopped"));
                } else {
                    logService.addLogEntry(LogEntry.createWithSystemUser(LogObject.COMPOUND_WORKFLOW, cWorkflow.getNodeRef(), "applog_compoundWorkflow_stopped"));
                }
            } else if (status == Status.FINISHED) {
                if (Boolean.TRUE.equals(queue.getParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY))) {
                    logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, cWorkflow.getNodeRef(), "applog_compoundWorkflow_finished"));
                } else {
                    logService.addLogEntry(LogEntry.createWithSystemUser(LogObject.COMPOUND_WORKFLOW, cWorkflow.getNodeRef(), "applog_compoundWorkflow_finished"));
                }
            }
        }
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
        extras.add(originalStatus);
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
        boolean isCompoundWorkflow = object instanceof CompoundWorkflow && !(object instanceof CompoundWorkflowDefinition);
        if (isStatus(object, Status.STOPPED)) {
            object.setStoppedDateTime(queue.getNow());
            // reopening
            if (isCompoundWorkflow && originalStatus == Status.FINISHED) {
                ((CompoundWorkflow) object).setFinishedDateTime(null);
            }
        } else {
            object.setStoppedDateTime(null);
        }
        if (isCompoundWorkflow && status != originalStatus && isStatus(object, Status.FINISHED)) {
            ((CompoundWorkflow) object).setFinishedDateTime(queue.getNow());
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
                stepAndCheck(queue, otherCompoundWorkflow);
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
        if (log2.isDebugEnabled()) {
            log2.debug("Changing task (" + task.getNodeRef() + ") with outcome " + outcomeIndex + " status: " + task.getStatus() + " -> " + newStatus);
        }
        setStatus(queue, task, newStatus);
        if (isStoppingNeeded(task, outcomeIndex)) {
            stopIfNeeded(task, queue);
        }
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        if (!compoundWorkflow.isCaseFileWorkflow() && task.isType(WorkflowSpecificModel.Types.REVIEW_TASK)) {
            List<File> files = new ArrayList<File>();
            if (compoundWorkflow.isDocumentWorkflow()) {
                files.addAll(fileService.getAllFilesExcludingDigidocSubitems(compoundWorkflow.getParent()));
            } else if (compoundWorkflow.isIndependentWorkflow()) {
                for (NodeRef docRef : getCompoundWorkflowDocumentRefs(compoundWorkflow.getNodeRef())) {
                    files.addAll(fileService.getAllFilesExcludingDigidocSubitems(docRef));
                }
            }
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
        if (log2.isDebugEnabled()) {
            log2.debug("Checking if stopping is needed for " + task.getNode().getType().getLocalName() + " (" + task.getNodeRef() + ")");
        }
        if (outcomeIndex == SIGNATURE_TASK_OUTCOME_NOT_SIGNED && Types.SIGNATURE_TASK.equals(task.getNode().getType())) {
            return true;
        } else if (task.isType(Types.REVIEW_TASK)) {
            // sometimes here value of TEMP_OUTCOME is Integer, sometimes String
            final Integer tempOutcome = DefaultTypeConverter.INSTANCE.convert(Integer.class, task.getProp(WorkflowSpecificModel.Props.TEMP_OUTCOME));
            if (tempOutcome != null && tempOutcome == REVIEW_TASK_OUTCOME_REJECTED) { // What is tempOutcome and why was it null?
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
        if (log2.isDebugEnabled()) {
            log2.debug("Stopping of compoundworkflow (" + compoundWorkflow.getNodeRef() + ") triggered by task " + task.getNodeRef());
        }
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
                if (log2.isDebugEnabled()) {
                    log2.debug("Stopping parent workflow (" + parentWorkFlow.getNodeRef() + ")");
                }
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
        if (object instanceof Task) {
            return createOrUpdateTask((Task) object, parent);
        }

        Assert.isTrue(assocType != null, "Must specify child association type!");
        if (object.isUnsaved()) {
            object.setCreatorName(userService.getUserFullName()); // If saving a new node, then set creator to current user
        }

        object.preSave();

        Map<QName, Serializable> props = getSaveProperties(object.getChangedProperties());
        boolean changed = (object.isUnsaved() ? createWorkflow(object, parent, assocType, props) : updateWorkflow(queue, object, props));

        object.setChangedProperties(props);
        return changed;
    }

    private boolean createWorkflow(BaseWorkflowObject object, NodeRef parent, QName assocType, Map<QName, Serializable> props) {
        WmNode node = object.getNode();
        if (log.isDebugEnabled()) {
            log.debug("Creating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                    + WmNode.toString(props.entrySet()));
        }
        NodeRef nodeRef = nodeService.createNode(parent, assocType, assocType, node.getType(), props).getChildRef();
        node.updateNodeRef(nodeRef);

        // Add additional aspects
        Set<QName> aspects = new HashSet<>(node.getAspects());
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
        return true;
    }

    private boolean updateWorkflow(WorkflowEventQueue queue, BaseWorkflowObject object, Map<QName, Serializable> propertiesToUpdate) {
        boolean changed = false;
        if (propertiesToUpdate.isEmpty()) {
            return changed;
        }

        WmNode node = object.getNode();
        if (log.isDebugEnabled()) {
            log.debug("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                    + WmNode.toString(propertiesToUpdate.entrySet()));
        }

        if (CollectionUtils.containsAny(ownerRelatedKeys, propertiesToUpdate.keySet()) || propertiesToUpdate.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL)) {
            if (isCompoundWorkflow(object)) {
                CompoundWorkflow compoundWorkflow = (CompoundWorkflow) object;
                String originalStatus = (String) object.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS);
                if (compoundWorkflow.isDocumentWorkflow()) {
                    requireStatus(object, originalStatus, Status.NEW, Status.IN_PROGRESS, Status.FINISHED, Status.STOPPED, Status.UNFINISHED);
                } else if (compoundWorkflow.isIndependentWorkflow()) {
                    requireStatus(object, originalStatus, Status.NEW, Status.IN_PROGRESS, Status.STOPPED, Status.UNFINISHED);
                } else if (compoundWorkflow.isCaseFileWorkflow()) {
                    requireStatus(object, originalStatus, Status.NEW, Status.IN_PROGRESS, Status.STOPPED);
                }
                else {
                    throw new RuntimeException("Unsupported compound workflow type, nodeRef=" + compoundWorkflow.getNodeRef());
                }
            } else {
                requireValue(object, (String) object.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS), WorkflowCommonModel.Props.STATUS,
                        Status.NEW.getName(), Status.IN_PROGRESS.getName());
            }
        }

        nodeService.addProperties(node.getNodeRef(), propertiesToUpdate); // do not replace non-changed properties
        changed = true;

        if (isCompoundWorkflow(object)) {
            logCompoundWorkflowDataChanged((CompoundWorkflow) object);
        }

        queueEvent(queue, WorkflowEventType.UPDATED, object, propertiesToUpdate);
        return changed;
    }

    private boolean createOrUpdateTask(Task task, NodeRef parent) {
        boolean changed = false;
        WmNode node = task.getNode();
        NodeRef taskParentRef = workflowConstantsBean.getWorkflowTypesByTask().get(task.getType()).getWorkflowType() != null ? null : getLinkedReviewTaskSpace();

        Map<QName, Serializable> props = prepareTaskForSaving(task, parent);
        if (task.isUnsaved()) {
            NodeRef nodeRef = new NodeRef(parent.getStoreRef(), GUID.generate());
            node.updateNodeRef(nodeRef);
            changed = true;
            workflowDbService.createTaskEntry(task, taskParentRef, getLinkedReviewTaskSpace().equals(taskParentRef));
        } else {
            if (!props.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                            + WmNode.toString(props.entrySet()));
                }
                verifyRequiredStatusOnUpdate(task, props);
            }

            // TODO: move querying independent tasks root to appropriate WorkflowType implementations
            // when more than one type of such tasks is created
            // Currently only linkedReviewTasks are created outside of workflow.
            workflowDbService.updateTaskEntry(task, props, (taskParentRef == null) ? parent : taskParentRef);
        }

        task.setChangedProperties(props);
        return changed;
    }

    private void verifyRequiredStatusOnUpdate(Task task, Map<QName, Serializable> props) {
        if ((CollectionUtils.containsAny(ownerRelatedKeys, props.keySet()) || props.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL))
                && !onlyOwnerEmailWasAddedToTask(props, (String) task.getOriginalProperties().get(WorkflowCommonModel.Props.OWNER_EMAIL))) {
            requireValue(task, (String) task.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS), WorkflowCommonModel.Props.STATUS,
                    Status.NEW.getName(), Status.IN_PROGRESS.getName());
        }
    }

    private Map<QName, Serializable> prepareTaskForSaving(Task task, NodeRef parent) {
        if (task.isUnsaved() && !(task instanceof LinkedReviewTask)) {
            task.setCreatorName(userService.getUserFullName());
            String username = userService.getCurrentUserName();
            task.setCreatorId(username);
            task.setCreatorEmail(userService.getUserEmail(username));
            task.setDocumentType(getDocumentTypeFromTaskParent(parent));

            if (task.isType(Types.EXTERNAL_REVIEW_TASK)) {
                task.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, dvkService.getInstitutionCode());
            }
        }

        if (task.isType(Types.REVIEW_TASK) && workflowConstantsBean.isReviewToOtherOrgEnabled()) {
            task.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_NAME, parametersService.getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT));
            task.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, dvkService.getInstitutionCode());
            OrganizationStructure organizationStructure = getOwnerInstitution(task);
            task.setProp(WorkflowSpecificModel.Props.INSTITUTION_NAME, organizationStructure != null ? organizationStructure.getName() : null);
            task.setProp(WorkflowSpecificModel.Props.INSTITUTION_CODE, organizationStructure != null ? organizationStructure.getInstitutionRegCode() : null);
        }

        task.preSave();

        return getSaveProperties(task.getChangedProperties());
    }

    private boolean onlyOwnerEmailWasAddedToTask(Map<QName, Serializable> saveProps, String prevoiusEmail) {
        boolean newEmailAdded = saveProps.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL);
        boolean prevoiusEmailBlank = StringUtils.isBlank(prevoiusEmail);
        boolean otherPropsNotChanged = !(CollectionUtils.containsAny(ownerRelatedKeys, saveProps.keySet()));
        return prevoiusEmailBlank && newEmailAdded && otherPropsNotChanged;
    }

    @Override
    public List<String> checkAndAddMissingOwnerEmails(CompoundWorkflow compoundWorkflow) {
        List<String> ownersNames = new ArrayList<String>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.isStatus(Status.FINISHED, Status.UNFINISHED)) {
                    continue;
                }
                String ownerEmail = task.getOwnerEmail();
                String ownerName = task.getOwnerName();
                if (StringUtils.isNotBlank(ownerName) && StringUtils.isBlank(ownerEmail)) {
                    if (addUserEmailToTaskIfPossible(task)) {
                        continue;
                    }
                    ownersNames.add(ownerName);
                }
            }
        }
        return ownersNames;
    }

    private static boolean addUserEmailToTaskIfPossible(Task task) {
        String ownerEmail = BeanHelper.getUserService().getUserEmail(task.getOwnerId());
        if (StringUtils.isNotBlank(ownerEmail)) {
            task.setOwnerEmail(ownerEmail);
            return true;
        }
        return false;
    }

    private OrganizationStructure getOwnerInstitution(Task task) {
        String ownerId = task.getOwnerId();
        Node user = userService.getUser(ownerId);
        if (user == null) {
            return null;
        }
        String organizationId = (String) user.getProperties().get(ContentModel.PROP_ORGID);
        if (StringUtils.isBlank(organizationId)) {
            return null;
        }
        OrganizationStructure organizationStructure = organizationStructureService.getOrganizationStructure(organizationId);
        while (organizationStructure != null) {
            String institutionRegCode = organizationStructure.getInstitutionRegCode();
            if (StringUtils.isNotBlank(institutionRegCode)) {
                List<Node> institutions = BeanHelper.getAddressbookService().getContactsByRegNumber(institutionRegCode);
                if (!institutions.isEmpty()) {
                    Map<String, Object> orgProps = institutions.get(0).getProperties();
                    if (Boolean.TRUE.equals(orgProps.get(Props.DVK_CAPABLE))) {
                        // dvk capable organization contact found
                        return organizationStructure;
                    }
                    // contact found, but dvk not enabled
                    if (task.getParent() != null && task.getParent().getParent() != null) {
                        task.getParent()
                        .getParent()
                        .getReviewTaskDvkInfoMessages()
                        .add(new Pair<String, Object[]>("review_task_organization_contact_dvk_disabled", new Object[] { task.getOwnerName(),
                                orgProps.get(Props.ORGANIZATION_NAME), institutionRegCode }));
                    }
                    return null;
                }
                // organization contact not found
                if (task.getParent() != null && task.getParent().getParent() != null) {
                    task.getParent().getParent().getReviewTaskDvkInfoMessages()
                    .add(new Pair<String, Object[]>("review_task_organization_missing_contact", new Object[] { task.getOwnerName(), institutionRegCode }));
                }
                return null;
            }
            organizationStructure = organizationStructureService.getOrganizationStructure(organizationStructure.getSuperUnitId());
        }
        // organization with non-empty reg code is not found
        return null;
    }

    private boolean isCompoundWorkflow(BaseWorkflowObject object) {
        return object instanceof CompoundWorkflow && !(object instanceof CompoundWorkflowDefinition);
    }

    private void logCompoundWorkflowDataChanged(CompoundWorkflow compoundWorkflow) {
        Map<QName, Serializable> changedProps = compoundWorkflow.getChangedProperties();
        Map<QName, Serializable> originalProps = compoundWorkflow.getOriginalProperties();
        String emptyLabel = PropDiffHelper.getEmptyLabel();
        for (Map.Entry<QName, Serializable> entry : changedProps.entrySet()) {
            QName propQName = entry.getKey();
            if (WorkflowCommonModel.Props.OWNER_ID.equals(propQName) || WorkflowCommonModel.Props.OWNER_JOB_TITLE.equals(propQName)
                    || WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propQName)) {
                continue;
            }
            String oldValueStr;
            String newValueStr;
            Serializable oldValue = originalProps.get(entry.getKey());
            Serializable newValue = entry.getValue();
            if (WorkflowCommonModel.Props.MAIN_DOCUMENT.equals(propQName)) {
                oldValueStr = getDocumentLog((NodeRef) oldValue, emptyLabel);
                newValueStr = getDocumentLog((NodeRef) newValue, emptyLabel);
            } else if (WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN.equals(propQName)) {
                oldValueStr = getDocumentsLog(emptyLabel, (List<NodeRef>) oldValue);
                newValueStr = getDocumentsLog(emptyLabel, (List<NodeRef>) newValue);
            } else {
                oldValueStr = value(oldValue, emptyLabel);
                newValueStr = value(newValue, emptyLabel);
            }
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_data_changed",
                    MessageUtil.getPropertyName(propQName), oldValueStr, newValueStr));
        }
    }

    protected String getDocumentsLog(String emptyLabel, List<NodeRef> documents) {
        if (documents == null || documents.isEmpty()) {
            return emptyLabel;
        }
        List<String> values = new ArrayList<String>();
        for (NodeRef docRef : documents) {
            values.add(getDocumentLog(docRef, emptyLabel));
        }
        return TextUtil.joinNonBlankStringsWithComma(values);
    }

    private String getDocumentLog(NodeRef oldValue, String emptyLabel) {
        if (oldValue == null) {
            return emptyLabel;
        }
        Map<QName, Serializable> docProps = nodeService.getProperties(oldValue);
        String typeName = BeanHelper.getDocumentAdminService().getDocumentTypeName((String) docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID));
        String docTitle = (String) docProps.get(DocumentCommonModel.Props.DOC_NAME);
        return MessageUtil.getMessage("applog_document_type_and_title", typeName, docTitle);
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
    public void createDueDateExtension(String reason, Date newDate, Date dueDate, Task initiatingTask, NodeRef containerRef, String dueDateExtenderUsername,
            String dueDateExtenderUserFullname) {
        CompoundWorkflow extensionCompoundWorkflow = getNewCompoundWorkflow(getNewCompoundWorkflowDefinition().getNode(), containerRef);
        extensionCompoundWorkflow.setTypeEnum(initiatingTask.getParent().getParent().getTypeEnum());
        Workflow workflow = getWorkflowService().addNewWorkflow(extensionCompoundWorkflow, WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW,
                extensionCompoundWorkflow.getWorkflows().size(), true);
        if (workflowConstantsBean.isWorkflowTitleEnabled()) {
            extensionCompoundWorkflow.setTitle(MessageUtil.getMessage("compoundWorkflow_due_date_extension_title"));
        }
        Task extensionTask = workflow.addTask();
        String creatorName = StringUtils.isBlank(dueDateExtenderUserFullname) ? initiatingTask.getCreatorName() : dueDateExtenderUserFullname;
        extensionTask.setOwnerName(creatorName);
        extensionTask.setOwnerId(StringUtils.isBlank(dueDateExtenderUsername) ? initiatingTask.getCreatorId() : dueDateExtenderUsername);
        extensionTask.setOwnerEmail(initiatingTask.getCreatorEmail()); // updater
        extensionTask.setCompoundWorkflowId(initiatingTask.getCompoundWorkflowId());
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
        logDueDateExtension(initiatingTask, extensionCompoundWorkflow, initiatingTask.getParent().getParent().getNodeRef(), "applog_compoundWorkflow_due_date_extension_request");

        if (extensionCompoundWorkflow.isIndependentWorkflow()) {
            CompoundWorkflow initiatingCompoundWorkflow = initiatingTask.getParent().getParent();
            String dueDateStr = initiatingTask.getDueDate() != null ? Task.dateTimeFormat.format(initiatingTask.getDueDate()) : null;
            String proposedDueDateStr = extensionTask.getProposedDueDateStr();
            String taskTypeName = MessageUtil.getTypeName(initiatingTask.getType());
            generateWorkflowRelatedUrl(extensionCompoundWorkflow, initiatingCompoundWorkflow, creatorName, "compoundWorkflow_related_task_comment", taskTypeName, dueDateStr,
                    proposedDueDateStr);
            generateWorkflowRelatedUrl(initiatingCompoundWorkflow, extensionCompoundWorkflow, creatorName, "compoundWorkflow_related_initiatortask_comment", taskTypeName,
                    initiatingTask.getOwnerName(), dueDateStr, proposedDueDateStr);
        }
        extensionCompoundWorkflow = startCompoundWorkflow(extensionCompoundWorkflow);
    }

    private void generateWorkflowRelatedUrl(CompoundWorkflow compoundWorkflowFrom, CompoundWorkflow compoundWorkflowTo, String creatorName, String messageId,
            Object... messageParams) {
        RelatedUrl relatedUrl = new RelatedUrl(new WmNode(RepoUtil.createNewUnsavedNodeRef(), WorkflowCommonModel.Types.RELATED_URL));
        relatedUrl.setCreated(new Date());
        relatedUrl.setUrlCreatorName(creatorName);
        relatedUrl.setUrl(BeanHelper.getDocumentTemplateService().getCompoundWorkflowUrl(compoundWorkflowTo.getNodeRef()));
        relatedUrl.setUrlComment(MessageUtil.getMessage(messageId, messageParams));
        getWorkflowService().saveRelatedUrl(relatedUrl, compoundWorkflowFrom.getNodeRef());
    }

    private void logDueDateExtension(Task initiatingTask, CompoundWorkflow extensionCompoundWorkflow, NodeRef initiatingCompoundWorkflowNodeRef, String logKey) {
        String extensionUrl = BeanHelper.getDocumentTemplateService().getCompoundWorkflowUrl(extensionCompoundWorkflow.getNodeRef());
        Date initiatingTaskDueDate = initiatingTask.getDueDate();
        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, initiatingCompoundWorkflowNodeRef,
                logKey, MessageUtil.getTypeName(initiatingTask.getType()), initiatingTask.getOwnerName(),
                initiatingTaskDueDate != null ? Task.dateTimeFormat.format(initiatingTaskDueDate) : null, extensionUrl));
    }

    @Override
    public void changeInitiatingTaskDueDate(Task task, WorkflowEventQueue queue) {
        Task initiatingTask = getInitiatingTask(task);
        addDueDateHistoryRecord(initiatingTask, task);
        initiatingTask.setDueDate(task.getConfirmedDueDate());
        saveTask(initiatingTask);
        NodeRef initiatingCompoundWorkflowNodeRef = generalService.getAncestorNodeRefWithType(initiatingTask.getWorkflowNodeRef(), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        logDueDateExtension(initiatingTask, task.getParent().getParent(), initiatingCompoundWorkflowNodeRef, "applog_compoundWorkflow_due_date_extension_accepted");
    }

    private Task getInitiatingTask(Task extensionTask) {
        List<Task> initiatingTasks = workflowDbService.getDueDateExtensionInitiatingTask(extensionTask.getNodeRef(), workflowConstantsBean.getTaskPrefixedQNames());
        if (initiatingTasks.size() != 1) {
            throw new RuntimeException("dueDateExtension task must have exactly one initiating task; current task has " + initiatingTasks.size() + " initiating tasks.");
        }
        Task initiatingTask = getTask(initiatingTasks.get(0).getNodeRef(), true);
        return initiatingTask;
    }

    @Override
    public void rejectDueDateExtension(Task task) {
        Task initiatingTask = getInitiatingTask(task);
        NodeRef initiatingCompoundWorkflowNodeRef = generalService.getAncestorNodeRefWithType(initiatingTask.getWorkflowNodeRef(), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        logDueDateExtension(initiatingTask, task.getParent().getParent(), initiatingCompoundWorkflowNodeRef, "applog_compoundWorkflow_due_date_extension_rejected");
    }

    @SuppressWarnings("unchecked")
    private void addDueDateHistoryRecord(Task initiatingTask, Task task) {
        Date previousDueDate = initiatingTask.getDueDate();
        String changeReason = StringUtils.defaultIfEmpty(task.getComment(), task.getResolution());
        NodeRef initatingTaskRef = initiatingTask.getNodeRef();
        workflowDbService.createTaskDueDateHistoryEntries(initiatingTask.getNodeRef(),
                Arrays.asList(new DueDateHistoryRecord(initatingTaskRef.getId(), changeReason, previousDueDate, task.getNodeRef().getId(), null)));
        String previousDueDateStr = previousDueDate != null ? Task.dateFormat.format(previousDueDate) : null;
        String confirmedDueDateStr = task.getConfirmedDueDate() != null ? Task.dateFormat.format(task.getConfirmedDueDate()) : null;
        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, initatingTaskRef, "applog_task_deadline",
                initiatingTask.getOwnerName(), MessageUtil.getTypeName(task.getType()), previousDueDateStr, confirmedDueDateStr, changeReason));
    }

    @Override
    public void updateMainDocument(NodeRef workflowRef, NodeRef mainDocRef) {
        nodeService.setProperty(workflowRef, WorkflowCommonModel.Props.MAIN_DOCUMENT, mainDocRef);
    }

    @Override
    public void updateIndependentWorkflowDocumentData(NodeRef workflowRef, NodeRef mainDocRef, List<NodeRef> documentsToSign) {
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(WorkflowCommonModel.Props.MAIN_DOCUMENT, mainDocRef);
        properties.put(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, (Serializable) documentsToSign);
        nodeService.addProperties(workflowRef, properties);
    }

    @Override
    public String getIndependentCompoundWorkflowProcedureId(NodeRef compoundWorkflowRef) {
        if (CompoundWorkflowType.INDEPENDENT_WORKFLOW == getCompoundWorkflowType(compoundWorkflowRef)) {
            return (String) nodeService.getProperty(compoundWorkflowRef, WorkflowCommonModel.Props.PROCEDURE_ID);
        }
        return null;
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
        HashSet<QName> taskDefaultAspects = new HashSet<QName>(workflowConstantsBean.getTaskDataTypeDefaultAspects().get(workflowType.getTaskType()));
        NodeRef taskRef = new NodeRef(wfRef.getStoreRef(), GUID.generate());
        WmNode taskNode = new WmNode(taskRef, workflowType.getTaskType(), taskDefaultAspects, props);
        return Task.create(workflowType.getTaskClass(), taskNode, null, workflowType.getTaskOutcomes());
    }

    @Override
    public void changeTasksDocType(NodeRef docRef, String newTypeId) {
        List<CompoundWorkflow> compoundWorkflows = getCompoundWorkflows(docRef);
        Map<QName, Serializable> taskProps = new HashMap<QName, Serializable>();
        taskProps.put(WorkflowCommonModel.Props.DOCUMENT_TYPE, newTypeId);
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            List<Workflow> workflows = compoundWorkflow.getWorkflows();
            for (Workflow workflow : workflows) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    workflowDbService.updateWorkflowTaskProperties(task.getNodeRef(), taskProps);
                }
            }
        }
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

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    private DocumentAssociationsService getDocumentAssociationsService() {
        if (_documentAssociationsService == null) {
            _documentAssociationsService = (DocumentAssociationsService) beanFactory.getBean(DocumentAssociationsService.BEAN_NAME);
        }
        return _documentAssociationsService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setCaseFileLogService(CaseFileLogService caseFileLogService) {
        this.caseFileLogService = caseFileLogService;
    }

    // END: getters / setters

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    public void setCompoundWorkflowDefinitionsCache(SimpleCache<NodeRef, CompoundWorkflowDefinition> compoundWorkflowDefinitionsCache) {
        this.compoundWorkflowDefinitionsCache = compoundWorkflowDefinitionsCache;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
        this.workflowConstantsBean = workflowConstantsBean;
    }

    /**
     * The type of action performs (after getting confirmation)
     */
    public static enum DialogAction {
        SAVING, STARTING, CONTINUING;
    }

}
