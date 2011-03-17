package ee.webmedia.alfresco.workflow.service;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * @author Ats Uiboupin
 */
public class HasNoInprogressWorkflowsEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 778943700049418316L;

    @Override
    public boolean evaluate(Node node) {
        WorkflowService workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
        return !Boolean.TRUE.equals(node.getProperties().get(DocumentSpecificModel.Props.NOT_EDITABLE)) 
            && !workflowService.hasInprogressCompoundWorkflows(node.getNodeRef());
    }

}
