package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
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
 * an empty row at the end (a {@code null} element is added to each {@link List}). When cells are first generated, it is ensured that each column's {@link List} contains the same
 * amount of elements as the list with greatest amount of elements. Again, {@code null} elements are appended, where necessary. <br>
 * Component configuration attributes are documented at {@link MultiValueEditorGenerator}.
 * 
 * @author Alar Kvell
 */
public class MultiValueEditor extends UIComponentBase implements AjaxUpdateable, NamingContainer {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MultiValueEditor.class);

    protected static final String PROPERTY_SHEET_VAR = "propertySheetVar";
    public static final String PREPROCESS_CALLBACK = "preprocessCallback";
    protected static final String FILTERS = "filters";
    protected static final String FILTER_INDEX = "filterIndex";
    public static final String ATTR_CLICK_LINK_ID = "clickLinkId";

    public static final String MULTI_VALUE_EDITOR_FAMILY = MultiValueEditor.class.getCanonicalName();
    public static final String ADD_LABEL_ID = "addLabelId";
    public static final String SHOW_HEADERS = "showHeaders";
    public static final String INITIAL_ROWS = "initialRows";
    public static final String IS_AUTOMATICALLY_ADD_ROWS = "isAutomaticallyAddRows";
    public static final String NO_ADD_LINK_LABEL = "noAddLinkLabel";
    public static final String HIDDEN_PROP_NAMES = "hiddenPropNames";
    public static final String SETTER_CALLBACK_RETURNS_MAP = "setterCallbackReturnsMap";

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
        MethodBinding b = application.createMethodBinding(pickerCallback, GenericPickerTag.QUERYCALLBACK_CLASS_ARGS);
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
        picker.queueEvent(new UIGenericPicker.PickerEvent(picker, UIGenericPicker.ACTION_CLEAR, 0, null, null));
    }

    public void innerPickerFinish(int pickerFilterIndex, int rowIndex, String[] results, FacesContext context) {
        log.debug("Selected rowIndex=" + rowIndex + ", adding results: " + StringUtils.join(results, ", "));

        String preprocessCallback = (String) getAttributes().get(PREPROCESS_CALLBACK);
        if (StringUtils.isNotBlank(preprocessCallback)) {
            MethodBinding preprocessBind = getFacesContext().getApplication().createMethodBinding(
                    preprocessCallback, new Class[] { int.class, String[].class });
            results = (String[]) preprocessBind.invoke(context, new Object[] { pickerFilterIndex, results });
        }

        NamespaceService namespaceService = getNamespaceService();
        List<String> propNames = getRegularAndHiddenPropNames();
        List<Pair<QName, List<Object>>> columnListsWithPropNames = new ArrayList<Pair<QName, List<Object>>>();
        for (String propName : propNames) {
            List<Object> columnList = getList(context, propName);
            columnListsWithPropNames.add(Pair.newInstance(QName.resolveToQName(namespaceService, propName), columnList));
        }
        String setterCallback = (String) getAttributes().get(Search.SETTER_CALLBACK);
        boolean setterCallbackReturnsMap = Boolean.TRUE.equals(getAttributes().get(SETTER_CALLBACK_RETURNS_MAP));
        boolean insertInMiddle = columnListsWithPropNames.get(0).getSecond().size() > rowIndex + 1;
        for (int i = 0; i < results.length; i++) {
            MethodBinding b = getFacesContext().getApplication().createMethodBinding(setterCallback, new Class[] { String.class });
            Map<QName, Object> rowMap = null;
            List<Object> rowList = null;
            if (setterCallbackReturnsMap) {
                rowMap = (Map<QName, Object>) b.invoke(context, new Object[] { results[i] });
            } else {
                rowList = (List<Object>) b.invoke(context, new Object[] { results[i] });
            }

            if (columnListsWithPropNames.get(0).getSecond().size() > 0 && i == 0) { // Only first result overwrites row
                int columnIndex = 0;
                for (Pair<QName, List<Object>> columnListWithPropName : columnListsWithPropNames) {
                    List<Object> columnList = columnListWithPropName.getSecond();
                    QName propName = columnListWithPropName.getFirst();
                    processRowOverwrite(rowIndex, setterCallbackReturnsMap, i, rowMap, rowList, columnIndex, columnList, propName);
                    columnIndex++;
                }
            } else { // All followings results add rows
                int columnIndex = 0;
                for (Pair<QName, List<Object>> columnListWithPropName : columnListsWithPropNames) {
                    List<Object> columnList = columnListWithPropName.getSecond();
                    QName propName = columnListWithPropName.getFirst();
                    processRowAdd(rowIndex, setterCallbackReturnsMap, insertInMiddle, i, rowMap, rowList, columnIndex, columnList, propName);
                    columnIndex++;
                }
            }
        }
        clearChildren();
        UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
        int numRows = columnListsWithPropNames.get(0).getSecond().size();
        for (int ri = 0; ri < numRows; ri++) {
            appendRowComponent(context, ri, propertySheet);
        }

        if (log.isDebugEnabled()) {
            for (Pair<QName, List<Object>> columnListWithPropName : columnListsWithPropNames) {
                log.debug("Column list=" + columnListWithPropName);
            }
        }
    }

    private void processRowOverwrite(int rowIndex, boolean setterCallbackReturnsMap, int i, Map<QName, Object> rowMap, List<Object> rowList, int columnIndex,
            List<Object> columnList, QName propName) {
        if (setterCallbackReturnsMap) {
            // if propName doesn't exist, we don't overwrite value, because it is an existing row
            if (rowMap.containsKey(propName)) {
                columnList.set(rowIndex + i, rowMap.get(propName));
            }
        } else {
            if (rowList.size() > columnIndex) {
                columnList.set(rowIndex + i, rowList.get(columnIndex));
            } else {
                columnList.set(rowIndex + i, null);
            }
        }
    }

    private void processRowAdd(int rowIndex, boolean setterCallbackReturnsMap, boolean insertInMiddle, int i, Map<QName, Object> rowMap, List<Object> rowList, int columnIndex,
            List<Object> columnList, QName propName) {
        Object value;
        if (setterCallbackReturnsMap) {
            // even if propName doesn't exist, we must set the value to null, because we are adding a new row
            value = rowMap.get(propName);
        } else {
            if (rowList.size() > columnIndex) {
                value = rowList.get(columnIndex);
            } else {
                value = null;
            }
        }
        if (insertInMiddle) {
            columnList.add(rowIndex + i, value);
        } else {
            columnList.add(value);
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
        List<String> propNames = getRegularAndHiddenPropNames();
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
            if (ComponentUtil.isComponentDisabledOrReadOnly(this)) {
                ComponentUtil.setReadonlyAttributeRecursively(component);
            }
            columnIndex++;
        }
    }

    private void appendRow(FacesContext context) {
        Integer rowIndex = null;
        List<String> propNames = getRegularAndHiddenPropNames();
        for (String propName : propNames) {
            List<?> list = getList(context, propName);
            if (rowIndex == null) {
                rowIndex = list.size();
            }
            list.add(null);
        }
        UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
        appendRowComponent(context, rowIndex, propertySheet);
    }

    private void removeRow(FacesContext context, int removeIndex) {
        List<String> propNames = getRegularAndHiddenPropNames();
        for (String propName : propNames) {
            List<?> list = getList(context, propName);
            list.remove(removeIndex);
        }
        clearChildren();
        int numRows = getList(context, propNames.get(0)).size();
        for (int ri = 0; ri < numRows; ri++) {
            appendRowComponent(context, ri, ComponentUtil.getAncestorComponent(this, UIPropertySheet.class));
        }
    }

    public void clearChildren() {
        @SuppressWarnings("unchecked")
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

    private List<String> getRegularAndHiddenPropNames() {
        final List<ComponentPropVO> propsVOs = getPropertyGeneratorDescriptors();
        List<String> propNames;
        propNames = new ArrayList<String>(propsVOs.size());
        for (ComponentPropVO componentPropVO : propsVOs) {
            propNames.add(componentPropVO.getPropertyName());
        }
        String hiddenPropNames = (String) getAttributes().get(HIDDEN_PROP_NAMES);
        if (StringUtils.isNotBlank(hiddenPropNames)) {
            propNames.addAll(Arrays.asList(StringUtils.split(hiddenPropNames, ',')));
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
        return ComponentUtil.isComponentDisabledOrReadOnly(this);
    }

    public boolean isAutomaticallyAddRows() {
        return Boolean.TRUE.equals(getAttributes().get(IS_AUTOMATICALLY_ADD_ROWS));
    }

}
