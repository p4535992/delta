package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Kaarel Jõgeva
 */
public class MyTasksBean extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "MyTasksBean";

    private String dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
    private String listTitle = "";

    private List<TaskAndDocument> tasks;
    private List<TaskAndDocument> assignmentTasks;
    private List<TaskAndDocument> informationTasks;
    private List<TaskAndDocument> opinionTasks;
    private List<TaskAndDocument> reviewTasks;
    private List<TaskAndDocument> signatureTasks;

    transient private ParametersService parametersService;
    transient private DocumentService documentService;
    transient private DocumentSearchService documentSearchService;
    
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // nothing to do here
        return null;
    }
    
    @Override
    public String getContainerTitle() {
        return dialogTitle;
    }

    private void reset() {
        assignmentTasks = null;
        informationTasks = null;
        opinionTasks = null;
        reviewTasks = null;
        signatureTasks = null;
        tasks = null;
        dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
    }

    public void setupMyTasks(ActionEvent event) {
        assignmentTasks = filterTasksByDate(getAssignmentTasks());
        informationTasks = filterTasksByDate(getInformationTasks());
        opinionTasks = filterTasksByDate(getOpinionTasks());
        reviewTasks = filterTasksByDate(getReviewTasks());
        signatureTasks = filterTasksByDate(getSignatureTasks());
        dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
    }

    private List<TaskAndDocument> filterTasksByDate(List<TaskAndDocument> tasks) {
        Date tomorrow = Calendar.getInstance().getTime();
        List<TaskAndDocument> filteredTasks = new ArrayList<TaskAndDocument>(tasks.size());
        for (TaskAndDocument task : tasks) {
            final Date dueDate = task.getTask().getDueDate();
            if (dueDate != null && dueDate.before(tomorrow)) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    public void setupAssignmentTasks(ActionEvent event) {
        reset();
        tasks = getAssignmentTasks();
        dialogTitle = MessageUtil.getMessage("assignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_assignment_title");
    }

    public void setupInformationTasks(ActionEvent event) {
        reset();
        tasks = getInformationTasks();
        dialogTitle = MessageUtil.getMessage("informationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_information_title");
    }

    public void setupOpinionTasks(ActionEvent event) {
        reset();
        tasks = getOpinionTasks();
        dialogTitle = MessageUtil.getMessage("opinionWorkflow");
        listTitle = MessageUtil.getMessage("task_list_opinion_title");
    }

    public void setupReviewTasks(ActionEvent event) {
        reset();
        tasks = getReviewTasks();
        dialogTitle = MessageUtil.getMessage("reviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_review_title");
    }

    public void setupSignatureTasks(ActionEvent event) {
        reset();
        tasks = getSignatureTasks();
        dialogTitle = MessageUtil.getMessage("signatureWorkflow");
        listTitle = MessageUtil.getMessage("task_list_signature_title");
    }

    public List<TaskAndDocument> getTasks() {
        if(tasks == null) {
            tasks = new ArrayList<TaskAndDocument>();
        }
        return tasks;
    }

    public List<TaskAndDocument> getAssignmentTasks() {
//        if (assignmentTasks == null) {
            assignmentTasks = getDocumentService().getTasksWithDocuments(
                    getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.ASSIGNMENT_TASK));
//        }

        return assignmentTasks;
    }

    public List<TaskAndDocument> getInformationTasks() {
//        if (informationTasks == null) {
            informationTasks = getDocumentService().getTasksWithDocuments(
                    getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.INFORMATION_TASK));
//        }
        return informationTasks;
    }

    public List<TaskAndDocument> getOpinionTasks() {
//        if (opinionTasks == null) {
            opinionTasks = getDocumentService().getTasksWithDocuments(
                    getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.OPINION_TASK));
//        }
        return opinionTasks;
    }

    public List<TaskAndDocument> getReviewTasks() {
//        if (reviewTasks == null) {
            reviewTasks = getDocumentService().getTasksWithDocuments(
                    getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.REVIEW_TASK));
//        }
        return reviewTasks;
    }

    public List<TaskAndDocument> getSignatureTasks() {
//        if (signatureTasks == null) {
            signatureTasks = getDocumentService().getTasksWithDocuments(
                    getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.SIGNATURE_TASK));
//        }
        return signatureTasks;
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

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    // END: getters/setters

}
