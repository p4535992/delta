<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * UI action evaluator for validating whether document screen is in view mode.
 * 
 * @author Romet Aidla
 */
public class ViewStateActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return !BeanHelper.getDocumentDialogHelperBean().isInEditMode();
    }

}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * UI action evaluator for validating whether document screen is in view mode.
 */
public class ViewStateActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return !BeanHelper.getDocumentDialogHelperBean().isInEditMode();
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
