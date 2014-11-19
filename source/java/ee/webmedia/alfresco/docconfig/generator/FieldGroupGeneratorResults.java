package ee.webmedia.alfresco.docconfig.generator;

import java.util.Map;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface FieldGroupGeneratorResults {

    Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> generateItems(Field... fields);

    ItemConfigVO generateItemBase(Field field);

    ItemConfigVO generateAndAddViewModeText(String name, String label);

    void addItem(ItemConfigVO item);

    void addStateHolder(String key, PropertySheetStateHolder stateHolder);

}
