package ee.webmedia.alfresco.workflow.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;

public class CompoundWorkflowListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private List<CompoundWorkflowWithObject> compoundWorkflows;

    @Override
    public void restored() {
        compoundWorkflows = BeanHelper.getDocumentSearchService().searchCurrentUserCompoundWorkflows();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public String cancel() {
        compoundWorkflows = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        throw new RuntimeException("OK button not supported here.");
    }

    /**
     * Getter for JSP.
     */
    public List<CompoundWorkflowWithObject> getCompoundWorkflows() {
        return compoundWorkflows;
    }

    public boolean isShowTitle() {
        return BeanHelper.getWorkflowService().isWorkflowTitleEnabled();
    }

}
