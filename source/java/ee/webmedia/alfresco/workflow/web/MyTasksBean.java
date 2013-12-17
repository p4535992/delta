package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstitutionBean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Kaarel Jõgeva
 */
public class MyTasksBean extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MyTasksBean.class);
    private static final int PAGE_SIZE = 10;

    public static final String BEAN_NAME = "MyTasksBean";
    public static final String LIST_ASSIGNMENT = "assignment";
    public static final String LIST_ORDER_ASSIGNMENT = "orderAssignment";
    public static final String LIST_INFORMATION = "information";
    public static final String LIST_OPINION = "opinion";
    public static final String LIST_REVIEW = "review";
    public static final String LIST_SIGNATURE = "signature";
    public static final String LIST_EXTERNAL_REVIEW = "externalReview";
    public static final String LIST_CONFIRMATION = "confirmation";

    private String dialogTitle;
    private String listTitle;
    private boolean lessColumns = true;
    private String specificList;
    private List<TaskAndDocument> assignmentTasks;
    private List<TaskAndDocument> orderAssignmentTasks;
    private List<TaskAndDocument> informationTasks;
    private List<TaskAndDocument> opinionTasks;
    private List<TaskAndDocument> reviewTasks;
    private List<TaskAndDocument> signatureTasks;
    private List<TaskAndDocument> externalReviewTasks;
    private List<TaskAndDocument> confirmationTasks;
    private long lastLoadMillis = 0;

    private transient ParametersService parametersService;
    private transient DocumentService documentService;
    private transient DocumentSearchService documentSearchService;

    // START: dialog overrides

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // nothing to do here
        return null;
    }

    @Override
    public void restored() {
        loadTasks();
        super.restored();
    }

    @Override
    public String getContainerTitle() {
        return getDialogTitle();
    }

    // END: dialog overrides

    // START: dialog setup

    public String getSetupMyTasks() {
        // see Erko's comment in /web/jsp/dashboards/container.jsp before <h:outputText value="#{MyTasksBean.setupMyTasks}" /> (line 72)
        boolean forceReload = getSubstitutionBean().getForceSubstituteTaskReload();
        if (forceReload || (System.currentTimeMillis() - lastLoadMillis) > 30000) { // 30 seconds
            reset();
            dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
            loadTasks();
            lastLoadMillis = System.currentTimeMillis();
        }
        if (forceReload) {
            getSubstitutionBean().setForceSubstituteTaskReload(false);
        }
        return null;
    }

    public void setupAssignmentTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("assignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_assignment_title");
        lessColumns = false;
        specificList = LIST_ASSIGNMENT;
        loadTasks();
    }

    public void setupOrderAssignmentTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("orderAssignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_order_assignment_title");
        lessColumns = false;
        specificList = LIST_ORDER_ASSIGNMENT;
        loadTasks();
    }

    public void setupInformationTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("informationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_information_title");
        lessColumns = false;
        specificList = LIST_INFORMATION;
        loadTasks();
    }

    public void setupOpinionTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("opinionWorkflow");
        listTitle = MessageUtil.getMessage("task_list_opinion_title");
        lessColumns = false;
        specificList = LIST_OPINION;
        loadTasks();
    }

    public void setupReviewTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("reviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_review_title");
        lessColumns = true;
        specificList = LIST_REVIEW;
        loadTasks();
    }

    public void setupSignatureTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("signatureWorkflow");
        listTitle = MessageUtil.getMessage("task_list_signature_title");
        lessColumns = true;
        specificList = LIST_SIGNATURE;
        loadTasks();
    }

    public void setupExternalReviewTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("externalReviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_external_review_title");
        lessColumns = true;
        specificList = LIST_EXTERNAL_REVIEW;
        loadTasks();
    }

    public void setupConfirmationTasks(@SuppressWarnings("unused") ActionEvent event) {
        reset();
        dialogTitle = MessageUtil.getMessage("confirmationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_confirmation_title");
        lessColumns = false;
        specificList = LIST_CONFIRMATION;
        loadTasks();
    }

    // END: dialog setup

    public List<TaskAndDocument> getTasks() {
        List<TaskAndDocument> result = new ArrayList<TaskAndDocument>();
        if (LIST_ASSIGNMENT.equals(specificList)) {
            result = assignmentTasks;
        } else if (LIST_ORDER_ASSIGNMENT.equals(specificList)) {
            result = orderAssignmentTasks;
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

    public List<TaskAndDocument> getAssignmentTasks() {
        return filterTasksByDate(assignmentTasks);
    }

    public List<TaskAndDocument> getOrderAssignmentTasks() {
        return filterTasksByDate(orderAssignmentTasks);
    }

    public List<TaskAndDocument> getConfirmationTasks() {
        return filterTasksByDate(confirmationTasks);
    }

    public List<TaskAndDocument> getInformationTasks() {
        return filterTasksByDate(informationTasks);
    }

    public List<TaskAndDocument> getOpinionTasks() {
        return filterTasksByDate(opinionTasks);
    }

    public List<TaskAndDocument> getReviewTasks() {
        return filterTasksByDate(reviewTasks);
    }

    public List<TaskAndDocument> getSignatureTasks() {
        return filterTasksByDate(signatureTasks);
    }

    public List<TaskAndDocument> getExternalReviewTasks() {
        return filterTasksByDate(externalReviewTasks);
    }

    public boolean isAssignmentPagerVisible() {
        return getAssignmentTasks().size() > PAGE_SIZE;
    }

    public boolean isOrderAssignmentPagerVisible() {
        return getOrderAssignmentTasks().size() > PAGE_SIZE;
    }

    public boolean isConfirmationPagerVisible() {
        return getConfirmationTasks().size() > PAGE_SIZE;
    }

    public boolean isInformationPagerVisible() {
        return getInformationTasks().size() > PAGE_SIZE;
    }

    public boolean isSignaturePagerVisible() {

        return getSignatureTasks().size() > PAGE_SIZE;
    }

    public boolean isReviewPagerVisible() {
        return getReviewTasks().size() > PAGE_SIZE;
    }

    public boolean isOpinionPagerVisible() {
        return getOpinionTasks().size() > PAGE_SIZE;
    }

    public boolean isExternalReviewPagerVisible() {
        return getExternalReviewTasks().size() > PAGE_SIZE;
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
        lessColumns = true;
        specificList = null;
        assignmentTasks = null;
        informationTasks = null;
        opinionTasks = null;
        reviewTasks = null;
        signatureTasks = null;
        lastLoadMillis = 0;
    }

    private List<TaskAndDocument> filterTasksByDate(List<TaskAndDocument> tasks) {
        Date today = Calendar.getInstance().getTime();

        List<TaskAndDocument> filteredTasks = new ArrayList<TaskAndDocument>(tasks.size());
        for (TaskAndDocument task : tasks) {
            final Date dueDate = task.getTask().getDueDate();
            if (dueDate != null && (dueDate.before(today) || DateUtils.isSameDay(dueDate, today))) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    private void loadTasks() {
        long startA = System.currentTimeMillis();
        log.debug("loadTasks - START");
        if (specificList == null || LIST_ASSIGNMENT.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.ASSIGNMENT_TASK);
            long startC = System.currentTimeMillis();
            assignmentTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - assignmentTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_ORDER_ASSIGNMENT.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
            long startC = System.currentTimeMillis();
            orderAssignmentTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - orderAssignmentTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_INFORMATION.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.INFORMATION_TASK);
            long startC = System.currentTimeMillis();
            informationTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - informationTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_OPINION.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.OPINION_TASK);
            long startC = System.currentTimeMillis();
            opinionTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - opinionTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_REVIEW.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.REVIEW_TASK);
            long startC = System.currentTimeMillis();
            reviewTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - reviewTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_SIGNATURE.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.SIGNATURE_TASK);
            long startC = System.currentTimeMillis();
            signatureTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - signatureTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_EXTERNAL_REVIEW.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK);
            long startC = System.currentTimeMillis();
            externalReviewTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - externalReviewTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        if (specificList == null || LIST_CONFIRMATION.equals(specificList)) {
            long startB = System.currentTimeMillis();
            List<Task> tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.CONFIRMATION_TASK);
            long startC = System.currentTimeMillis();
            confirmationTasks = getDocumentService().getTasksWithDocuments(tmpTasks);
            log.debug("loadTasks - confirmationTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
            startB = System.currentTimeMillis();
            tmpTasks = getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK);
            startC = System.currentTimeMillis();
            confirmationTasks.addAll(getDocumentService().getTasksWithDocuments(tmpTasks));
            log.debug("loadTasks - dueDateExtensionTasks: " + (startC - startB) + "ms + " + (System.currentTimeMillis() - startC) + "ms");
        }
        log.debug("loadTasks - END: " + (System.currentTimeMillis() - startA) + "ms");
    }

    // END: PRIVATE METHODS

}
