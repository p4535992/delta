package ee.webmedia.alfresco.workflow.web.evaluator;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorItemFilter;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;

/**
 * @author Riina Tens
 */
public class CompoundWorkflowDefinitionTypeFilter implements EnumSelectorItemFilter<CompoundWorkflowType> {

    @Override
    public boolean showItem(CompoundWorkflowType enumItem) {
        if (enumItem == CompoundWorkflowType.CASE_FILE_WORKFLOW) {
            return BeanHelper.getVolumeService().isCaseVolumeEnabled();
        } else if (enumItem == CompoundWorkflowType.INDEPENDENT_WORKFLOW) {
            return BeanHelper.getWorkflowService().isIndependentWorkflowEnabled();
        } else if (enumItem == CompoundWorkflowType.DOCUMENT_WORKFLOW) {
            return BeanHelper.getWorkflowService().isDocumentWorkflowEnabled();
        }
        return true;
    }

}
