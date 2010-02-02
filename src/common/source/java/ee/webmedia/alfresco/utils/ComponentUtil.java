package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItem;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.generator.IComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

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

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> void getChildrenByClass(List<T> list, UIComponent parentComponent, Class<T> childComponentClass, String idSuffix) {
        List<UIComponent> children = parentComponent.getChildren();
        for (UIComponent childComponent : children) {
            if (childComponentClass.isAssignableFrom(childComponent.getClass())) {
                if (childComponent.getId().endsWith(idSuffix)) {
                    list.add((T) childComponent);
                    continue;
                }
            }
            getChildrenByClass(list, childComponent, childComponentClass, idSuffix);
        }
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

    public static void setSelectItems(FacesContext context, UIComponent component, List<SelectItem> selectItems) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.clear();
        children.addAll(generateSelectItems(context, selectItems));
    }

    public static void addSelectItems(FacesContext context, UIComponent component, List<SelectItem> selectItems) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.addAll(generateSelectItems(context, selectItems));
    }

    public static List<UISelectItem> generateSelectItems(FacesContext context, List<SelectItem> selectItems) {
        if (selectItems == null) {
            return Collections.emptyList();
        }
        List<UISelectItem> results = new ArrayList<UISelectItem>(selectItems.size());
        for (SelectItem selectItem : selectItems) {
            UISelectItem uiSelectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            uiSelectItem.setItemValue(selectItem.getValue());
            uiSelectItem.setItemLabel(selectItem.getLabel());
            uiSelectItem.setItemDescription(selectItem.getDescription());
            uiSelectItem.setItemDisabled(selectItem.isDisabled());
            results.add(uiSelectItem);
        }
        return results;
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

    public static final String ATTR_DISPLAY_LABEL = "displayLabel";

    // parent component (whose 'children' is provided) needs to be added to component tree before calling this method, because the validations are setup and
    // these need access to the component id, which in turn needs to have a parent to get the correct id
    //
    // spec (fields separated by |):
    //   1) propName, mandatory, e.g. doccom:docName
    //   2) component generator name, optional, e.g. TextFieldGenerator, see more from RepoConstants
    //   3-...) custom attributes, e.g. styleClass=inline
    public static UIComponent generateComponent(FacesContext context, String propertySheetVar, String spec, UIPropertySheet propertySheet,
            PropertySheetItem item, final List<UIComponent> children) {

        String[] fields = spec.split("\\|");

        final String propName;
        if (fields.length >= 1) {
            propName = fields[0];
        } else {
            propName = null;
        }
        if (StringUtils.isEmpty(propName)) {
            throw new RuntimeException("Property name must be specified");
        }
        PropertyDefinition propDef = getPropertyDefinition(context, propertySheet.getNode(), propName);

        String generatorName;
        if (fields.length >= 2 && StringUtils.isNotEmpty(fields[1])) {
            generatorName = fields[1];
        } else {
            if (propDef != null) {
                QName dataTypeName = propDef.getDataType().getName();
                generatorName = getDefaultGeneratorName(dataTypeName);
                if (generatorName == null) {
                    throw new RuntimeException("Component generator name not specified and default generator not found for data type " + dataTypeName
                            + ", property name '" + propName + "'");
                }
            } else {
                throw new RuntimeException("Component generator name not specified and property definition not found for property name '" + propName + "'");
            }
        }

        final String label = resolveDisplayLabel(context, propDef, propName);
        PropertySheetItem fakeItem = new WMUIProperty() {
            @Override
            public String getName() {
                return propName;
            }
            @Override
            public String getResolvedDisplayLabel() {
                return label;
            }
            @Override
            @SuppressWarnings("unchecked")
            public List getChildren() {
                return children;
            }
        };

        IComponentGenerator generator = FacesHelper.getComponentGenerator(context, generatorName);
        if (generator instanceof CustomAttributes && fields.length >= 3) {
            Map<String, String> customAttributes = new HashMap<String, String>();
            for (int i = 2; i < fields.length; i++) {
                if (fields[i] == null || fields[i].split("=").length != 2) {
                    throw new RuntimeException("Field " + (i + 1) + " does not contain custom attribute, spec '" + spec + "'");
                }
                String[] parts = fields[i].split("=");
                customAttributes.put(parts[0], parts[1]);
            }
            ((CustomAttributes) generator).setCustomAttributes(customAttributes);
        }
        UIComponent component = generator.generateAndAdd(context, propertySheet, fakeItem);

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put(ATTR_DISPLAY_LABEL, label);

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

    public static PropertyDefinition getPropertyDefinition(FacesContext context, Node node, String propName) {
        PropertyDefinition propDef = null;
        DictionaryService dictionaryService = Repository.getServiceRegistry(context).getDictionaryService();
        TypeDefinition typeDef = dictionaryService.getAnonymousType(node.getType(), node.getAspects());
        if (typeDef != null) {
            Map<QName, PropertyDefinition> properties = typeDef.getProperties();
            propDef = properties.get(Repository.resolveToQName(propName));
        }
        return propDef;
    }

    public static String resolveDisplayLabel(FacesContext context, PropertyDefinition propDef, String propName) {
        String displayLabel = null;
        if (propDef != null) {
            // try and get the repository assigned label
            displayLabel = propDef.getTitle();
            if (displayLabel == null) {
                // if the label is still null default to the local name of the property
                displayLabel = propDef.getName().getLocalName();
            }
        }
        if (displayLabel == null) {
            displayLabel = propName;
        }
        return displayLabel;
    }

    public static String getDefaultGeneratorName(QName dataTypeName) {
        String generatorName = null;
        if (dataTypeName.equals(DataTypeDefinition.TEXT) || dataTypeName.equals(DataTypeDefinition.DOUBLE) || dataTypeName.equals(DataTypeDefinition.FLOAT)
                || dataTypeName.equals(DataTypeDefinition.INT) || dataTypeName.equals(DataTypeDefinition.LONG)) {
            generatorName = RepoConstants.GENERATOR_TEXT_FIELD;
        } else if (dataTypeName.equals(DataTypeDefinition.BOOLEAN)) {
            generatorName = RepoConstants.GENERATOR_CHECKBOX;
        } else if (dataTypeName.equals(DataTypeDefinition.DATETIME)) {
            generatorName = RepoConstants.GENERATOR_DATETIME_PICKER;
        } else if (dataTypeName.equals(DataTypeDefinition.DATE)) {
            generatorName = RepoConstants.GENERATOR_DATE_PICKER;
        }
        return generatorName;
    }

}
