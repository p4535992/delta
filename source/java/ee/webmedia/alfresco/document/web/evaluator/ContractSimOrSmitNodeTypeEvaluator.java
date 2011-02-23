package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ContractSimOrSmitNodeTypeEvaluator extends AbstractFollowUpNodeTypeEvaluator {

    private static final long serialVersionUID = 1L;
    
    public ContractSimOrSmitNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.CONTRACT_SIM);
        nodeTypes.add(DocumentSubtypeModel.Types.CONTRACT_SMIT);
    }

}
