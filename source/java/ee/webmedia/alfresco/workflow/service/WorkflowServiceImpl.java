package ee.webmedia.alfresco.workflow.service;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.log.PropDiffHelper.value;
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
import org.alfresco.service.cmr.repository.AssociationRef;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
<<<<<<< HEAD
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
=======
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
<<<<<<< HEAD
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Props;
import ee.webmedia.alfresco.casefile.log.service.CaseFileLogService;
=======
import org.springframework.util.Assert;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput.FileWithContentType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
<<<<<<< HEAD
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
<<<<<<< HEAD
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException.ErrorCause;
import ee.webmedia.alfresco.workflow.generated.DeleteLinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.LinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;
=======
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
public class WorkflowServiceImpl implements WorkflowService, WorkflowModifications, BeanFactoryAware {
=======
public class WorkflowServiceImpl implements WorkflowService, WorkflowModifications {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    private DocumentLogService documentLogService;
    private VolumeService volumeService;
    private CaseFileLogService caseFileLogService;
    // START: properties that would cause dependency cycle when trying to inject them
    private DocumentAssociationsService _documentAssociationsService;
    // END: properties that would cause dependency cycle when trying to inject them
    protected BeanFactory beanFactory;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private WorkflowDbService workflowDbService;

    private final Map<QName, WorkflowType> workflowTypesByWorkflow = new HashMap<QName, WorkflowType>();
    private final Map<QName, WorkflowType> workflowTypesByTask = new HashMap<QName, WorkflowType>();
    private final Map<QName, Collection<QName>> taskDataTypeDefaultAspects = new HashMap<QName, Collection<QName>>();
    private final Map<QName, List<QName>> taskDataTypeDefaultProps = new HashMap<QName, List<QName>>();
    private final Map<QName, QName> taskPrefixedQNames = new HashMap<QName, QName>();
    private final List<WorkflowEventListener> eventListeners = new ArrayList<WorkflowEventListener>();
    private final List<WorkflowMultiEventListener> multiEventListeners = new ArrayList<WorkflowMultiEventListener>();
    private final List<WorkflowEventListenerWithModifications> immediateEventListeners = new ArrayList<WorkflowEventListenerWithModifications>();
<<<<<<< HEAD
    private List<QName> taskDataTypeSearchableProps;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
<<<<<<< HEAD
    private boolean groupAssignmentWorkflowEnabled;
    private boolean independentWorkflowEnabled;
    private boolean documentWorkflowEnabled;
    private boolean workflowTitleEnabled;
    private boolean reviewToOtherOrgEnabled;
    private boolean finishDocumentsWhenWorkflowFinishes;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public void registerWorkflowType(WorkflowType workflowType) {
        Assert.notNull(workflowType);
        if (log.isDebugEnabled()) {
            log.debug("Registering workflowType:\n" + workflowType);
        }

        Assert.isTrue(!workflowTypesByWorkflow.containsKey(workflowType.getWorkflowType()));
<<<<<<< HEAD
        boolean isIndependentTaskType = workflowType.isIndependentTaskType();
        if (!isIndependentTaskType) {
            QName workflowTypeQName = workflowType.getWorkflowType();
            Assert.isTrue(dictionaryService.isSubClass(workflowTypeQName, WorkflowCommonModel.Types.WORKFLOW));
            workflowTypesByWorkflow.put(workflowTypeQName, workflowType);
        }
=======
        QName workflowTypeQName = workflowType.getWorkflowType();
        Assert.isTrue(dictionaryService.isSubClass(workflowTypeQName, WorkflowCommonModel.Types.WORKFLOW));
        workflowTypesByWorkflow.put(workflowTypeQName, workflowType);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        QName taskTypeQName = workflowType.getTaskType();
        if (taskTypeQName != null) {
            Assert.notNull(workflowType.getTaskClass());
            Assert.isTrue(!workflowTypesByTask.containsKey(taskTypeQName));
            Assert.isTrue(dictionaryService.isSubClass(taskTypeQName, WorkflowCommonModel.Types.TASK));
            workflowTypesByTask.put(taskTypeQName, workflowType);
            Collection<QName> aspects = RepoUtil.getAspectsIgnoringSystem(generalService.getDefaultAspects(taskTypeQName));
<<<<<<< HEAD
            taskDataTypeDefaultAspects.put(taskTypeQName, aspects);
            List<QName> taskDefaultProps = new ArrayList<QName>();
            taskDataTypeDefaultProps.put(taskTypeQName, taskDefaultProps);
=======
            taskDataTypeDefaultAspects.put(workflowTypeQName, aspects);
            List<QName> taskDefaultProps = new ArrayList<QName>();
            taskDataTypeDefaultProps.put(workflowTypeQName, taskDefaultProps);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    public NodeRef getIndependentWorkflowsRoot() {
        return generalService.getNodeRef(WorkflowCommonModel.Repo.INDEPENDENT_WORKFLOWS_SPACE);
    }

    @Override
    public List<CompoundWorkflowDefinition> getActiveCompoundWorkflowDefinitions(boolean getUserFullName) {
        return getCompoundWorkflowDefinitions(getUserFullName, volumeService.isCaseVolumeEnabled(), isIndependentWorkflowEnabled(), isDocumentWorkflowEnabled());
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
        return getCompoundWorkflowDefinitionsByType(userId, CompoundWorkflowType.INDEPENDENT_WORKFLOW);
    }

    @Override
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitionsByType(String userId, CompoundWorkflowType workflowType) {
        List<ChildAssociationRef> childAssocs = getAllCompoundWorkflowDefinitionRefs();
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
            String typeStr = (String) props.get(WorkflowCommonModel.Props.TYPE);
            String compWorkflowUserId = (String) props.get(WorkflowCommonModel.Props.USER_ID);
            if (StringUtils.isNotBlank(typeStr) && workflowType == CompoundWorkflowType.valueOf(typeStr)
                    && (StringUtils.isBlank(compWorkflowUserId) || StringUtils.equals(userId, compWorkflowUserId))) {
                compoundWorkflowDefinitions.add(getCompoundWorkflowDefinition(nodeRef, getRoot(), false));

            }
=======
    public List<CompoundWorkflowDefinition> getCompoundWorkflowDefinitions(boolean getUserFullName) {
        NodeRef root = getRoot();
        List<ChildAssociationRef> childAssocs = getWorkflows(root);
        List<CompoundWorkflowDefinition> compoundWorkflowDefinitions = new ArrayList<CompoundWorkflowDefinition>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            compoundWorkflowDefinitions.add(getCompoundWorkflowDefinition(nodeRef, root, getUserFullName));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
        return compoundWorkflowDefinitions;
    }

    @Override
<<<<<<< HEAD
    public List<ChildAssociationRef> getAllCompoundWorkflowDefinitionRefs() {
        return nodeService.getChildAssocs(getRoot());
    }

    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false, true, true, true)) {
=======
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        for (CompoundWorkflowDefinition compWorkflowDefinition : getActiveCompoundWorkflowDefinitions(false)) {
=======
        for (CompoundWorkflowDefinition compWorkflowDefinition : getCompoundWorkflowDefinitions(false)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        compWorkflowDefinition.setTypeEnum(compoundWorkflow.getTypeEnum());
        if (compoundWorkflow.isDocumentWorkflow()) {
            NodeRef docRef = compoundWorkflow.getParent();
=======
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef != null) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        QName parentType = nodeService.getType(parent);
        Assert.isTrue(!WorkflowCommonModel.Types.INDEPENDENT_COMPOUND_WORKFLOWS_ROOT.equals(parentType), "Querying all independent compound workflows is not supported!");
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW);
        List<NodeRef> compoundWorkflows = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            compoundWorkflows.add(childAssoc.getChildRef());
        }
        return compoundWorkflows;
    }

    @Override
<<<<<<< HEAD
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef) {
        return getCompoundWorkflow(nodeRef, true);
    }

