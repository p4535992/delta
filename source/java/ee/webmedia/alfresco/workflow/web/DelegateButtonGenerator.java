package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Generates button for delegation
 */
public class DelegateButtonGenerator extends BaseComponentGenerator {
    private static final String PARAM_NODEREF = "nodeRef";
    private static final String DELEGATE_BINDING = "#{" + DelegationBean.DELEGATE + "}";

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Application app = context.getApplication();
        Node assignmentTaskNode = propertySheet.getNode();
        HtmlCommandButton delegateButton = createDelegateButton(app, assignmentTaskNode);

        Integer delegatableTaskIndex = ComponentUtil.getAttribute(propertySheet, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class);
        NodeRef compoundWorkflowRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(
                BeanHelper.getWorkflowDbService().getTaskParentNodeRef(assignmentTaskNode.getNodeRef()), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
        addChildren(delegateButton,
                createUIParam(PARAM_NODEREF, compoundWorkflowRef, app)
                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex, app));

        UIOutput wrapper = (UIOutput) app.createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
        FacesHelper.setupComponentId(context, wrapper, "delegBtnUnderLine");
        ComponentUtil.putAttribute(wrapper, "styleClass", "delegationWrapper");
        ComponentUtil.addChildren(wrapper, delegateButton);
        return wrapper;
    }

    public static HtmlCommandButton createDelegateButton(Application app, Node taskNode) {
        HtmlCommandButton delegateButton = new HtmlCommandButton();
        delegateButton.setId("delegate-id-" + taskNode.getId());
        delegateButton.setActionListener(app.createMethodBinding(DELEGATE_BINDING, new Class[] { ActionEvent.class }));
        delegateButton.setValue(MessageUtil.getMessage("task_delegate_" + taskNode.getType().getLocalName()));
        delegateButton.setStyleClass("delegateBtn");
        return delegateButton;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

}
