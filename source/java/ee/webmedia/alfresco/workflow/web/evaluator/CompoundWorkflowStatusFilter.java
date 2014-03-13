package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorItemFilter;
import ee.webmedia.alfresco.workflow.model.Status;

public class CompoundWorkflowStatusFilter implements EnumSelectorItemFilter<Status> {

    @Override
    public boolean showItem(Status enumItem) {
        if (enumItem == Status.NEW || enumItem == Status.IN_PROGRESS || enumItem == Status.STOPPED || enumItem == Status.FINISHED) {
            return true;
        }
        return false;
    }

}
