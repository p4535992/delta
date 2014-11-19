package ee.webmedia.alfresco.docconfig.web;

import java.io.Serializable;
<<<<<<< HEAD
import java.util.HashMap;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Map;

import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class PropertySheetStateBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "PropertySheetStateBean";
    public static final String STATE_HOLDERS_BINDING_NAME = BEAN_NAME + ".stateHolders";
<<<<<<< HEAD
    public static final String ADDITIONAL_STATE_HOLDERS_BINDING_NAME = BEAN_NAME + ".additionalStateHolders";

    private Map<String, PropertySheetStateHolder> stateHolders;
    /** Used to store additional state holders if dialog has more than one property sheet that requires state holders. */
    private final Map<String, Map<String, PropertySheetStateHolder>> additionalStateHolders = new HashMap<String, Map<String, PropertySheetStateHolder>>();
=======

    private Map<String, PropertySheetStateHolder> stateHolders;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * Reset all fields and components
     * 
     * @param dialogDataProvider may be {@code null}
     */
    public void reset(Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
<<<<<<< HEAD
        resetStateHolders(stateHolders, dialogDataProvider);
        this.stateHolders = stateHolders;
    }

    public void resetAdditionalStateHolders(String propertySheetKey, Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
        resetStateHolders(stateHolders, dialogDataProvider);
        additionalStateHolders.put(propertySheetKey, stateHolders);
    }

    private void resetStateHolders(Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        if (stateHolders != null) {
            for (PropertySheetStateHolder stateHolder : stateHolders.values()) {
                stateHolder.reset(dialogDataProvider);
            }
        }
<<<<<<< HEAD
=======
        this.stateHolders = stateHolders;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public Map<String, PropertySheetStateHolder> getStateHolders() {
        return stateHolders;
    }

<<<<<<< HEAD
    public Map<String, Map<String, PropertySheetStateHolder>> getAdditionalStateHolders() {
        return additionalStateHolders;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public <E extends PropertySheetStateHolder> E getStateHolder(String key, Class<E> clazz) {
        if (stateHolders == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E stateHolder = (E) stateHolders.get(key);
        return stateHolder;
    }

}
