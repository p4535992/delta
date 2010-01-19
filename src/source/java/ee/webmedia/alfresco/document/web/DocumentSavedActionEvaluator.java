package ee.webmedia.alfresco.document.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;

/**
 * UI action evaluator for validating whether document is saved (is not draft).
 * <p/>
 * Can be used only with {@link ee.webmedia.alfresco.document.web.DocumentDialog}.
 *
 * @author Romet Aidla
 */
public class DocumentSavedActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;
    
    @Override
    public boolean evaluate(Node node) {
        DocumentDialog dialog = (DocumentDialog) Application.getDialogManager().getState().getDialog();
        return !dialog.isDraft();
    }
}
