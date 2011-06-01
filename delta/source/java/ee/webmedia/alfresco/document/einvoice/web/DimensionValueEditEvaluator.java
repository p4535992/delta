package ee.webmedia.alfresco.document.einvoice.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Riina Tens
 */
public class DimensionValueEditEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object object) {
        return BeanHelper.getDimensionDetailsDialog().isEditableDimension();
    }
}
