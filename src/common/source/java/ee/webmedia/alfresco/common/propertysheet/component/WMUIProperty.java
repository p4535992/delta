package ee.webmedia.alfresco.common.propertysheet.component;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.IComponentGenerator;
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
public class WMUIProperty extends UIProperty implements CustomAttributes {

    public static final String LABEL_STYLE_CLASS = "labelStyleClass";
    public static final String REPO_NODE = "__repo_node";
    protected Map<String, String> propertySheetItemAttributes;

    @Override
    protected IComponentGenerator getComponentGenerator(FacesContext context, String componentGeneratorName) {
        IComponentGenerator compGenerator = FacesHelper.getComponentGenerator(context, componentGeneratorName);
        // add all attributes from property-sheet/show-property element if current generator supports custom attributes
        if (compGenerator instanceof CustomAttributes) {
            CustomAttributes gen = (CustomAttributes) compGenerator;
            gen.setCustomAttributes(propertySheetItemAttributes);
        }
        return compGenerator;
    }

    // START: getters / setters
    @Override
    public Map<String, String> getCustomAttributes() {
        if (propertySheetItemAttributes == null) {
            propertySheetItemAttributes = new HashMap<String, String>(0);
        }
        return propertySheetItemAttributes;
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
