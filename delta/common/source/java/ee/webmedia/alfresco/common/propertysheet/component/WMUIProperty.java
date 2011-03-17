package ee.webmedia.alfresco.common.propertysheet.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.IComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
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
    public static final String DONT_RENDER_IF_DISABLED_ATTR = "dontRenderIfDisabled";
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

    @Override
    public boolean isRendered() {
        // --------------------------------------------------------
        // The same as UIProperty#encodeBegin
        if (getChildCount() == 0) {
            // get the variable being used from the parent
            UIComponent parent = this.getParent();
            if ((parent instanceof UIPropertySheet) == false) {
                throw new IllegalStateException(getIncorrectParentMsg());
            }
            // only build the components if there are currently no children
            int howManyKids = getChildren().size();
            if (howManyKids == 0) {
                try {
                    generateItem(FacesContext.getCurrentInstance(), (UIPropertySheet) parent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // --------------------------------------------------------
        if (getChildCount() >= 2) {
            UIComponent child = (UIComponent) getChildren().get(1);
            if (Boolean.TRUE.equals(child.getAttributes().get(DONT_RENDER_IF_DISABLED_ATTR)) && Utils.isComponentDisabledOrReadOnly(child)) {
                return false;
            }
        }
        return super.isRendered();
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

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        this.propertySheetItemAttributes = (Map<String, String>) values[1];
    }

    @Override
    public Object saveState(FacesContext context) {
        Object values[] = new Object[] {
            super.saveState(context),
            this.propertySheetItemAttributes
        };
        return values;
    }

}