package ee.webmedia.alfresco.casefile.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.evaluator.CaseFileActionsGroupResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class CaseFileEventPlanEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        if (BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        if (getUserService().isArchivist() || getUserService().isDocumentManager()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean evaluate() {
        CaseFileActionsGroupResource resource = (CaseFileActionsGroupResource) sharedResource;
        return !resource.isInEditMode() && (resource.isArchivist() || getUserService().isDocumentManager());
    }

}
