package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

/**
 * Abstract evaluator of followUps
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 *         FIXME DLSeadist pole vist enam kasutusel
 */
public class AbstractFollowUpNodeTypeEvaluator extends NodeTypeEvaluator {

  private static final long serialVersionUID = 7673537928859090920L;

  protected void throwException() {
    throw new RuntimeException("FollowUpNodeTypeEvaluatorshouldn't be used");
  }

  @Override
  public boolean evaluate(Object obj) {
    throwException();
    return false;
  }

  @Override
  public boolean evaluate(Node docNode) {
    throwException();
    if (true) {
      throwException();
    }
    return evaluateViewSatate(docNode) && (RegisterDocumentEvaluator.isRegNumFilled(docNode) && evaluateType(docNode));
  }
}