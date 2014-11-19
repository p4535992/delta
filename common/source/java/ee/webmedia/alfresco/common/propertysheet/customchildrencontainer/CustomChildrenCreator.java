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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
