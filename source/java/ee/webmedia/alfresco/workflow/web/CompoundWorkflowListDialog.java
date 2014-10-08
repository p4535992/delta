package ee.webmedia.alfresco.workflow.web;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class CompoundWorkflowListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowListDialog";

    private CompoundWorkflowWithObjectDataProvider compoundWorkflows;

    @Override
    public void restored() {
        compoundWorkflows = new CompoundWorkflowWithObjectDataProvider(BeanHelper.getDocumentSearchService().searchCurrentUserCompoundWorkflowRefs());
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
        compoundWorkflows = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        throw new RuntimeException("OK button not supported here.");
    }

    /**
     * Getter for JSP.
     */
    public CompoundWorkflowWithObjectDataProvider getCompoundWorkflows() {
        return compoundWorkflows;
    }

    public boolean isShowTitle() {
        return BeanHelper.getWorkflowConstantsBean().isWorkflowTitleEnabled();
    }

}
