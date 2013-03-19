package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;

/**
 * @author Alar Kvell
 */
public class DocumentConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WMPropertySheetConfigElement propertySheetConfigElement;
    private final Map<String, PropertySheetStateHolder> stateHolders;
    private final List<String> saveListenerBeanNames;
    private final DynamicType dynamicType;
    private final DocumentTypeVersion docVersion;

    public DocumentConfig(WMPropertySheetConfigElement propertySheetConfigElement, Map<String, PropertySheetStateHolder> stateHolders
                          , List<String> saveListenerBeanNames, DocumentTypeVersion docVersion) {
        this.propertySheetConfigElement = propertySheetConfigElement;
        this.stateHolders = stateHolders;
        this.saveListenerBeanNames = saveListenerBeanNames;
        dynamicType = docVersion == null ? null : (DynamicType) docVersion.getParent();
        this.docVersion = docVersion;
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
        return dynamicType != null ? dynamicType.getName() : null;
    }

    public boolean isDocumentTypeRegistrationEnabled() {
        return dynamicType != null && dynamicType instanceof DocumentType ? ((DocumentType) dynamicType).isRegistrationEnabled() : null;
    }

    public DynamicType getDocType() {
        return dynamicType;
    }

    public DocumentTypeVersion getDocVersion() {
        return docVersion;
    }

    protected DocumentConfig cloneAsUnmodifiable() {
        return new DocumentConfig(getPropertySheetConfigElement(), Collections.unmodifiableMap(getStateHolders())
                , Collections.unmodifiableList(getSaveListenerBeanNames()), getDocVersion());
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  propertySheetConfigElement=" + WmNode.toString(propertySheetConfigElement.getItems().entrySet()) + "\n  stateHolders="
                + WmNode.toString(stateHolders.entrySet()) + "\n  saveListenerBeanNames=" + WmNode.toString(saveListenerBeanNames)
                + "\n  documentTypeName=" + getDocumentTypeName() + "\n]";
    }

}
