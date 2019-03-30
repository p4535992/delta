package ee.webmedia.alfresco.common.propertysheet.workflow;

import javax.faces.component.UIComponent;

import org.alfresco.web.bean.dialog.IDialogBean;

import ee.webmedia.alfresco.casefile.web.CaseFileDialog;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDefinitionDialog;

public class DelegationTaskListContainer extends TaskListContainer {

    public DelegationTaskListContainer() {
        super();
    }

    public DelegationTaskListContainer(UIComponent parent, int workflowIndex, String currentPageAttributeKey) {
        super(parent, workflowIndex, currentPageAttributeKey);
    }

    @Override
    protected void updateView() {
        BeanHelper.getDelegationBean().updatePanelGroup();
    }

    @Override
    protected UIComponent getPersistentParent(UIComponent searchFromComponent) {
        IDialogBean bean = BeanHelper.getDialogManager().getBean();
        if (bean instanceof CompoundWorkflowDefinitionDialog) {
            return super.getPersistentParent(searchFromComponent);
        } else if (bean instanceof DocumentDynamicDialog) {
            return ComponentUtil.findParentComponentById(searchFromComponent, "document-dynamic-dialog-workflow-block");
        } else if (bean instanceof CaseFileDialog) {
            return ComponentUtil.findParentComponentById(searchFromComponent, "case-file-dialog-workflow-block");
        }
        throw new RuntimeException("Unsupported bean class for delegation task list: " + bean.getClass().getName());
    }

}
