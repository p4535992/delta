package ee.webmedia.alfresco.docconfig.generator;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface GeneratorResults {

    ItemConfigVO getAndAddPreGeneratedItem();

    ItemConfigVO generateAndAddViewModeText(String name, String label);

    void addItem(ItemConfigVO item);

<<<<<<< HEAD
    void addItemAfterPregeneratedItem(ItemConfigVO item);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    void addStateHolder(String key, PropertySheetStateHolder stateHolder);

    boolean hasStateHolder(String key);

}
