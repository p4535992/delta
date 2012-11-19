package ee.webmedia.alfresco.docconfig.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;

/**
 * @author Alar Kvell
 */
public class PropertySheetStateBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "PropertySheetStateBean";
    public static final String STATE_HOLDERS_BINDING_NAME = BEAN_NAME + ".stateHolders";
    public static final String ADDITIONAL_STATE_HOLDERS_BINDING_NAME = BEAN_NAME + ".additionalStateHolders";

    private Map<String, PropertySheetStateHolder> stateHolders;
    /** Used to store additional state holders if dialog has more than one property sheet that requires state holders. */
    private final Map<String, Map<String, PropertySheetStateHolder>> additionalStateHolders = new HashMap<String, Map<String, PropertySheetStateHolder>>();

    /**
     * Reset all fields and components
     * 
     * @param dialogDataProvider may be {@code null}
     */
    public void reset(Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
        resetStateHolders(stateHolders, dialogDataProvider);
        this.stateHolders = stateHolders;
    }

    public void resetAdditionalStateHolders(String propertySheetKey, Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
        resetStateHolders(stateHolders, dialogDataProvider);
        additionalStateHolders.put(propertySheetKey, stateHolders);
    }

    private void resetStateHolders(Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
        if (stateHolders != null) {
            for (PropertySheetStateHolder stateHolder : stateHolders.values()) {
                stateHolder.reset(dialogDataProvider);
            }
        }
    }

    public Map<String, PropertySheetStateHolder> getStateHolders() {
        return stateHolders;
    }

    public Map<String, Map<String, PropertySheetStateHolder>> getAdditionalStateHolders() {
        return additionalStateHolders;
    }

    public <E extends PropertySheetStateHolder> E getStateHolder(String key, Class<E> clazz) {
        if (stateHolders == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E stateHolder = (E) stateHolders.get(key);
        return stateHolder;
    }

}
