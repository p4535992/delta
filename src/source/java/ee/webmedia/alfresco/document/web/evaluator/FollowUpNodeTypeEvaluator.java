package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;

/**
 * Evaluator that is ment for evaluating createFollowUp button(simple button, not dropdown)
 * 
 * @author Ats Uiboupin
 */
public class FollowUpNodeTypeEvaluator extends AbstractFollowUpNodeTypeEvaluator {

    private static final long serialVersionUID = -3926331459547502498L;

    public FollowUpNodeTypeEvaluator() {
        nodeTypes.add(ERRAND_APPLICATION_DOMESTIC);
        nodeTypes.add(ERRAND_ORDER_ABROAD);
        nodeTypes.add(LEAVING_LETTER);
        nodeTypes.add(TRAINING_APPLICATION);
        nodeTypes.add(INTERNAL_APPLICATION);
        nodeTypes.add(REPORT);
    }

}