package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class FollowUpNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = 1L;

    public FollowUpNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC);
        nodeTypes.add(DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD);
        nodeTypes.add(DocumentSubtypeModel.Types.LEAVING_LETTER);
        nodeTypes.add(DocumentSubtypeModel.Types.TRAINING_APPLICATION);
        nodeTypes.add(DocumentSubtypeModel.Types.INTERNAL_APPLICATION);
        nodeTypes.add(DocumentSubtypeModel.Types.REPORT);
    }

}