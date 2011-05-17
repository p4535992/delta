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
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

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
    private static final String VALUE_MARKUP_START = "<span style=\"display: none;\">";
    private static final String VALUE_MARKUP_END = "</span>";

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchSuggest() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String callback = params.get(Search.PICKER_CALLBACK_KEY);
        String query = params.get("q");
        if (StringUtils.isBlank(callback) || StringUtils.isBlank(query)) {
            return;
        }

        MethodBinding b = context.getApplication().createMethodBinding("#{" + callback + "}", new Class[] { int.class, String.class });
        SelectItem[] result = (SelectItem[]) b.invoke(context, new Object[] { 0, query });

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
    public void setterCallback() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();

        String viewName = params.get(VIEW_NAME_PARAM);
        Assert.hasLength(viewName, "viewName was not found in request");
        String clientId = params.get("clientId");
        Assert.hasLength(clientId, "clientId was not found in request");
        String containerClientId = params.get("containerClientId");
        Assert.hasLength(containerClientId, "containerClientId was not found in request");
        String data = params.get("data");
        Assert.hasLength(data, "data was not found in request");

        UIViewRoot viewRoot = restoreViewRoot(context, viewName);
        UIComponent input = ComponentUtil.findChildComponentById(context, viewRoot, StringUtils.substringAfterLast(clientId, ":"), clientId);
        Assert.notNull(input, String.format("Component with id=%s was not found", clientId));

        String value = StringUtils.substringBetween(data, VALUE_MARKUP_START, VALUE_MARKUP_END);
        // Call out setter callback if it exists and then update web-client state
        UIComponent searchComponent = null;
        if ((searchComponent = ComponentUtil.getAncestorComponent(input, Search.class)) != null) {
            ((Search) searchComponent).singleValuedPickerFinish(context, value);
        } else if ((searchComponent = ComponentUtil.getAncestorComponent(input, MultiValueEditor.class)) != null) {
            int rowIndex = Integer.parseInt(StringUtils.substringAfterLast(clientId, "_"));
            ((MultiValueEditor) searchComponent).innerPickerFinish(0, rowIndex, new String[] { value }, context);
        } else {
            throw new RuntimeException("Missing parent component with search capabilities! (Search or MultiValueEditor)");
        }

        UIComponent ancestorComponent = ComponentUtil.findChildComponentById(context, viewRoot, 
                StringUtils.substringAfterLast(containerClientId, ":"), containerClientId);

        updateModelValues(context, viewRoot, ancestorComponent);

        // Phase 6: Render response
        Utils.encodeRecursive(context, ancestorComponent);

        String viewState = saveView(context, viewRoot);
        ResponseWriter out = context.getResponseWriter();
        out.write("VIEWSTATE:" + viewState);
    }
}
