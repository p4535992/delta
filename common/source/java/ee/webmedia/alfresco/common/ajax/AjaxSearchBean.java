package ee.webmedia.alfresco.common.ajax;

import static ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator.predefinedFilters;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.FILTER_INDEX_SEPARATOR;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.USERS_FILTER;
import static ee.webmedia.alfresco.parameters.model.Parameters.MAX_MODAL_SEARCH_RESULT_ROWS;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.apache.commons.lang.StringUtils.substringBetween;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.model.SelectItem;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorRenderer;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Class that various search implementations can use to fetch data using AJAX.
 * 
 * @author Kaarel Jõgeva
 */
public class AjaxSearchBean extends AjaxBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AjaxSearchBean.class);
    private static final long serialVersionUID = 1L;

    private static final String DATA = "data";
    private static final String CONTAINER_CLIENT_ID = "containerClientId";
    private static final String CONTAINS = "contains";
    private static final String VALUE_MARKUP_START = "<span style=\"display: none;\">";
    private static final String VALUE_MARKUP_END = "</span>";

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchSuggest() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String callback = getParam(params, Search.PICKER_CALLBACK_KEY);
        String query = getParam(params, "q");
        if (StringUtils.isBlank(callback) || StringUtils.isBlank(query)) {
            return;
        }

        SelectItem[] result = getSelectItems(context, callback, query, getParam(params, Search.PICKER_CALLBACK_KEY_PARAM), null, false, true);
        if (result == null || result.length < 1) {
            return;
        }

        StringBuffer sb = new StringBuffer();
        for (SelectItem selectItem : result) {
            sb.append(selectItem.getLabel()).append(VALUE_MARKUP_START).append(selectItem.getValue()).append(VALUE_MARKUP_END).append("\n");
        }
        context.getResponseWriter().write(sb.toString());
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchPickerResults() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        SelectItem[] results = getSelectItems(context, getParam(params, Search.PICKER_CALLBACK_KEY), getParam(params, CONTAINS), getParam(params, "filterValue"),
                getParam(params, "hiddenValue", true), new Boolean(getParam(params, "filterByStructUnit")), false);

        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.write(UIGenericPicker.getResultSize(results) + "|");
        ComponentUtil.renderSelectItems(responseWriter, results);
    }

    @SuppressWarnings("unchecked")
    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchDimensionValues() throws IOException, ParseException {
        FacesContext context = FacesContext.getCurrentInstance();

        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String dimensionName = params.get(DimensionSelectorGenerator.ATTR_DIMENSION_NAME);
        String searchString = params.get("term");
        String predefinedFilterName = params.get(DimensionSelectorGenerator.ATTR_PREDEFINED_FILTER_NAME);
        String entryDateString = params.get("entryDate");
        Date entryDate = null;
        if (StringUtils.isNotBlank(entryDateString)) {
            DateFormat dateFormat = new SimpleDateFormat("dd.M.yyyy");
            dateFormat.setLenient(false);
            entryDate = dateFormat.parse(entryDateString);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching values for dimension='" + dimensionName + "', term='" + searchString + "', filter='" + predefinedFilterName + ", entryDate='" + entryDateString
                    + "'");
        }
        List<DimensionValue> result = new ArrayList<DimensionValue>();
        List<DimensionValue> dimensionValues = BeanHelper.getEInvoiceService()
                .searchDimensionValues(searchString, BeanHelper.getEInvoiceService().getDimension(Dimensions.get(dimensionName)), entryDate,
                        (searchString == null || searchString.length() < 3));
        if (predefinedFilters.containsKey(predefinedFilterName)) {
            Predicate filter = predefinedFilters.get(predefinedFilterName);
            result.addAll(CollectionUtils.select(dimensionValues, filter));
        } else {
            result.addAll(dimensionValues);
        }
        context.getResponseWriter().write(DimensionSelectorRenderer.getValuesAsJsArrayString(result));
    }

    /**
     * Handles an action outcome and invokes an action listener. Parameter for the action listener is assumed to be in DATA.
     */
    public void invokeActionListener() {
        FacesContext context = FacesContext.getCurrentInstance();
        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();

        // Fetch data
        String data = substringBeforeLast(substringBetween(getParam(context, DATA), VALUE_MARKUP_START, VALUE_MARKUP_END), FILTER_INDEX_SEPARATOR);
        String actionListener = getParam(params, "actionListener");

        // Invoke action listener
        context.getApplication().createMethodBinding("#{" + actionListener + "}", new Class[] { String.class }).invoke(context, new Object[] { data });
    }

    private SelectItem[] getSelectItems(FacesContext context, String callback, String contains, String filterValue, String hiddenValue, boolean filterByStructUnit,
            boolean includeFilterIndex) {
        int filter = UserContactGroupSearchBean.USERS_FILTER; // Default to this
        if (StringUtils.isNotBlank(filterValue) && !"undefined".equals(filterValue) && StringUtils.isNumeric(filterValue)) {
            filter = Integer.parseInt(filterValue);
        }

        MethodBinding b = context.getApplication().createMethodBinding("#{" + callback + "}", GenericPickerTag.QUERYCALLBACK_CLASS_ARGS);
        SelectItem[] result = (SelectItem[]) b.invoke(context,
                new Object[] { new PickerSearchParams(filter, contains, getParametersService().getLongParameter(MAX_MODAL_SEARCH_RESULT_ROWS).intValue(), hiddenValue,
                        filterByStructUnit, includeFilterIndex) });
        return result;
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void setterCallback() throws IOException {
        submit();
    }

    @Override
    protected void executeCallback(FacesContext context, String componentClientId, UIComponent component) {
        String value = StringUtils.substringBetween(getParam(context, DATA), VALUE_MARKUP_START, VALUE_MARKUP_END);
        int filterIndex = USERS_FILTER;
        if (value.lastIndexOf(FILTER_INDEX_SEPARATOR) > -1) {
            filterIndex = Integer.parseInt(StringUtils.substringAfterLast(value, FILTER_INDEX_SEPARATOR));
            value = StringUtils.substringBeforeLast(value, FILTER_INDEX_SEPARATOR);
        }

        // Call out setter callback if it exists and then update web-client state
        UIComponent searchComponent = null;
        if ((searchComponent = ComponentUtil.getAncestorComponent(component, Search.class)) != null) {
            Search search = (Search) searchComponent;
            if (!search.isMultiValued()) {
                search.singleValuedPickerFinish(context, value);
            } else {
                String[] results = new String[] { value };
                Pair<String[], String[]> preprocessedResults = MultiValueEditor.preprocessResults(context, search.getPreprocesCallback(), results, filterIndex);
                int rowIndex = Integer.parseInt(StringUtils.substringAfterLast(componentClientId, "_"));
                search.multiValuedPickerFinish(preprocessedResults.getFirst(), context, rowIndex);
            }
        } else if ((searchComponent = ComponentUtil.getAncestorComponent(component, MultiValueEditor.class)) != null) {
            int rowIndex = Integer.parseInt(StringUtils.substringAfterLast(componentClientId, "_"));
            ((MultiValueEditor) searchComponent).innerPickerFinish(filterIndex, rowIndex, new String[] { value }, context);
        } else {
            throw new RuntimeException("Missing parent component with search capabilities! (Search or MultiValueEditor)");
        }
    }

    @Override
    protected UIComponent getRenderedContainer(FacesContext context, UIViewRoot viewRoot) {
        String containerClientId = getParam(context, CONTAINER_CLIENT_ID);
        return ComponentUtil.findChildComponentById(context, viewRoot, containerClientId);
    }
}
