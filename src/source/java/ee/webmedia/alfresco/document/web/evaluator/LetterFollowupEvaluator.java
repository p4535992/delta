package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INCOMING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.OUTGOING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TENDERING_APPLICATION;

import org.alfresco.web.bean.repository.Node;

/**
 * Evaluator that decides whether to show both INCOMING_LETTER and OUTGOING_LETTER followUps
 * 
 * @author Ats Uiboupin
 */
public class LetterFollowupEvaluator extends AbstractFollowUpNodeTypeEvaluator {

    private static final long serialVersionUID = -6148380602568703707L;

    public LetterFollowupEvaluator() {
        nodeTypes.add(INCOMING_LETTER);
        nodeTypes.add(OUTGOING_LETTER);
        nodeTypes.add(TENDERING_APPLICATION);
    }

    @Override
    public boolean evaluate(Node docNode) {
        if (OUTGOING_LETTER.equals(docNode.getType())) {
            return evaluateViewSatate(docNode); // OUTGOING_LETTER doesn't need filled regNum
        }
        return super.evaluate(docNode);
    }
}
