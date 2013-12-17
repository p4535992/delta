package ee.webmedia.alfresco.document.web.evaluator;

import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;

public class OutgoingLetterNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = -4155919836161159743L;

    public OutgoingLetterNodeTypeEvaluator() {
        nodeTypes.addAll(DocumentTypeHelper.outgoingLetterTypes);
    }

}
