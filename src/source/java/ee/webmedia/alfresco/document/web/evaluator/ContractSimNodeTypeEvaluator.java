package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ContractSimNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = 1L;

    public ContractSimNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.CONTRACT_SIM);
    }
}
