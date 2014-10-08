<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import java.util.List;

import javax.faces.component.UIComponent;

/**
 * Generates UIComponent elements based on given parameter list
 * 
 * @author Riina Tens
 */
public interface CustomChildrenCreator {

    List<UIComponent> createChildren(List<Object> params, int rowCounter);

}
=======
package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import java.util.List;

import javax.faces.component.UIComponent;

/**
 * Generates UIComponent elements based on given parameter list
 */
public interface CustomChildrenCreator {

    List<UIComponent> createChildren(List<Object> params, int rowCounter);

}
>>>>>>> develop-5.1
