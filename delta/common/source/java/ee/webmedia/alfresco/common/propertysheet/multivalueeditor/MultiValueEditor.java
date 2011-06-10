package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor.MultiValueEditorEvent;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Edit multiple multi-valued properties as a table. A {@code javax.faces.Input} component is generated for each cell. Supports deleting any row and appending
 * an empty row at the end (a {@code null} element is added to each {@link List}). When cells are first generated, it is ensured that each column's {@link List}
 * contains the same
 * amount of elements as the list with greatest amount of elements. Again, {@code null} elements are appended, where necessary.
 * 
 * @author Alar Kvell
 */
public class MultiValueEditor extends UIComponentBase implements AjaxUpdateable, NamingContainer {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MultiValueEditor.class);

    protected static final String PROPERTY_SHEET_VAR = "propertySheetVar";
    protected static final String PREPROCESS_CALLBACK = "preprocessCallback";
    protected static final String FILTERS = "filters";
    protected static final String FILTER_INDEX = "filterIndex";

    public static final String MULTI_VALUE_EDITOR_FAMILY = MultiValueEditor.class.getCanonicalName();
    public static final String ADD_LABEL_ID = "addLabelId";
    public static final String SHOW_HEADERS = "showHeaders";
    public static final String INITIAL_ROWS = "initialRows";

    @Override
    public String getFamily() {
        return MULTI_VALUE_EDITOR_FAMILY;
    }

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(context) + "_container";
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (getChildCount() == 0) {
            UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
            createExistingComponents(context, propertySheet);
            if (!isDisabled()) {
                // If requested create a single empty row when there are no existing rows
                Integer initialRows = (Integer) getAttributes().get(MultiValueEditor.INITIAL_ROWS);
                if (initialRows != null && getChildCount() == 0) {
                    for (int i = 0; i < initialRows; i++) {
                        appendRow(context);
                    }
                }
                createPicker(context);
            }
        }
        super.encodeBegin(context);
    }

    protected void createPicker(FacesContext context) {
        if (getPickerCallback() == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();

        UIGenericPicker picker = (UIGenericPicker) context.getApplication().createComponent("org.alfresco.faces.GenericPicker");
        FacesHelper.setupComponentId(context, picker, "picker");
        picker.setShowFilter(false);
        picker.setWidth(400);
        picker.setMultiSelect(true);
        String pickerCallback = getPickerCallback();
        Application application = getFacesContext().getApplication();
        MethodBinding b = application.createMethodBinding(pickerCallback, new Class[] { int.class, String.class });
        picker.setQueryCallback(b);

        String filters = (String) getAttributes().get(FILTERS);
        if (StringUtils.isNotBlank(filters)) {
            ValueBinding filtersBind = context.getApplication().createValueBinding(filters);
            picker.setValueBinding(FILTERS, filtersBind);
            picker.setShowFilter(true);
        }

        String filterIndex = (String) getAttributes().get(FILTER_INDEX);
        if (StringUtils.isNotBlank(filterIndex) && StringUtils.isNumeric(filterIndex)) {
            picker.setDefaultFilterIndex(Integer.parseInt(filterIndex));
        }

        picker.addActionListener(new PickerFinishActionListener());

        children.add(picker);
    }

    public static class PickerFinishActionListener implements ActionListener {

        @Override
        public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
            UIComponent parent = actionEvent.getComponent().getParent();
            if (parent instanceof MultiValueEditor) {
                ((MultiValueEditor) parent).pickerFinish((UIGenericPicker) actionEvent.getComponent());
            } else {
                throw new RuntimeException();
            }
        }

    }

    private void pickerFinish(UIGenericPicker picker) {
        String[] results = picker.getSelectedResults();
        String strRowIndex = (String) getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (results == null || StringUtils.isBlank(strRowIndex)) {
            return;
        }

        int rowIndex = Integer.parseInt(strRowIndex);
        innerPickerFinish(picker.getFilterIndex(), rowIndex, results, FacesContext.getCurrentInstance());

        getAttributes().remove(Search.OPEN_DIALOG_KEY);
        picker.queueEvent(new UIGenericPicker.PickerEvent(picker, 1 /* ACTION_CLEAR */, 0, null, null));
    }

    public void innerPickerFinish(int pickerFilterIndex, int rowIndex, String[] results, FacesContext context) {
        log.debug("Selected rowIndex=" + rowIndex + ", adding results: " + StringUtils.join(results, ", "));

        String preprocessCallback = (String) getAttributes().get(PREPROCESS_CALLBACK);
        if (StringUtils.isNotBlank(preprocessCallback)) {
            MethodBinding preprocessBind = getFacesContext().getApplication().createMethodBinding(
                    preprocessCallback, new Class[] { String[].class, Integer.class });
            results = (String[]) preprocessBind.invoke(context, new Object[] { results, pickerFilterIndex });
        }

        List<String> propNames = getPropNames();
        List<List<Object>> columnLists = new ArrayList<List<Object>>(propNames.size());
        for (String propName : propNames) {
            columnLists.add(getList(context, propName));
        }
        UIPropertySheet propertySheet = null;
        String setterCallback = (String) getAttributes().get(Search.SETTER_CALLBACK);
        boolean insertInMiddle = columnLists.get(0).size() > rowIndex + 1;
        for (int i = 0; i < results.length; i++) {
            MethodBinding b = getFacesContext().getApplication().createMethodBinding(setterCallback, new Class[] { String.class });
            @SuppressWarnings("unchecked")
            List<Object> rowList = (List<Object>) b.invoke(context, new Object[] { results[i] });

            if (columnLists.get(0).size() > 0 && i == 0) {
                int columnIndex = 0;
                for (List<Object> columnList : columnLists) {
                    if (rowList.size() > columnIndex) {
                        columnList.set(rowIndex + i, rowList.get(columnIndex));
                    } else {
                        columnList.set(rowIndex + i, null);
                    }
                    columnIndex++;
                }
            } else {
                int columnIndex = 0;
                for (List<Object> columnList : columnLists) {
                    if (rowList.size() > columnIndex) {
                        if (insertInMiddle) {
                            columnList.add(rowIndex + i, rowList.get(columnIndex));
                        } else {
                            columnList.add(rowList.get(columnIndex));
                        }
                    } else {
                        if (insertInMiddle) {
                            columnList.add(rowIndex + i, null);
                        } else {
                            columnList.add(null);
                        }
                    }
                    columnIndex++;
                }
            }
        }
        if (propertySheet == null) {
            propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
        }
        clearChildren();
        int numRows = columnLists.get(0).size();
        for (int ri = 0; ri < numRows; ri++) {
            appendRowComponent(context, ri, propertySheet);
        }

        for (List<Object> columnList : columnLists) {
            log.debug("Column list=" + columnList);
        }
    }

    // We can reuse org.alfresco.web.ui.repo.component.MultiValueEditorEvent and don't have to define it ourselves,
    // because org.alfresco.web.ui.repo.renderer.BaseMultiValueRenderer fires this event in just the way that is needed
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof MultiValueEditorEvent) {
            if (isDisabled()) {
                throw new RuntimeException("Disabled component should not fire MultiValueEditorEvent: " + getId());
            }
            FacesContext context = FacesContext.getCurrentInstance();
            MultiValueEditorEvent assocEvent = (MultiValueEditorEvent) event;
            if (assocEvent.Action == UIMultiValueEditor.ACTION_ADD) {
                appendRow(context);
            } else if (assocEvent.Action == UIMultiValueEditor.ACTION_REMOVE) {
                removeRow(context, assocEvent.RemoveIndex);
            }
        } else {
            super.broadcast(event);
        }
    }

    private void createExistingComponents(FacesContext context, UIPropertySheet propertySheet) {
        int rows = initializeRows(context);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            appendRowComponent(context, rowIndex, propertySheet);
        }
    }

    private int initializeRows(FacesContext context) {
        // Ensure that all lists contain the same amount of elements, append null elements if necessary
        int rows = 0;
        List<String> propNames = getPropNames();
        List<List<?>> data = new ArrayList<List<?>>(propNames.size());
        for (String propName : propNames) {
            List<?> list = getList(context, propName);
            if (list.size() > rows) {
                rows = list.size();
            }
            data.add(list);
        }
        for (List<?> list : data) {
            while (list.size() < rows) {
                list.add(null);
            }
        }
        return rows;
    }

    private void appendRowComponent(FacesContext context, Integer rowIndex, UIPropertySheet propertySheet) {
        UIComponent rowContainer = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();
        children.add(rowContainer);

        @SuppressWarnings("unchecked")
        List<UIComponent> rowContainerChildren = rowContainer.getChildren();
        int columnIndex = 0;
        for (ComponentPropVO componentPropVO : getPropertyGeneratorDescriptors()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            requestMap.put(VALUE_INDEX_IN_MULTIVALUED_PROPERTY, rowIndex);
            componentPropVO.getCustomAttributes().put(VALUE_INDEX_IN_MULTIVALUED_PROPERTY, rowIndex.toString());
            final UIComponent component = ComponentUtil.generateAndAddComponent(context, componentPropVO, propertySheet, rowContainerChildren);
            // save valueIndex also to component, as it can be used in MandatoryIfValidator,
            // to find other UIInputs based on given property name and valueIndex(if component is multiValued)
            ComponentUtil.putAttribute(component, VALUE_INDEX_IN_MULTIVALUED_PROPERTY, rowIndex);
            requestMap.remove(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
            if (!componentPropVO.isUseComponentGenerator()) {
                // component was not generated using componentGenerator, so we have to add bindings manually
                FacesHelper.setupComponentId(context, component, componentPropVO.getPropertyName() + "_" + rowIndex);
                setValueBinding(context, component, componentPropVO.getPropertyName(), rowIndex);
            }
            if (Utils.isComponentDisabledOrReadOnly(this)) {
                ComponentUtil.setDisabledAttributeRecursively(component);
            }
            columnIndex++;
        }
    }

    private void appendRow(FacesContext context) {
        Integer rowIndex = null;
        for (String propName : getPropNames()) {
            List<?> list = getList(context, propName);
            rowIndex = list.size();
            list.add(null);
        }
        UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
        appendRowComponent(context, rowIndex, propertySheet);
    }

    private void removeRow(FacesContext context, int removeIndex) {
        List<String> propNames = getPropNames();
        for (String propName : propNames) {
            List<?> list = getList(context, propName);
            list.remove(removeIndex);
        }
        clearChildren();
        int numRows = getList(context, getPropNames().get(0)).size();
        for (int ri = 0; ri < numRows; ri++) {
            appendRowComponent(context, ri, ComponentUtil.getAncestorComponent(this, UIPropertySheet.class));
        }
    }

    public void clearChildren() {
        List<UIComponent> children = getChildren();
        for (Iterator<UIComponent> i = children.iterator(); i.hasNext();) {
            UIComponent container = i.next();
            if (!(container instanceof HtmlPanelGroup)) {
                continue;
            }
            i.remove();
        }
    }

    private void setValueBinding(FacesContext context, UIComponent component, String propName, int rowIndex) {
        ValueBinding vb = createValueBinding(context, propName, rowIndex);
        component.setValueBinding("value", vb);
    }

    private ValueBinding createValueBinding(FacesContext context, String propName) {
        return createValueBinding(context, propName, -1);
    }

    private ValueBinding createValueBinding(FacesContext context, String propName, int rowIndex) {
        ValueBinding vb = context.getApplication().createValueBinding(
                "#{" + getPropertySheetVar() + ".properties[\"" + propName + "\"]" + (rowIndex >= 0 ? "[" + rowIndex + "]" : "") + "}");
        return vb;
    }

    private List<Object> getList(FacesContext context, String propName) {
        ValueBinding vb = createValueBinding(context, propName);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) vb.getValue(context);
        if (list == null) {
            list = new ArrayList<Object>();
            vb.setValue(context, list);
        }
        return list;
    }

    private List<String> getPropNames() {
        final List<ComponentPropVO> propsVOs = getPropertyGeneratorDescriptors();
        List<String> propNames;
        propNames = new ArrayList<String>(propsVOs.size());
        for (ComponentPropVO componentPropVO : propsVOs) {
            propNames.add(componentPropVO.getPropertyName());
        }
        return propNames;
    }

    private List<ComponentPropVO> getPropertyGeneratorDescriptors() {
        @SuppressWarnings("unchecked")
        final List<ComponentPropVO> propsVOs = (List<ComponentPropVO>) getAttributes().get(PROP_GENERATOR_DESCRIPTORS);
        return propsVOs;
    }

    private String getPropertySheetVar() {
        return (String) getAttributes().get(PROPERTY_SHEET_VAR);
    }

    protected String getPickerCallback() {
        return (String) getAttributes().get(Search.PICKER_CALLBACK_KEY);
    }

    private boolean isDisabled() {
        return Utils.isComponentDisabledOrReadOnly(this);
    }

}