    private CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, boolean loadTasks) {
<<<<<<< HEAD
        return getCompoundWorkflow(nodeRef, loadTasks, true);
    }

    @Override
    public CompoundWorkflow getCompoundWorkflow(NodeRef nodeRef, boolean loadTasks, boolean loadWorkflows) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
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
        compoundWorkflow.setOtherCompoundWorkflows(
                compoundWorkflow.isDocumentWorkflow() ? getCompoundWorkflows(compoundWorkflow.getParent(), compoundWorkflow.getNodeRef(), false)
                        : new ArrayList<CompoundWorkflow>());
    }

    @Override
    public CompoundWorkflow getCompoundWorkflowOfType(NodeRef nodeRef, List<QName> types) {
        WmNode node = getNode(nodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, false, false);
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        CompoundWorkflow compoundWorkflow = new CompoundWorkflow(node, parent);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            if (types.contains((nodeService.getType(childAssoc.getChildRef())))) {
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                workflowIndex = addWorkflow(compoundWorkflow, false, workflowIndex, childAssoc, true);
            }
        }
        return compoundWorkflow;
    }

    @Override
<<<<<<< HEAD
    public CompoundWorkflowWithObject getCompoundWorkflowWithObject(NodeRef compoundWorkflowRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(compoundWorkflowRef);
        CompoundWorkflowWithObject compoundWorkflowWithObject = new CompoundWorkflowWithObject(compoundWorkflow);
        if (compoundWorkflow.isDocumentWorkflow()) {
            compoundWorkflowWithObject.setObjectTitle("D: " + (String) nodeService.getProperty(compoundWorkflow.getParent(), DocumentCommonModel.Props.DOC_NAME));
        } else if (compoundWorkflow.isCaseFileWorkflow()) {
            compoundWorkflowWithObject.setObjectTitle("A: " + (String) nodeService.getProperty(compoundWorkflow.getParent(), DocumentDynamicModel.Props.TITLE));
        } else if (compoundWorkflow.isIndependentWorkflow()) {
            compoundWorkflow.setNumberOfDocuments(getCompoundWorkflowDocumentCount(compoundWorkflowRef));
        }
        return compoundWorkflowWithObject;
    }

    private void getAndAddWorkflows(NodeRef parent, CompoundWorkflow compoundWorkflow, boolean copy, boolean addTasks) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent, workflowTypesByWorkflow.keySet());
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        int workflowIndex = 0;
        for (ChildAssociationRef childAssoc : childAssocs) {
            workflowIndex = addWorkflow(compoundWorkflow, copy, workflowIndex, childAssoc, addTasks);
        }
    }

