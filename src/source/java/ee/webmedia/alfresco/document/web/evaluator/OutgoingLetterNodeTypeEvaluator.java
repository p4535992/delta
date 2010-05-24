package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class OutgoingLetterNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = -4155919836161159743L;

    public OutgoingLetterNodeTypeEvaluator() {
        nodeTypes.add(DocumentSubtypeModel.Types.OUTGOING_LETTER);
    }

}
