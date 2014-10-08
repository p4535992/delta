<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.config.PropertySheetElementReader;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.alfresco.web.ui.common.renderer.ActionLinkRenderer;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.tag.LoadBundleTag;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.component.NodeAssocBrand;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem.AddRemoveActionListener;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.web.DocPermissionEvaluator;

/**
 * Util methods for JSF components/component trees
 * 
 * @author Ats Uiboupin
 */
public class ComponentUtil {
    private static final String ATTR_STYLE_CLASS = "styleClass";
    private static final String JSF_CONVERTER = "jsfConverter";
    public static final String IS_ALWAYS_EDIT = "isAlwaysEdit";
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ComponentUtil.class);
    private static GeneralService generalService;
    public static final String DEFAULT_SELECT_VALUE = "";

    private static final Comparator<UIComponent> BY_LABEL_COMPARATOR;
    static {
        @SuppressWarnings("unchecked")
        Comparator<UIComponent> byLabelComparator = new TransformingComparator(new ComparableTransformer<UIComponent>() {
            @Override
            public Comparable<?> tr(UIComponent input) {
                if (input instanceof UISelectItem) {
                    return ((UISelectItem) input).getItemLabel();
                }
                return null;
            }
        }, new NullComparator());
        BY_LABEL_COMPARATOR = byLabelComparator;
    }

    public static void sortByLabel(List<UIComponent> selectOptions) {
        Collections.sort(selectOptions, BY_LABEL_COMPARATOR);
    }

    public static UIComponent makeCondenced(final UIComponent component, int condenceSize) {
        putAttribute(component, ATTR_STYLE_CLASS, "condence" + condenceSize);
        return component;
    }

    public static void writeModalHeader(ResponseWriter out, String modalId, String modalTitle, String closeOnClick) throws IOException {
        writeModalHeader(out, modalId, modalTitle, "", closeOnClick);
    }

    public static void writeModalHeader(ResponseWriter out, String modalId, String modalTitle, String modalWrapStyleClass, String closeOnClick) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"");
        sb.append(modalId);
        sb.append("\" class=\"modalpopup modalwrap " + modalWrapStyleClass + "\">");
        sb.append("<div class=\"modalpopup-header clear\"><h1>");
        sb.append(modalTitle);
        sb.append("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        if (StringUtils.isBlank(closeOnClick)) {
            sb.append("return hideModal()");
        } else {
            sb.append(closeOnClick);
        }
        sb.append("\">");
        sb.append(MessageUtil.getMessage("close_window"));
        sb.append("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner modalpopup-filter\">");

        out.write(sb.toString());
    }

    public static void writeModalFooter(ResponseWriter out) throws IOException {
        // close modal popup
        out.write("</div></div></div>");
    }

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

    /**
     * Add all attributes from given map to given component
     */
    public static Map<String, Object> addAttributes(UIComponent component, Map<String, Object> attributesToAdd) {
        final Map<String, Object> attributes = getAttributes(component);
        if (attributesToAdd != null) {
            attributes.putAll(attributesToAdd);
        }
        return attributes;
    }

    /**
     * Convenience method that doesn't produce compiler warning
     * "Type safety: The expression of type Map needs unchecked conversion to conform to Map<String,Object>"
     * 
     * @param component
     * @return <code>component.getAttributes()</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAttributes(UIComponent component) {
        return component.getAttributes();
    }

    public static <T> Object getAttribute(UIComponent component, String key) {
        return getAttributes(component).get(key);
    }

    public static <T> T getAttribute(UIComponent component, String key, Class<T> requiredClazz) {
        Object value = getAttribute(component, key);
        T result;
        try {
            result = DefaultTypeConverter.INSTANCE.convert(requiredClazz, value);
        } catch (TypeConversionException e) {
            throw new TypeConversionException("Failed to convert component attribute '" + key + "'='" + value + "' to " + requiredClazz.getCanonicalName(), e);
        }
        return result;
    }

    /**
     * @param paramName
     * @param paramValue - could also be string to be used as value binding
     * @param application
     * @return
     */
    public static UIParameter createUIParam(String paramName, Object paramValue, Application application) {
        UIParameter param = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        param.setName(paramName);
        if (paramValue instanceof String) {
            UIComponentTagUtils.setValueProperty(FacesContext.getCurrentInstance(), param, (String) paramValue);
        } else {
            param.setValue(paramValue);
        }
        return param;
    }

    /**
     * @param actionDef
     * @param application
     * @param context
     * @param evaluationCheckNode - optional, but if present, then null is returned when there is evaluator and evaluation fails
     * @return
     */
    public static UIActionLink createActionFromConf(ActionDefinition actionDef, Application application, FacesContext context, Node evaluationCheckNode) {
        if (evaluationCheckNode != null && actionDef.Evaluator != null && !actionDef.Evaluator.evaluate(evaluationCheckNode)) {
            return null;
        }
        String actionId = actionDef.getId();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        link.setImage(actionDef.Image);
        FacesHelper.setupComponentId(context, link, actionId);
        String lblMessage = actionDef.LabelMsg != null ? MessageUtil.getMessage(context, actionDef.LabelMsg) : actionDef.Label;
        link.setValue(lblMessage);
        link.setActionListener(application.createMethodBinding(actionDef.ActionListener, new Class[] { javax.faces.event.ActionEvent.class }));
        String tooltip = actionDef.TooltipMsg != null ? MessageUtil.getMessage(context, actionDef.TooltipMsg) : actionDef.Tooltip;
        link.setTooltip(tooltip);

        final AddRemoveActionListener listener = new AddRemoveActionListener();
        link.addActionListener(listener);
        link.setShowLink(actionDef.ShowLink);

        @SuppressWarnings("unchecked")
        List<UIComponent> children = link.getChildren();
        for (Entry<String, String> entry : actionDef.getParams().entrySet()) {
            UIParameter param = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            param.setName(entry.getKey());
            param.setValue(entry.getValue());
            children.add(param);
        }

        return link;
    }

    public static UIComponent setTooltip(final UIComponent component, final String tooltip) {
        putAttribute(component, "title", tooltip);
        return component;
    }

    /**
     * Add children to the component
     * 
     * @param component
     * @param children - children to be added or null - then just children of the component are returned without adding any children
     * @return children of the component
     */
    public static List<UIComponent> addChildren(UIComponent component, UIComponent... children) {
        final List<UIComponent> componentChildren = getChildren(component);
        if (children != null) {
            for (UIComponent child : children) {
                componentChildren.add(child);
            }
        }
        return componentChildren;
    }

    public static List<UIComponent> getChildren(UIComponent component) {
        return component.getChildren();
    }

    public static Map<String, UIComponent> addFacet(UIComponent component, String facetName, UIComponent facet) {
        final Map<String, UIComponent> facets = component.getFacets();
        facets.put(facetName, facet);
        return facets;
    }

    /**
     * @param component - UIComponent to be searched from(looking towards the ancestors up to UIPropertySheet and then down to UIProperty matching
     *            <code>searchPropertyIdSuffix</code>)
     * @param searchPropertyIdSuffix - used to validate if <code>uiProperty.getId().endsWith(propertyIdSuffix)</code>
     * @return UIInput from the same UIPropertySheet as the given <code>component</code> where id ends with given <code>searchPropertyIdSuffix</code>
     */
    public static UIInput getInputFromSamePropertySheet(UIComponent component, String searchPropertyIdSuffix) {
        UIPropertySheet propSheetComponent = getAncestorComponent(component, UIPropertySheet.class, true);
        UIProperty matchingProperty = findUIPropertyByIdSuffix(propSheetComponent, searchPropertyIdSuffix);
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
        if (componentFrom == null) {
            return null;
        }
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

    public static String getDisplayLabel(UIComponent component) {
        String labelTranslated = (String) component.getAttributes().get(ATTR_DISPLAY_LABEL);
        if (StringUtils.isBlank(labelTranslated)) {
            UIProperty thisUIProperty = getAncestorComponent(component, UIProperty.class, true);
            if (thisUIProperty != null) {
                labelTranslated = getPropertyLabel(thisUIProperty, component.getId());
                if (StringUtils.isBlank(labelTranslated)) {
                    QName propName = QName.createQName(thisUIProperty.getName(), BeanHelper.getNamespaceService());
                    PropertyDefinition propDef = BeanHelper.getDictionaryService().getProperty(propName);
                    labelTranslated = propDef.getTitle();
                }
            }
        }
        return StringUtils.trim(labelTranslated);
    }

    public static String getPanelLabel(UIComponent descendantOfPanel) {
        UIPanel panel = getAncestorComponent(descendantOfPanel, UIPanel.class, true);
        String panelLabel = ""; // if panel not found then ignore
        if (panel != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
            panelLabel = panel.getLabel();
            Assert.notNull(panelLabel, "Panel lable shouldn't be null"); // panel found, but without label - this shouldn't happen
        }
        return StringUtils.trim(panelLabel);
    }

    public static String getColumnLabel(UIComponent descendantOfColumn) {
        UIColumn column = getAncestorComponent(descendantOfColumn, UIColumn.class, true);
        String label = ""; // if column not found then ignore
        if (column != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            UIComponent headerFacet = column.getFacet("header");
            if (headerFacet instanceof UISortLink) {
                UISortLink sortLink = (UISortLink) headerFacet;
                loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
                label = sortLink.getLabel();
            } else if (headerFacet instanceof UIOutput) {
                UIOutput sortLink = (UIOutput) headerFacet;
                loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
                label = DefaultTypeConverter.INSTANCE.convert(String.class, sortLink.getValue());
            } else {
                String msg = "TODO: unimplemented - see example few lines above in code.";
                if (headerFacet != null) {
                    msg += " facetClass=" + headerFacet.getClass();
                }
                throw new RuntimeException(msg);
            }
            Assert.notNull(label, "Column lable shouldn't be null"); // column found, but without label - this shouldn't happen
        }
        return StringUtils.trim(label);
    }

    private static void loadMsgBundleIfNeeded(FacesContext facesContext, String msgBundleVar) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        if (!requestMap.containsKey(msgBundleVar)) { // bundle is not jet loaded using var msgBundleVar
            ResourceBundle bundle = org.alfresco.web.app.Application.getBundle(facesContext);
            requestMap.put(msgBundleVar, new LoadBundleTag.BundleMap(bundle));
        }
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
     * @param component - this component and all its children will recursively be set readonly
     */
    public static void setReadonlyAttributeRecursively(UIComponent component) {
        setReadonlyAttributeRecursively(component, Boolean.TRUE);
    }

    public static void setReadonlyAttributeRecursively(UIComponent component, Boolean value) {
        if (isAlwaysEditComponent(component)) {
            return;
        }
        putAttribute(component, "readonly", value);
        if (component instanceof UIOutput) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            setReadonlyAttributeRecursively(childComponent, value);
        }
    }

    /**
     * Determines whether the given component is disabled or readonly
     * 
     * @param component The component to test
     * @return true if the component is either disabled or set to readonly
     */
    public static boolean isComponentDisabledOrReadOnly(UIComponent component) {
        boolean disabled = false;
        boolean readOnly = false;

        Map<String, Object> attributes = getAttributes(component);
        Object disabledAttr = attributes.get("disabled");
        if (disabledAttr != null) {
            disabled = disabledAttr.equals(Boolean.TRUE);
        }

        Object readOnlyAttr = attributes.get("readonly");
        if (readOnlyAttr != null) {
            readOnly = readOnlyAttr.equals(Boolean.TRUE);
        }

        return (disabled || readOnly) && !isAlwaysEditComponent(component);
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
        setReadonlyAttributeRecursively(component, selectItems == null);
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

    public static void renderSelectItems(ResponseWriter responseWriter, SelectItem[] items) throws IOException {
        if (items != null) {
            // show each of the items in the results listbox
            for (SelectItem item : items) {
                responseWriter.write("<option value=\"");
                responseWriter.write(item.getValue().toString());
                if (item.getDescription() != null) {
                    responseWriter.write("\" title=\"");
                    responseWriter.write(Utils.encode(item.getDescription()));
                }
                responseWriter.write("\">");
                responseWriter.write(Utils.encode(item.getLabel()));
                responseWriter.write("</option>");
            }
        }
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
     */
    public static String generateFieldSetter(FacesContext context, UIComponent component, String value) {
        String fieldId = component.getClientId(context);
        return generateFieldSetter(context, component, fieldId, value);
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
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

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value, Integer parentLevel) {
        if (parentLevel == null) {
            parentLevel = 0;
        }
        return generateAjaxFormSubmit(context, component, fieldId, value, null, parentLevel);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value //
            , Map<String, String> params, int parentLevel) {
        return generateAjaxFormSubmit(context, component, fieldId, value, params, parentLevel, null);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value //
            , Map<String, String> params, int parentLevel, String uri) {
        Assert.isTrue(parentLevel >= 0, "parentLevel cannot be negative");
        // Find parent AJAX component to update and find out parent level.
        Pair<Integer, UIComponent> levelAndComponent = findAncestorAjaxComponent(component, null, parentLevel);
        int parentLevelFound = levelAndComponent.getFirst();
        UIComponent ajaxComponent = levelAndComponent.getSecond();

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

        if (StringUtils.isBlank(uri)) {
            uri = context.getExternalContext().getRequestContextPath() + "/ajax/invoke/AjaxBean.submit?componentClientId="
                    + clientId + "&viewName=" + context.getViewRoot().getViewId();
        }

        s.append("ajaxSubmit('").append(clientId).append("','");
        s.append(((AjaxUpdateable) ajaxComponent).getAjaxClientId(context)).append("', [");
        for (Iterator<String> i = submittableParams.iterator(); i.hasNext();) {
            String submittableParam = i.next();
            s.append("'").append(submittableParam).append("'");
            if (i.hasNext()) {
                s.append(",");
            }
        }
        s.append("], '").append(uri).append("');return false;");
        return s.toString();
    }

    public static Pair<Integer, UIComponent> findAncestorAjaxComponent(UIComponent childComponent, UIComponent ajaxComponent, int parentLevel) {
        int parentLevelFound = (ajaxComponent == null) ? -1 : 0;

        while (parentLevelFound < parentLevel) {
            if (ajaxComponent == null) {
                ajaxComponent = childComponent;
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

        return new Pair<Integer, UIComponent>(parentLevelFound, ajaxComponent);
    }

    /**
     * NB! Before calling this, check if given child is rendered!
     * 
     * @param child
     * @param search
     * @param out
     * @throws IOException
     */
    public static String generateSuggestScript(FacesContext context, UIComponent child, String pickerCallback) {
        if (!(child instanceof UIInput) || StringUtils.isBlank(pickerCallback) || isComponentDisabledOrReadOnly(child)) {
            return "";
        }

        String clientId = child.getClientId(context);
        // Strip method binding delimiters for javascript
        if (StringUtils.isNotBlank(pickerCallback) && pickerCallback.contains("#{")) {
            pickerCallback = pickerCallback.substring("#{".length(), pickerCallback.length() - 1);
        }

        int ajaxParentLevel = 0;
        UIComponent searchComponent = getAncestorComponent(child, Search.class);
        if (searchComponent == null) {
            searchComponent = getAncestorComponent(child, MultiValueEditor.class);
            ajaxParentLevel++;
        }

        if (searchComponent == null) {
            throw new RuntimeException("Missing parent component with search capabilities! (Search or MultiValueEditor)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> searchAttrs = searchComponent.getAttributes();
        Integer addition = (Integer) searchAttrs.get(Search.AJAX_PARENT_LEVEL_KEY);
        if (addition != null) {
            ajaxParentLevel += addition;
        } else if (ajaxParentLevel == 0) {
            ajaxParentLevel++; // set default level to 1
        }
        UIComponent ancestorAjaxComponent = findAncestorAjaxComponent(searchComponent, null, ajaxParentLevel).getSecond();
        if (ancestorAjaxComponent == null) {
            throw new RuntimeException("Couldn't find parent ajax component to update for " + clientId + "!");
        }

        // Add filter info
        int filter = UserContactGroupSearchBean.USERS_FILTER;
        String filters = (String) searchAttrs.get(Search.FILTERS_KEY);
        if (filters != null) {
            SelectItem[] filterSelects = (SelectItem[]) context.getApplication().createValueBinding(filters).getValue(context);
            for (SelectItem selectItem : filterSelects) {
                filter = filter | ((Integer) selectItem.getValue());
            }
        }

        String containerClientId = ancestorAjaxComponent.getClientId(context);
        String submitUri = context.getExternalContext().getRequestContextPath() + "/ajax/invoke/AjaxSearchBean.setterCallback?componentClientId="
                + clientId + "&containerClientId=" + containerClientId + "&viewName=" + context.getViewRoot().getViewId();

        String sep = "\", \"";
        StringBuffer sb = new StringBuffer("<script type=\"text/javascript\">");
        sb.append("addSearchSuggest(\"")
        .append(clientId).append(sep)
        .append(containerClientId).append(sep)
        .append(pickerCallback).append(sep)
        .append(filter).append(sep)
        .append(submitUri).append("\");");
        sb.append("</script>");
        return sb.toString();
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
     * @return component generated based on <code>singlePropVO</code> that is added to list of given <code>children</code> that must come from given <code>propertySheet</code>
     *         where the generated component is added.
     */
    public static UIComponent generateAndAddComponent(FacesContext context, ComponentPropVO componentPropVO, UIPropertySheet propertySheet,
            final List<UIComponent> children) {
        if (!componentPropVO.isUseComponentGenerator()) {
            final UIComponent component = createCellComponent(context, componentPropVO, propertySheet);
            children.add(component);
            return component;
        }
        final Map<String, String> customAttributes = componentPropVO.getCustomAttributes();
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
            public List<UIComponent> getChildren() {
                return children;
            }

            @Override
            public String getComponentGenerator() {
                return customAttributes.get(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR);
            }

            @Override
            public String getDisplayLabel() {
                return customAttributes.get(PropertySheetElementReader.ATTR_DISPLAY_LABEL);
            }

            @Override
            public String getConverter() {
                return customAttributes.get(PropertySheetElementReader.ATTR_CONVERTER);
            }

            @Override
            public boolean getIgnoreIfMissing() {
                return Boolean.parseBoolean(customAttributes.get(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING));
            }

            @Override
            public boolean isReadOnly() {
                return Boolean.parseBoolean(customAttributes.get(PropertySheetElementReader.ATTR_READ_ONLY));
            }

        };
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
     * @param propertySheet
     * @return
     */
    private static UIComponent createCellComponent(FacesContext context, ComponentPropVO vo, UIPropertySheet propertySheet) {
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
            attributes.put(ATTR_STYLE_CLASS, "date");
            createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
        } else if (StringUtils.equals("ClassificatorSelectorGenerator", type)) {
            if (voCustomAttributes.containsKey(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME)) {
                ClassificatorSelectorGenerator classificGenerator = new ClassificatorSelectorGenerator();
                classificGenerator.setCustomAttributes(voCustomAttributes);
                component = classificGenerator.generateSelectComponent(context, null, false);
                classificGenerator.setupSelectComponent(context, propertySheet, null, null, component, false);
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
        QName propertyQName = Repository.resolveToQName(propName);
        PropertyDefinition propDef = BeanHelper.getDocumentConfigService().getPropertyDefinition(node, propertyQName);
        if (propDef == null) {
            TypeDefinition typeDef = getGeneralService().getAnonymousType(node);
            if (typeDef != null) {
                Map<QName, PropertyDefinition> properties = typeDef.getProperties();
                propDef = properties.get(propertyQName);
            }
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

        for (UIComponent child : getChildren(root)) {
            component = findComponentById(context, child, id);
            if (component != null) {
                break;
            }
        }

        if (root.getId() != null) {
            if (component == null && root.getId().equals(id)) {
                component = root;
            }
        }
        return component;
    }

    public static UIComponent findChildComponentById(FacesContext context, UIComponent component, String clientId) {
        return findChildComponentById(context, component, clientId, false);
    }

    public static UIComponent findChildComponentById(FacesContext context, UIComponent component, String clientId, boolean includeFacets) {
        if (component == null) {
            return null;
        }
        if (clientId.equals(component.getClientId(context))) {
            return component;
        }
        for (UIComponent child : getChildren(component)) {
            UIComponent result = findChildComponentById(context, child, clientId, includeFacets);
            if (result != null) {
                return result;
            }
        }
        if (includeFacets) {
            Collection<UIComponent> facetComponents = component.getFacets().values();
            for (UIComponent facet : facetComponents) {
                UIComponent result = findChildComponentById(context, facet, clientId, true);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static <T> List<T> findChildComponentsByClass(FacesContext context, UIComponent component, Class<? extends T> clazz) {
        List<T> results = null;
        if (component == null) {
            return null;
        }
        for (UIComponent child : getChildren(component)) {
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

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T findParentWithClass(FacesContext context, UIComponent component, Class<T> clazz) {
        UIComponent parent = component.getParent();
        while (parent != null) {
            if (parent.getClass().isAssignableFrom(clazz)) {
                return (T) parent;
            }
            parent = parent.getParent();
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
                    final SubPropertySheetItem subPropSheetItem = getAncestorComponent(ancestorPropSheet, SubPropertySheetItem.class, true);
                    assocInfoHolder.assocTypeQName = subPropSheetItem.getAssocTypeQName();
                    assocInfoHolder.associationBrand = wmPropSheet.getAssociationBrand();
                    assocInfoHolder.associationIndex = associationIndex;
                    assocInfoHolder.validate();
                    pathInfos.add(assocInfoHolder);
                    UIPropertySheet nextAncestorPropSheet = getAncestorComponent(subPropSheetItem, UIPropertySheet.class, true);
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

    public static <T extends PropertySheetItem> T getPropSheetItem(UIPropertySheet propertySheet, Class<T> componentClass, QName uiPropertyName) {
        String localName = uiPropertyName.getLocalName();
        NamespaceService namespaceService = BeanHelper.getNamespaceService();
        for (UIComponent uiComponent : getChildren(propertySheet)) {
            if (componentClass.isAssignableFrom(uiComponent.getClass())) {
                @SuppressWarnings("unchecked")
                T propSheetItem = (T) uiComponent;
                String psItemLocalName = propSheetItem.getName();
                if (psItemLocalName.endsWith(localName)) {
                    String resolveToQNameString = QName.resolveToQNameString(namespaceService, propSheetItem.getName());
                    if (uiPropertyName.toString().equals(resolveToQNameString)) {
                        return propSheetItem;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Didn't find " + componentClass.getSimpleName() + " with name " + uiPropertyName + " from given propertySheet");
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

    /**
     * Creates new {@link FacesEvent} using closure, that is executed once in given phase
     * 
     * @param phaseId
     * @param uiComponent
     * @param closure
     */
    public static void executeLater(PhaseId phaseId, UIComponent uiComponent, final Closure closure) {
        @SuppressWarnings("unused")
        ExecuteLater executeLater = new ExecuteLater(phaseId, uiComponent) {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute() {
                closure.execute(null);
            }
        };
    }

    /**
     * Similar method for {@link UISelectItem} objects is {@link GeneralSelectorGenerator#addDefault(FacesContext, List)}
     * 
     * @param results
     * @param context
     */
    public static void addDefault(List<SelectItem> results, FacesContext context) {
        SelectItem selectItem = new SelectItem(DEFAULT_SELECT_VALUE, MessageUtil.getMessage(context, "select_default_label"));
        results.add(0, selectItem);
    }

    public static boolean isAlwaysEditComponent(UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        return attributes.containsKey(IS_ALWAYS_EDIT) && Boolean.valueOf((Boolean) attributes.get(IS_ALWAYS_EDIT));
    }

    public static int getRenderedChildrenCount(UIComponent parent) {
        if (parent == null || parent.getChildCount() == 0) {
            return 0;
        }
        int count = 0;
        for (Object child : parent.getChildren()) {
            if (child instanceof UIComponent && ((UIComponent) child).isRendered()) {
                count++;
            }
        }
        return count;
    }

    public static void addOnchangeClickLink(Application application, List<UIComponent> children, String methodBindingStr, String linkId, UIParameter... parameters) {
        UIActionLink hiddenLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        hiddenLink.setId(linkId);
        hiddenLink.setValue(linkId);
        hiddenLink.setActionListener(application.createMethodBinding(methodBindingStr, new Class[] { ActionEvent.class }));
        children.add(hiddenLink);
        hiddenLink.getAttributes().put("style", "display: none;");
        for (UIParameter parameter : parameters) {
            addChildren(hiddenLink, parameter);
        }
    }

    public static void addOnchangeJavascript(UIComponent component) {
        Map<String, Object> attributes = getAttributes(component);
        String styleClass = (String) attributes.get(ATTR_STYLE_CLASS);
        attributes.put(ATTR_STYLE_CLASS, (StringUtils.isNotBlank(styleClass) ? styleClass + " " : "") + getOnChangeStyleClass());
    }

    public static String getOnChangeStyleClass() {
        return GeneralSelectorGenerator.ONCHANGE_PARAM_MARKER_CLASS + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER
                + "var link = jQuery('#' + escapeId4JQ(currElId)).nextAll('a').eq(0); link.click();";
    }

    public static void addStyleClass(UIComponent uiComponent, String styleClassName) {
        String styleClass = (String) getAttribute(uiComponent, STYLE_CLASS);
        styleClass = styleClassName + (StringUtils.isBlank(styleClass) ? "" : " " + styleClass);
        putAttribute(uiComponent, STYLE_CLASS, styleClass);
    }

    public static Integer getIndexFromValueBinding(String vb) {
        Integer index = null;
        if (vb.endsWith("]}")) {
            String indexStr = vb.substring(vb.lastIndexOf('[') + 1, vb.length() - 2);
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                // Do nothing
            }
        }
        return index;
    }

    public static void addRecipientGrouping(Field field, ItemConfigVO item, NamespaceService namespaceService) {
        if (field == null || namespaceService == null) {
            return;
        }

        QName groupColumnProp = null;
        if (DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName().equals(field.getOriginalFieldId())) {
            groupColumnProp = DocumentCommonModel.Props.RECIPIENT_GROUP;
        } else if (DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName().equals(field.getOriginalFieldId())) {
            groupColumnProp = DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP;
        }

        if (groupColumnProp != null && item != null) {
            String prefixProp = groupColumnProp.getPrefixedQName(namespaceService).getPrefixString();
            String hiddenPropNames = item.getCustomAttributes().get(MultiValueEditor.HIDDEN_PROP_NAMES);
            hiddenPropNames = StringUtils.isBlank(hiddenPropNames) ? prefixProp : hiddenPropNames + "," + prefixProp;
            item.getCustomAttributes().put(MultiValueEditor.GROUP_BY_COLUMN_NAME, prefixProp);
        }
    }

    public static CustomChildrenCreator getDocumentRowFileGenerator(final Application application) {
        return new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    int fileCounter = 0;
                    for (Object obj : params) {
                        File file = (File) obj;
                        final DocPermissionEvaluator evaluatorAllow = createEvaluator(application, fileCounter, "evalAllow-" + rowCounter + "-");
                        evaluatorAllow.setAllow("viewDocumentFiles");

                        String fileName = file.getDisplayName();
                        String imageText = getFileImage(file);

                        final UIActionLink fileAllowLink = generateFileReadOnlyLink(application, rowCounter, fileCounter, file, fileName, imageText);
                        ComponentUtil.addChildren(evaluatorAllow, fileAllowLink);
                        components.add(evaluatorAllow);

                        final DocPermissionEvaluator evaluatorDeny = createEvaluator(application, fileCounter, "evalDeny-" + rowCounter + "-");
                        evaluatorDeny.setDeny("viewDocumentFiles");

                        final HtmlGraphicImage image = (HtmlGraphicImage) application.createComponent(HtmlGraphicImage.COMPONENT_TYPE);
                        image.setValue(imageText);
                        image.setId("doc-file-img-" + rowCounter + "-" + fileCounter);
                        image.setTitle(fileName);
                        image.setRendered(file != null);
                        image.setAlt(fileName);

                        ComponentUtil.addChildren(evaluatorDeny, image);
                        components.add(evaluatorDeny);
                        fileCounter++;
                    }
                    rowCounter++;
                }
                return components;
            }

            private DocPermissionEvaluator createEvaluator(Application application, int fileCounter, String evalNamePrefix) {
                final DocPermissionEvaluator evaluatorAllow = (DocPermissionEvaluator) application
                        .createComponent("ee.webmedia.alfresco.privilege.web.DocPermissionEvaluator");
                evaluatorAllow.setId(evalNamePrefix + fileCounter);
                evaluatorAllow.setValueBinding("value", application.createValueBinding("#{r.files[" + fileCounter + "].node}"));
                return evaluatorAllow;
            }
        };
    }

    /** Generate read-only open links for all given files (no permission check is performed) */
    public static CustomChildrenCreator getRowFileGenerator(final Application application) {
        return new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    int fileCounter = 0;
                    for (Object obj : params) {
                        File file = (File) obj;
                        components.add(generateFileReadOnlyLink(application, rowCounter, fileCounter, file, file.getDisplayName(), getFileImage(file)));
                        fileCounter++;
                    }
                    rowCounter++;
                }
                return components;
            }

        };
    }

    private static UIActionLink generateFileReadOnlyLink(final Application application, int rowCounter, int fileCounter, File file, String fileName, String imageText) {
        final UIActionLink fileAllowLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        fileAllowLink.setId("doc-file-link-" + rowCounter + "-" + fileCounter);
        fileAllowLink.setValue("");
        fileAllowLink.setTooltip(fileName);
        fileAllowLink.setShowLink(false);
        fileAllowLink.setHref(file.getDownloadUrl());
        fileAllowLink.setImage(imageText);
        fileAllowLink.setTarget("_blank");
        ComponentUtil.getAttributes(fileAllowLink).put("styleClass", "inlineAction webdav-readOnly");
        return fileAllowLink;
    }

    private static String getFileImage(File file) {
        return file.isDigiDocContainer() ? "/images/icons/ddoc_sign_small.gif" : "/images/icons/attachment.gif";
    }

    public static void setAjaxEnabledOnActionLinksRecursive(UIComponent component, int ajaxParentLevel) {
        if (component instanceof UIActionLink) {
            component.getAttributes().put(ActionLinkRenderer.AJAX_ENABLED, Boolean.TRUE);
            component.getAttributes().put(ActionLinkRenderer.AJAX_PARENT_LEVEL, ajaxParentLevel);
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        if (component instanceof AjaxUpdateable) {
            ajaxParentLevel++;
        }
        for (UIComponent childComponent : children) {
            setAjaxEnabledOnActionLinksRecursive(childComponent, ajaxParentLevel);
        }
        @SuppressWarnings("unchecked")
        Collection<UIComponent> facets = component.getFacets().values();
        if (facets == null) {
            return;
        }
        for (UIComponent facet : facets) {
            setAjaxEnabledOnActionLinksRecursive(facet, ajaxParentLevel);
        }
    }

}
=======
package ee.webmedia.alfresco.utils;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.config.PropertySheetElementReader;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.alfresco.web.ui.common.renderer.ActionLinkRenderer;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.tag.LoadBundleTag;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.component.NodeAssocBrand;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem.AddRemoveActionListener;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.web.DocPermissionEvaluator;

/**
 * Util methods for JSF components/component trees
 */
public class ComponentUtil {
    private static final String ATTR_STYLE_CLASS = "styleClass";
    private static final String JSF_CONVERTER = "jsfConverter";
    public static final String IS_ALWAYS_EDIT = "isAlwaysEdit";
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ComponentUtil.class);
    private static GeneralService generalService;
    public static final String DEFAULT_SELECT_VALUE = "";

    private static final Comparator<UIComponent> BY_LABEL_COMPARATOR;
    static {
        @SuppressWarnings("unchecked")
        Comparator<UIComponent> byLabelComparator = new TransformingComparator(new ComparableTransformer<UIComponent>() {
            @Override
            public Comparable<?> tr(UIComponent input) {
                if (input instanceof UISelectItem) {
                    return ((UISelectItem) input).getItemLabel();
                }
                return null;
            }
        }, new NullComparator());
        BY_LABEL_COMPARATOR = byLabelComparator;
    }

    public static void sortByLabel(List<UIComponent> selectOptions) {
        Collections.sort(selectOptions, BY_LABEL_COMPARATOR);
    }

    public static UIComponent makeCondenced(final UIComponent component, int condenceSize) {
        putAttribute(component, ATTR_STYLE_CLASS, "condence" + condenceSize);
        return component;
    }

    public static void writeModalHeader(ResponseWriter out, String modalId, String modalTitle, String closeOnClick) throws IOException {
        writeModalHeader(out, modalId, modalTitle, "", closeOnClick);
    }

    public static void writeModalHeader(ResponseWriter out, String modalId, String modalTitle, String modalWrapStyleClass, String closeOnClick) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"");
        sb.append(modalId);
        sb.append("\" class=\"modalpopup modalwrap " + modalWrapStyleClass + "\">");
        sb.append("<div class=\"modalpopup-header clear\"><h1>");
        sb.append(modalTitle);
        sb.append("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        if (StringUtils.isBlank(closeOnClick)) {
            sb.append("return hideModal()");
        } else {
            sb.append(closeOnClick);
        }
        sb.append("\">");
        sb.append(MessageUtil.getMessage("close_window"));
        sb.append("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner modalpopup-filter\">");

        out.write(sb.toString());
    }

    public static void writeModalFooter(ResponseWriter out) throws IOException {
        // close modal popup
        out.write("</div></div></div>");
    }

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

    /**
     * Add all attributes from given map to given component
     */
    public static Map<String, Object> addAttributes(UIComponent component, Map<String, Object> attributesToAdd) {
        final Map<String, Object> attributes = getAttributes(component);
        if (attributesToAdd != null) {
            attributes.putAll(attributesToAdd);
        }
        return attributes;
    }

    /**
     * Convenience method that doesn't produce compiler warning
     * "Type safety: The expression of type Map needs unchecked conversion to conform to Map<String,Object>"
     *
     * @param component
     * @return <code>component.getAttributes()</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAttributes(UIComponent component) {
        return component.getAttributes();
    }

    public static <T> Object getAttribute(UIComponent component, String key) {
        return getAttributes(component).get(key);
    }

    public static <T> T getAttribute(UIComponent component, String key, Class<T> requiredClazz) {
        Object value = getAttribute(component, key);
        T result;
        try {
            result = DefaultTypeConverter.INSTANCE.convert(requiredClazz, value);
        } catch (TypeConversionException e) {
            throw new TypeConversionException("Failed to convert component attribute '" + key + "'='" + value + "' to " + requiredClazz.getCanonicalName(), e);
        }
        return result;
    }

    /**
     * @param paramName
     * @param paramValue - could also be string to be used as value binding
     * @param application
     * @return
     */
    public static UIParameter createUIParam(String paramName, Object paramValue, Application application) {
        UIParameter param = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        param.setName(paramName);
        if (paramValue instanceof String) {
            UIComponentTagUtils.setValueProperty(FacesContext.getCurrentInstance(), param, (String) paramValue);
        } else {
            param.setValue(paramValue);
        }
        return param;
    }

    /**
     * @param actionDef
     * @param application
     * @param context
     * @param evaluationCheckNode - optional, but if present, then null is returned when there is evaluator and evaluation fails
     * @return
     */
    public static UIActionLink createActionFromConf(ActionDefinition actionDef, Application application, FacesContext context, Node evaluationCheckNode) {
        if (evaluationCheckNode != null && actionDef.Evaluator != null && !actionDef.Evaluator.evaluate(evaluationCheckNode)) {
            return null;
        }
        String actionId = actionDef.getId();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        link.setImage(actionDef.Image);
        FacesHelper.setupComponentId(context, link, actionId);
        String lblMessage = actionDef.LabelMsg != null ? MessageUtil.getMessage(context, actionDef.LabelMsg) : actionDef.Label;
        link.setValue(lblMessage);
        link.setActionListener(application.createMethodBinding(actionDef.ActionListener, new Class[] { javax.faces.event.ActionEvent.class }));
        String tooltip = actionDef.TooltipMsg != null ? MessageUtil.getMessage(context, actionDef.TooltipMsg) : actionDef.Tooltip;
        link.setTooltip(tooltip);

        final AddRemoveActionListener listener = new AddRemoveActionListener();
        link.addActionListener(listener);
        link.setShowLink(actionDef.ShowLink);

        @SuppressWarnings("unchecked")
        List<UIComponent> children = link.getChildren();
        for (Entry<String, String> entry : actionDef.getParams().entrySet()) {
            UIParameter param = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            param.setName(entry.getKey());
            param.setValue(entry.getValue());
            children.add(param);
        }

        return link;
    }

    public static UIComponent setTooltip(final UIComponent component, final String tooltip) {
        putAttribute(component, "title", tooltip);
        return component;
    }

    /**
     * Add children to the component
     *
     * @param component
     * @param children - children to be added or null - then just children of the component are returned without adding any children
     * @return children of the component
     */
    public static List<UIComponent> addChildren(UIComponent component, UIComponent... children) {
        final List<UIComponent> componentChildren = getChildren(component);
        if (children != null) {
            for (UIComponent child : children) {
                componentChildren.add(child);
            }
        }
        return componentChildren;
    }

    public static List<UIComponent> getChildren(UIComponent component) {
        return component.getChildren();
    }

    public static Map<String, UIComponent> addFacet(UIComponent component, String facetName, UIComponent facet) {
        final Map<String, UIComponent> facets = component.getFacets();
        facets.put(facetName, facet);
        return facets;
    }

    /**
     * @param component - UIComponent to be searched from(looking towards the ancestors up to UIPropertySheet and then down to UIProperty matching
     *            <code>searchPropertyIdSuffix</code>)
     * @param searchPropertyIdSuffix - used to validate if <code>uiProperty.getId().endsWith(propertyIdSuffix)</code>
     * @return UIInput from the same UIPropertySheet as the given <code>component</code> where id ends with given <code>searchPropertyIdSuffix</code>
     */
    public static UIInput getInputFromSamePropertySheet(UIComponent component, String searchPropertyIdSuffix) {
        UIPropertySheet propSheetComponent = getAncestorComponent(component, UIPropertySheet.class, true);
        UIProperty matchingProperty = findUIPropertyByIdSuffix(propSheetComponent, searchPropertyIdSuffix);
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
        if (componentFrom == null) {
            return null;
        }
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

    public static String getDisplayLabel(UIComponent component) {
        String labelTranslated = (String) component.getAttributes().get(ATTR_DISPLAY_LABEL);
        if (StringUtils.isBlank(labelTranslated)) {
            UIProperty thisUIProperty = getAncestorComponent(component, UIProperty.class, true);
            if (thisUIProperty != null) {
                labelTranslated = getPropertyLabel(thisUIProperty, component.getId());
                if (StringUtils.isBlank(labelTranslated)) {
                    QName propName = QName.createQName(thisUIProperty.getName(), BeanHelper.getNamespaceService());
                    PropertyDefinition propDef = BeanHelper.getDictionaryService().getProperty(propName);
                    labelTranslated = propDef.getTitle();
                }
            }
        }
        return StringUtils.trim(labelTranslated);
    }

    public static String getPanelLabel(UIComponent descendantOfPanel) {
        UIPanel panel = getAncestorComponent(descendantOfPanel, UIPanel.class, true);
        String panelLabel = ""; // if panel not found then ignore
        if (panel != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
            panelLabel = panel.getLabel();
            Assert.notNull(panelLabel, "Panel lable shouldn't be null"); // panel found, but without label - this shouldn't happen
        }
        return StringUtils.trim(panelLabel);
    }

    public static String getColumnLabel(UIComponent descendantOfColumn) {
        UIColumn column = getAncestorComponent(descendantOfColumn, UIColumn.class, true);
        String label = ""; // if column not found then ignore
        if (column != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            UIComponent headerFacet = column.getFacet("header");
            if (headerFacet instanceof UISortLink) {
                UISortLink sortLink = (UISortLink) headerFacet;
                loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
                label = sortLink.getLabel();
            } else if (headerFacet instanceof UIOutput) {
                UIOutput sortLink = (UIOutput) headerFacet;
                loadMsgBundleIfNeeded(facesContext, "msg"); // needed to translate panel label
                label = DefaultTypeConverter.INSTANCE.convert(String.class, sortLink.getValue());
            } else {
                String msg = "TODO: unimplemented - see example few lines above in code.";
                if (headerFacet != null) {
                    msg += " facetClass=" + headerFacet.getClass();
                }
                throw new RuntimeException(msg);
            }
            Assert.notNull(label, "Column lable shouldn't be null"); // column found, but without label - this shouldn't happen
        }
        return StringUtils.trim(label);
    }

    private static void loadMsgBundleIfNeeded(FacesContext facesContext, String msgBundleVar) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        if (!requestMap.containsKey(msgBundleVar)) { // bundle is not jet loaded using var msgBundleVar
            ResourceBundle bundle = org.alfresco.web.app.Application.getBundle(facesContext);
            requestMap.put(msgBundleVar, new LoadBundleTag.BundleMap(bundle));
        }
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
     * @param component - this component and all its children will recursively be set readonly
     */
    public static void setReadonlyAttributeRecursively(UIComponent component) {
        setReadonlyAttributeRecursively(component, Boolean.TRUE);
    }

    public static void setReadonlyAttributeRecursively(UIComponent component, Boolean value) {
        if (isAlwaysEditComponent(component)) {
            return;
        }
        putAttribute(component, "readonly", value);
        if (component instanceof UIOutput) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            setReadonlyAttributeRecursively(childComponent, value);
        }
    }

    /**
     * Determines whether the given component is disabled or readonly
     *
     * @param component The component to test
     * @return true if the component is either disabled or set to readonly
     */
    public static boolean isComponentDisabledOrReadOnly(UIComponent component) {
        boolean disabled = false;
        boolean readOnly = false;

        Map<String, Object> attributes = getAttributes(component);
        Object disabledAttr = attributes.get("disabled");
        if (disabledAttr != null) {
            disabled = disabledAttr.equals(Boolean.TRUE);
        }

        Object readOnlyAttr = attributes.get("readonly");
        if (readOnlyAttr != null) {
            readOnly = readOnlyAttr.equals(Boolean.TRUE);
        }

        return (disabled || readOnly) && !isAlwaysEditComponent(component);
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
        setReadonlyAttributeRecursively(component, selectItems == null);
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

    public static void renderSelectItems(ResponseWriter responseWriter, SelectItem[] items) throws IOException {
        if (items != null) {
            // show each of the items in the results listbox
            for (SelectItem item : items) {
                responseWriter.write("<option value=\"");
                responseWriter.write(item.getValue().toString());
                responseWriter.write("\" title=\"");
                responseWriter.write(Utils.encode(StringUtils.defaultIfEmpty(item.getDescription(), item.getLabel())));
                responseWriter.write("\">");
                responseWriter.write(Utils.encode(item.getLabel()));
                responseWriter.write("</option>");
            }
        }
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
     */
    public static String generateFieldSetter(FacesContext context, UIComponent component, String value) {
        String fieldId = component.getClientId(context);
        return generateFieldSetter(context, component, fieldId, value);
    }

    /**
     * Generate JavaScript that sets a hidden parameter. Implementation based on {@link Utils#generateFormSubmit(FacesContext, UIComponent, String, String, boolean, Map)}.
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
        buf.append(StringEscapeUtils.escapeJavaScript(formClientId));
        buf.append("']['");
        buf.append(StringEscapeUtils.escapeJavaScript(fieldId));
        buf.append("'].value='");
        String escapeJavaScript = StringEscapeUtils.escapeJavaScript(value);
        // Take back forward slash modifications for JMeter tests
        buf.append(escapeJavaScript.replaceAll("\\\\/", "/"));
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

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value, Integer parentLevel) {
        if (parentLevel == null) {
            parentLevel = 0;
        }
        return generateAjaxFormSubmit(context, component, fieldId, value, null, parentLevel);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value //
            , Map<String, String> params, int parentLevel) {
        return generateAjaxFormSubmit(context, component, fieldId, value, params, parentLevel, null);
    }

    public static String generateAjaxFormSubmit(FacesContext context, UIComponent component, String fieldId, String value //
            , Map<String, String> params, int parentLevel, String uri) {
        Assert.isTrue(parentLevel >= 0, "parentLevel cannot be negative");
        // Find parent AJAX component to update and find out parent level.
        Pair<Integer, UIComponent> levelAndComponent = findAncestorAjaxComponent(component, null, parentLevel);
        int parentLevelFound = levelAndComponent.getFirst();
        UIComponent ajaxComponent = levelAndComponent.getSecond();

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

        if (StringUtils.isBlank(uri)) {
            uri = context.getExternalContext().getRequestContextPath() + "/ajax/invoke/AjaxBean.submit?componentClientId="
                    + clientId + "&viewName=" + context.getViewRoot().getViewId();
        }

        s.append("ajaxSubmit('").append(clientId).append("','");
        s.append(((AjaxUpdateable) ajaxComponent).getAjaxClientId(context)).append("', [");
        for (Iterator<String> i = submittableParams.iterator(); i.hasNext();) {
            String submittableParam = i.next();
            s.append("'").append(submittableParam).append("'");
            if (i.hasNext()) {
                s.append(",");
            }
        }
        s.append("], '").append(uri).append("');return false;");
        return s.toString();
    }

    public static Pair<Integer, UIComponent> findAncestorAjaxComponent(UIComponent childComponent, UIComponent ajaxComponent, int parentLevel) {
        int parentLevelFound = (ajaxComponent == null) ? -1 : 0;

        while (parentLevelFound < parentLevel) {
            if (ajaxComponent == null) {
                ajaxComponent = childComponent;
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

        return new Pair<Integer, UIComponent>(parentLevelFound, ajaxComponent);
    }

    /**
     * NB! Before calling this, check if given child is rendered!
     *
     * @param child
     * @param search
     * @param out
     * @throws IOException
     */
    public static String generateSuggestScript(FacesContext context, UIComponent child, String pickerCallback) {
        if (!(child instanceof UIInput) || StringUtils.isBlank(pickerCallback) || isComponentDisabledOrReadOnly(child)) {
            return "";
        }

        String clientId = child.getClientId(context);
        // Strip method binding delimiters for javascript
        if (StringUtils.isNotBlank(pickerCallback) && pickerCallback.contains("#{")) {
            pickerCallback = pickerCallback.substring("#{".length(), pickerCallback.length() - 1);
        }

        int ajaxParentLevel = 0;
        UIComponent searchComponent = getAncestorComponent(child, Search.class);
        if (searchComponent == null) {
            searchComponent = getAncestorComponent(child, MultiValueEditor.class);
            ajaxParentLevel++;
        }

        if (searchComponent == null) {
            throw new RuntimeException("Missing parent component with search capabilities! (Search or MultiValueEditor)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> searchAttrs = searchComponent.getAttributes();
        Integer addition = (Integer) searchAttrs.get(Search.AJAX_PARENT_LEVEL_KEY);
        if (addition != null) {
            ajaxParentLevel += addition;
        } else if (ajaxParentLevel == 0) {
            ajaxParentLevel++; // set default level to 1
        }
        UIComponent ancestorAjaxComponent = findAncestorAjaxComponent(searchComponent, null, ajaxParentLevel).getSecond();
        if (ancestorAjaxComponent == null) {
            throw new RuntimeException("Couldn't find parent ajax component to update for " + clientId + "!");
        }

        // Add filter info
        int filter = searchAttrs.containsKey(Search.FILTER_INDEX) ? (Integer) searchAttrs.get(Search.FILTER_INDEX) : 0;
        String filters = (String) searchAttrs.get(Search.FILTERS_KEY);
        if (filters != null) {
            SelectItem[] filterSelects = (SelectItem[]) context.getApplication().createValueBinding(filters).getValue(context);
            for (SelectItem selectItem : filterSelects) {
                filter = filter | ((Integer) selectItem.getValue());
            }
        }

        if (filter == 0) {
            filter = UserContactGroupSearchBean.USERS_FILTER; // Default to user search if no other option is applicable.
        }

        String containerClientId = ancestorAjaxComponent.getClientId(context);
        String submitUri = context.getExternalContext().getRequestContextPath() + "/ajax/invoke/AjaxSearchBean.setterCallback?componentClientId="
                + clientId + "&containerClientId=" + containerClientId + "&viewName=" + context.getViewRoot().getViewId();

        String sep = "\", \"";
        StringBuffer sb = new StringBuffer("<script type=\"text/javascript\">");
        sb.append("addSearchSuggest(\"")
        .append(clientId).append(sep)
        .append(containerClientId).append(sep)
        .append(pickerCallback).append(sep)
        .append(filter).append(sep)
        .append(submitUri).append("\");");
        sb.append("</script>");
        return sb.toString();
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
     * @return component generated based on <code>singlePropVO</code> that is added to list of given <code>children</code> that must come from given <code>propertySheet</code>
     *         where the generated component is added.
     */
    public static UIComponent generateAndAddComponent(FacesContext context, ComponentPropVO componentPropVO, UIPropertySheet propertySheet,
            final List<UIComponent> children) {
        if (!componentPropVO.isUseComponentGenerator()) {
            final UIComponent component = createCellComponent(context, componentPropVO, propertySheet);
            children.add(component);
            return component;
        }
        final Map<String, String> customAttributes = componentPropVO.getCustomAttributes();
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
            public List<UIComponent> getChildren() {
                return children;
            }

            @Override
            public String getComponentGenerator() {
                return customAttributes.get(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR);
            }

            @Override
            public String getDisplayLabel() {
                return customAttributes.get(PropertySheetElementReader.ATTR_DISPLAY_LABEL);
            }

            @Override
            public String getConverter() {
                return customAttributes.get(PropertySheetElementReader.ATTR_CONVERTER);
            }

            @Override
            public boolean getIgnoreIfMissing() {
                return Boolean.parseBoolean(customAttributes.get(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING));
            }

            @Override
            public boolean isReadOnly() {
                return Boolean.parseBoolean(customAttributes.get(PropertySheetElementReader.ATTR_READ_ONLY));
            }

        };
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
     * @param propertySheet
     * @return
     */
    private static UIComponent createCellComponent(FacesContext context, ComponentPropVO vo, UIPropertySheet propertySheet) {
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
            attributes.put(ATTR_STYLE_CLASS, "date");
            createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
        } else if (StringUtils.equals("ClassificatorSelectorGenerator", type)) {
            if (voCustomAttributes.containsKey(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME)) {
                ClassificatorSelectorGenerator classificGenerator = new ClassificatorSelectorGenerator();
                classificGenerator.setCustomAttributes(voCustomAttributes);
                component = classificGenerator.generateSelectComponent(context, null, false);
                classificGenerator.setupSelectComponent(context, propertySheet, null, null, component, false);
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
        QName propertyQName = Repository.resolveToQName(propName);
        PropertyDefinition propDef = BeanHelper.getDocumentConfigService().getPropertyDefinition(node, propertyQName);
        if (propDef == null) {
            TypeDefinition typeDef = getGeneralService().getAnonymousType(node);
            if (typeDef != null) {
                Map<QName, PropertyDefinition> properties = typeDef.getProperties();
                propDef = properties.get(propertyQName);
            }
        }
        return propDef;
    }

    public static String resolveDisplayLabel(FacesContext context, PropertyDefinition propDef, String propName) {
        String displayLabel = null;
        if (propDef != null) {
            // try and get the repository assigned label
            displayLabel = propDef.getTitle();

            // If title is null, it is most probably because property is on a non-dynamic node. Meaning the property
            // definition couldn't be fetched using DocumentConfigService. Relying on naming conventions, we could try
            // to fetch the translation from *.properties files.
            String propLocalName = propDef.getName().getLocalName();
            if (displayLabel == null) {
                String containerName = propDef.getContainerClass().getName().getLocalName();
                String messageKey = containerName + "_" + propLocalName;
                displayLabel = MessageUtil.getMessage(messageKey);
                if (!MessageUtil.isMessageTranslated(messageKey, displayLabel)) {
                    displayLabel = null; // Don't display missing message key (I18NUtil already returns null)
                }
            }

            // If the label is still null default to the local name of the property
            if (displayLabel == null) {
                displayLabel = propLocalName;
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

        for (UIComponent child : getChildren(root)) {
            component = findComponentById(context, child, id);
            if (component != null) {
                break;
            }
        }

        if (root.getId() != null) {
            if (component == null && root.getId().equals(id)) {
                component = root;
            }
        }
        return component;
    }

    public static UIComponent findChildComponentById(FacesContext context, UIComponent component, String clientId) {
        return findChildComponentById(context, component, clientId, false);
    }

    public static UIComponent findChildComponentById(FacesContext context, UIComponent component, String clientId, boolean includeFacets) {
        if (component == null) {
            return null;
        }
        if (clientId.equals(component.getClientId(context))) {
            return component;
        }
        for (UIComponent child : getChildren(component)) {
            UIComponent result = findChildComponentById(context, child, clientId, includeFacets);
            if (result != null) {
                return result;
            }
        }
        if (includeFacets) {
            Collection<UIComponent> facetComponents = component.getFacets().values();
            for (UIComponent facet : facetComponents) {
                UIComponent result = findChildComponentById(context, facet, clientId, true);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static <T> List<T> findChildComponentsByClass(FacesContext context, UIComponent component, Class<? extends T> clazz) {
        List<T> results = null;
        if (component == null) {
            return null;
        }
        for (UIComponent child : getChildren(component)) {
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

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T findParentWithClass(FacesContext context, UIComponent component, Class<T> clazz) {
        UIComponent parent = component.getParent();
        while (parent != null) {
            if (parent.getClass().isAssignableFrom(clazz)) {
                return (T) parent;
            }
            parent = parent.getParent();
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
                    final SubPropertySheetItem subPropSheetItem = getAncestorComponent(ancestorPropSheet, SubPropertySheetItem.class, true);
                    assocInfoHolder.assocTypeQName = subPropSheetItem.getAssocTypeQName();
                    assocInfoHolder.associationBrand = wmPropSheet.getAssociationBrand();
                    assocInfoHolder.associationIndex = associationIndex;
                    assocInfoHolder.validate();
                    pathInfos.add(assocInfoHolder);
                    UIPropertySheet nextAncestorPropSheet = getAncestorComponent(subPropSheetItem, UIPropertySheet.class, true);
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

    public static <T extends PropertySheetItem> T getPropSheetItem(UIPropertySheet propertySheet, Class<T> componentClass, QName uiPropertyName) {
        String localName = uiPropertyName.getLocalName();
        NamespaceService namespaceService = BeanHelper.getNamespaceService();
        for (UIComponent uiComponent : getChildren(propertySheet)) {
            if (componentClass.isAssignableFrom(uiComponent.getClass())) {
                @SuppressWarnings("unchecked")
                T propSheetItem = (T) uiComponent;
                String psItemLocalName = propSheetItem.getName();
                if (psItemLocalName.endsWith(localName)) {
                    String resolveToQNameString = QName.resolveToQNameString(namespaceService, propSheetItem.getName());
                    if (uiPropertyName.toString().equals(resolveToQNameString)) {
                        return propSheetItem;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Didn't find " + componentClass.getSimpleName() + " with name " + uiPropertyName + " from given propertySheet");
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

    /**
     * Creates new {@link FacesEvent} using closure, that is executed once in given phase
     *
     * @param phaseId
     * @param uiComponent
     * @param closure
     */
    public static void executeLater(PhaseId phaseId, UIComponent uiComponent, final Closure closure) {
        @SuppressWarnings("unused")
        ExecuteLater executeLater = new ExecuteLater(phaseId, uiComponent) {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute() {
                closure.execute(null);
            }
        };
    }

    /**
     * Similar method for {@link UISelectItem} objects is {@link GeneralSelectorGenerator#addDefault(FacesContext, List)}
     *
     * @param results
     * @param context
     */
    public static void addDefault(List<SelectItem> results, FacesContext context) {
        SelectItem selectItem = new SelectItem(DEFAULT_SELECT_VALUE, MessageUtil.getMessage(context, "select_default_label"));
        results.add(0, selectItem);
    }

    public static boolean isAlwaysEditComponent(UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        return attributes.containsKey(IS_ALWAYS_EDIT) && Boolean.valueOf((Boolean) attributes.get(IS_ALWAYS_EDIT));
    }

    public static int getRenderedChildrenCount(UIComponent parent) {
        if (parent == null || parent.getChildCount() == 0) {
            return 0;
        }
        int count = 0;
        for (Object child : parent.getChildren()) {
            if (child instanceof UIComponent && ((UIComponent) child).isRendered()) {
                count++;
            }
        }
        return count;
    }

    public static void addOnchangeClickLink(Application application, List<UIComponent> children, String methodBindingStr, String linkId, UIParameter... parameters) {
        UIActionLink hiddenLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        hiddenLink.setId(linkId);
        hiddenLink.setValue(linkId);
        hiddenLink.setActionListener(application.createMethodBinding(methodBindingStr, new Class[] { ActionEvent.class }));
        children.add(hiddenLink);
        hiddenLink.getAttributes().put("style", "display: none;");
        for (UIParameter parameter : parameters) {
            addChildren(hiddenLink, parameter);
        }
    }

    public static void addOnchangeJavascript(UIComponent component) {
        addOnChangeJavascript(component, null);
    }

    public static void addOnChangeJavascript(UIComponent component, String prependJs) {
        Map<String, Object> attributes = getAttributes(component);
        String styleClass = (String) attributes.get(ATTR_STYLE_CLASS);
        attributes.put(ATTR_STYLE_CLASS, (StringUtils.isNotBlank(styleClass) ? styleClass + " " : "") + getOnChangeStyleClass(prependJs));
    }

    public static String getOnChangeStyleClass() {
        return getOnChangeStyleClass(null);
    }

    private static String getOnChangeStyleClass(String prependJs) {
        String prepend = StringUtils.isNotBlank(prependJs) ? prependJs : "";
        return GeneralSelectorGenerator.ONCHANGE_PARAM_MARKER_CLASS + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER
                + prepend + "var link = jQuery('#' + escapeId4JQ(currElId)).nextAll('a').eq(0); link.click();";
    }

    public static void addStyleClass(UIComponent uiComponent, String styleClassName) {
        String styleClass = (String) getAttribute(uiComponent, STYLE_CLASS);
        styleClass = styleClassName + (StringUtils.isBlank(styleClass) ? "" : " " + styleClass);
        putAttribute(uiComponent, STYLE_CLASS, styleClass);
    }

    public static Integer getIndexFromValueBinding(String vb) {
        Integer index = null;
        if (vb.endsWith("]}")) {
            String indexStr = vb.substring(vb.lastIndexOf('[') + 1, vb.length() - 2);
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                // Do nothing
            }
        }
        return index;
    }

    public static void addRecipientGrouping(Field field, ItemConfigVO item, NamespaceService namespaceService) {
        if (field == null || namespaceService == null) {
            return;
        }

        QName groupColumnProp = null;
        if (DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName().equals(field.getOriginalFieldId())) {
            groupColumnProp = DocumentCommonModel.Props.RECIPIENT_GROUP;
        } else if (DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName().equals(field.getOriginalFieldId())) {
            groupColumnProp = DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP;
        }

        if (groupColumnProp != null && item != null) {
            String prefixProp = groupColumnProp.getPrefixedQName(namespaceService).getPrefixString();
            String hiddenPropNames = item.getCustomAttributes().get(MultiValueEditor.HIDDEN_PROP_NAMES);
            hiddenPropNames = StringUtils.isBlank(hiddenPropNames) ? prefixProp : hiddenPropNames + "," + prefixProp;
            item.getCustomAttributes().put(MultiValueEditor.GROUP_BY_COLUMN_NAME, prefixProp);
        }
    }

    public static CustomChildrenCreator getDocumentRowFileGenerator(final Application application) {
        return new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    int fileCounter = 0;
                    for (Object obj : params) {
                        File file = (File) obj;
                        final DocPermissionEvaluator evaluatorAllow = createEvaluator(application, fileCounter, "evalAllow-" + rowCounter + "-");
                        evaluatorAllow.setAllow("viewDocumentFiles");

                        String fileName = file.getDisplayName();
                        String imageText = getFileImage(file);

                        final UIActionLink fileAllowLink = generateFileReadOnlyLink(application, rowCounter, fileCounter, file, fileName, imageText);
                        ComponentUtil.addChildren(evaluatorAllow, fileAllowLink);
                        components.add(evaluatorAllow);

                        final DocPermissionEvaluator evaluatorDeny = createEvaluator(application, fileCounter, "evalDeny-" + rowCounter + "-");
                        evaluatorDeny.setDeny("viewDocumentFiles");

                        final HtmlGraphicImage image = (HtmlGraphicImage) application.createComponent(HtmlGraphicImage.COMPONENT_TYPE);
                        image.setValue(imageText);
                        image.setId("doc-file-img-" + rowCounter + "-" + fileCounter);
                        image.setTitle(fileName);
                        image.setRendered(file != null);
                        image.setAlt(fileName);

                        ComponentUtil.addChildren(evaluatorDeny, image);
                        components.add(evaluatorDeny);
                        fileCounter++;
                    }
                    rowCounter++;
                }
                return components;
            }

            private DocPermissionEvaluator createEvaluator(Application application, int fileCounter, String evalNamePrefix) {
                final DocPermissionEvaluator evaluatorAllow = (DocPermissionEvaluator) application
                        .createComponent("ee.webmedia.alfresco.privilege.web.DocPermissionEvaluator");
                evaluatorAllow.setId(evalNamePrefix + fileCounter);
                evaluatorAllow.setValueBinding("value", application.createValueBinding("#{r.files[" + fileCounter + "].node}"));
                return evaluatorAllow;
            }
        };
    }

    /** Generate read-only open links for all given files (no permission check is performed) */
    public static CustomChildrenCreator getRowFileGenerator(final Application application) {
        return new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    int fileCounter = 0;
                    for (Object obj : params) {
                        File file = (File) obj;
                        components.add(generateFileReadOnlyLink(application, rowCounter, fileCounter, file, file.getDisplayName(), getFileImage(file)));
                        fileCounter++;
                    }
                    rowCounter++;
                }
                return components;
            }

        };
    }

    private static UIActionLink generateFileReadOnlyLink(final Application application, int rowCounter, int fileCounter, File file, String fileName, String imageText) {
        final UIActionLink fileAllowLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        fileAllowLink.setId("doc-file-link-" + rowCounter + "-" + fileCounter);
        fileAllowLink.setValue("");
        fileAllowLink.setTooltip(fileName);
        fileAllowLink.setShowLink(false);
        fileAllowLink.setHref(file.getReadOnlyUrl());
        fileAllowLink.setImage(imageText);
        fileAllowLink.setTarget("_blank");
        ComponentUtil.getAttributes(fileAllowLink).put("styleClass", "inlineAction");
        return fileAllowLink;
    }

    private static String getFileImage(File file) {
        return file.isDigiDocContainer() ? "/images/icons/ddoc_sign_small.gif" : "/images/icons/attachment.gif";
    }

    public static void setAjaxEnabledOnActionLinksRecursive(UIComponent component, int ajaxParentLevel) {
        if (component instanceof UIActionLink) {
            component.getAttributes().put(ActionLinkRenderer.AJAX_ENABLED, Boolean.TRUE);
            component.getAttributes().put(ActionLinkRenderer.AJAX_PARENT_LEVEL, ajaxParentLevel);
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        if (component instanceof AjaxUpdateable) {
            ajaxParentLevel++;
        }
        for (UIComponent childComponent : children) {
            setAjaxEnabledOnActionLinksRecursive(childComponent, ajaxParentLevel);
        }
        @SuppressWarnings("unchecked")
        Collection<UIComponent> facets = component.getFacets().values();
        if (facets == null) {
            return;
        }
        for (UIComponent facet : facets) {
            setAjaxEnabledOnActionLinksRecursive(facet, ajaxParentLevel);
        }
    }

    public static UIOutput createMandatoryMarker(FacesContext context) {
        UIOutput marker = (UIOutput) context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
        marker.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
        FacesHelper.setupComponentId(context, marker, null);
        @SuppressWarnings("unchecked")
        Map<String, String> attributes = marker.getAttributes();
        attributes.put(CustomAttributeNames.STYLE_CLASS, "red");
        marker.setValue("* ");
        return marker;
    }

    public static UIOutput createUnescapedOutputText(FacesContext context, String id) {
        HtmlOutputText outputText = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        FacesHelper.setupComponentId(context, outputText, id);
        outputText.setEscape(false);
        return outputText;
    }

}
>>>>>>> develop-5.1
