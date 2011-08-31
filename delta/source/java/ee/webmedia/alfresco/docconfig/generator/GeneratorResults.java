package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;

/**
 * @author Alar Kvell
 */
public interface GeneratorResults {

    ItemConfigVO getAndAddPreGeneratedItem();

    ItemConfigVO generateAndAddViewModeText(String name, String label);

    void addStateHolder(String key, PropertySheetStateHolder stateHolder);

}
