package ee.webmedia.alfresco.workflow.web.evaluator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class WorkflowRemoveNotificationEvaluator extends WorkflowAddNotificationEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean evaluateAssocs(CompoundWorkflow compoundWorkflow, NodeRef userRef, QName assocType) {
        return !super.evaluateAssocs(compoundWorkflow, userRef, assocType);
    }

}
