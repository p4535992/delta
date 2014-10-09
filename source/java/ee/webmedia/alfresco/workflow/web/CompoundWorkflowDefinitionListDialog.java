package ee.webmedia.alfresco.workflow.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Dialog bean for compound workflow list.
 */
public class CompoundWorkflowDefinitionListDialog extends BaseDialogBean {

    public static final String BEAN_NAME = "CompoundWorkflowDefinitionListDialog";

    private static final long serialVersionUID = 1L;

    private transient WorkflowService workflowService;
    private List<CompoundWorkflowDefinition> workflows;

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        workflows = getWorkflowService().getActiveCompoundWorkflowDefinitions(true);
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public String cancel() {
        clean();
        return super.cancel();
    }

    @Override
    public void clean() {
        workflows = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        throw new RuntimeException("OK button not supported here.");
    }

    /**
     * Getter for JSP.
     */
    public List<CompoundWorkflowDefinition> getWorkflows() {
        return workflows;
    }

    public boolean isShowCaseFileColumn() {
        return BeanHelper.getApplicationConstantsBean().isCaseVolumeEnabled();
    }

    public boolean isShowDocumentColumn() {
        return BeanHelper.getWorkflowConstantsBean().isDocumentWorkflowEnabled();
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    public void updateCache() {
        getWorkflowService().removeDeletedCompoundWorkflowDefinitionFromCache();
    }

}
