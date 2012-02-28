package ee.webmedia.alfresco.common.propertysheet.component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.InlinePropertyGroupGenerator;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Class that supports having custom attributes on property-sheet/show-property element, <br>
 * that for example generators could be read by ClassificatorSelectorAndTextGenerator.
 * 
 * @author Ats Uiboupin
 */
public class WMUIProperty extends UIProperty {

    public static final String AFTER_LABEL_BOOLEAN = "_AfterLabelBoolean";
    public static final String LABEL_STYLE_CLASS = "labelStyleClass";
    public static final String DISPLAY_LABEL_PARAMETER = "displayLabelParameter";
    public static final String REPO_NODE = "__repo_node";
    public static final String DONT_RENDER_IF_DISABLED_ATTR = "dontRenderIfDisabled";
    public static final String RENDER_CHECKBOX_AFTER_LABEL = "renderCheckboxAfterLabel";

    @Override
    public boolean isRendered() {
        // --------------------------------------------------------
        // The same as UIProperty#encodeBegin
        if (getChildCount() == 0) {
            // get the variable being used from the parent
            UIComponent parent = getParent();
            if (!(parent instanceof UIPropertySheet)) {
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
            UIComponent child = getChildren().get(1);
            if (Boolean.TRUE.equals(child.getAttributes().get(DONT_RENDER_IF_DISABLED_ATTR)) && ComponentUtil.isComponentDisabledOrReadOnly(child)) {
                return false;
            }
            if (getParent() instanceof WMUIPropertySheet) {
                WMUIPropertySheet parent = (WMUIPropertySheet) getParent();
                if (!parent.inEditMode() && !parent.isShowUnvalued()) {
                    if (child instanceof HandlesShowUnvalued) {
                        if (!((HandlesShowUnvalued) child).isShow()) {
                            return false;
                        }
                    } else {
                        @SuppressWarnings("unchecked")
                        List<QName> inlinePropertyGroupPropNames = (List<QName>) ComponentUtil.getAttributes(child).get(
                                InlinePropertyGroupGenerator.INLINE_PROPERTY_GROUP_PROP_NAMES_ATTR);
                        if (inlinePropertyGroupPropNames != null) {
                            Node node = parent.getNode();
                            for (QName propName : inlinePropertyGroupPropNames) {
                                Object value = node.getProperties().get(propName.toString());
                                if (isValueOrListNotBlank(value)) {
                                    return super.isRendered();
                                }
                            }
                            return false;
                        }
                        ValueBinding vb = child.getValueBinding("value");
                        if (vb == null) {
                            return false;
                        }
                        Object value = vb.getValue(getFacesContext());
                        if (!isValueOrListNotBlank(value)) {
                            return false;
                        }
                    }
                }
            }
        }
        return super.isRendered();
    }

    private boolean isValueOrListNotBlank(Object value) {
        if (value instanceof String) {
            if (StringUtils.isNotBlank((String) value)) {
                return true;
            }
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (Object listValue : list) {
                if (listValue instanceof String) {
                    if (StringUtils.isNotBlank((String) listValue)) {
                        return true;
                    }
                } else {
                    if (listValue != null) {
                        return true;
                    }
                }
            }
        } else {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void generateLabel(FacesContext context, UIPropertySheet propSheet, String displayLabel) {
        HtmlOutputText label = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        label.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
        FacesHelper.setupComponentId(context, label, "label_" + getName());
        if (isCheckboxRendered()) {
            getChildren().add(createCheckboxAfterLabel(context, propSheet, label));
        } else {
            getChildren().add(label);
        }

        // Check if config has overriden the label with parameter
        if (getCustomAttributes().containsKey(DISPLAY_LABEL_PARAMETER)) {
            String parameterName = getCustomAttributes().get(DISPLAY_LABEL_PARAMETER);
            ParametersService parametersService = (ParametersService) FacesHelper.getManagedBean(context, ParametersService.BEAN_NAME);
            displayLabel = parametersService.getStringParameter(Parameters.get(parameterName));
        }

        // remember the display label used (without the : separator)
        resolvedDisplayLabel = displayLabel;

        label.getAttributes().put("value", displayLabel);
        label.setStyleClass(getCustomAttributes().get(LABEL_STYLE_CLASS));
    }

    private boolean isCheckboxRendered() {
        return new Boolean(getCustomAttributes().get(RENDER_CHECKBOX_AFTER_LABEL));
    }

    private UIComponent createCheckboxAfterLabel(FacesContext context, UIPropertySheet propSheet, HtmlOutputText label) {
        final HtmlPanelGrid container = (HtmlPanelGrid) context.getApplication().createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        container.setWidth("100%");
        container.setColumns(2);
        UIComponent component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
        component.setRendererType(ComponentConstants.JAVAX_FACES_CHECKBOX);
        component.setValueBinding("value", ComponentUtil.createValueBinding(context, propSheet.getVar(), getName() + AFTER_LABEL_BOOLEAN));
        ComponentUtil.addChildren(container, label, component);
        container.setColumnClasses((String) propSheet.getAttributes().get("labelStyleClass"));
        return container;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        propertySheetItemAttributes = (Map<String, String>) values[1];
    }

    @Override
    public Object saveState(FacesContext context) {
        Object values[] = new Object[] {
                super.saveState(context),
                propertySheetItemAttributes
        };
        return values;
    }

}
