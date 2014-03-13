package ee.webmedia.alfresco.casefile.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;

public class CaseFileEventPlanEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (!new ViewStateActionEvaluator().evaluate(node)) {
            return false;
        }
        if (getUserService().isArchivist() || getUserService().isDocumentManager()) {
            return true;
        }
        return false;
    }

}
