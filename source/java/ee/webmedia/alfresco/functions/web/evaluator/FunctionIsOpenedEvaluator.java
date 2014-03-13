package ee.webmedia.alfresco.functions.web.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class FunctionIsOpenedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return !BeanHelper.getFunctionsDetailsDialog().isClosed() && !BeanHelper.getFunctionsDetailsDialog().isNew();
    }

}
