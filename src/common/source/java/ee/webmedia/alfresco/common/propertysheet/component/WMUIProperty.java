package ee.webmedia.alfresco.common.propertysheet.component;

import java.util.Collections;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.IComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

/**
 * Class that supports having custom attributes on property-sheet/show-property element, <br>
 * that for example generators could be read by ClassificatorSelectorAndTextGenerator.
 * 
 * @author Ats Uiboupin
 */
public class WMUIProperty extends UIProperty implements NamingContainer, CustomAttributes {

    public static final String LABEL_STYLE_CLASS = "labelStyleClass";
    public static final String REPO_NODE = "__repo_node";
    protected Map<String, String> propertySheetItemAttributes;

    protected IComponentGenerator getComponentGenerator(FacesContext context, String componentGeneratorName) {
        IComponentGenerator compGenerator = FacesHelper.getComponentGenerator(context, componentGeneratorName);
        // add all attributes from property-sheet/show-property element if current generator supports custom attributes
        if (compGenerator instanceof CustomAttributes) {
            CustomAttributes gen = (CustomAttributes) compGenerator;
            gen.setCustomAttributes(propertySheetItemAttributes);
        }
        return compGenerator;
    }

    protected void saveExistingValue4ComponentGenerator(FacesContext context, Node node, String propertyName) {
        // subclasses can save value of existing property (for example to context) based on 
        // propertyName and value corresponding to propertyName from node properties.
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put(REPO_NODE, new Object[] {node, propertyName});
    }

    // START: getters / setters
    @Override
    public Map<String, String> getCustomAttributes() {
        if (propertySheetItemAttributes == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap(propertySheetItemAttributes);
        }
    }

    @Override
    public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
        this.propertySheetItemAttributes = propertySheetItemAttributes;
    }
    
    @Override
    protected void generateLabel(FacesContext context, UIPropertySheet propSheet, String displayLabel) {
        HtmlOutputText label = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        label.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
        FacesHelper.setupComponentId(context, label, "label_" + getName());
        getChildren().add(label);

        // remember the display label used (without the : separator)
        this.resolvedDisplayLabel = displayLabel;

        label.getAttributes().put("value", displayLabel);
        label.setStyleClass(getCustomAttributes().get(LABEL_STYLE_CLASS));
    }

    // END: getters / setters

}
