package ee.webmedia.alfresco.workflow.service;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * @author Ats Uiboupin
 */
public class HasNoStoppedOrInprogressWorkflowsEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 778943700049418316L;

    @Override
    public boolean evaluate(Node node) {
        WorkflowService workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        return workflowService.hasNoStoppedOrInprogressCompoundWorkflows(node.getNodeRef());
    }

}
