<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INCOMING_LETTER_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.OUTGOING_LETTER_MV;

import org.alfresco.web.bean.repository.Node;

/**
 * Evaluator that decides whether to show both INCOMING_LETTER_MV and OUTGOING_LETTER_MV followUps
 * 
 * @author Riina Tens
 *         FIXME DLSeadist
 */
public class LetterMvFollowupEvaluator extends AbstractFollowUpNodeTypeEvaluator {

  private static final long serialVersionUID = 1L;

  public LetterMvFollowupEvaluator() {
    nodeTypes.add(INCOMING_LETTER_MV);
    nodeTypes.add(OUTGOING_LETTER_MV);
  }

  @Override
  public boolean evaluate(Node docNode) {
    throwException();
    if (OUTGOING_LETTER_MV.equals(docNode.getType())) {
      return evaluateViewSatate(docNode); // OUTGOING_LETTER_MV don't need filled regNum
    }
    return super.evaluate(docNode);
  }
}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INCOMING_LETTER_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.OUTGOING_LETTER_MV;

import org.alfresco.web.bean.repository.Node;

/**
 * Evaluator that decides whether to show both INCOMING_LETTER_MV and OUTGOING_LETTER_MV followUps
 *         FIXME DLSeadist
 */
public class LetterMvFollowupEvaluator extends AbstractFollowUpNodeTypeEvaluator {

  private static final long serialVersionUID = 1L;

  public LetterMvFollowupEvaluator() {
    nodeTypes.add(INCOMING_LETTER_MV);
    nodeTypes.add(OUTGOING_LETTER_MV);
  }

  @Override
  public boolean evaluate(Node docNode) {
    throwException();
    if (OUTGOING_LETTER_MV.equals(docNode.getType())) {
      return evaluateViewSatate(docNode); // OUTGOING_LETTER_MV don't need filled regNum
    }
    return super.evaluate(docNode);
  }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
