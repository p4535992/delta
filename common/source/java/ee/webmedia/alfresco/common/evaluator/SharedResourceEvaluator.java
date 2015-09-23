package ee.webmedia.alfresco.common.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;


public abstract class SharedResourceEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    protected EvaluatorSharedResource sharedResource;

    public void setSharedResource(EvaluatorSharedResource evaluatorSharedResource) {
        sharedResource = evaluatorSharedResource;
    }

    public abstract boolean evaluate();
}
