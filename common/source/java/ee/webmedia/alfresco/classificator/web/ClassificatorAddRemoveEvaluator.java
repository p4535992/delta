<<<<<<< HEAD
package ee.webmedia.alfresco.classificator.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.model.Classificator;

/**
 * @author Vladimir Drozdik
 */
public class ClassificatorAddRemoveEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }

    @Override
    public boolean evaluate(Object obj) {
        if (obj == null) {
            return false;
        }
        return ((Classificator) obj).isAddRemoveValues();
    }

}
=======
package ee.webmedia.alfresco.classificator.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.model.Classificator;

public class ClassificatorAddRemoveEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }

    @Override
    public boolean evaluate(Object obj) {
        if (obj == null) {
            return false;
        }
        return ((Classificator) obj).isAddRemoveValues();
    }

}
>>>>>>> develop-5.1
