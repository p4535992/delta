package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;

public interface GeneratorResults {

    ItemConfigVO getAndAddPreGeneratedItem();

    ItemConfigVO generateAndAddViewModeText(String name, String label);

    void addItem(ItemConfigVO item);

    void addItemAfterPregeneratedItem(ItemConfigVO item);

    void addStateHolder(String key, PropertySheetStateHolder stateHolder);

    boolean hasStateHolder(String key);

}
