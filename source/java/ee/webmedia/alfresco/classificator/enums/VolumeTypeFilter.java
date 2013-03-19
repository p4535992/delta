package ee.webmedia.alfresco.classificator.enums;

import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorItemFilter;

public class VolumeTypeFilter implements EnumSelectorItemFilter<VolumeType> {

    @Override
    public boolean showItem(VolumeType enumItem) {
        return !VolumeType.CASE_FILE.equals(enumItem) || getVolumeService().isCaseVolumeEnabled();
    }

}
