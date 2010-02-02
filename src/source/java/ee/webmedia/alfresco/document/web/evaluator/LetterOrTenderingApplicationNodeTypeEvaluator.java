package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class LetterOrTenderingApplicationNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = 1L;

    public LetterOrTenderingApplicationNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.INCOMING_LETTER);
        nodeTypes.add(DocumentSubtypeModel.Types.OUTGOING_LETTER);
    }

    @Override
    public boolean evaluate(Node docNode) {
        return super.evaluate(docNode) || new TenderingApplicationNodeTypeEvaluator().evaluate(docNode);
    }
}
