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
    public static final String LIST_ASSIGNMENT = "assignment";
    public static final String LIST_INFORMATION = "information";
    public static final String LIST_OPINION = "opinion";
    public static final String LIST_REVIEW = "review";
    public static final String LIST_SIGNATURE = "signature";

    private String dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
    private String listTitle = "";
    private boolean filterTasks = true;
    private boolean lessColumns = true;
    private String specificList;
    private int pageSize = 10;

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
    public void restored() {
        if (specificList.equals(LIST_ASSIGNMENT)) {
            tasks = getAssignmentTasks();
        } else if (specificList.equals(LIST_INFORMATION)) {
            tasks = getInformationTasks();
        } else if (specificList.equals(LIST_OPINION)) {
            tasks = getOpinionTasks();
        } else if (specificList.equals(LIST_REVIEW)) {
            tasks = getReviewTasks();
        } else if (specificList.equals(LIST_SIGNATURE)) {
            tasks = getSignatureTasks();
        }
        super.restored();
    }

    @Override
    public String getContainerTitle() {
        return dialogTitle;
    }

    public boolean isTitlebarRendered() {
        return getContainerTitle().length() > 0;
    }

    private void reset() {
        assignmentTasks = null;
        informationTasks = null;
        opinionTasks = null;
        reviewTasks = null;
        signatureTasks = null;
        tasks = null;
        dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
        filterTasks = true;
        specificList = null;
    }

    public void setupMyTasks(ActionEvent event) {
        dialogTitle = getParametersService().getStringParameter(Parameters.WELCOME_TEXT);
        filterTasks = true;
    }

    private List<TaskAndDocument> filterTasksByDate(List<TaskAndDocument> tasks) {
        Date today = Calendar.getInstance().getTime();

        List<TaskAndDocument> filteredTasks = new ArrayList<TaskAndDocument>(tasks.size());
        for (TaskAndDocument task : tasks) {
            final Date dueDate = task.getTask().getDueDate();
            if (dueDate != null && dueDate.before(today)) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    public void setupAssignmentTasks(ActionEvent event) {
        reset();
        setSpecificList(LIST_ASSIGNMENT);
        filterTasks = false;
        lessColumns = false;
        tasks = getAssignmentTasks();
        dialogTitle = MessageUtil.getMessage("assignmentWorkflow");
        listTitle = MessageUtil.getMessage("task_list_assignment_title");
    }

    public void setupInformationTasks(ActionEvent event) {
        reset();
        setSpecificList(LIST_INFORMATION);
        filterTasks = false;
        lessColumns = false;
        tasks = getInformationTasks();
        dialogTitle = MessageUtil.getMessage("informationWorkflow");
        listTitle = MessageUtil.getMessage("task_list_information_title");
    }

    public void setupOpinionTasks(ActionEvent event) {
        reset();
        setSpecificList(LIST_OPINION);
        filterTasks = false;
        lessColumns = false;
        tasks = getOpinionTasks();
        dialogTitle = MessageUtil.getMessage("opinionWorkflow");
        listTitle = MessageUtil.getMessage("task_list_opinion_title");
    }

    public void setupReviewTasks(ActionEvent event) {
        reset();
        setSpecificList(LIST_REVIEW);
        filterTasks = false;
        lessColumns = true;
        tasks = getReviewTasks();
        dialogTitle = MessageUtil.getMessage("reviewWorkflow");
        listTitle = MessageUtil.getMessage("task_list_review_title");
    }

    public void setupSignatureTasks(ActionEvent event) {
        reset();
        setSpecificList(LIST_SIGNATURE);
        filterTasks = false;
        lessColumns = true;
        tasks = getSignatureTasks();
        dialogTitle = MessageUtil.getMessage("signatureWorkflow");
        listTitle = MessageUtil.getMessage("task_list_signature_title");
    }

    public List<TaskAndDocument> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<TaskAndDocument>();
        }
        return tasks;
    }

    public List<TaskAndDocument> getAssignmentTasks() {
        assignmentTasks = getDocumentService().getTasksWithDocuments(
                getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.ASSIGNMENT_TASK));

        if (filterTasks) {
            return filterTasksByDate(assignmentTasks);
        }
        return assignmentTasks;
    }

    public List<TaskAndDocument> getInformationTasks() {
        informationTasks = getDocumentService().getTasksWithDocuments(
                getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.INFORMATION_TASK));

        if (filterTasks) {
            return filterTasksByDate(informationTasks);
        }
        return informationTasks;
    }

    public List<TaskAndDocument> getOpinionTasks() {
        opinionTasks = getDocumentService().getTasksWithDocuments(
                getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.OPINION_TASK));

        if (filterTasks) {
            return filterTasksByDate(opinionTasks);
        }
        return opinionTasks;
    }

    public List<TaskAndDocument> getReviewTasks() {
        reviewTasks = getDocumentService().getTasksWithDocuments(
                getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.REVIEW_TASK));

        if (filterTasks) {
            return filterTasksByDate(reviewTasks);
        }
        return reviewTasks;
    }

    public List<TaskAndDocument> getSignatureTasks() {
        signatureTasks = getDocumentService().getTasksWithDocuments(
                getDocumentSearchService().searchCurrentUsersTasksInProgress(WorkflowSpecificModel.Types.SIGNATURE_TASK));

        if (filterTasks) {
            return filterTasksByDate(signatureTasks);
        }
        return signatureTasks;
    }

    public String getListTitle() {
        return listTitle;
    }
    
    public boolean isAssignmentPagerVisible() {
        if(filterTasks && getAssignmentTasks().size() > pageSize) {
            return true;
        }
        return false;
    }
    
    public boolean isInformationPagerVisible() {
        if(filterTasks && getInformationTasks().size() > pageSize) {
            return true;
        }
        return false;
    }
    
    public boolean isSignaturePagerVisible() {
        if(filterTasks && getSignatureTasks().size() > pageSize) {
            return true;
        }
        return false;
    }
    
    public boolean isReviewPagerVisible() {
        if(filterTasks && getReviewTasks().size() > pageSize) {
            return true;
        }
        return false;
    }
    
    public boolean isOpinionPagerVisible() {
        if(filterTasks && getOpinionTasks().size() > pageSize) {
            return true;
        }
        return false;
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

    public boolean isLessColumns() {
        return lessColumns;
    }

    public String getSpecificList() {
        return specificList;
    }

    public void setSpecificList(String specificList) {
        this.specificList = specificList;
    }

    public int getPageSize() {
        return pageSize;
    }

    // END: getters/setters

}
