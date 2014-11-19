package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MINUTES_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PROJECT_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.RESOLUTION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;

/**
 * Evaluator that is ment for evaluating createFollowUp button(simple button, not dropdown)
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class FollowUpNodeTypeEvaluator extends AbstractFollowUpNodeTypeEvaluator {

    private static final long serialVersionUID = -3926331459547502498L;

    public FollowUpNodeTypeEvaluator() {
        nodeTypes.add(ERRAND_APPLICATION_DOMESTIC);
        nodeTypes.add(ERRAND_ORDER_ABROAD);
        nodeTypes.add(ERRAND_ORDER_ABROAD_MV);
        nodeTypes.add(LEAVING_LETTER);
        nodeTypes.add(TRAINING_APPLICATION);
        nodeTypes.add(INTERNAL_APPLICATION);
        nodeTypes.add(INTERNAL_APPLICATION_MV);
        nodeTypes.add(REPORT);
        nodeTypes.add(REPORT_MV);
        nodeTypes.add(INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV);
        nodeTypes.add(MINUTES_MV);
        nodeTypes.add(RESOLUTION_MV);
        nodeTypes.add(PROJECT_APPLICATION);
    }

}