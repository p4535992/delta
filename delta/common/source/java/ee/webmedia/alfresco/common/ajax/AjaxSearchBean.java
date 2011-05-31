package ee.webmedia.alfresco.common.ajax;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.model.SelectItem;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Class that various search implementations can use to fetch data using AJAX.
 * 
 * @author Kaarel Jõgeva
 */
public class AjaxSearchBean extends AjaxBean {
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

        SelectItem[] result = getSelectItems(context, callback, query);
        if (result == null || result.length < 1) {
            return;
        }

        StringBuffer sb = new StringBuffer();
        for (SelectItem selectItem : result) {
            sb.append(selectItem.getLabel()).append(VALUE_MARKUP_START).append(selectItem.getValue()).append(VALUE_MARKUP_END).append("\n");
        }
        context.getResponseWriter().write(sb.toString());
    }

    private SelectItem[] getSelectItems(FacesContext context, String callback, String contains) {
        MethodBinding b = context.getApplication().createMethodBinding("#{" + callback + "}", new Class[] { int.class, String.class });
        SelectItem[] result = (SelectItem[]) b.invoke(context, new Object[] { 0, contains });
        return result;
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchPickerResults() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        SelectItem[] results = getSelectItems(context, getParam(params, Search.PICKER_CALLBACK_KEY), getParam(params, CONTAINS));

        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.write(UIGenericPicker.getResultSize(results) + "|");
        ComponentUtil.renderSelectItems(responseWriter, results);
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void setterCallback() throws IOException {
        submit();
    }

    @Override
    protected void executeCallback(FacesContext context, String componentClientId, UIComponent component) {
        String value = StringUtils.substringBetween(getParam(context, DATA), VALUE_MARKUP_START, VALUE_MARKUP_END);
        // Call out setter callback if it exists and then update web-client state
        UIComponent searchComponent = null;
        if ((searchComponent = ComponentUtil.getAncestorComponent(component, Search.class)) != null) {
            ((Search) searchComponent).singleValuedPickerFinish(context, value);
        } else if ((searchComponent = ComponentUtil.getAncestorComponent(component, MultiValueEditor.class)) != null) {
            int rowIndex = Integer.parseInt(StringUtils.substringAfterLast(componentClientId, "_"));
            ((MultiValueEditor) searchComponent).innerPickerFinish(0, rowIndex, new String[] { value }, context);
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