<<<<<<< HEAD
=======
    private List<ChildAssociationRef> getWorkflows(NodeRef parent) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        return childAssocs;
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    @Override
    public CompoundWorkflowType getWorkflowCompoundWorkflowType(NodeRef workflowRef) {
        NodeRef compoundWorkflowRef = (nodeService.getPrimaryParent(workflowRef)).getParentRef();
        return getCompoundWorkflowType(compoundWorkflowRef);
    }

    private void getAndAddTasks(NodeRef parent, Workflow workflow, boolean copy) {
        WorkflowType workflowType = workflowTypesByWorkflow.get(workflow.getType());
        QName workflowTaskType = workflowType.getTaskType();
        workflow.addTasks(workflowDbService.getWorkflowTasks(parent, taskDataTypeDefaultAspects.get(workflowTaskType), taskDataTypeDefaultProps.get(workflowTaskType),
                taskPrefixedQNames, workflowType, workflow, copy));
        for (Task task : workflow.getTasks()) {
            loadDueDateData(task);
=======
    private void getAndAddTasks(NodeRef parent, Workflow workflow, boolean copy) {
        QName workflowType = workflow.getType();
        workflow.addTasks(workflowDbService.getWorkflowTasks(parent, taskDataTypeDefaultAspects.get(workflowType), taskDataTypeDefaultProps.get(workflowType),
                taskPrefixedQNames, workflowTypesByWorkflow.get(workflowType), workflow, copy));
        for (Task task : workflow.getTasks()) {
            loadDueDateHistory(task);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    @Override
    public Task getTask(NodeRef nodeRef, boolean fetchWorkflow) {
<<<<<<< HEAD
        return getTask(nodeRef, fetchWorkflow, false);
    }

    private Task getTask(NodeRef nodeRef, boolean fetchWorkflow, boolean fetchCompoundWorkflow) {
        Workflow workflow = null;
        if (fetchWorkflow) {
            NodeRef parent = workflowDbService.getTaskParentNodeRef(nodeRef);
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(parent).getParentRef();
            CompoundWorkflow compoundWorkflow = fetchCompoundWorkflow ? getCompoundWorkflow(compoundWorkflowRef) : null;
            workflow = getWorkflow(parent, compoundWorkflow, false);
=======
        Workflow workflow = null;
        if (fetchWorkflow) {
            NodeRef parent = workflowDbService.getTaskParentNodeRef(nodeRef);
            workflow = getWorkflow(parent, null, false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
        return getTask(nodeRef, workflow, false);
    }

    @Override
    public List<Task> getWorkflowTasks(NodeRef workflowRef) {
<<<<<<< HEAD
        WorkflowType workflowType = workflowTypesByWorkflow.get(nodeService.getType(workflowRef));
        QName workflowTaskType = workflowTypesByWorkflow.get(workflowType).getTaskType();
        return workflowDbService.getWorkflowTasks(workflowRef, taskDataTypeDefaultAspects.get(workflowTaskType), taskDataTypeDefaultProps.get(workflowTaskType),
                taskPrefixedQNames, workflowType, null, false);
=======
        QName workflowType = nodeService.getType(workflowRef);
        return workflowDbService.getWorkflowTasks(workflowRef, taskDataTypeDefaultAspects.get(workflowType), taskDataTypeDefaultProps.get(workflowType),
                taskPrefixedQNames, workflowTypesByWorkflow.get(workflowType), null, false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public Set<Task> getTasks(NodeRef docRef, Predicate<Task> taskPredicate) {
        return WorkflowUtil.getTasks(new HashSet<Task>(), getCompoundWorkflows(docRef), taskPredicate);
    }

<<<<<<< HEAD
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
=======
    private Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        Task task = getTaskWithoutParentAndChildren(nodeRef, workflow, copy);
        loadDueDateHistory(task);
        return task;
    }

    private void loadDueDateHistory(Task task) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        if (Boolean.FALSE.equals(task.getHasDueDateHistory()) || !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.TASK_DUE_DATE_EXTENSION_CONTAINER)) {
            task.setHasDueDateHistory(Boolean.FALSE);
            return;
        }
<<<<<<< HEAD
        // due date initiating task (assignment or order assignment task)
        List<DueDateHistoryRecord> historyRecords = task.getDueDateHistoryRecords();
=======
        List<Pair<String, Date>> historyRecords = task.getDueDateHistoryRecords();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        historyRecords.addAll(workflowDbService.getDueDateHistoryRecords(task.getNodeRef()));
        task.setHasDueDateHistory(!historyRecords.isEmpty());
    }

    @Override
    public Task getTaskWithoutParentAndChildren(NodeRef nodeRef, Workflow workflow, boolean copy) {
        return workflowDbService.getTask(nodeRef, taskPrefixedQNames, workflow, copy);
    }

<<<<<<< HEAD
    private WorkflowType getWorkflowType(WmNode taskNode) {
        QName type = taskNode.getType();
        WorkflowType workflowType = workflowTypesByTask.get(type);
        if (workflowType == null) {
            throw new RuntimeException("Task type '" + taskNode.getType() + "' not registered in service, but existing node has it: " + taskNode.getNodeRef());
        }
        return workflowType;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
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
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
        List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        copyInfoMessages(originalReviewTaskDvkInfoMessages, freshCompoundWorkflow);
=======
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(compoundWorkflow.getNodeRef());
        checkCompoundWorkflow(freshCompoundWorkflow);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        return freshCompoundWorkflow;
    }

    public void saveCompoundWorkflow(WorkflowEventQueue queue, CompoundWorkflow compoundWorkflow) {
        proccessPreSave(queue, compoundWorkflow);
<<<<<<< HEAD

        boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
=======
        saveCompoundWorkflow(queue, compoundWorkflow, null);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        if (!wasSaved) {
            compoundWorkflow.setCreatedDateTime(queue.getNow());
        }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
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
        }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        if (compoundWorkflow.isDocumentWorkflow() && CompoundWorkflow.class.equals(compoundWorkflow.getClass())) { // compound worflow tied to document (not compoundWorkflowDef)
=======
        if (CompoundWorkflow.class.equals(compoundWorkflow.getClass())) { // compound worflow tied to document (not compoundWorkflowDef)
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            if (!StringUtils.equals(compoundWorkflow.getOwnerId(), previousOwnerId)) {
                // doesn't matter what status cWF has, just add the privileges
                NodeRef docRef = nodeService.getPrimaryParent(compoundWorkflow.getNodeRef()).getParentRef();
                privilegeService.setPermissions(docRef, compoundWorkflow.getOwnerId(), Privileges.VIEW_DOCUMENT_FILES);
            }
        }
<<<<<<< HEAD
        if (isCompoundWorkflow(compoundWorkflow) && !wasSaved) {
            if (compoundWorkflow.isDocumentWorkflow()) {
                documentLogService.addDocumentLog(compoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
            }
            if (compoundWorkflow.isCaseFileWorkflow()) {
                caseFileLogService.addCaseFileLog(compoundWorkflow.getParent(), "casefile_log_status_workflow");
            }
            logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_created"));
        }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        return changed;
    }

    private boolean saveWorkflow(WorkflowEventQueue queue, Workflow workflow) {
<<<<<<< HEAD
        // If this node is not saved, then all children are newly created, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and no children are being added or removed, then childAssociationIndexes are already in the correct order.
        // If this node was previously saved and at least one child is being added or removed, then childAssociationIndexes have to be set on all children
        // (because maybe all children have assocIndex=-1).
        boolean setChildAssocIndexes = false;
        boolean wasSaved = workflow.isSaved();
        boolean changed = createOrUpdate(queue, workflow, workflow.getParent().getNodeRef(), WorkflowCommonModel.Assocs.WORKFLOW);

        // Remove tasks
=======
        boolean changed = createOrUpdate(queue, workflow, workflow.getParent().getNodeRef(), WorkflowCommonModel.Assocs.WORKFLOW);

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        for (Task removedTask : workflow.getRemovedTasks()) {
            NodeRef removedTaskNodeRef = removedTask.getNodeRef();
            if (removedTask.isSaved()) {
                checkTask(getTask(removedTaskNodeRef, workflow, false), Status.NEW);
                workflowDbService.deleteTask(removedTaskNodeRef);
                changed = true;
<<<<<<< HEAD
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
=======
            }
        }
        workflow.getRemovedTasks().clear();
        int index = 0;
        log.debug("Starting to save " + workflow.getTasks().size() + " tasks");
        for (Task task : workflow.getTasks()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            task.setTaskIndexInWorkflow(index);
            saveTask(queue, task);
            // TODO: is it necessary to determine if task's index in workflow changed and according to that change value returned by the function?
            // At the moment the returned value is not used anywhere
            index++;
        }
        return changed;
    }

    @Override
<<<<<<< HEAD
    public MessageDataWrapper delegate(Task assignmentTaskOriginal) throws UnableToPerformMultiReasonException {
        MessageDataWrapper feedback = new MessageDataWrapper();
=======
    public void delegate(Task assignmentTaskOriginal) {
        CompoundWorkflow compoundWorkflow = preprocessAndCopyCompoundWorkflow(assignmentTaskOriginal);
        WorkflowEventQueue queue = getNewEventQueue();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (isGeneratedByDelegation(workflow)) {
                setStatus(queue, workflow, Status.IN_PROGRESS);
            }
        }
        saveCompoundWorkflow(compoundWorkflow, queue);
    }

    private CompoundWorkflow preprocessAndCopyCompoundWorkflow(Task assignmentTaskOriginal) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        Workflow workflowOriginal = assignmentTaskOriginal.getParent();
        CompoundWorkflow cWorkflowOriginal = workflowOriginal.getParent();
        // assuming that originalWFIndex doesn't change after removing or saving
        int originalWFIndex = cWorkflowOriginal.getWorkflows().indexOf(workflowOriginal);
<<<<<<< HEAD
        log.debug("originalCWorkflow=" + cWorkflowOriginal);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
        { // removeEmptyDelegationTasks
            WorkflowUtil.removeEmptyTasks(cWorkflowCopy);
            // also remove empty workflows that could be created when information or opinion tasks are added during delegating assignment task
            WorkflowUtil.removeEmptyWorkflowsGeneratedByDelegation(cWorkflowCopy);
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
    public NodeRef importLinkedReviewTask(LinkedReviewTaskType taskToImport, String dvkId) {
        if (taskToImport == null) {
            return null;
        }
        String originalNoderefId = taskToImport.getOriginalNoderefId();
        NodeRef existingTaskRef = BeanHelper.getDocumentSearchService().searchLinkedReviewTaskByOriginalNoderefId(originalNoderefId);
        Task task;
        if (existingTaskRef == null) {
            WorkflowType workflowType = workflowTypesByTask.get(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK);
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
        task.setCompoundWorkflowComment(taskToImport.getCompoundWorkflowComment());
        task.setWorkflowResolution(taskToImport.getWorkflowResolution());
        task.setCreatorInstitutionName(taskToImport.getCreatorInstitutionName());
        task.setCreatorInstitutionCode(taskToImport.getCreatorInstitutionCode());
        task.setReceivedDvkId(dvkId);
        task.setOriginalNoderefId(originalNoderefId);
        task.setOriginalTaskObjectUrl(taskToImport.getOriginalTaskObjectUrl());
        task.setOutcome(taskToImport.getOutcome(), 0);
        task.setComment(taskToImport.getComment());
        task.setCompletedDateTime(XmlUtil.getDate(taskToImport.getCompletedDateTime()));
        task.setStoppedDateTime(XmlUtil.getDate(taskToImport.getStoppedDateTime()));

        WorkflowEventQueue queue = new WorkflowEventQueue();
        boolean isUnsaved = task.isUnsaved();
        createOrUpdate(queue, task, getLinkedReviewTaskSpace(), null);
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
            createOrUpdate(new WorkflowEventQueue(), task, null, null);
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
    public Map<NodeRef, List<File>> getCompoundWorkflowSigningFiles(CompoundWorkflow compoundWorkflow) {
        Map<NodeRef, List<File>> activeFiles = new HashMap<NodeRef, List<File>>();
        List<String> documentsToSign = compoundWorkflow.getDocumentsToSignNodeRefIds();
        for (NodeRef docRef : getCompoundWorkflowDocumentRefs(compoundWorkflow.getNodeRef())) {
            if (documentsToSign.contains(docRef.getId())) {
                List<File> documentActiveFiles = getFileService().getAllActiveFiles(docRef);
                activeFiles.put(docRef, documentActiveFiles);
            }
        }
        return activeFiles;
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
    public boolean isWorkflowEnabled() {
        return volumeService.isCaseVolumeEnabled() || isIndependentWorkflowEnabled() || isDocumentWorkflowEnabled();
=======
        WorkflowUtil.removeEmptyTasks(cWorkflowCopy);
        // also remove empty workflows that could be created when information or opinion tasks are added during delegating assignment task
        ArrayList<Integer> emptyWfIndexes = new ArrayList<Integer>();
        int wfIndex = 0;
        for (Workflow workflow : cWorkflowWorkflowsCopy) {
            if (WorkflowUtil.isGeneratedByDelegation(workflow) && workflow.getTasks().isEmpty()) {
                emptyWfIndexes.add(wfIndex);
            }
            wfIndex++;
        }
        Collections.reverse(emptyWfIndexes);
        for (int emptyWfIndex : emptyWfIndexes) {
            cWorkflowCopy.removeWorkflow(emptyWfIndex);
        }

        for (Workflow workflow : cWorkflowWorkflowsCopy) {
            for (Task task : workflow.getTasks()) {
                if (isGeneratedByDelegation(task)) {
                    Date dueDate = task.getDueDate();
                    if (dueDate != null) {
                        dueDate.setHours(23);
                        dueDate.setMinutes(59);
                    }
                }
            }
        }

        return cWorkflowCopy;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    @Override
    public boolean isGroupAssignmentWorkflowEnabled() {
        return groupAssignmentWorkflowEnabled;
    }

    public void setGroupAssignmentWorkflowEnabled(boolean enabled) {
        groupAssignmentWorkflowEnabled = enabled;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public void setConfirmationWorkflowEnabled(boolean enabled) {
        confirmationWorkflowEnabled = enabled;
    }

    @Override
    public boolean isConfirmationWorkflowEnabled() {
        return confirmationWorkflowEnabled;
    }

<<<<<<< HEAD
    public void setIndependentWorkflowEnabled(boolean independentWorkflowEnabled) {
        this.independentWorkflowEnabled = independentWorkflowEnabled;
    }

    @Override
    public boolean isIndependentWorkflowEnabled() {
        return independentWorkflowEnabled;
    }

    @Override
    public boolean isDocumentWorkflowEnabled() {
        return documentWorkflowEnabled;
    }

    public void setDocumentWorkflowEnabled(boolean documentWorkflowEnabled) {
        this.documentWorkflowEnabled = documentWorkflowEnabled;
    }

    public void setWorkflowTitleEnabled(boolean workflowTitleEnabled) {
        this.workflowTitleEnabled = workflowTitleEnabled;
    }

    @Override
    public boolean isWorkflowTitleEnabled() {
        return workflowTitleEnabled;
    }

    @Override
    public boolean isReviewToOtherOrgEnabled() {
        return reviewToOtherOrgEnabled;
    }

    public void setReviewToOtherOrgEnabled(boolean reviewToOtherOrgEnabled) {
        this.reviewToOtherOrgEnabled = reviewToOtherOrgEnabled;
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        List<NodeRef> newFileRefs = new ArrayList<NodeRef>();
=======
        List<Pair<NodeRef, NodeRef>> newFileRefs = new ArrayList<Pair<NodeRef, NodeRef>>();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
            newFileRefs.add(fileRef);
            // Add the privilege so everyone can open the file
            privilegeService.setPermissions(fileRef, PermissionService.ALL_AUTHORITIES, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
            existingDisplayNames.add(fileDisplayName);
        }
        workflowDbService.createTaskFileEntriesFromNodeRefs(taskRef, newFileRefs);
=======
            newFileRefs.add(new Pair<NodeRef, NodeRef>(taskRef, fileRef));
            existingDisplayNames.add(fileDisplayName);
        }
        workflowDbService.createTaskFileEntriesFromNodeRefs(newFileRefs);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    public void updateTaskSearchableProperties(NodeRef taskRef) {
        if (!nodeService.hasAspect(taskRef, WorkflowSpecificModel.Aspects.SEARCHABLE)) {
            return;
        }
        NodeRef compoundWorkflowRef = generalService.getAncestorNodeRefWithType(taskRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        if (compoundWorkflowRef == null) {
            return;
        }
        Map<QName, Serializable> taskSearchableProps = WorkflowUtil.getTaskSearchableProps(nodeService.getProperties(compoundWorkflowRef));
        nodeService.addProperties(taskRef, taskSearchableProps);
        workflowDbService.updateTaskProperties(taskRef, taskSearchableProps);
    }

    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        queue.setParameter(WorkflowQueueParameter.INITIATING_GROUP_TASK, task.isType(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK) ? task.getNodeRef() : null);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                        DocumentCommonModel.Props.NOT_EDITABLE)))
                || (!isInternalTesting() && !isResponsibleCurrenInstitution(task)));
=======
                        DocumentSpecificModel.Props.NOT_EDITABLE)))
                        || (!isInternalTesting() && !isResponsibleCurrenInstitution(task)));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
    }

    @Override
    public Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow) {
=======
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
    }

    private Task replaceTask(Task replacementTask, CompoundWorkflow compoundWorkflow) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    public void deleteCompoundWorkflow(NodeRef nodeRef, boolean validateStatuses) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        if (validateStatuses) {
            checkCompoundWorkflow(compoundWorkflow, Status.NEW, Status.FINISHED);
        }
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + compoundWorkflow);
        }
        logService.addLogEntry(
                LogEntry.createLoc(LogObject.WORKFLOW, BeanHelper.getUserService().getCurrentUserName(), BeanHelper.getUserService().getUserFullName(),
                        nodeRef,
                        MessageUtil.getMessage("compoundWorklfow_log_deleted", compoundWorkflow.getTitle()))
                );
        if (reviewToOtherOrgEnabled) {
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
=======
    public void deleteCompoundWorkflow(NodeRef nodeRef) {
        CompoundWorkflow compoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(compoundWorkflow, Status.NEW, Status.FINISHED);
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + compoundWorkflow);
        }

        final NodeRef docRef = compoundWorkflow.getParent();

        BeanHelper.getDocumentLogService().addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_workflow_deleted"));
        nodeService.deleteNode(nodeRef);
        final boolean hasAllFinishedCompoundWorkflows = hasAllFinishedCompoundWorkflows(docRef);

        // Ignore document locking, because we are not changing a property that is user-editable or related to one
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                nodeService.setProperty(docRef, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, hasAllFinishedCompoundWorkflows);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public CompoundWorkflow startCompoundWorkflow(CompoundWorkflow compoundWorkflowOriginal) {
        CompoundWorkflow compoundWorkflow = compoundWorkflowOriginal.copy();
<<<<<<< HEAD
        if (compoundWorkflow.isDocumentWorkflow()) {
            ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
        }
=======
        ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(compoundWorkflow);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        List<Pair<String, Object[]>> originalReviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        // also check repo status
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
<<<<<<< HEAD
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        handleEvents(queue);
        copyInfoMessages(originalReviewTaskDvkInfoMessages, freshCompoundWorkflow);
=======
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
        handleEvents(queue);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
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
=======
        logService.addLogEntry(LogEntry.create(LogObject.WORKFLOW, userService, compoundWorkflowOriginal.getNodeRef(), "applog_workflow_finished"));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                deleteCompoundWorkflow(compoundWorkflowRef, true);
            } else {
                checkCompoundWorkflow(compoundWorkflow);
                checkActiveResponsibleAssignmentTasks(compoundWorkflow);
=======
                deleteCompoundWorkflow(compoundWorkflowRef);
            } else {
                checkCompoundWorkflow(compoundWorkflow);
                checkActiveResponsibleAssignmentTasks(compoundWorkflow.getParent());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                    if (task.isStatus(Status.NEW, Status.IN_PROGRESS) && Task.Action.FINISH != task.getAction()) {
=======
                    if (task.isStatus(Status.NEW, Status.IN_PROGRESS) && Task.Action.FINISH != task.getAction() && Task.Action.UNFINISH != task.getAction()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                deleteCompoundWorkflow(compoundWorkflow.getNodeRef(), true);
=======
                deleteCompoundWorkflow(compoundWorkflow.getNodeRef());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        checkCompoundWorkflow(freshCompoundWorkflow, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
        return freshCompoundWorkflow;
    }

    private boolean isInProgressCurrentUserAssignmentTask(Task task) {
        return task.getOwnerId() != null && task.getOwnerId().equals(AuthenticationUtil.getRunAsUser())
                && task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
                && isStatus(task, Status.IN_PROGRESS);
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
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
            if (save) {
                saveCompoundWorkflow(queue, compoundWorkflow, null);
                if (log.isDebugEnabled()) {
                    log.debug("Stopped " + compoundWorkflow);
                }
            }
        }
=======
            saveCompoundWorkflow(queue, compoundWorkflow, null);
            logService.addLogEntry(LogEntry.create(LogObject.WORKFLOW, userService, compoundWorkflowOriginal.getNodeRef(), "applog_workflow_stopped"));
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
            queue.setParameter(WorkflowQueueParameter.WORKFLOW_CONTINUED, true);
            stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
            boolean changed = saveCompoundWorkflow(queue, compoundWorkflow, null);
=======
            stepAndCheck(queue, compoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
            saveCompoundWorkflow(queue, compoundWorkflow, null);
            logService.addLogEntry(LogEntry.create(LogObject.WORKFLOW, userService, compoundWorkflowOriginal.getNodeRef(), "applog_workflow_continue"));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            if (log.isDebugEnabled()) {
                log.debug("Continued " + compoundWorkflow);
            }
        }

        // also check repo status
        NodeRef nodeRef = compoundWorkflow.getNodeRef();
<<<<<<< HEAD
        List<Pair<String, Object[]>> reviewTaskDvkInfoMessages = compoundWorkflow.getReviewTaskDvkInfoMessages();
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef, reviewTaskDvkInfoMessages);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow);
=======
        // free memory, otherwise OutOfMemory error may occur when working with large compound workflows
        compoundWorkflow = null;
        CompoundWorkflow freshCompoundWorkflow = getCompoundWorkflow(nodeRef);
        checkCompoundWorkflow(freshCompoundWorkflow, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED);
        checkActiveResponsibleAssignmentTasks(freshCompoundWorkflow.getParent());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        handleEvents(queue);
        return freshCompoundWorkflow;
    }

    private void ensureNoTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow) {
<<<<<<< HEAD
        if (hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(cWorkflow, true)) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "workflow_compound_start_error_twoInprogressOrStoppedWorkflows");
        }
    }

    @Override
    public boolean hasTwoInProgressOrStoppedCWorkflowsWithMultipleWorkflows(CompoundWorkflow cWorkflow, boolean checkCurrentWorkflow) {
        boolean hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows = false;
        if (!checkCurrentWorkflow || cWorkflow.getWorkflows().size() > 1) {
            addOtherCompundWorkflows(cWorkflow);
            for (CompoundWorkflow otherCWf : cWorkflow.getOtherCompoundWorkflows()) {
                if (otherCWf.isStatus(Status.IN_PROGRESS, Status.STOPPED) && otherCWf.getWorkflows().size() > 1) {
                    hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows = true;
                    break;
                }
            }
        }
        return hasOtherInProgressOrStoppedWorkflowWithMultipleWorkflows;
=======
        if (cWorkflow.getWorkflows().size() > 1) {
            addOtherCompundWorkflows(cWorkflow);
            for (CompoundWorkflow otherCWf : cWorkflow.getOtherCompoundWorkflows()) {
                if (otherCWf.isStatus(Status.IN_PROGRESS, Status.STOPPED) && otherCWf.getWorkflows().size() > 1) {
                    throw new UnableToPerformException(MessageSeverity.ERROR, "workflow_compound_start_error_twoInprogressOrStoppedWorkflows");
                }
            }
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    @Override
    public CompoundWorkflow copyCompoundWorkflowInMemory(CompoundWorkflow compoundWorkflowOriginal) {
        return compoundWorkflowOriginal.copy();
    }

    @Override
    public Map<String, Object> getTaskChangedProperties(Task task) {
        return RepoUtil.toStringProperties(RepoUtil.getPropertiesIgnoringSystem(task.getChangedProperties(),
                BeanHelper.getDictionaryService()));
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    private void reset(BaseWorkflowObject object) {
=======
    private static void reset(BaseWorkflowObject object) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                    || key.equals(WorkflowCommonModel.Props.OWNER_JOB_TITLE) || key.equals(WorkflowCommonModel.Props.TYPE)
                    || key.equals(WorkflowSpecificModel.Props.CATEGORY) || key.equals(WorkflowSpecificModel.Props.SIGNING_TYPE)
                    || (key.equals(WorkflowCommonModel.Props.TITLE) && isWorkflowTitleEnabled())) {
=======
                    || key.equals(WorkflowCommonModel.Props.OWNER_JOB_TITLE)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                // keep value
            } else {
                prop.setValue(null);
            }
        }
    }

<<<<<<< HEAD
    private void checkActiveResponsibleAssignmentTasks(CompoundWorkflow compoundWorkflow) {
        int count = getConnectedActiveResponsibleTasksCount(compoundWorkflow, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);
        if (count > 1) {
            log.debug("Compound workflow and connected workflows have " + count + " active responsible tasks.");
=======
    private void checkActiveResponsibleAssignmentTasks(NodeRef document) {
        int count = getActiveResponsibleTasks(document, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);
        if (count > 1) {
            log.debug("Document has " + count + " active responsible tasks: " + document);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            throw new WorkflowActiveResponsibleTaskException();
        }
    }

    @Override
<<<<<<< HEAD
    public int getConnectedActiveResponsibleTasksCount(CompoundWorkflow compoundWorkflow, QName workflowType) {
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
        return getActiveresponsibleTasks(compoundWorkflows);
    }

    private int getActiveresponsibleTasks(List<CompoundWorkflow> compoundWorkflows) {
        int counter = 0;
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (!isStatus(compoundWorkflow, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!isStatus(workflow, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (!isStatus(task, Status.NEW, Status.IN_PROGRESS, Status.STOPPED)) {
=======
    public int getActiveResponsibleTasks(NodeRef document, QName workflowType) {
        return getActiveResponsibleTasks(document, workflowType, false);
    }

    @Override
    public int getActiveResponsibleTasks(NodeRef document, QName workflowType, boolean allowFinished) {
        return getActiveResponsibleTasks(document, workflowType, allowFinished, null);
    }

    @Override
    public int getActiveResponsibleTasks(NodeRef document, QName workflowType, boolean allowFinished, NodeRef compoundWorkflowToSkip) {
        Status[] allowedStatuses = allowFinished ? new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED, Status.FINISHED } :
            new Status[] { Status.NEW, Status.IN_PROGRESS, Status.STOPPED };
        int counter = 0;
        for (CompoundWorkflow compoundWorkflow : getCompoundWorkflowsOfType(document, Collections.singletonList(workflowType))) {
            if (!compoundWorkflow.isStatus(allowedStatuses) || compoundWorkflow.getNodeRef().equals(compoundWorkflowToSkip)) {
                continue;
            }
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.isType(workflowType) || !workflow.isStatus(allowedStatuses)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (!task.isStatus(allowedStatuses)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
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
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                    + StringUtils.replace(object.toString(), "\n", "\n  "));
=======
                    + StringUtils.replace(object.toString(), "\n", "\n  "), null);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
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
        boolean hasAllFinishedCompoundWorkflows = hasAllFinishedCompoundWorkflows(docRef);
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
    public boolean hasAllFinishedCompoundWorkflows(NodeRef docRef) {
        List<NodeRef> docCompoundWorkflows = getCompoundWorkflowNodeRefs(docRef);
        if (!checkAllFinishedCompoundWorkflows(docCompoundWorkflows)) {
            return false;
        }
        List<AssociationRef> independentCompWorkflowAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        if (docCompoundWorkflows.isEmpty() && independentCompWorkflowAssocs.isEmpty()) {
            return false;
        }
        List<NodeRef> independentCompWorkflows = new ArrayList<NodeRef>(independentCompWorkflowAssocs.size());
        for (AssociationRef assocRef : independentCompWorkflowAssocs) {
            independentCompWorkflows.add(assocRef.getTargetRef());
        }
        return checkAllFinishedCompoundWorkflows(independentCompWorkflows);
    }

    private boolean checkAllFinishedCompoundWorkflows(List<NodeRef> compoundWorkflows) {
=======
    public boolean hasAllFinishedCompoundWorkflows(NodeRef parent) {
        List<NodeRef> compoundWorkflows = getCompoundWorkflowNodeRefs(parent);
        if (compoundWorkflows.isEmpty()) {
            return false;
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        for (NodeRef compoundWorkflow : compoundWorkflows) {
            if (!Status.FINISHED.equals(getRepoStatus(compoundWorkflow))) {
                return false;
            }
        }
        return true;
    }

    @Override
<<<<<<< HEAD
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
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
=======
                logService.addLogEntry(LogEntry.createWithSystemUser(LogObject.WORKFLOW, workflow.getParent().getNodeRef(), "applog_workflow_stopped"));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
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
                        throw new WorkflowChangedException("workflow_compound_no_documents", errorCause);
                    }
                    break;
                }
                isFirstWorkflow = false;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
        }

        // If all workflows are finished, then finish the compoundWorkflow
        if (isStatusAll(workflows, Status.FINISHED)) {
            setStatus(queue, compoundWorkflow, Status.FINISHED);
<<<<<<< HEAD
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
=======
            logService.addLogEntry(LogEntry.createWithSystemUser(LogObject.WORKFLOW, compoundWorkflow.getNodeRef(), "applog_workflow_finished"));
        }
    }

    // added sepparate setStatus methods to find places where the status of some concrete workflow object could be changed

    private void setStatus(WorkflowEventQueue queue, CompoundWorkflow cWorkflow, Status status) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        extras.add(originalStatus);
        if (Boolean.TRUE.equals(queue.getParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY))) {
            extras.add(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY);
        }
=======
        if (Boolean.TRUE.equals(queue.getParameter(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY))) {
            extras.add(WorkflowQueueParameter.WORKFLOW_CANCELLED_MANUALLY);
        }
        extras.add(originalStatus);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        queueEvent(queue, WorkflowEventType.STATUS_CHANGED, object, extras.toArray());
        addExternalReviewWorkflowData(queue, object, originalStatus);

        // status based property setting
        if (object.getStartedDateTime() == null && isStatus(object, Status.IN_PROGRESS)) {
            object.setStartedDateTime(queue.getNow());
        }
<<<<<<< HEAD
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
=======
        if (isStatus(object, Status.STOPPED)) {
            object.setStoppedDateTime(queue.getNow());
        } else {
            object.setStoppedDateTime(null);
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                stepAndCheck(queue, otherCompoundWorkflow);
=======
                stepAndCheck(queue, otherCompoundWorkflow, checkedNodeRefs);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        CompoundWorkflow compoundWorkflow = task.getParent().getParent();
        if (!compoundWorkflow.isCaseFileWorkflow() && task.isType(WorkflowSpecificModel.Types.REVIEW_TASK)
                && (outcomeIndex == REVIEW_TASK_OUTCOME_ACCEPTED || outcomeIndex == REVIEW_TASK_OUTCOME_ACCEPTED_WITH_COMMENT)) {
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
                NodeRef fileRef = file.getNodeRef();
                String nextVersionLabel = versionsService.calculateNextVersionLabel(fileRef);
                filesWithVersions.add(file.getDisplayName() + " " + nextVersionLabel);
                nodeService.setProperty(fileRef, FileModel.Props.NEW_VERSION_ON_NEXT_SAVE, Boolean.TRUE);
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
            if (tempOutcome != null && tempOutcome == REVIEW_TASK_OUTCOME_REJECTED) { // KAAREL: What is tempOutcome and why was it null?
=======
            if (tempOutcome != null && tempOutcome == REVIEW_TASK_OUTCOME_REJECTED) { // What is tempOutcome and why was it null?
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        NodeRef taskParentRef = isTask && workflowTypesByTask.get(((Task) object).getType()).getWorkflowType() != null ? null : getLinkedReviewTaskSpace();
        if (object.isUnsaved() && !(object instanceof LinkedReviewTask)) {
=======
        if (object.isUnsaved()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
        OrganizationStructure organizationStructure = null;
        if (isTask && object.isType(WorkflowSpecificModel.Types.REVIEW_TASK) && reviewToOtherOrgEnabled) {
            object.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_NAME, parametersService.getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT));
            object.setProp(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, dvkService.getInstitutionCode());
            organizationStructure = getOwnerInstitution((Task) object);
            object.setProp(WorkflowSpecificModel.Props.INSTITUTION_NAME, organizationStructure != null ? organizationStructure.getName() : null);
            object.setProp(WorkflowSpecificModel.Props.INSTITUTION_CODE, organizationStructure != null ? organizationStructure.getInstitutionRegCode() : null);
        }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
                workflowDbService.createTaskEntry((Task) object, taskParentRef, getLinkedReviewTaskSpace().equals(taskParentRef));
=======
                workflowDbService.createTaskEntry((Task) object);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
            // removing aspects is not implemented - not needed for now
        } else {
            // Update workflow
            if (!props.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " //
                            + WmNode.toString(props.entrySet()));
                }
<<<<<<< HEAD
                if (props.containsKey(WorkflowCommonModel.Props.OWNER_ID) || props.containsKey(WorkflowCommonModel.Props.OWNER_NAME)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL) || props.containsKey(WorkflowCommonModel.Props.PARALLEL_TASKS)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME) || props.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE)) {
                    if (!(object instanceof CompoundWorkflow && !(object instanceof CompoundWorkflowDefinition))) {
                        requireValue(object, (String) object.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS), WorkflowCommonModel.Props.STATUS, Status.NEW.getName());
                    } else {
                        CompoundWorkflow compoundWorkflow = (CompoundWorkflow) object;
                        if (compoundWorkflow.isDocumentWorkflow()) {
                            requireStatus(object, Status.NEW, Status.FINISHED, Status.STOPPED, Status.UNFINISHED);
                        } else if (compoundWorkflow.isIndependentWorkflow()) {
                            requireStatus(object, Status.NEW, Status.IN_PROGRESS, Status.STOPPED, Status.UNFINISHED);
                        } else {
                            requireStatus(object, Status.NEW);
                        }
                    }
=======

                if (props.containsKey(WorkflowCommonModel.Props.OWNER_ID) || props.containsKey(WorkflowCommonModel.Props.OWNER_NAME)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_EMAIL) || props.containsKey(WorkflowCommonModel.Props.PARALLEL_TASKS)
                        || props.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME) || props.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE)) {
                    // if original status in this process was not "uus" trigger standard functionality to throw workflow exception
                    requireValue(object, (String) object.getOriginalProperties().get(WorkflowCommonModel.Props.STATUS), WorkflowCommonModel.Props.STATUS, Status.NEW.getName());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                }

                if (!isTask) {
                    nodeService.addProperties(node.getNodeRef(), props); // do not replace non-changed properties
                }
                changed = true;
<<<<<<< HEAD
                if (isCompoundWorkflow(object)) {
                    logCompoundWorkflowDataChanged((CompoundWorkflow) object);
                }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                // task update event is not queued as currently no action is required on task update
                // and for large workflows it can impose performance issues (OutOfMemory: java heap space)
                if (!isTask) {
                    queueEvent(queue, WorkflowEventType.UPDATED, object, props);
                }
                // adding/removing aspects is not implemented - not needed for now
            }
            if (isTask) {
<<<<<<< HEAD
                // TODO: Riina - move querying independent tasks root to apprpriate WorkflowType implementations
                // when more than one type of such tasks is created
                // Currently only linkedReviewTasks are created ouside of workflow.
                workflowDbService.updateTaskEntry((Task) object, props, taskParentRef);
=======
                workflowDbService.updateTaskEntry((Task) object, props);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
        }
        object.setChangedProperties(props);
        return changed;
    }

