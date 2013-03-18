package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isGeneratedByDelegation;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.markAsGeneratedByDelegation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.DelegationTaskListGenerator.DelegatableTaskType;

/**
 * Bean that helps to create controls and manage state related to delegating assignment task
 * 
 * @author Ats Uiboupin
 */
public class DelegationBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DelegationBean.class);
    public static final String BEAN_NAME = "DelegationBean";

    /** Index of the assignment task, that could be delegated */
    public static final String ATTRIB_DELEGATABLE_TASK_INDEX = "delegatableTaskIndex";

    /** if this property added to assignmentTask, then web-client will generate responsible assignment task delegation */
    private static final String TMP_GENERATE_RESPONSIBLE_ASSIGNMENT_TASK_DELEGATION = "{temp}delegateAsResponsibleAssignmentTask";
    /** if this property added to assignmentTask, then web-client will generate opinion task delegation */
    private static final String TMP_GENERATE_OPINION_TASK_DELEGATION = "{temp}delegateAsOpinionTask";

    private transient NodeService nodeService;
    private transient OrganizationStructureService organizationStructureService;
    private transient WorkflowService workflowService;
    private transient UserService userService;
    private WorkflowBlockBean workflowBlockBean;
    private final List<Task> delegatableTasks = new ArrayList<Task>();

    /**
     * @param event passed to MethodBinding
     */
    public void addDelegationTask(ActionEvent event) {
        Integer taskIndex = null;
        if (ActionUtil.hasParam(event, TASK_INDEX)) {
            taskIndex = Integer.parseInt(ActionUtil.getParam(event, TASK_INDEX));
        }
        DelegatableTaskType delegateTaskType = DelegatableTaskType.valueOf(ActionUtil.getParam(event, DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE));
        Workflow workflowForNewTask = getWorkflowByAction(event);
        String defaultResolution = null;
        if (ActionUtil.hasParam(event, ATTRIB_DELEGATABLE_TASK_INDEX)) {
            int originalTaskIndex = ActionUtil.getParam(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class);
            defaultResolution = delegatableTasks.get(originalTaskIndex).getResolutionOfTask();
        }
        addDelegationTask(delegateTaskType, workflowForNewTask, taskIndex, defaultResolution);
        updatePanelGroup("addDelegationTask");
    }

    private void addDelegationTask(DelegatableTaskType delegateTaskType, Workflow workflow, Integer taskIndex, String defaultResolution) {
        addDelegationTask(delegateTaskType.isResponsibleTask(), workflow, taskIndex, defaultResolution);
    }

    public void addDelegationTask(boolean hasResponsibleAspect, Workflow workflow, Integer taskIndex, String defaultResolution) {
        Task task = taskIndex != null ? workflow.addTask(taskIndex) : workflow.addTask();
        markAsGeneratedByDelegation(task);
        task.setParallel(true);
        if (hasResponsibleAspect) {
            task.setResponsible(true);
            task.setActive(true);
        }
        if (workflow.hasTaskResolution()) {
            task.setResolution(defaultResolution);
        }
    }

    private void updatePanelGroup(String action) {
        workflowBlockBean.constructTaskPanelGroup(action);
    }

    public void removeDelegationTask(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        workflow.removeTask(taskIndex);
        updatePanelGroup("removeDelegationTask");
    }

    public void resetDelegationTask(ActionEvent event) {
        Task task = getTaskByActionParams(event);
        Map<String, Object> props = task.getNode().getProperties();
        props.put(WorkflowCommonModel.Props.OWNER_NAME.toString(), null);
        props.put(WorkflowSpecificModel.Props.RESOLUTION.toString(), null);
        props.put(WorkflowSpecificModel.Props.DUE_DATE.toString(), null);
    }

    /**
     * @param assignmentTask
     * @return pair(delegatableTaskIndex, delegatableTask). delegatableTask == assignmentTask when task with the same noderef hasn't been added yet
     */
    public Pair<Integer, Task> initDelegatableTask(Task assignmentTask) {
        NodeRef delegatableTaskRef = assignmentTask.getNodeRef();
        int delegatableTaskIndex = 0;
        for (Task t : delegatableTasks) {
            if (delegatableTaskRef.equals(t.getNodeRef())) {
                // don't add new delegatable task if this is yet another clone of existing task or this method was called after update.
                return new Pair<Integer, Task>(delegatableTaskIndex, t);
            }
            delegatableTaskIndex++;
        }
        delegatableTasks.add(assignmentTask);
        delegatableTaskIndex = delegatableTasks.size() - 1;
        Workflow workflow = assignmentTask.getParent();
        { // by default there should be one empty responsible assignment task and one empty non-responsible assignment task for delegating
            String resolutionOfTask = assignmentTask.getResolutionOfTask();
            if (assignmentTask.isResponsible()) {
                assignmentTask.getNode().getProperties().put(TMP_GENERATE_RESPONSIBLE_ASSIGNMENT_TASK_DELEGATION, Boolean.TRUE);
                addDelegationTask(true, workflow, null, resolutionOfTask);
            } else {
                addDelegationTask(false, workflow, null, resolutionOfTask);
            }
        }
        // create information and opinion workflows under the compoundWorkflow of the assignment task in case user adds corresponding task.
        // If no tasks are added to following workflow, then that workflows is not saved when saving compound workflow
        NodeRef docRef = assignmentTask.getParent().getParent().getParent();
        String docStatus = (String) getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS);
        if (!DocumentStatus.FINISHED.getValueName().equals(docStatus)) {
            Node taskNode = assignmentTask.getNode();
            taskNode.getProperties().put(TMP_GENERATE_OPINION_TASK_DELEGATION, Boolean.TRUE);
            getOrCreateWorkflow(workflow, DelegatableTaskType.OPINION);
        }
        getOrCreateWorkflow(workflow, DelegatableTaskType.INFORMATION);
        return new Pair<Integer, Task>(delegatableTaskIndex, assignmentTask);
    }

    public void reset() {
        delegatableTasks.clear();
    }

    private Workflow getWorkflowByAction(ActionEvent event) {
        UIComponent component = event.getComponent();
        DelegatableTaskType dTaskType;
        Workflow originalTaskWorkflow;
        if (component instanceof UIActionLink && ActionUtil.hasParam(event, ATTRIB_DELEGATABLE_TASK_INDEX)) {
            originalTaskWorkflow = getWorkflowByOriginalTask(ActionUtil.getParam(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class));
            dTaskType = DelegatableTaskType.valueOf(ActionUtil.getParam(event, DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE));
        } else { // when component is for example picker
            Map<String, Object> attributes = ComponentUtil.getAttributes(component);
            int delegatableTaskIndex = (Integer) attributes.get(ATTRIB_DELEGATABLE_TASK_INDEX);
            originalTaskWorkflow = getWorkflowByOriginalTask(delegatableTaskIndex);
            dTaskType = (DelegatableTaskType) attributes.get(DelegationTaskListGenerator.ATTRIB_DELEGATE_TASK_TYPE);
        }
        return dTaskType.isOrderAssignmentOrAssignmentWorkflow() ? originalTaskWorkflow : getOrCreateWorkflow(originalTaskWorkflow, dTaskType);
    }

    private Workflow getOrCreateWorkflow(Workflow originalTaskWorkflow, DelegatableTaskType dTaskType) {
        boolean isInformation = DelegatableTaskType.INFORMATION.equals(dTaskType);
        if (!isInformation && !DelegatableTaskType.OPINION.equals(dTaskType)) {
            throw new IllegalStateException("Unknown DelegatableTaskType=" + dTaskType);
        }
        CompoundWorkflow compoundWorkflow = originalTaskWorkflow.getParent();
        QName workflowTypeQName = dTaskType.getWorkflowTypeQName();
        int lastInprogressWfIndex = 0;
        int i = 0;
        for (Workflow otherWorkflow : compoundWorkflow.getWorkflows()) {
            boolean isGeneratedByDelegation = isGeneratedByDelegation(otherWorkflow);
            if (isGeneratedByDelegation) {
                if (workflowTypeQName.equals(otherWorkflow.getType())) {
                    return otherWorkflow;
                }
            }
            if (WorkflowUtil.isStatus(otherWorkflow, Status.IN_PROGRESS)) {
                lastInprogressWfIndex = i;
            }
            i++;
        }
        Workflow newWorkflow = getWorkflowService().addNewWorkflow(compoundWorkflow, workflowTypeQName, lastInprogressWfIndex + 1, false);
        markAsGeneratedByDelegation(newWorkflow);
        return newWorkflow;
    }

    private Task getTaskByActionParams(ActionEvent event) {
        Workflow workflow = getWorkflowByAction(event);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        return workflow.getTasks().get(taskIndex);
    }

    private Workflow getWorkflowByOriginalTask(int delegatableTaskIndex) {
        return delegatableTasks.get(delegatableTaskIndex).getParent();
    }

    /**
     * If <code>!dTaskType.isAssignmentWorkflow()</code> then result should be equivalent to
     * <code>getNewWorkflowTasksFetchers().get(delegatableTaskIndex).getNonAssignmentTasksByType().get(dTaskType.name());</code><br>
     * that is slower, but used for value binding of non-assignment tasks.
     * 
     * @param delegatableTaskIndex
     * @param dTaskType
     * @return tasks of type <code>dTaskType</code> that should be displayed for delegating when showing assigment task <code>delegatableTaskIndex</code>
     */
    public List<Task> getTasks(int delegatableTaskIndex, DelegatableTaskType dTaskType) {
        Workflow workflow = getWorkflowByOriginalTask(delegatableTaskIndex);
        List<Task> tasks;
        if (dTaskType.isOrderAssignmentOrAssignmentWorkflow()) {
            tasks = workflow.getTasks();
        } else {
            tasks = getNonAssignmentTasks(dTaskType, workflow.getParent().getWorkflows());
        }
        return tasks;
    }

    /**
     * Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()}
     * 
     * @throws Exception
     */
    public void delegate(ActionEvent event) throws Exception {
        Task originalTask = delegatableTasks.get(ActionUtil.getParam(event, ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class));
        if (originalTask.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            originalTask.setAction(Action.FINISH);
        }
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            MessageDataWrapper feedback = getWorkflowService().delegate(originalTask);
            MessageUtil.addStatusMessages(context, feedback);
            if (!feedback.hasErrors()) {
                workflowBlockBean.restore("delegate");
                MessageUtil.addInfoMessage("delegated_successfully_" + originalTask.getType().getLocalName());
                BeanHelper.getDocumentDynamicDialog().switchMode(false); // document metadata might have changed (for example owner)
            }
        } catch (UnableToPerformMultiReasonException e) {
            MessageUtil.addStatusMessages(context, e.getMessageDataWrapper());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(context, e);
        } catch (NodeLockedException e) {
            LOG.debug("Compound workflow action failed: document locked!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_docLocked");
        } catch (WorkflowChangedException e) {
            CompoundWorkflowDialog.handleException(e, null);
        } catch (Exception e) {
            LOG.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(context, null);
        }
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()} */
    public void processResponsibleOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        if (filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER) {
            filterIndex = UserContactGroupSearchBean.CONTACTS_FILTER;
        }
        processOwnerSearchResults(event, filterIndex);
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#setPickerBindings()} */
    public void processOwnerSearchResults(ActionEvent event) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();
        int filterIndex = picker.getFilterIndex();
        processOwnerSearchResults(event, filterIndex);
    }

    /** Used for JSF binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} */
    public List<Task> getDelegatableTasks() {
        return delegatableTasks;
    }

    /**
     * Used for value binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} <br>
     * (key must be Long, as numeric map keys are converted to Long when resolving ValueBinding)
     */
    public Map<Long/* delegatableTaskIndex */, NewWorkflowTasksFetcher> getNewWorkflowTasksFetchers() {
        Map<Long, DelegationBean.NewWorkflowTasksFetcher> wfWrappersByDelegatableTaskIndex = new LinkedHashMap<Long, DelegationBean.NewWorkflowTasksFetcher>();
        for (long delegatableTaskIndex = 0; delegatableTaskIndex < delegatableTasks.size(); delegatableTaskIndex++) {
            wfWrappersByDelegatableTaskIndex.put(delegatableTaskIndex, new NewWorkflowTasksFetcher((int) delegatableTaskIndex));
        }
        return wfWrappersByDelegatableTaskIndex;
    }

    public class NewWorkflowTasksFetcher {
        private final int delegatableTaskIndex;

        public NewWorkflowTasksFetcher(int delegatableTaskIndex) {
            this.delegatableTaskIndex = delegatableTaskIndex;
        }

        /**
         * Used for value binding in {@link DelegationTaskListGenerator#createTaskPropValueBinding()} <br>
         * (key must be Long, as numeric map keys are converted to Long when resolving ValueBinding)
         */
        public Map<String/* delegatableTaskType.name() */, List<Task>> getNonAssignmentTasksByType() {
            List<Workflow> workflows = getWorkflow().getParent().getWorkflows();
            HashMap<String, List<Task>> results = new HashMap<String, List<Task>>();
            results.put(DelegatableTaskType.INFORMATION.name(), getNonAssignmentTasks(DelegatableTaskType.INFORMATION, workflows));
            results.put(DelegatableTaskType.OPINION.name(), getNonAssignmentTasks(DelegatableTaskType.OPINION, workflows));
            return results;
        }

        public Map<String/* delegatableTaskType.name() */, Workflow> getNonAssignmentWorkflowsByType() {
            List<Workflow> workflows = getWorkflow().getParent().getWorkflows();
            HashMap<String, Workflow> results = new HashMap<String, Workflow>();
            for (Workflow otherWorkflow : workflows) {
                if (isGeneratedByDelegation(otherWorkflow)) {
                    if (DelegatableTaskType.INFORMATION.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                        results.put(DelegatableTaskType.INFORMATION.name(), otherWorkflow);
                    } else if (DelegatableTaskType.OPINION.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                        results.put(DelegatableTaskType.OPINION.name(), otherWorkflow);
                    }
                }
            }
            return results;
        }

        /**
         * Used for value binding in {@link DelegationTaskListGenerator#createWorkflowPropValueBinding(DelegatableTaskType, int, QName, javax.faces.application.Application)}
         */
        private Workflow getWorkflow() {
            return getWorkflowByOriginalTask(delegatableTaskIndex);
        }
    }

    private List<Task> getNonAssignmentTasks(DelegatableTaskType dTaskType, List<Workflow> workflows) {
        for (Workflow otherWorkflow : workflows) {
            if (isGeneratedByDelegation(otherWorkflow) && dTaskType.getWorkflowTypeQName().equals(otherWorkflow.getType())) {
                return otherWorkflow.getTasks();
            }
        }
        return Collections.<Task> emptyList();
    }

    private void processOwnerSearchResults(ActionEvent event, int filterIndex) {
        UIGenericPicker picker = (UIGenericPicker) event.getComponent();

        int taskIndex = Integer.parseInt((String) picker.getAttributes().get(Search.OPEN_DIALOG_KEY));
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        Workflow workflow = getWorkflowByAction(event);
        for (String result : results) {
            // users
            if (filterIndex == UserContactGroupSearchBean.USERS_FILTER) {
                setPersonPropsToTask(workflow, taskIndex, result);
            }
            // user groups
            else if (filterIndex == UserContactGroupSearchBean.USER_GROUPS_FILTER) {
                Set<String> children = BeanHelper.getUserService().getUserNamesInGroup(result);
                int j = 0;
                Task task = workflow.getTasks().get(taskIndex);
                DelegatableTaskType delegateTaskType = DelegatableTaskType.getTypeByTask(task);
                String resolution = task.getResolution();
                for (String userName : children) {
                    if (j > 0) {
                        addDelegationTask(delegateTaskType, workflow, ++taskIndex, resolution);
                    }
                    setPersonPropsToTask(workflow, taskIndex, userName);
                    j++;
                }
            }
            // contacts
            else if (filterIndex == UserContactGroupSearchBean.CONTACTS_FILTER) {
                setContactPropsToTask(workflow, taskIndex, new NodeRef(result));
            }
            // contact groups
            else if (filterIndex == UserContactGroupSearchBean.CONTACT_GROUPS_FILTER) {
                List<NodeRef> contacts = BeanHelper.getAddressbookService().getContactGroupContents(new NodeRef(result));
                taskIndex = addContactGroupTasks(taskIndex, workflow, contacts);
            } else {
                throw new RuntimeException("Unknown filter index value: " + filterIndex);
            }
        }
        updatePanelGroup("processOwnerSearchResults");
    }

    private void setPersonPropsToTask(Workflow workflow, int taskIndex, String userName) {
        Map<QName, Serializable> resultProps = getUserService().getUserProperties(userName);
        String name = UserUtil.getPersonFullName1(resultProps);
        Serializable id = resultProps.get(ContentModel.PROP_USERNAME);
        Serializable email = resultProps.get(ContentModel.PROP_EMAIL);
        Serializable orgName = (Serializable) getOrganizationStructureService().getOrganizationStructurePaths((String) resultProps.get(ContentModel.PROP_ORGID));
        Serializable jobTitle = resultProps.get(ContentModel.PROP_JOBTITLE);
        setPropsToTask(workflow, taskIndex, name, id, email, orgName, jobTitle);
    }

    private void setPropsToTask(Workflow workflow, int taskIndex, String name, Serializable id, Serializable email, Serializable orgName,
            Serializable jobTitle) {
        Task task = workflow.getTasks().get(taskIndex);
        task.setOwnerName(name);
        task.setOwnerId((String) id);
        task.setOwnerEmail((String) email);
        Map<String, Object> props = task.getNode().getProperties();
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), orgName);
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE.toString(), jobTitle);
    }

    private void setContactPropsToTask(Workflow block, int index, NodeRef contact) {
        Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
        QName resultType = getNodeService().getType(contact);

        String name = null;
        if (resultType.equals(Types.ORGANIZATION)) {
            name = (String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME) //
                    , (String) resultProps.get(AddressbookModel.Props.PERSON_LAST_NAME));
        }
        setPropsToTask(block, index, name, null, resultProps.get(AddressbookModel.Props.EMAIL), null, null);
    }

    public int addContactGroupTasks(int taskIndex, Workflow block, List<NodeRef> contacts) {
        int taskCounter = 0;
        Task task = block.getTasks().get(taskIndex);
        DelegatableTaskType delegateTaskType = DelegatableTaskType.getTypeByTask(task);
        String resolution = task.getResolution();
        for (int j = 0; j < contacts.size(); j++) {
            Map<QName, Serializable> contactProps = getNodeService().getProperties(contacts.get(j));
            if (getNodeService().hasAspect(contacts.get(j), AddressbookModel.Aspects.ORGANIZATION_PROPERTIES)
                    && Boolean.TRUE.equals(contactProps.get(AddressbookModel.Props.TASK_CAPABLE))) {
                if (taskCounter > 0) {
                    addDelegationTask(delegateTaskType, block, ++taskIndex, resolution);
                }
                setContactPropsToTask(block, taskIndex, contacts.get(j));
                taskCounter++;
            }
        }
        return taskIndex;
    }

    private NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
    }

    private OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    private WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setWorkflowBlockBean(WorkflowBlockBean workflowBlockBean) {
        this.workflowBlockBean = workflowBlockBean;
    }

}
