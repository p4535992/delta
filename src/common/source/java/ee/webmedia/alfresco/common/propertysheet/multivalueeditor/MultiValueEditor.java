package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Edit multiple multi-valued properties as a table. A {@code javax.faces.Input} component is generated for each cell. Supports deleting any row and appending
 * an empty row at the end (a {@code null} element is added to each {@link List}). When cells are first generated, it is ensured that each column's {@link List}
 * contains the same amount of elements as the list with greatest amount of elements. Again, {@code null} elements are appended, where necessary.
 * 
 * @author Alar Kvell
 */
public class MultiValueEditor extends UIComponentBase {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MultiValueEditor.class);

    public static final String MULTI_VALUE_EDITOR_FAMILY = MultiValueEditor.class.getCanonicalName();

    @Override
    public String getFamily() {
        return MULTI_VALUE_EDITOR_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (getChildCount() == 0) {
            createExistingComponents(context);
            createPicker(context);
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
        MethodBinding b = getFacesContext().getApplication().createMethodBinding(pickerCallback, new Class[] { int.class, String.class });
        picker.setQueryCallback(b);
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

    protected void pickerFinish(UIGenericPicker picker) {
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();

        int rowIndex = Integer.parseInt((String) getAttributes().get(Search.OPEN_DIALOG_KEY));
        log.debug("Selected rowIndex=" + rowIndex + ", adding results: " + StringUtils.join(results, ", "));

        List<String> propNames = getPropNames();
        List<List<Object>> columnLists = new ArrayList<List<Object>>(propNames.size());
        for (String propName : propNames) {
            columnLists.add(getList(context, propName));
        }

        for (int i = 0; i < results.length; i++) {
            String setterCallback = (String) getAttributes().get("setterCallback");
            MethodBinding b = getFacesContext().getApplication().createMethodBinding(setterCallback, new Class[] { String.class });
            @SuppressWarnings("unchecked")
            List<Object> rowList = (List<Object>) b.invoke(context, new Object[] { results[i] });

            if (columnLists.get(0).size() > rowIndex + i) {
                int columnIndex = 0;
                for (List<Object> columnList : columnLists) {
                    columnList.set(rowIndex + i, rowList.get(columnIndex++));
                }
            } else {
                int columnIndex = 0;
                for (List<Object> columnList : columnLists) {
                    columnList.add(rowList.get(columnIndex++));
                }
                appendRowComponent(context, rowIndex + i);
            }
        }
        for (List<Object> columnList : columnLists) {
            log.debug("Column list=" + columnList);
        }
        getAttributes().remove(Search.OPEN_DIALOG_KEY);
        picker.queueEvent(new UIGenericPicker.PickerEvent(picker, 1 /* ACTION_CLEAR */, 0, null, null));
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

    protected void createExistingComponents(FacesContext context) {
        int rows = initializeRows(context);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            appendRowComponent(context, rowIndex);
        }
    }

    protected int initializeRows(FacesContext context) {
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

    protected void appendRowComponent(FacesContext context, int rowIndex) {
        UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();
        children.add(container);
        List<String> types = getComponentTypes();

        @SuppressWarnings("unchecked")
        List<UIComponent> containerChildren = container.getChildren();
        int columnIndex = 0;
        for (String propName : getPropNames()) {
            String type = null;
            if (types != null && types.size() > columnIndex) {
                type = types.get(columnIndex);
            }
            UIComponent component = generateCellComponent(context, type);
            FacesHelper.setupComponentId(context, component, null);
            setValueBinding(context, component, propName, rowIndex);
            containerChildren.add(component);
            if (isDisabled()) {
                ComponentUtil.setDisabledAttributeRecursively(component);
            }
            columnIndex++;
        }
    }

    protected UIComponent generateCellComponent(FacesContext context, String spec) {
        UIComponent component;
        String[] fields = spec.split(":");

        String type = "";
        if (fields.length >= 1) {
            type = fields[0];
        }

        if ("textarea".equals(type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            component.setRendererType(ComponentConstants.JAVAX_FACES_TEXTAREA);
            FacesHelper.setupComponentId(context, component, null);
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            // Default values from TextAreaGenerator
            attributes.put("rows", 3);
            attributes.put("cols", 32);
        } else if ("date".equals(type)) {
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            FacesHelper.setupComponentId(context, component, null);
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put("styleClass", "date");
            ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
        } else {
            if (StringUtils.isNotEmpty(type) && !"input".equals(type)) {
                log.warn("Component type '" + type + "' is not supported, defaulting to input");
            }
            component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            FacesHelper.setupComponentId(context, component, null);
        }

        if (fields.length >= 2) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put("styleClass", fields[1]);
        }

        return component;
    }

    protected void appendRow(FacesContext context) {
        int rowIndex = Integer.MAX_VALUE;
        for (String propName : getPropNames()) {
            List<?> list = getList(context, propName);
            rowIndex = list.size();
            list.add(null);
        }
        appendRowComponent(context, rowIndex);
    }

    protected void removeRow(FacesContext context, int removeIndex) {
        List<String> propNames = getPropNames();
        for (String propName : propNames) {
            List<?> list = getList(context, propName);
            list.remove(removeIndex);
        }

        int rowIndex = 0;
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();
        for (Iterator<UIComponent> i = children.iterator(); i.hasNext();) {
            UIComponent container = i.next();
            if (!(container instanceof HtmlPanelGroup)) {
                continue;
            }
            if (rowIndex == removeIndex) {
                // remove a row from the middle
                i.remove();
            } else if (rowIndex > removeIndex) {
                // iterate over all rows, starting from the removed spot, and correct their value-binding to match the correct list element
                if (container instanceof HtmlPanelGroup) {
                    int columnIndex = 0;
                    for (String propName : propNames) {
                        UIComponent component = (UIComponent) container.getChildren().get(columnIndex++);
                        setValueBinding(context, component, propName, rowIndex - 1);
                    }
                }
            }
            rowIndex++;
        }
    }

    protected void setValueBinding(FacesContext context, UIComponent component, String propName, int rowIndex) {
        ValueBinding vb = createValueBinding(context, propName, rowIndex);
        component.setValueBinding("value", vb);
    }

    protected ValueBinding createValueBinding(FacesContext context, String propName) {
        return createValueBinding(context, propName, -1);
    }

    protected ValueBinding createValueBinding(FacesContext context, String propName, int rowIndex) {
        ValueBinding vb = context.getApplication().createValueBinding(
                "#{" + getPropertySheetVar() + ".properties[\"" + propName + "\"]" + (rowIndex >= 0 ? "[" + rowIndex + "]" : "") + "}");
        return vb;
    }

    protected List<Object> getList(FacesContext context, String propName) {
        ValueBinding vb = createValueBinding(context, propName);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) vb.getValue(context);
        if (list == null) {
            list = new ArrayList<Object>();
            vb.setValue(context, list);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getPropNames() {
        return (List<String>) getAttributes().get("propNames");
    }

    @SuppressWarnings("unchecked")
    protected List<String> getComponentTypes() {
        return (List<String>) getAttributes().get("componentTypes");
    }

    protected String getPropertySheetVar() {
        return (String) getAttributes().get("propertySheetVar");
    }

    protected String getPickerCallback() {
        return (String) getAttributes().get(Search.PICKER_CALLBACK_KEY);
    }

    protected boolean isDisabled() {
        return Utils.isComponentDisabledOrReadOnly(this);
    }

}
