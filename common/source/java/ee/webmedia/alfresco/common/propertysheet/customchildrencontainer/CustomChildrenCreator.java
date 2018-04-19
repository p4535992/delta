package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import java.util.List;

import javax.faces.component.UIComponent;

/**
 * Generates UIComponent elements based on given parameter list
 */
public interface CustomChildrenCreator {

    List<UIComponent> createChildren(Object params, int rowCounter);

}
