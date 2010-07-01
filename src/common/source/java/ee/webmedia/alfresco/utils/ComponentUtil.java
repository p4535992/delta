package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItem;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.component.NodeAssocBrand;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Util methods for JSF components/component trees
 * 
 * @author Ats Uiboupin
 */
public class ComponentUtil {
    private static final String JSF_CONVERTER = "jsfConverter";
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ComponentUtil.class);
    private static GeneralService generalService;

    /**
     * Put attribute to given component
     * 
     * @param component
     * @param key
     * @param value
     * @return attributes of the component
     */
    public static Map<String, Object> putAttribute(UIComponent component, final String key, final Object value) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        attributes.put(key, value);
        return attributes;
    }

    public static <T> T gettAttribute(UIComponent component, String key, Class<T> requiredClazz) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        return DefaultTypeConverter.INSTANCE.convert(requiredClazz, attributes.get(key));
    }

    /**
     * Add children to the component
     * 
     * @param component
     * @param children - children to be added or null - then just children of the component are returned without adding any children
     * @return children of the component
     */
    public static List<UIComponent> addChildren(UIComponent component, UIComponent... children) {
        @SuppressWarnings("unchecked")
        final List<UIComponent> componentChildren = component.getChildren();
        if (children != null) {
            for (UIComponent child : children) {
                componentChildren.add(child);
            }
        }
        return componentChildren;
    }

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

    public static <T extends UIComponent> T getAncestorComponent(UIComponent componentFrom, Class<T> toComponentClass) {
        return getAncestorComponent(componentFrom, toComponentClass, true, null);
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
        Assert.notNull(parentComponent, "parentComponent is not supposed to be null when searching childComponents by class '"
                + childComponentClass.getCanonicalName() + "'");
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
                final String childId = childComponent.getId();
                if (childId != null && childId.endsWith(idSuffix)) {
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
        setDisabledAttributeRecursively(component, Boolean.TRUE);
    }

    public static void setDisabledAttributeRecursively(UIComponent component, Boolean value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("readonly", value);
        if (component instanceof UIOutput) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            setDisabledAttributeRecursively(childComponent, value);
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

    public static void setSelectItems(FacesContext context, UIInput component, List<SelectItem> selectItems) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.clear();
        setDisabledAttributeRecursively(component, selectItems == null);
        if (selectItems == null) {
            component.setValue(null);
        } else {
            children.addAll(generateSelectItems(context, selectItems));
        }
        setHtmlSelectManyListboxSize(component, selectItems);
    }

    /**
     * Sets the listbox size (if is listbox) so that size is 1 .. 4
     * 
     * @param component
     * @param selectItems
     */
    public static void setHtmlSelectManyListboxSize(UIComponent component, List<?> selectItems) {
        if (component instanceof HtmlSelectManyListbox) {
            int itemCount = selectItems.size();

            if (itemCount > 4) {
                itemCount = 4;
            }
            if (itemCount < 1) {
                itemCount = 1;
            }

            ((HtmlSelectManyListbox) component).setSize(itemCount);
        }
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
        return generateFieldSetter(context, form, fieldId, value);
    }

    public static String generateFieldSetter(FacesContext context, UIForm form, String fieldId, String value) {
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

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component) {
        return generateAjaxFormSubmit(context, component, null, null);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value) {
        return generateAjaxFormSubmit(context, component, fieldId, value, null, 0);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value, Map<String, String> params) {
        return generateAjaxFormSubmit(context, component, fieldId, value, params, 0);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value, int parentLevel) {
        return generateAjaxFormSubmit(context, component, fieldId, value, null, parentLevel);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value //
            , Map<String, String> params, int parentLevel) {
        Assert.isTrue(parentLevel >= 0, "parentLevel cannot be negative");
        UIComponent ajaxComponent = null;
        int parentLevelFound = -1;
        while (parentLevelFound < parentLevel) {
            if (ajaxComponent == null) {
                ajaxComponent = component;
            } else {
                ajaxComponent = ajaxComponent.getParent();
                if (ajaxComponent == null) {
                    break;
                }
            }
            if (ajaxComponent instanceof AjaxUpdateable && !Boolean.TRUE.equals(ajaxComponent.getAttributes().get(AjaxUpdateable.AJAX_DISABLED_ATTR))) {
                parentLevelFound++;
            }
        }

        // If desired upper level parent AjaxUpdateable component not found, do full submit
        if (ajaxComponent == null || parentLevelFound < parentLevel) {
            if (fieldId == null) {
                return Utils.generateFormSubmit(context, component);
            } else if (params == null) {
                return Utils.generateFormSubmit(context, component, fieldId, value);
            }
            return Utils.generateFormSubmit(context, component, fieldId, value, params);
        }

        // Otherwise do AJAX submit

        UIForm form = Utils.getParentForm(context, component);
        if (form == null) {
            throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
        }

        Set<String> submittableParams = new HashSet<String>();
        String clientId = ajaxComponent.getClientId(context);
        StringBuilder s = new StringBuilder();

        if (fieldId != null) {
            s.append(generateFieldSetter(context, form, fieldId, value));
            if (!fieldId.startsWith(clientId)) {
                submittableParams.add(fieldId);
            }
        }

        if (params != null) {
            for (Entry<String, String> param : params.entrySet()) {
                String paramName = param.getKey();
                s.append(generateFieldSetter(context, form, paramName, param.getValue()));
                if (!paramName.startsWith(clientId)) {
                    submittableParams.add(paramName);
                }
            }
        }

        s.append("ajaxSubmit('").append(ajaxComponent.getId()).append("','");
        s.append(clientId).append("','");
        s.append(((AjaxUpdateable) ajaxComponent).getAjaxClientId(context)).append("','");
        s.append(form.getClientId(context)).append("','");
        s.append(context.getViewRoot().getViewId()).append("',[");
        for (Iterator<String> i = submittableParams.iterator(); i.hasNext();) {
            String submittableParam = i.next();
            s.append("'").append(submittableParam).append("'");
            if (i.hasNext()) {
                s.append(",");
            }
        }
        s.append("]);return false;");
        return s.toString();
    }

    public static void setAjaxDisabled(UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put(AjaxUpdateable.AJAX_DISABLED_ATTR, Boolean.TRUE);
    }

    public static final String ATTR_DISPLAY_LABEL = "displayLabel";

    /**
     * NB! If NOT <code>componentPropVO.isUseComponentGenerator()</code>, then component generators are not used, and hence for example value bindings and
     * validations are not set up.<br>
     * If <code>componentPropVO.isUseComponentGenerator()</code> then parent component (whose 'children' is provided) needs to be added to component tree before
     * calling this method, because the validations are setup and
     * these need access to the component id, which in turn needs to have a parent to get the correct id.<br>
     * 
     * @param context
     * @param componentPropVO
     * @param propertySheet
     * @param children
     * @return component generated based on <code>singlePropVO</code> that is added to list of given <code>children</code> that must come from given
     *         <code>propertySheet</code> where the generated component is added.
     */
    public static UIComponent generateAndAddComponent(FacesContext context, ComponentPropVO componentPropVO, UIPropertySheet propertySheet,
            final List<UIComponent> children) {
        if (!componentPropVO.isUseComponentGenerator()) {
            final UIComponent component = createCellComponent(context, componentPropVO);
            children.add(component);
            return component;
        }
        final String propName = componentPropVO.getPropertyName();

        final String label = componentPropVO.getPropertyLabel();
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

        Map<String, String> customAttributes = componentPropVO.getCustomAttributes();
        ((CustomAttributes) fakeItem).setCustomAttributes(customAttributes);

        UIComponent component = componentPropVO.getComponentGenerator(context).generateAndAdd(context, propertySheet, fakeItem);

        if (component instanceof UIOutput) {
            final String converterName = customAttributes.get(JSF_CONVERTER);
            if (StringUtils.isNotBlank(converterName)) {
                try {// XXX: pm võiks külge panna ka property tüübi järgi mingid default
                    // converterid(a la double tüübi puhul DoubleConverter), et ei peaks käsitsi attribuute lisama
                    @SuppressWarnings("unchecked")
                    final Class<Converter> converterClass = (Class<Converter>) Class.forName(converterName);
                    final Converter converter = converterClass.newInstance();
                    ((UIOutput) component).setConverter(converter);
                } catch (Exception e) {
                    throw new RuntimeException("Can't initialize converter with class name '" + converterName // 
                            + "' while creating property '" + propName + "'", e);
                }
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put(ATTR_DISPLAY_LABEL, label);

        return component;
    }

    /**
     * Method that constructs component without using componentGenerators
     * 
     * @param context
     * @param vo
     * @return
     */
    private static UIComponent createCellComponent(FacesContext context, ComponentPropVO vo) {
        UIComponent component = null;

        String type = vo.getGeneratorName();
        final Map<String, String> voCustomAttributes = vo.getCustomAttributes();
        if (StringUtils.equals(RepoConstants.GENERATOR_TEXT_AREA, type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            component.setRendererType(ComponentConstants.JAVAX_FACES_TEXTAREA);
        } else if (StringUtils.equals(RepoConstants.GENERATOR_DATE_PICKER, type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put("styleClass", "date");
            ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
        } else if (StringUtils.equals("ClassificatorSelectorGenerator", type)) {
            if (voCustomAttributes.containsKey(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME)) {
                ClassificatorSelectorGenerator classificGenerator = new ClassificatorSelectorGenerator();
                classificGenerator.setCustomAttributes(voCustomAttributes);
                component = classificGenerator.generateSelectComponent(context, null, false);
                classificGenerator.setupSelectComponent(context, null, null, null, component, false);
            } else {
                throw new RuntimeException("Component type '" + type + "' requires a classificator name in definition. Failing fast!");
            }
        } else if (StringUtils.isNotEmpty(type) && !StringUtils.equals(RepoConstants.GENERATOR_TEXT_FIELD, type)) {
            log.warn("Component type '" + type + "' is not supported, defaulting to input");
        }

        if (component == null) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
        }

        final String styleClass = voCustomAttributes.get(BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS);
        if (StringUtils.isNotBlank(styleClass)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put(BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS, styleClass);
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

    public static PropertyDefinition getPropertyDefinition(FacesContext context, Node node, String propName) {
        PropertyDefinition propDef = null;
        TypeDefinition typeDef = getGeneralService().getAnonymousType(node);
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

    public static UIComponent findComponentById(FacesContext context, UIComponent root, String id) {
        UIComponent component = null;

        for (int i = 0; i < root.getChildCount() && component == null; i++) {
            UIComponent child = (UIComponent) root.getChildren().get(i);
            component = findComponentById(context, child, id);
        }

        if (root.getId() != null) {
            if (component == null && root.getId().equals(id)) {
                component = root;
            }
        }
        return component;
    }

    public static UIComponent findChildComponentById(FacesContext context, UIComponent component, String id, String clientId) {
        if (component == null) {
            return null;
        }
        if (id.equals(component.getId()) && clientId.equals(component.getClientId(context))) {
            return component;
        }
        for (int i = 0; i < component.getChildCount(); i++) {
            UIComponent child = (UIComponent) component.getChildren().get(i);
            UIComponent result = findChildComponentById(context, child, id, clientId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static <T> List<T> findChildComponentsByClass(FacesContext context, UIComponent component, Class<? extends T> clazz) {
        List<T> results = null;
        if (component == null) {
            return null;
        }
        for (int i = 0; i < component.getChildCount(); i++) {
            UIComponent child = (UIComponent) component.getChildren().get(i);
            List<T> results2 = findChildComponentsByClass(context, child, clazz);
            if (results2 != null) {
                if (results == null) {
                    results = results2;
                } else {
                    results.addAll(results2);
                }
            }
        }
        if (clazz.isAssignableFrom(component.getClass())) {
            @SuppressWarnings("unchecked")
            T resultComponent = (T) component;
            if (results == null) {
                results = new ArrayList<T>();
                results.add(resultComponent);
            } else {
                results.add(resultComponent);
            }
        }
        return results;
    }

    public static UIComponent findParentComponentById(FacesContext context, UIComponent component, String id, String clientId) {
        while (component != null) {
            if (id.equals(component.getId()) && clientId.equals(component.getClientId(context))) {
                return component;
            }
            component = component.getParent();
        }
        return null;
    }

    /**
     * @param context
     * @param propertySheet
     * @param propKey
     * @param valueIndexSuffix - index between square brackets or empty string, but not null!
     * @return If given propSheetItem <code>item</code> is on nested propertySheet then reference is returned that could be used to create valueBinding, <br>
     *         null otherwise
     */
    public static String getValueBindingFromSubPropSheet(final FacesContext context, UIPropertySheet propertySheet, String propKey, String valueIndexSuffix) {
        final List<AssocInfoHolder> pathInfos = new ArrayList<AssocInfoHolder>();
        UIPropertySheet ancestorPropSheet = propertySheet;
        do {
            if (ancestorPropSheet instanceof WMUIPropertySheet) {
                WMUIPropertySheet wmPropSheet = (WMUIPropertySheet) ancestorPropSheet;
                final Integer associationIndex = wmPropSheet.getAssociationIndex();
                if (associationIndex != null) {
                    final AssocInfoHolder assocInfoHolder = new AssocInfoHolder();
                    final SubPropertySheetItem subPropSheetItem = ComponentUtil.getAncestorComponent(ancestorPropSheet, SubPropertySheetItem.class, true);
                    assocInfoHolder.assocTypeQName = subPropSheetItem.getAssocTypeQName();
                    assocInfoHolder.associationBrand = wmPropSheet.getAssociationBrand();
                    assocInfoHolder.associationIndex = associationIndex;
                    assocInfoHolder.validate();
                    pathInfos.add(assocInfoHolder);
                    UIPropertySheet nextAncestorPropSheet = ComponentUtil.getAncestorComponent(subPropSheetItem, UIPropertySheet.class, true);
                    if (nextAncestorPropSheet != null) {
                        ancestorPropSheet = nextAncestorPropSheet;
                        continue;
                    }
                }
            }
            break; // exit loop when no next ancestorPropSheet found
        } while (true);

        if (pathInfos.size() != 0) { // item is on SubPropertySheet?
            String vb = "";
            for (AssocInfoHolder assocInf : pathInfos) {
                if (assocInf.associationBrand == NodeAssocBrand.CHILDREN) {
                    vb = assocInf.getValueBindingPart() + vb;
                } else {
                    throw new RuntimeException("Creating value binding for associationBrand " + assocInf.associationBrand + " is unimplemented!");
                }
            }
            Assert.notNull(valueIndexSuffix, "valueIndexSuffix index between square brackets or empty string, but not null!");
            return "#{" + ancestorPropSheet.getVar() + vb + ".properties[\"" + propKey + "\"]" + valueIndexSuffix + "}";
        }
        return null;
    }

    private static GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    /** private class to hold association info for creating value binding */
    private static class AssocInfoHolder {
        int associationIndex;
        NodeAssocBrand associationBrand;
        QName assocTypeQName;

        void validate() {
            Assert.notNull(associationIndex, "associationIndex is mandatory for subPropertySheetItem");
            Assert.notNull(associationBrand, "wmPropSheet has associationIndex=" + associationIndex + ", but no associationBrand=" + associationBrand);
            Assert.notNull(assocTypeQName, "association type unKnown!");
        }

        public String getValueBindingPart() {
            if (NodeAssocBrand.CHILDREN.equals(associationBrand)) {
                return ".allChildAssociationsByAssocType[\"" + assocTypeQName.toString() + "\"][" + associationIndex + "]";
            }
            throw new RuntimeException("Getting ValueBinding for associationBrand='" + associationBrand + "' is unimplemented");
        }

        @Override
        public String toString() {
            return "AssocInfoHolder: " + getValueBindingPart();
        }
    }

}