package ee.webmedia.alfresco.docconfig.web;

import java.io.Serializable;
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

    private Map<String, PropertySheetStateHolder> stateHolders;

    /**
     * Reset all fields and components
     * 
     * @param dialogDataProvider may be {@code null}
     */
    public void reset(Map<String, PropertySheetStateHolder> stateHolders, DialogDataProvider dialogDataProvider) {
        if (stateHolders != null) {
            for (PropertySheetStateHolder stateHolder : stateHolders.values()) {
                stateHolder.reset(dialogDataProvider);
            }
        }
        this.stateHolders = stateHolders;
    }

    public Map<String, PropertySheetStateHolder> getStateHolders() {
        return stateHolders;
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
