package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstitutionBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowConstantsBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class MyTasksBean extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MyTasksBean.class);
    private static final int PAGE_SIZE = 10;

    public static final String BEAN_NAME = "MyTasksBean";
    public static final String LIST_ASSIGNMENT = "assignment";
    public static final String LIST_ORDER_ASSIGNMENT = "orderAssignment";
    public static final String LIST_GROUP_ASSIGNMENT = "groupAssignment";
    public static final String LIST_INFORMATION = "information";
    public static final String LIST_OPINION = "opinion";
    public static final String LIST_REVIEW = "review";
    public static final String LIST_SIGNATURE = "signature";
    public static final String LIST_EXTERNAL_REVIEW = "externalReview";
    public static final String LIST_LINKED_REVIEW = "linkedReview";
    public static final String LIST_CONFIRMATION = "confirmation";
    public static final String TASK_LIST_DIALOG = "dialog:taskListDialog";

    private String dialogTitle;
    private String listTitle;
    private boolean lessColumns = true;
    private String specificList;
    private TaskAndDocumentDataProvider assignmentTasks;
    private TaskAndDocumentDataProvider orderAssignmentTasks;
    private TaskAndDocumentDataProvider groupAssignmentTasks;
    private TaskAndDocumentDataProvider informationTasks;
    private TaskAndDocumentDataProvider opinionTasks;
    private TaskAndDocumentDataProvider reviewTasks;
    private TaskAndDocumentDataProvider signatureTasks;
    private TaskAndDocumentDataProvider externalReviewTasks;
    private TaskAndDocumentDataProvider linkedReviewTasks;
    private TaskAndDocumentDataProvider confirmationTasks;
    private TaskAndDocumentDataProvider additionalTasks;
    private long lastLoadMillis = 0;
    private boolean containsAdditionalTasks = false;

    private transient ParametersService parametersService;
    private transient DocumentService documentService;
    private transient DocumentSearchService documentSearchService;
    private String additionalListTitle;
    private boolean hidePrimaryList;

    // START: dialog overrides

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // nothing to do here
        return null;
    }

    @Override
    public void restored() {
        loadTasksWithoutOverdueCondition();
        super.restored();
    }

    @Override
    public String getContainerTitle() {
        return getDialogTitle();
    }

    // END: dialog overrides

    // START: dialog setup

    public String getSetupMyTasks() {
        // see comment in /web/jsp/dashboards/container.jsp before <h:outputText value="#{MyTasksBean.setupMyTasks}" /> (line 72)
        boolean forceReload = getSubstitutionBean().getForceSubstituteTaskReload();
        if (forceReload || (System.currentTimeMillis() - lastLoadMillis) > 30000) { // 30 seconds
            reset();
            dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
            loadTasks(true);
            lastLoadMillis = System.currentTimeMillis();
        }
        if (forceReload) {
            getSubstitutionBean().setForceSubstituteTaskReload(false);
        }
        return null;
    }

    @Override
    public void clean() {
        reset();
    }

    public void setupAssignmentTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("assignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_assignment_title");
        lessColumns = false;
        specificList = LIST_GROUP_ASSIGNMENT;
        loadTasksWithoutOverdueCondition();
        additionalTasks = groupAssignmentTasks;
        additionalListTitle = MessageUtil.getMessage("task_list_group_assignment_title");
        containsAdditionalTasks = additionalTasks != null && additionalTasks.getListSize() > 0;
        specificList = LIST_ASSIGNMENT;
        loadTasksWithoutOverdueCondition();
    }

    public void setupOrderAssignmentTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("orderAssignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_order_assignment_title");
        lessColumns = false;
        specificList = LIST_ORDER_ASSIGNMENT;
        loadTasksWithoutOverdueCondition();
    }

    public void setupInformationTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("informationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_information_title");
        lessColumns = false;
        specificList = LIST_INFORMATION;
        loadTasksWithoutOverdueCondition();
    }

    public void setupOpinionTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("opinionWorkflow");
        listTitle = MessageUtil.getMessage("task_list_opinion_title");
        lessColumns = false;
        specificList = LIST_OPINION;
        loadTasksWithoutOverdueCondition();
    }

    public void setupReviewTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("reviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_review_title");
        lessColumns = true;
        specificList = LIST_REVIEW;
        loadTasksWithoutOverdueCondition();
    }

    public void setupSignatureTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("signatureWorkflow");
        listTitle = MessageUtil.getMessage("task_list_signature_title");
        lessColumns = true;
        specificList = LIST_SIGNATURE;
        loadTasksWithoutOverdueCondition();
    }

    public void setupExternalReviewTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("externalReviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_external_review_title");
        lessColumns = true;
        if (getWorkflowConstantsBean().isExternalReviewWorkflowEnabled()) {
            specificList = LIST_EXTERNAL_REVIEW;
            loadTasksWithoutOverdueCondition();
        } else {
            hidePrimaryList = true;
        }
        if (getWorkflowConstantsBean().isReviewToOtherOrgEnabled()) {
            specificList = LIST_LINKED_REVIEW;
            loadTasksWithoutOverdueCondition();
        }
    }

    public void setupConfirmationTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("confirmationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_confirmation_title");
        lessColumns = false;
        specificList = LIST_CONFIRMATION;
        loadTasksWithoutOverdueCondition();
    }

    // END: dialog setup

    public TaskAndDocumentDataProvider getTasks() {
        TaskAndDocumentDataProvider result = new TaskAndDocumentDataProvider(new ArrayList<NodeRef>());
        if (LIST_ASSIGNMENT.equals(specificList)) {
            result = assignmentTasks;
        } else if (LIST_ORDER_ASSIGNMENT.equals(specificList)) {
            result = orderAssignmentTasks;
        } else if (LIST_GROUP_ASSIGNMENT.equals(specificList)) {
            result = groupAssignmentTasks;
        } else if (LIST_INFORMATION.equals(specificList)) {
            result = informationTasks;
        } else if (LIST_OPINION.equals(specificList)) {
            result = opinionTasks;
        } else if (LIST_REVIEW.equals(specificList)) {
            result = reviewTasks;
        } else if (LIST_SIGNATURE.equals(specificList)) {
            result = signatureTasks;
        } else if (LIST_EXTERNAL_REVIEW.equals(specificList)) {
            result = externalReviewTasks;
        } else if (LIST_CONFIRMATION.equals(specificList)) {
            result = confirmationTasks;
        }
        return result;
    }

    public TaskAndDocumentDataProvider getAdditionalTasks() {
        return additionalTasks;
    }

    public boolean isContainsAdditionalTasks() {
        return containsAdditionalTasks;
    }

    public TaskAndDocumentDataProvider getLinkedReviewTasks() {
        return linkedReviewTasks;
    }

    public TaskAndDocumentDataProvider getAssignmentTasks() {
        return assignmentTasks;
    }

    public TaskAndDocumentDataProvider getOrderAssignmentTasks() {
        return orderAssignmentTasks;
    }

    public TaskAndDocumentDataProvider getGroupAssignmentTasks() {
        return groupAssignmentTasks;
    }

    public TaskAndDocumentDataProvider getConfirmationTasks() {
        return confirmationTasks;
    }

    public TaskAndDocumentDataProvider getInformationTasks() {
        return informationTasks;
    }

    public TaskAndDocumentDataProvider getOpinionTasks() {
        return opinionTasks;
    }

    public TaskAndDocumentDataProvider getReviewTasks() {
        return reviewTasks;
    }

    public TaskAndDocumentDataProvider getSignatureTasks() {
        return signatureTasks;
    }

    public TaskAndDocumentDataProvider getExternalReviewTasks() {
        return externalReviewTasks;
    }

    public boolean isAssignmentPagerVisible() {
        return getAssignmentTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isGroupAssignmentPagerVisible() {
        return getGroupAssignmentTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isOrderAssignmentPagerVisible() {
        return getOrderAssignmentTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isConfirmationPagerVisible() {
        return getConfirmationTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isInformationPagerVisible() {
        return getInformationTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isSignaturePagerVisible() {

        return getSignatureTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isReviewPagerVisible() {
        return getReviewTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isOpinionPagerVisible() {
        return getOpinionTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isExternalReviewPagerVisible() {
        return getExternalReviewTasks().getListSize() > PAGE_SIZE;
    }

    public boolean isTitlebarRendered() {
        return getContainerTitle().length() > 0;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public boolean isLessColumns() {
        return lessColumns;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public String getListTitle() {
        return listTitle;
    }

    public String getAdditionalListTitle() {
        return additionalListTitle;
    }

    public boolean isCaseFileOrDocumentWorkflowEnabled() {
        return BeanHelper.getApplicationConstantsBean().isCaseVolumeEnabled() || isDocumentWorkflowEnabled();
    }

    public boolean isDocumentWorkflowEnabled() {
        return getWorkflowConstantsBean().isDocumentWorkflowEnabled();
    }

    public boolean isLinkedReviewTaskEnabled() {
        return linkedReviewTasks != null && getWorkflowConstantsBean().isReviewToOtherOrgEnabled();
    }

    public boolean isHidePrimaryList() {
        return hidePrimaryList;
    }

    // START: getters/setters

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    // END: getters/setters

    // START: PRIVATE METHODS

    private void reset() {
        dialogTitle = null;
        listTitle = null;
        additionalListTitle = null;
        lessColumns = true;
        specificList = null;
        assignmentTasks = null;
        orderAssignmentTasks = null;
        groupAssignmentTasks = null;
        informationTasks = null;
        opinionTasks = null;
        reviewTasks = null;
        externalReviewTasks = null;
        signatureTasks = null;
        externalReviewTasks = null;
        linkedReviewTasks = null;
        confirmationTasks = null;
        additionalTasks = null;
        lastLoadMillis = 0;
        hidePrimaryList = false;
        containsAdditionalTasks = false;
    }

    protected void loadTasksWithoutOverdueCondition() {
        loadTasks(false);
    }

    private void loadTasks(boolean onlyOverdueOrToday) {
        long startA = System.currentTimeMillis();
        log.debug("loadTasks - START");
        List<QName> tasksToLoad = new ArrayList<>();
        if (specificList == null || LIST_ASSIGNMENT.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
        }
        if ((specificList == null || LIST_ORDER_ASSIGNMENT.equals(specificList)) && getWorkflowConstantsBean().isOrderAssignmentWorkflowEnabled()) {
            tasksToLoad.add(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        }
        if (specificList == null || LIST_GROUP_ASSIGNMENT.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK);
        }
        if (specificList == null || LIST_INFORMATION.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.INFORMATION_TASK);
        }
        if (specificList == null || LIST_OPINION.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.OPINION_TASK);
        }
        if (specificList == null || LIST_REVIEW.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.REVIEW_TASK);
        }
        if (specificList == null || LIST_SIGNATURE.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.SIGNATURE_TASK);
        }
        if (specificList == null || LIST_EXTERNAL_REVIEW.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK);
        }
        if (specificList == null || LIST_LINKED_REVIEW.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK);
        }
        if (specificList == null || LIST_CONFIRMATION.equals(specificList)) {
            tasksToLoad.add(WorkflowSpecificModel.Types.CONFIRMATION_TASK);
            tasksToLoad.add(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK);
        }

        List<Pair<NodeRef, QName>> tasks = getDocumentSearchService().searchCurrentUsersInProgressTaskRefs(onlyOverdueOrToday, tasksToLoad.toArray(new QName[tasksToLoad.size()]));
        Map<QName, List<NodeRef>> taskLists = new HashMap<>();
        for (QName taskType : tasksToLoad) {
            if (!WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(taskType)) {
                taskLists.put(taskType, new ArrayList<NodeRef>());
            }
        }
        for (Pair<NodeRef, QName> taskAndTpe : tasks) {
            QName taskType = taskAndTpe.getSecond();
            if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(taskType)) {
                taskType = WorkflowSpecificModel.Types.CONFIRMATION_TASK;
            }
            taskLists.get(taskType).add(taskAndTpe.getFirst());
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            assignmentTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.ASSIGNMENT_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
            orderAssignmentTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK)) {
            groupAssignmentTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
            informationTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.INFORMATION_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.OPINION_TASK)) {
            opinionTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.OPINION_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.REVIEW_TASK)) {
            reviewTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.REVIEW_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK)) {
            linkedReviewTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
            signatureTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.SIGNATURE_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
            externalReviewTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK));
        }
        if (tasksToLoad.contains(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
            confirmationTasks = new TaskAndDocumentDataProvider(taskLists.get(WorkflowSpecificModel.Types.CONFIRMATION_TASK));
        }
        log.debug("loadTasks - END: " + (System.currentTimeMillis() - startA) + "ms");
    }
    // END: PRIVATE METHODS

}
