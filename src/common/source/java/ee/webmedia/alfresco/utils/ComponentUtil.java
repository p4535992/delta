package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;

import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;

/**
 * Util methods for JSF components/component trees
 * 
 * @author Ats Uiboupin
 */
public class ComponentUtil {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ComponentUtil.class);

    /**
     * @param component - UIComponent to be searched from(looking towards the ancestors up to UIPropertySheet and then down to UIProperty matching
     *            <code>searchPropertyIdSuffix</code>)
     * @param searchPropertyIdSuffix - used to validate if <code>uiProperty.getId().endsWith(propertyIdSuffix)</code>
     * @return UIInput from the same UIPropertySheet as the given <code>component</code> where id ends with given <code>searchPropertyIdSuffix</code>
     */
    public static UIInput getInputFromSamePropertySheet(UIComponent component, String searchPropertyIdSuffix) {
        UIPropertySheet propSheetComponent = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class, true);
        UIProperty matchingProperty = ComponentUtil.findUIPropertyByIdSuffix(propSheetComponent, searchPropertyIdSuffix);
        UIInput propertyInput = getInputOfProperty(matchingProperty);
        return propertyInput;
    }

    /**
     * @see ComponentUtil#getAncestorComponent(UIComponent, Class, boolean, StringBuilder)
     */
    public static <T extends UIComponent> T getAncestorComponent(UIComponent componentFrom, Class<T> toComponentClass, boolean useInstanceOf) {
        return getAncestorComponent(componentFrom, toComponentClass, useInstanceOf, null);
    }

    /**
     * @param <T> Type of component to be searched for and returned.
     * @param componentFrom - component that is used as a base for finding ancestor component matching given class
     * @param toComponentClass - class to be used to determine if one of the parent components is actually a component that is being searched for.
     * @param useInstanceOf - if true, then subclasses of given class also qualify as as component being searched for, otherwise classes must be equal.
     * @param debugBuffer - buffer that will contain path to component that is being searched for(handy for debugging in development).
     * @return null if no component found, otherwise component that is found.
     */
    public static <T extends UIComponent> T getAncestorComponent(UIComponent componentFrom //
            , Class<T> toComponentClass, boolean useInstanceOf, StringBuilder debugBuffer) {
        if (componentFrom == null)
            return null;
        if (debugBuffer != null) {
            StringBuilder intBuf = new StringBuilder("[Class: ");
            intBuf.append(componentFrom.getClass().getName());
            if (componentFrom instanceof UIViewRoot) {
                intBuf.append(",ViewId: ");
                intBuf.append(((UIViewRoot) componentFrom).getViewId());
            } else {
                intBuf.append(",Id: ");
                intBuf.append(componentFrom.getId());
            }
            intBuf.append("]\n");
            debugBuffer.insert(0, intBuf.toString());
        }
        if (componentFrom.getClass().equals(toComponentClass)) {
            @SuppressWarnings("unchecked")
            T result = (T) componentFrom;
            return result;
        } else if (useInstanceOf && toComponentClass.isAssignableFrom(componentFrom.getClass())) {
            @SuppressWarnings("unchecked")
            T result = (T) componentFrom;
            return result;
        }
        return getAncestorComponent(componentFrom.getParent(), toComponentClass, useInstanceOf, debugBuffer);
    }

    /**
     * @param propSheetComponent
     * @param searchPropertyIdSuffix - used to validate if <code>uiProperty.getId().endsWith(propertyIdSuffix)</code>
     * @return UIProperty from given <code>propSheetComponent</code> that has id ending with given <code>searchPropertyIdSuffix</code>
     */
    public static UIProperty findUIPropertyByIdSuffix(UIPropertySheet propSheetComponent, String propertyIdSuffix) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = propSheetComponent.getChildren();
        ArrayList<UIProperty> matchingProperties = new ArrayList<UIProperty>(2);
        for (UIComponent childComponent : children) {
            if (childComponent instanceof UIProperty) {
                String childId = ((UIProperty) childComponent).getId();
                if (StringUtils.isNotBlank(childId) && childId.endsWith(propertyIdSuffix)) {
                    matchingProperties.add((UIProperty) childComponent);
                }
            }
        }
        if (matchingProperties.size() != 1) {
            log.error("found " + matchingProperties.size() + " children with propertyIdSuffix='" + propertyIdSuffix + "':");
            for (UIProperty uiProperty : matchingProperties) {
                log.error("\nuiProperty=" + uiProperty);
            }
            throw new RuntimeException("Expected to find only one UIProperty with given suffix '" + propertyIdSuffix + "', but found "
                    + matchingProperties.size());
        }
        return matchingProperties.get(0);
    }

    /**
     * @param <T>
     * @param propSheetComponent
     * @param toComponentClass
     * @return all inputs of class <code>toComponentClass</code> from given <code>propSheetComponent</code>
     */
    public static <T extends UIInput> List<T> findInputsByClass(UIPropertySheet propSheetComponent, Class<T> toComponentClass) {
        final List<UIProperty> uiProperties = getChildrenByClass(propSheetComponent, UIProperty.class);
        ArrayList<T> matchingComponents = new ArrayList<T>();
        for (UIProperty uiProperty : uiProperties) {
            matchingComponents.addAll(getChildrenByClass(uiProperty, toComponentClass));
        }
        return matchingComponents;
    }

    // public static <F extends UIComponent, T extends UIComponent> List<T> getChildrenByClass(F parentComponent, Class<T> childComponentClass) {
    // XXX: võiks toimida ka eelnev rida, aga miskipärast ei leia runtime's sobivat meetodit..peaks üle vaatama selle
    public static <F, T extends UIComponent> List<T> getChildrenByClass(F parentComponent, Class<T> childComponentClass) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = ((UIComponent) parentComponent).getChildren();
        ArrayList<T> matchingComponents = new ArrayList<T>();
        for (UIComponent childComponent : children) {
            if (childComponentClass.isAssignableFrom(childComponent.getClass())) {
                @SuppressWarnings("unchecked")
                final T comp = (T) childComponent;
                matchingComponents.add(comp);
            }
        }
        return matchingComponents;
    }

    /**
     * @param uiProperty - component used to find label from
     * @param propertyName - used only in case of exception
     * @return label that is shown to the user(without ":" in the end if it is present)
     */
    public static String getPropertyLabel(UIProperty uiProperty, String propertyName) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = uiProperty.getChildren();
        for (UIComponent comp : children) {
            if (comp instanceof UIOutput) {
                String otherLabel = (String) ((UIOutput) comp).getValue();
                if (otherLabel.endsWith(":")) {
                    otherLabel = otherLabel.substring(0, otherLabel.length() - 1);
                }
                return otherLabel;
            }
        }
        throw new RuntimeException("Can't find the lable for '" + propertyName + "'");
    }

    /**
     * @param uiProperty
     * @return first(hopefully the only) input of given <code>uiProperty</code>
     */
    private static UIInput getInputOfProperty(UIProperty uiProperty) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = uiProperty.getChildren();
        for (UIComponent uiComponent : children) {
            if (uiComponent instanceof UIInput) {
                return (UIInput) uiComponent;
            }
        }
        return null;
    }

    /**
     * @param component - this component and all its children will recursively be set disabled
     */
    public static void setDisabledAttributeRecursively(UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("readonly", Boolean.TRUE);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        } else {
            for (UIComponent childComponent : children) {
                setDisabledAttributeRecursively(childComponent);
            }
        }
    }

    /**
     * Creates the converter with the given id and adds it to the component. Implementation copied from {@link BaseComponentGenerator}.
     * 
     * @param context FacesContext
     * @param converterId The name of the converter to create, ignored if {@code null}
     * @param component The component to add the converter to, ignored if not instance of {@link ValueHolder}
     */
    public static void createAndSetConverter(FacesContext context, String converterId, UIComponent component) {
        if (converterId != null && component instanceof ValueHolder) {
            try {
                Converter conv = context.getApplication().createConverter(converterId);
                ((ValueHolder) component).setConverter(conv);
            } catch (NullPointerException npe) {
                // workaround a NPE bug in MyFaces
                log.warn("Converter '" + converterId + "' could not be applied to component: " + component.getId());
            } catch (FacesException fe) {
                log.warn("Converter '" + converterId + "' could not be applied to component: " + component.getId());
            }
        }
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on
     * {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
     */
    public static String generateFieldSetter(FacesContext context, UIComponent component, String value) {
        String fieldId = component.getClientId(context);
        return generateFieldSetter(context, component, fieldId, value);
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on
     * {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
     */
    public static String generateFieldSetter(FacesContext context, UIComponent component, String fieldId, String value) {
        UIForm form = Utils.getParentForm(context, component);
        if (form == null) {
            throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
        }
        String formClientId = form.getClientId(context);
        HtmlFormRendererBase.addHiddenCommandParameter(context, form, fieldId);
        StringBuilder buf = new StringBuilder(200);
        buf.append("document.forms['");
        buf.append(formClientId);
        buf.append("']['");
        buf.append(fieldId);
        buf.append("'].value='");
        String val = StringUtils.replace(value, "\\", "\\\\"); // encode escape character
        val = StringUtils.replace(val, "'", "\\'"); // encode single quote as we wrap string with that
        buf.append(val);
        buf.append("';");
        return buf.toString();
    }

    public static UIComponent generateComponent(FacesContext context, String propertySheetVar, String idPrefix, String spec) {
        UIComponent component;
        String[] fields = spec.split("\\|");

        String propName = null;
        String id = null;
        if (fields.length >= 1) {
            propName = fields[0];
            id = idPrefix + "_" + propName;
        }
        String type = "";
        if (fields.length >= 2) {
            type = fields[1];
        }

        String styleClass = "";
        if ("textarea".equals(type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            FacesHelper.setupComponentId(context, component, id);
            component.setRendererType(ComponentConstants.JAVAX_FACES_TEXTAREA);
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            // Default values from TextAreaGenerator
            attributes.put("rows", 3);
            attributes.put("cols", 32);

        } else if ("boolean".equals(type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
            FacesHelper.setupComponentId(context, component, id);
            component.setRendererType(ComponentConstants.JAVAX_FACES_CHECKBOX);

        } else if ("date".equals(type)) {
                component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
                FacesHelper.setupComponentId(context, component, id);
                styleClass = "date ";
                createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
/*
                List<String> params = new ArrayList<String>(2);

                // add the value parameter
                String value = "document.getElementById('" + component.getClientId(context) + "')";
                params.add(value);

                // add the validation failed messages
                String matchMsg = Application.getMessage(context, "validation_date_failed");
                addStringConstraintParam(params, MessageFormat.format(matchMsg, new Object[] { property.getResolvedDisplayLabel() }));

                // add the validation case to the property sheet
                propertySheet.addClientValidation(new ClientValidation("validateDate", params, true));
*/

        } else {
            if (StringUtils.isNotEmpty(type) && !"input".equals(type)) {
                log.warn("Component type '" + type + "' is not supported, defaulting to input");
            }
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            FacesHelper.setupComponentId(context, component, id);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();

        if (StringUtils.isNotEmpty(propName)) {
            component.setValueBinding("value", createValueBinding(context, propertySheetVar, propName));
        }

        if (fields.length >= 3) {
            styleClass += fields[2];
        }
        if (StringUtils.isNotEmpty(styleClass)) {
            attributes.put("styleClass", styleClass);
        }

        return component;
    }

    public static ValueBinding createValueBinding(FacesContext context, String propertySheetVar, String propName) {
        return createValueBinding(context, propertySheetVar, propName, -1);
    }

    public static ValueBinding createValueBinding(FacesContext context, String propertySheetVar, String propName, int rowIndex) {
        ValueBinding vb = context.getApplication().createValueBinding(
                "#{" + propertySheetVar + ".properties[\"" + propName + "\"]" + (rowIndex >= 0 ? "[" + rowIndex + "]" : "") + "}");
        return vb;
    }

}
