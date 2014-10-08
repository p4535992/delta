<<<<<<< HEAD
package ee.webmedia.alfresco.classificator.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Vladimir Drozdik
 */
public class ClassificatorDeleteEnabledEvaluator extends BaseActionEvaluator {

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
        return (((Classificator) obj).isDeleteEnabled() && !BeanHelper.getClassificatorService().isClassificatorUsed(((Classificator) obj).getName()));
    }

}
=======
package ee.webmedia.alfresco.classificator.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class ClassificatorDeleteEnabledEvaluator extends BaseActionEvaluator {

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
        return (((Classificator) obj).isDeleteEnabled() && !BeanHelper.getClassificatorService().isClassificatorUsed(((Classificator) obj).getName()));
    }

}
>>>>>>> develop-5.1
