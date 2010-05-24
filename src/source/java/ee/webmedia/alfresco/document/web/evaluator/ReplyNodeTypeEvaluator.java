package ee.webmedia.alfresco.document.web.evaluator;

import java.util.ArrayList;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class ReplyNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = 1L;

    public ReplyNodeTypeEvaluator() {
        nodeTypes = new ArrayList<QName>(2);
        nodeTypes.add(DocumentSubtypeModel.Types.INCOMING_LETTER);
        nodeTypes.add(DocumentSubtypeModel.Types.CONTRACT_SIM);
    }

    @Override
    public boolean evaluate(Node docNode) {
        return super.evaluate(docNode) && RegisterDocumentEvaluator.isRegistered(docNode);
    }
}
