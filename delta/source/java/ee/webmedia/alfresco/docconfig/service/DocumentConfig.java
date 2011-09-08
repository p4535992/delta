package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;

/**
 * @author Alar Kvell
 */
public class DocumentConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WMPropertySheetConfigElement propertySheetConfigElement;
    private final Map<String, PropertySheetStateHolder> stateHolders;
    private final List<String> saveListenerBeanNames;
    private final String documentTypeName;

    public DocumentConfig(WMPropertySheetConfigElement propertySheetConfigElement, Map<String, PropertySheetStateHolder> stateHolders, List<String> saveListenerBeanNames,
                          String documentTypeName) {
        this.propertySheetConfigElement = propertySheetConfigElement;
        this.stateHolders = stateHolders;
        this.saveListenerBeanNames = saveListenerBeanNames;
        this.documentTypeName = documentTypeName;
    }

    public WMPropertySheetConfigElement getPropertySheetConfigElement() {
        return propertySheetConfigElement;
    }

    public Map<String, PropertySheetStateHolder> getStateHolders() {
        return stateHolders;
    }

    public List<String> getSaveListenerBeanNames() {
        return saveListenerBeanNames;
    }

    public String getDocumentTypeName() {
        return documentTypeName;
    }

    protected DocumentConfig cloneAsUnmodifiable() {
        return new DocumentConfig(getPropertySheetConfigElement(), Collections.unmodifiableMap(getStateHolders()), Collections.unmodifiableList(getSaveListenerBeanNames()),
                getDocumentTypeName());
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  propertySheetConfigElement=" + WmNode.toString(propertySheetConfigElement) + "\n  stateHolders="
                + WmNode.toString(stateHolders.entrySet()) + "\n  saveListenerBeanNames=" + WmNode.toString(saveListenerBeanNames) + "\n  documentTypeName=" + documentTypeName
                + "\n]";
    }

}
