package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.bean.repository.Node;

/**
 * Abstract evaluator of followUps
 * 
 * @author Ats Uiboupin
 */
public class AbstractFollowUpNodeTypeEvaluator extends NodeTypeEvaluator {

    private static final long serialVersionUID = 7673537928859090920L;

    @Override
    public boolean evaluate(Node docNode) {
        return evaluateViewSatate(docNode) && (RegisterDocumentEvaluator.isRegNumFilled(docNode) && evaluateType(docNode));
    }

}