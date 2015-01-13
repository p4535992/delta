package ee.webmedia.alfresco.addressbook.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Evaluator to check if creating children (orgainzations, persons, groups) to addressbook is enabled
 */
public class AddressbookCreateChildrenEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 8856155730090956174L;

    @Override
    public boolean evaluate(Node node) {
        return evaluate();
    }

    @Override
    public boolean evaluate(Object obj) {
        return evaluate();
    }

    private boolean evaluate() {
        return BeanHelper.getAddressbookService().hasCreatePermission();
    }

}
