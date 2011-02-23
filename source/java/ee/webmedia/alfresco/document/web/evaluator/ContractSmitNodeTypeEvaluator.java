package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ContractSmitNodeTypeEvaluator extends AbstractFollowUpNodeTypeEvaluator {

    private static final long serialVersionUID = 1L;

    public ContractSmitNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.CONTRACT_SMIT);
    }
}
