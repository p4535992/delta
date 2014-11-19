package ee.webmedia.alfresco.workflow.service;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

<<<<<<< HEAD
/**
 * @author Ats Uiboupin
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class HasNoStoppedOrInprogressWorkflowsEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 778943700049418316L;

    @Override
    public boolean evaluate(Node node) {
        WorkflowService workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        return workflowService.hasNoStoppedOrInprogressCompoundWorkflows(node.getNodeRef());
    }

}
