package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
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
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Edit a multi-valued property. Supports removing any element. Supports appending elements at the end by using a search component which returns selected
 * results.
 * 
 * @author Alar Kvell
 */
public class Search extends UIComponentBase implements AjaxUpdateable, NamingContainer {

    public static final String SETTER_CALLBACK = "setterCallback";
    public static final String PREPROCESS_CALLBACK = "preprocessCallback";

    public static final String SETTER_CALLBACK_TAKES_NODE = "setterCallbackTakesNode";

    public static final String SEARCH_FAMILY = Search.class.getCanonicalName();

    public static final String OPEN_DIALOG_KEY = "openDialog";
    public static final String DATA_TYPE_KEY = "dataType";
    public static final String DATA_MULTI_VALUED = "dataMultiValued";
    public static final String SEARCH_LINK_LABEL = "searchLinkLabel";
    public static final String SEARCH_LINK_TOOLTIP = "searchLinkTooltip";
    /** determines if only unique values should be added to multiValued property values. Default value (if attribute is missing) is true */
    public static final String ALLOW_DUPLICATES_KEY = "allowDuplicates";
    public static final String DIALOG_TITLE_ID_KEY = "dialogTitleId";
    public static final String CONVERTER_KEY = "converter";
    public static final String PICKER_CALLBACK_KEY = "pickerCallback";
    public static final String PICKER_CALLBACK_KEY_PARAM = "pickerCallbackParams";
    public static final String VALUE_KEY = "value";
    public static final String SHOW_FILTER_KEY = "showFilter";
    public static final String FILTERS_KEY = "filters";
    public static final String FILTERS_ALLOW_GROUP_SELECT_KEY = "filtersAllowGroupSelect";
    public static final String ID_KEY = "id";
    public static final String STYLE_CLASS_KEY = "styleClass";
    public static final String AJAX_PARENT_LEVEL_KEY = "ajaxParentLevel";
    /** method binding that can be used to add tooltip to rows added using search component */
    public static final String ATTR_TOOLTIP_MB = "tooltip";
    /** should delete(clear value) link be rendered when component is singlevalued (by default not rendered) */
    public static final String ALLOW_CLEAR_SINGLE_VALUED = "allowClearSingleValued";
    public static final String FILTER_INDEX = "filterIndex";
    public static final String TEXTAREA = "textarea";
    public static final String SEARCH_SUGGEST_DISABLED = "searchSuggestDisabled";

