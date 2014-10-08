package ee.webmedia.alfresco.classificator.enums;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorItemFilter;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class VolumeTypeFilter implements EnumSelectorItemFilter<VolumeType> {

    @Override
    public boolean showItem(VolumeType enumItem) {
        return !VolumeType.CASE_FILE.equals(enumItem) || BeanHelper.getApplicationConstantsBean().isCaseVolumeEnabled();
    }

}
