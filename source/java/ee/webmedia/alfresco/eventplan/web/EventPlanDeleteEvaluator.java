package ee.webmedia.alfresco.eventplan.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Evaluator for whether EventPlan Delete button is visible.
 * 
 * @see EventPlanDialog
 */
public class EventPlanDeleteEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        EventPlanDialog dialog = BeanHelper.getEventPlanDialog();
        return !dialog.isNew() && dialog.isInEditMode();
    }
}
