package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Edit a multi-valued property. Supports removing any element. Supports appending elements at the end by using a search component which returns selected
 * results.
 * 
 * @author Alar Kvell
 */
public class Search extends UIComponentBase {

    public static final String SEARCH_FAMILY = Search.class.getCanonicalName();

    public static final String OPEN_DIALOG_KEY = "openDialog";
    public static final String DATA_TYPE_KEY = "dataType";
    public static final String DATA_MULTI_VALUED = "dataMultiValued";
    public static final String DIALOG_TITLE_ID_KEY = "dialogTitleId";
    public static final String CONVERTER_KEY = "converter";
    public static final String PICKER_CALLBACK_KEY = "pickerCallback";
    public static final String VALUE_KEY = "value";
//    public static final String FILLED_KEY = "filled";

    @Override
    public String getFamily() {
        return SEARCH_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (getChildCount() == 0) {
            createExistingComponents(context);
            if (!isDisabled() || isEditable()) {
                createPicker(context);
            }
        }

        boolean empty;
        if (isMultiValued()) {
            empty = getList(context).isEmpty();
        } else {
            Object value = getValue(context);
            empty = (value == null || (value instanceof String && ((String) value).length() == 0));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        attributes.put("empty", empty);

        super.encodeBegin(context);
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (event instanceof SearchRemoveEvent) {
            if (isDisabled() && !isEditable()) {
                throw new RuntimeException("Disabled component should not fire SearchRemoveEvent: " + getId());
            }
            if (isMultiValued()) {
                removeRow(context, ((SearchRemoveEvent) event).index);
            } else if (isMandatory()) {
                throw new RuntimeException("Single-valued mandatory component should not fire SearchRemoveEvent: " + getId());
            } else {
                ((UIComponent) getChildren().get(0)).getChildren().remove(0);
                setValue(context, null);
            }
        } else {
            super.broadcast(event);
        }
    }

    protected void createPicker(FacesContext context) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();

        UIGenericPicker picker = (UIGenericPicker) context.getApplication().createComponent("org.alfresco.faces.GenericPicker");
        FacesHelper.setupComponentId(context, picker, "picker");
        picker.setShowFilter(false);
        picker.setWidth(400);
        picker.setMultiSelect(isMultiValued());
        String pickerCallback = (String) getAttributes().get(PICKER_CALLBACK_KEY);
        MethodBinding b = getFacesContext().getApplication().createMethodBinding(pickerCallback, new Class[] { int.class, String.class });
        picker.setQueryCallback(b);
        picker.addActionListener(new PickerFinishActionListener());
        children.add(picker);
    }