<<<<<<< HEAD
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

    public boolean isCompoundWorkflow(BaseWorkflowObject object) {
        return object instanceof CompoundWorkflow && !(object instanceof CompoundWorkflowDefinition);
    }

    private void logCompoundWorkflowDataChanged(CompoundWorkflow compoundWorkflow) {
        Map<QName, Serializable> changedProps = compoundWorkflow.getChangedProperties();
        Map<QName, Serializable> originalProps = compoundWorkflow.getOriginalProperties();
        String emptyLabel = PropDiffHelper.getEmptyLabel();
        for (Map.Entry<QName, Serializable> entry : changedProps.entrySet()) {
            QName propQName = entry.getKey();
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    public void createDueDateExtension(String reason, Date newDate, Date dueDate, Task initiatingTask, NodeRef containerRef) {
        CompoundWorkflow extensionCompoundWorkflow = getNewCompoundWorkflow(getNewCompoundWorkflowDefinition().getNode(), containerRef);
        extensionCompoundWorkflow.setTypeEnum(initiatingTask.getParent().getParent().getTypeEnum());
        Workflow workflow = getWorkflowService().addNewWorkflow(extensionCompoundWorkflow, WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW,
                extensionCompoundWorkflow.getWorkflows().size(), true);
        if (isWorkflowTitleEnabled()) {
            extensionCompoundWorkflow.setTitle(MessageUtil.getMessage("compoundWorkflow_due_date_extension_title"));
        }
        Task extensionTask = workflow.addTask();
        String creatorName = initiatingTask.getCreatorName();
        extensionTask.setOwnerName(creatorName);
        extensionTask.setOwnerId(initiatingTask.getCreatorId());
        extensionTask.setOwnerEmail(initiatingTask.getCreatorEmail()); // updater
        extensionTask.setCompoundWorkflowId(initiatingTask.getCompoundWorkflowId());
=======
    public void createDueDateExtension(String reason, Date newDate, Date dueDate, Task initiatingTask, NodeRef containerRef, String dueDateExtenderUsername,
            String dueDateExtenderUserFullname) {
        CompoundWorkflow extensionCompoundWorkflow = getNewCompoundWorkflow(getNewCompoundWorkflowDefinition().getNode(), containerRef);
        Workflow workflow = getWorkflowService().addNewWorkflow(extensionCompoundWorkflow, WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW,
                extensionCompoundWorkflow.getWorkflows().size(), true);
        Task extensionTask = workflow.addTask();
        String creatorName = StringUtils.isBlank(dueDateExtenderUserFullname) ? initiatingTask.getCreatorName() : dueDateExtenderUserFullname;
        extensionTask.setOwnerName(creatorName);
        extensionTask.setOwnerId(StringUtils.isBlank(dueDateExtenderUsername) ? initiatingTask.getCreatorId() : dueDateExtenderUsername);
        extensionTask.setOwnerEmail(initiatingTask.getCreatorEmail()); // updater
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        extensionCompoundWorkflow = startCompoundWorkflow(extensionCompoundWorkflow);
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
=======
        extensionCompoundWorkflow = saveCompoundWorkflow(extensionCompoundWorkflow);
        NodeRef dueDateExtensionTask = extensionCompoundWorkflow.getWorkflows().get(0).getTasks().get(0).getNodeRef();
        List<Pair<NodeRef, NodeRef>> dueDateExtensionAssocs = new ArrayList<Pair<NodeRef, NodeRef>>();
        dueDateExtensionAssocs.add(new Pair(initiatingTask.getNodeRef(), dueDateExtensionTask));
        workflowDbService.createTaskDueDateExtensionAssocEntries(dueDateExtensionAssocs);

        BeanHelper.getDocumentLogService().addDocumentLog(extensionCompoundWorkflow.getParent(), MessageUtil.getMessage("document_log_status_workflow"));
        extensionCompoundWorkflow = startCompoundWorkflow(extensionCompoundWorkflow);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public void changeInitiatingTaskDueDate(Task task, WorkflowEventQueue queue) {
<<<<<<< HEAD
        Task initiatingTask = getInitiatingTask(task);
        addDueDateHistoryRecord(initiatingTask, task);
        initiatingTask.setDueDate(task.getConfirmedDueDate());
        initiatingTask.setHasDueDateHistory(true);
        saveTask(queue, initiatingTask);
        NodeRef initiatingCompoundWorkflowNodeRef = generalService.getAncestorNodeRefWithType(initiatingTask.getWorkflowNodeRef(), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        logDueDateExtension(initiatingTask, task.getParent().getParent(), initiatingCompoundWorkflowNodeRef, "applog_compoundWorkflow_due_date_extension_accepted");
    }

    private Task getInitiatingTask(Task extensionTask) {
        List<Task> initiatingTasks = workflowDbService.getDueDateExtensionInitiatingTask(extensionTask.getNodeRef(), taskPrefixedQNames);
=======
        List<Task> initiatingTasks = workflowDbService.getDueDateExtensionInitiatingTask(task.getNodeRef(), taskPrefixedQNames);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        if (initiatingTasks.size() != 1) {
            throw new RuntimeException("dueDateExtension task must have exactly one initiating task; current task has " + initiatingTasks.size() + " initiating tasks.");
        }
        Task initiatingTask = getTask(initiatingTasks.get(0).getNodeRef(), true);
<<<<<<< HEAD
        return initiatingTask;
    }

    @Override
    public void rejectDueDateExtension(Task task) {
        Task initiatingTask = getInitiatingTask(task);
        NodeRef initiatingCompoundWorkflowNodeRef = generalService.getAncestorNodeRefWithType(initiatingTask.getWorkflowNodeRef(), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        logDueDateExtension(initiatingTask, task.getParent().getParent(), initiatingCompoundWorkflowNodeRef, "applog_compoundWorkflow_due_date_extension_rejected");
=======
        addDueDateHistoryRecord(initiatingTask, task);
        initiatingTask.setDueDate(task.getConfirmedDueDate());
        initiatingTask.setHasDueDateHistory(true);
        saveTask(queue, initiatingTask);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @SuppressWarnings("unchecked")
    private void addDueDateHistoryRecord(Task initiatingTask, Task task) {
        String comment = task.getComment();
<<<<<<< HEAD
        workflowDbService.createTaskDueDateHistoryEntries(initiatingTask.getNodeRef(),
                Arrays.asList(new DueDateHistoryRecord(initiatingTask.getNodeRef().getId(), StringUtils.isNotBlank(comment) ? comment : task.getResolution(), initiatingTask
                        .getDueDate(), task.getNodeRef().getId(), null)));
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
=======
        Date previousDueDate = initiatingTask.getDueDate();
        String changeReason = StringUtils.isNotBlank(comment) ? comment : task.getResolution();
        workflowDbService.createTaskDueDateHistoryEntries(Arrays.asList(new Pair<NodeRef, Pair<String, Date>>(initiatingTask.getNodeRef(), new Pair<String, Date>(changeReason,
                previousDueDate))));
        NodeRef initatingTaskRef = initiatingTask.getNodeRef();
        String previousDueDateStr = previousDueDate != null ? Task.dateFormat.format(previousDueDate) : null;
        String confirmedDueDateStr = task.getConfirmedDueDate() != null ? Task.dateFormat.format(task.getConfirmedDueDate()) : null;
        logService.addLogEntry(LogEntry.create(LogObject.TASK, userService, initatingTaskRef, "applog_task_deadline",
                initiatingTask.getOwnerName(), MessageUtil.getTypeName(task.getType()), previousDueDateStr, confirmedDueDateStr, changeReason));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        HashSet<QName> taskDefaultAspects = new HashSet<QName>(taskDataTypeDefaultAspects.get(workflowType.getTaskType()));
=======
        HashSet<QName> taskDefaultAspects = new HashSet<QName>(taskDataTypeDefaultAspects.get(workflowType.getWorkflowType()));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    public void changeTasksDocType(NodeRef docRef, String newTypeId) {
        List<CompoundWorkflow> compoundWorkflows = getCompoundWorkflows(docRef);
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            List<Workflow> workflows = compoundWorkflow.getWorkflows();
            for (Workflow workflow : workflows) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    nodeService.setProperty(task.getNodeRef(), WorkflowCommonModel.Props.DOCUMENT_TYPE, newTypeId);
                }
            }
        }
    }

    @Override
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    public void setCaseFileLogService(CaseFileLogService caseFileLogService) {
        this.caseFileLogService = caseFileLogService;
    }

    // END: getters / setters

    @Override
    public List<QName> getTaskDataTypeSearchableProps() {
        if (taskDataTypeSearchableProps == null) {
            taskDataTypeSearchableProps = new ArrayList<QName>();
            Collection<QName> aspects = RepoUtil.getAspectsIgnoringSystem(generalService.getDefaultAspects(WorkflowSpecificModel.Aspects.SEARCHABLE));
            for (QName aspect : aspects) {
                addPropertyDefs(taskDataTypeSearchableProps, dictionaryService.getPropertyDefs(aspect));
            }
        }
        return taskDataTypeSearchableProps;
    }

    public boolean isFinishDocumentsWhenWorkflowFinishes() {
        return finishDocumentsWhenWorkflowFinishes;
    }

    public void setFinishDocumentsWhenWorkflowFinishes(boolean finishDocumentsWhenWorkflowFinishes) {
        this.finishDocumentsWhenWorkflowFinishes = finishDocumentsWhenWorkflowFinishes;
    }

    /**
     * The type of action performs (after getting confirmation)
     * 
     * @author Vladimir Drozdik
=======
    // END: getters / setters

    /**
     * The type of action performs (after getting confirmation)
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
     */
    public static enum DialogAction {
        SAVING, STARTING, CONTINUING;
    }

<<<<<<< HEAD
}
=======
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