    @Override
    public String getFamily() {
        return SEARCH_FAMILY;
    }

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(context) + "_container";
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        getChildren().clear();
        createExistingComponents(context);
        if (!isDisabled() || isChildOfUIRichList()) {
            createPicker(context);
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
            if (isDisabled()) {
                throw new RuntimeException("Disabled component should not fire SearchRemoveEvent: " + getId());
            }
            if (isMultiValued()) {
                removeRow(context, ((SearchRemoveEvent) event).index);
            } else if (isMandatory()) {
                throw new RuntimeException("Single-valued mandatory component should not fire SearchRemoveEvent: " + getId());
            } else {
                ComponentUtil.getChildren(this).get(0).getChildren().remove(0);
                setValue(context, null);
                invokeSetterCallbackIfNeeded(context, null); // so that if needed, related components could be updated
            }
        } else if (event instanceof SearchAddEvent) {
            if (isDisabled() || !isMultiValued() || !isEditable()) {
                throw new RuntimeException("Disabled or single-valued or non-editable component should not fire SearchAddEvent: " + getId());
            }
            appendRow(context, null);
        } else {
            super.broadcast(event);
        }
    }

    protected void createPicker(FacesContext context) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getChildren();

        UIGenericPicker picker = (UIGenericPicker) context.getApplication().createComponent("org.alfresco.faces.GenericPicker");
        String id = (String) getAttributes().get(ID_KEY);
        FacesHelper.setupComponentId(context, picker, "picker_" + id);
        picker.setShowFilter(isAttributeTrue(SHOW_FILTER_KEY));
        if (picker.getShowFilter()) {
            ValueBinding pickerV = context.getApplication().createValueBinding((String) getAttributes().get(FILTERS_KEY));
            picker.setValueBinding("filters", pickerV);
        }
        picker.setShowSelectButton(isAttributeTrue(FILTERS_ALLOW_GROUP_SELECT_KEY));
        picker.setWidth(400);
        picker.setMultiSelect(isMultiSelect());
        String pickerCallback = (String) getAttributes().get(PICKER_CALLBACK_KEY);
        MethodBinding b = getFacesContext().getApplication().createMethodBinding(pickerCallback, GenericPickerTag.QUERYCALLBACK_CLASS_ARGS);
        picker.setQueryCallback(b);
        picker.addActionListener(new PickerFinishActionListener());

        Integer filterIndex = (Integer) getAttributes().get(FILTER_INDEX);
        if (filterIndex != null) {
            picker.setDefaultFilterIndex(filterIndex);
        } else {
            picker.setDefaultFilterIndex(UserContactGroupSearchBean.USERS_FILTER);
        }

        children.add(picker);
    }

    protected boolean isMultiSelect() {
        return isMultiValued();
    }

    protected void pickerFinish(UIGenericPicker picker, int index) {
        String[] results = picker.getSelectedResults();
        FacesContext context = FacesContext.getCurrentInstance();

        String preprocessCallback = getPreprocesCallback();
        if (StringUtils.isNotBlank(preprocessCallback)) {
            MethodBinding preprocessBind = getFacesContext().getApplication().createMethodBinding(preprocessCallback, new Class[] { int.class, String[].class });
            List<Pair<String, String>> groupedResults = null;
            Object preprocessed = preprocessBind.invoke(context, new Object[] { picker.getFilterIndex(), results });
            if (preprocessed instanceof List) {
                groupedResults = (List<Pair<String, String>>) preprocessed;
                String[] extractedResults = new String[groupedResults.size()];
                for (int i = 0; i < groupedResults.size(); i++) {
                    extractedResults[i] = groupedResults.get(i).getSecond();
                }
                results = extractedResults;
            } else {
                results = (String[]) preprocessed;
            }
        }

        if (results == null) {
            return;
        }

        if (isMultiValued()) {
            multiValuedPickerFinish(results, context, index);
        } else {
            if (results.length > 1) {
                throw new RuntimeException("Single-valued property does not support multiple values");
            }
            if (results.length == 1) {
                singleValuedPickerFinish(context, results[0]);
            }
        }
        getAttributes().remove(OPEN_DIALOG_KEY);
        picker.queueEvent(new UIGenericPicker.PickerEvent(picker, UIGenericPicker.ACTION_CLEAR, UserContactGroupSearchBean.USERS_FILTER, null, null));
    }

    /**
     * Multi-value picker will try to add the new value to the row where the search icon was clicked. When the row is filled, it continues until the end of the list to find an
     * empty row. When all rows are filled
     * 
     * @param results
     * @param context
     * @param index
     */
    public void multiValuedPickerFinish(String[] results, FacesContext context, int index) {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) getList(context);
        // collect new values
        boolean firstItem = true;
        for (String result : results) {
            if (!isAllowDuplicates() && list.contains(result)) {
                continue;
            }
            if (index == -1) {
                list.add(result);
            } else if (firstItem) {
                list.set(index++, result);
                firstItem = false;
            } else {
                list.add(index++, result);
            }
        }
        // clear old components
        clearChildren();
        // create components for new values
        for (int rowIndex = 0; rowIndex < list.size(); rowIndex++) {
            appendRowComponent(context, rowIndex);
        }
    }

    public void singleValuedPickerFinish(FacesContext context, String value) {
        clearChildren();
        appendRow(context, value);

        invokeSetterCallbackIfNeeded(context, value);
    }

    public void clearChildren() {
        List<UIComponent> children = ComponentUtil.getChildren(ComponentUtil.getChildren(this).get(0));
        if (!children.isEmpty()) {
            children.clear();
        }
    }

    protected void invokeSetterCallbackIfNeeded(FacesContext context, String value) {
        // Invoke setter callback if needed
        String setterCallback = getSetterCallback();
        if (StringUtils.isBlank(setterCallback)) {
            return;
        }
        // first argument is always String(result from picker)
        // second argument can be either Object(value from RichList)
        // or Node(of surrounding propertySheet, that is the last argument of method binding) if setterCallbackTakesNode()
        final List<Class<?>> paramsTypes = new ArrayList<Class<?>>(3);
        final List<Object> argValues = new ArrayList<Object>(3);
        paramsTypes.add(String.class);
        argValues.add(value);
        if (isChildOfUIRichList()) {
            Integer rowIndex = (Integer) getAttributes().get(Search.OPEN_DIALOG_KEY);
            Object rowObject = getRowObjectByIndex(rowIndex);
            paramsTypes.add(rowObject.getClass());
            argValues.add(rowObject);
        }

        Node node = null;
        if (setterCallbackTakesNode()) {
            final UIPropertySheet propSheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class);
            paramsTypes.add(Node.class);
            node = propSheet.getNode();
            argValues.add(node);
        }
        MethodBinding b = getFacesContext().getApplication()
                .createMethodBinding(setterCallback, paramsTypes.toArray(new Class[paramsTypes.size()]));
        b.invoke(context, argValues.toArray());
    }

    public boolean isChildOfUIRichList() {
        UIComponent comp = getParent().getParent();
        return comp instanceof UIRichList;
    }

    private Object getRowObjectByIndex(Integer index) {
        UIComponent comp = getParent().getParent();
        UIRichList list = (UIRichList) comp;
        return list.getDataModel().getRow(index);
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
        List<UIComponent> children = ComponentUtil.getChildren(this).get(0).getChildren();
        String id = (String) getAttributes().get(ID_KEY);
        boolean createTextarea = isEditable() && isTextarea();
        UIOutput component;
        if (createTextarea) {
            TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
            textAreaGenerator.setColumns(64);
            component = (UIOutput) textAreaGenerator.generate(context, id);
        } else {
            component = (UIOutput) context.getApplication().createComponent(isEditable() ? ComponentConstants.JAVAX_FACES_INPUT : ComponentConstants.JAVAX_FACES_OUTPUT);
        }
        FacesHelper.setupComponentId(context, component, "picker_" + id + "row_" + getNextCounterValue());
        ValueBinding vb = setValueBinding(context, component, rowIndex);
        String tooltipVB = ComponentUtil.getAttribute(this, ATTR_TOOLTIP_MB, String.class);
        if (StringUtils.isNotBlank(tooltipVB)) {
            Object value = vb.getValue(context);
            String tooltip = (String) context.getApplication().createMethodBinding(tooltipVB, new Class[] { Object.class }).invoke(context, new Object[] { value });
            if (StringUtils.isNotBlank(tooltip)) {
                ComponentUtil.putAttribute(component, "title", tooltip); // add tooltip
            }
        }
        ComponentUtil.createAndSetConverter(context, (String) getAttributes().get(CONVERTER_KEY), component);
        if (isDisabled()) {
            ComponentUtil.setReadonlyAttributeRecursively(component);
        } else if (isEditable()) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> attributes = component.getAttributes();
            attributes.put("onkeyup", "processButtonState();");
        }
        children.add(component);
    }

    protected void appendRow(FacesContext context, String value) {
        Class<?> dataType = (Class<?>) getAttributes().get(DATA_TYPE_KEY);
        Object convertedValue = DefaultTypeConverter.INSTANCE.convert(dataType, value);

        if (isMultiValued()) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) getList(context);
            if (isAllowDuplicates() || !list.contains(convertedValue)) {
                list.add(convertedValue);
                appendRowComponent(context, list.size() - 1);
            }
        } else {
            if (!isChildOfUIRichList()) { // only setter callback can be used with UIRichList
                setValue(context, convertedValue);
            }
            appendRowComponent(context, -1);
        }
    }

    protected void removeRow(FacesContext context, int removeIndex) {
        List<?> list = getList(context);
        list.remove(removeIndex);

        List<UIComponent> children = ComponentUtil.getChildren(this).get(0).getChildren();

        // remove a row from the middle
        children.remove(removeIndex);

        // iterate over all rows, starting from the removed spot, and correct their value-binding to match the correct list element
        for (int rowIndex = removeIndex; rowIndex < children.size(); rowIndex++) {
            UIComponent component = children.get(rowIndex);
            setValueBinding(context, component, rowIndex);
        }
    }

    protected ValueBinding setValueBinding(FacesContext context, UIComponent component, int rowIndex) {
        ValueBinding vb = createValueBinding(context, rowIndex);
        component.setValueBinding(VALUE_KEY, vb);
        return vb;
    }

    protected ValueBinding createValueBinding(FacesContext context, int rowIndex) {
        String list = getValueBinding(VALUE_KEY).getExpressionString();
        return context.getApplication().createValueBinding(list.substring(0, list.length() - 1) + (rowIndex >= 0 ? "[" + rowIndex + "]" : "") + "}");
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

    protected String getSetterCallback() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        return (String) attributes.get(SETTER_CALLBACK);
    }

    private boolean setterCallbackTakesNode() {
        return isAttributeTrue(Search.SETTER_CALLBACK_TAKES_NODE);
    }

    public String getPreprocesCallback() {
        return (String) ComponentUtil.getAttribute(this, PREPROCESS_CALLBACK);
    }

    private boolean isAllowDuplicates() {
        return isAttributeTrue(ALLOW_DUPLICATES_KEY);
    }

    protected boolean isDisabled() {
        return ComponentUtil.isComponentDisabledOrReadOnly(this);
    }

    public boolean isMultiValued() {
        return isAttributeTrue(DATA_MULTI_VALUED);
    }

    protected String getSearchLinkLabel() {
        return (String) ComponentUtil.getAttribute(this, SEARCH_LINK_LABEL);
    }

    protected String getSearchLinkTooltip() {
        return (String) ComponentUtil.getAttribute(this, SEARCH_LINK_TOOLTIP);
    }

    protected boolean isMandatory() {
        return isAttributeTrue("dataMandatory");
    }

    protected boolean isEmpty() {
        return isAttributeTrue("empty");
    }

    protected boolean isEditable() {
        return isAttributeTrue("editable");
    }

    protected boolean isTextarea() {
        return isAttributeTrue(TEXTAREA);
    }

    private boolean isAttributeTrue(String attributeName) {
        Boolean val = (Boolean) ComponentUtil.getAttribute(this, attributeName);
        return val != null && val;
    }

    protected boolean isRemoveLinkRendered() {
        return !isDisabled() && (isMultiValued() || isAttributeTrue(ALLOW_CLEAR_SINGLE_VALUED));
    }

    private int getNextCounterValue() {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        Integer counter = (Integer) attributes.get("counter");
        if (counter == null) {
            counter = 0;
        } else {
            counter = counter + 1;
        }
        attributes.put("counter", counter);
        return counter;
    }

    public static class PickerFinishActionListener implements ActionListener {

        @Override
        public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
            UIComponent parent = actionEvent.getComponent().getParent();
            if (parent instanceof Search) {
                FacesContext context = FacesContext.getCurrentInstance();
                Map params = context.getExternalContext().getRequestParameterMap();
                String indexStr = StringUtils.substringAfter((String) params.get(parent.getClientId(context) + "_action"), ";");
                int index = StringUtils.isNumeric(indexStr) && !indexStr.isEmpty() ? Integer.parseInt(indexStr) : -1;
                ((Search) parent).pickerFinish((UIGenericPicker) actionEvent.getComponent(), index);
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

    public static class SearchAddEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        public SearchAddEvent(UIComponent uiComponent) {
            super(uiComponent);
        }

    }

}