    protected void pickerFinish(UIGenericPicker picker) {
        String[] results = picker.getSelectedResults();
        if (results == null) {
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        if (isMultiValued()) {
            for (String result : results) {
                appendRow(context, result);
            }
        } else {
            if (results.length > 1) {
                throw new RuntimeException("Single-valued property does not support multiple values");
            }
            if (results.length == 1) {
                @SuppressWarnings("unchecked")
                List<UIComponent> children = ((UIComponent) getChildren().get(0)).getChildren();
                if (!children.isEmpty()) {
                    children.remove(0);
                }
                appendRow(context, results[0]);
                
                String setterCallback = getSetterCallback(context);
                if (setterCallback != null) {
                    MethodBinding b = getFacesContext().getApplication().createMethodBinding(setterCallback, new Class[] { String.class });
                    b.invoke(context, new Object[] { results[0] });
                }
            }
        }
        getAttributes().remove(OPEN_DIALOG_KEY);
        picker.queueEvent(new UIGenericPicker.PickerEvent(picker, 1 /* ACTION_CLEAR */, 0, null, null));
    }

    protected void createExistingComponents(FacesContext context) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();

        UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        FacesHelper.setupComponentId(context, container, null);
        children.add(container);
 
        if (isMultiValued()) {
            int rows = getList(context).size();
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                appendRowComponent(context, rowIndex);
            }
        } else {
            appendRowComponent(context, -1);
        }
    }

    protected void appendRowComponent(FacesContext context, int rowIndex) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = ((UIComponent) getChildren().get(0)).getChildren();

        UIOutput component = (UIOutput) context.getApplication().createComponent(isEditable() ? ComponentConstants.JAVAX_FACES_INPUT : ComponentConstants.JAVAX_FACES_OUTPUT);
        FacesHelper.setupComponentId(context, component, null);
        setValueBinding(context, component, rowIndex);
        ComponentUtil.createAndSetConverter(context, (String) getAttributes().get(CONVERTER_KEY), component);
        if (isDisabled()) {
            ComponentUtil.setDisabledAttributeRecursively(component);
        }
        children.add(component);
    }

    protected void appendRow(FacesContext context, String value) {
        Class<?> dataType = (Class<?>) getAttributes().get(DATA_TYPE_KEY);
        Object convertedValue = DefaultTypeConverter.INSTANCE.convert(dataType, value);

        if (isMultiValued()) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) getList(context);
            list.add(convertedValue);
            appendRowComponent(context, list.size() - 1);
        } else {
            setValue(context, convertedValue);
            appendRowComponent(context, -1);
        }
    }

    protected void removeRow(FacesContext context, int removeIndex) {
        List<?> list = getList(context);
        list.remove(removeIndex);

        @SuppressWarnings("unchecked")
        List<UIComponent> children = ((UIComponent) getChildren().get(0)).getChildren();

        // remove a row from the middle
        children.remove(removeIndex);

        // iterate over all rows, starting from the removed spot, and correct their value-binding to match the correct list element
        for (int rowIndex = removeIndex; rowIndex < children.size(); rowIndex++) {
            UIComponent component = children.get(rowIndex);
            setValueBinding(context, component, rowIndex);
        }
    }

    protected void setValueBinding(FacesContext context, UIComponent component, int rowIndex) {
        ValueBinding vb = createValueBinding(context, rowIndex);
        component.setValueBinding(VALUE_KEY, vb);
    }

    protected ValueBinding createValueBinding(FacesContext context, int rowIndex) {
        String list = getValueBinding(VALUE_KEY).getExpressionString();
        ValueBinding vb = context.getApplication().createValueBinding(list.substring(0, list.length() - 1) + (rowIndex >= 0 ? "[" + rowIndex + "]" : "") + "}");
        return vb;
    }

    protected Object getValue(FacesContext context) {
        ValueBinding vb = getValueBinding(VALUE_KEY);
        return vb.getValue(context);
    }

    protected void setValue(FacesContext context, Object value) {
        ValueBinding vb = getValueBinding(VALUE_KEY);
        vb.setValue(context, value);
    }

    protected List<?> getList(FacesContext context) {
        ValueBinding vb = getValueBinding(VALUE_KEY);
        List<?> list = (List<?>) vb.getValue(context);
        if (list == null) {
            list = new ArrayList<Object>();
            vb.setValue(context, list);
        }
        return list;
    }

    protected String getSetterCallback(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return (String) attributes.get("setterCallback");
    }

    protected boolean isDisabled() {
        return Utils.isComponentDisabledOrReadOnly(this);
    }

    protected boolean isMultiValued() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return attributes.containsKey(DATA_MULTI_VALUED) && (Boolean) attributes.get(DATA_MULTI_VALUED);
    }

    protected boolean isMandatory() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return attributes.containsKey("dataMandatory") && (Boolean) attributes.get("dataMandatory");
    }

    protected boolean isEmpty() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return attributes.containsKey("empty") && (Boolean) attributes.get("empty");
    }

    protected boolean isEditable() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return attributes.containsKey("editable") && (Boolean) attributes.get("editable");
    }

    public static class PickerFinishActionListener implements ActionListener {

        @Override
        public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
            UIComponent parent = actionEvent.getComponent().getParent();
            if (parent instanceof Search) {
                ((Search) parent).pickerFinish((UIGenericPicker) actionEvent.getComponent());
            } else {
                throw new RuntimeException();
            }
        }

    }

    public static class SearchRemoveEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        public int index;

        public SearchRemoveEvent(UIComponent uiComponent, int index) {
            super(uiComponent);
            this.index = index;
        }

    }

}
