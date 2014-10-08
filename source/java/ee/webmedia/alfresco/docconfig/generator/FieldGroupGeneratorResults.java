package ee.webmedia.alfresco.docconfig.generator;

import java.util.Map;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;

public interface FieldGroupGeneratorResults {

    Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> generateItems(Field... fields);

    ItemConfigVO generateItemBase(Field field);

    ItemConfigVO generateAndAddViewModeText(String name, String label);

    void addItem(ItemConfigVO item);

    void addStateHolder(String key, PropertySheetStateHolder stateHolder);

}
