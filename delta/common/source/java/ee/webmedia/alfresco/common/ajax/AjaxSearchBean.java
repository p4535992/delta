package ee.webmedia.alfresco.common.ajax;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.model.SelectItem;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.Search;

/**
 * Class that various search implementations can use to fetch data using AJAX.
 * 
 * @author Kaarel Jõgeva
 *
 */
public class AjaxSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void searchSuggest() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance(); 
        
        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String callback = params.get(Search.PICKER_CALLBACK_KEY);
        String query = params.get("q");
        if(StringUtils.isBlank(callback) || StringUtils.isBlank(query)) {
            return;
        }
        
        MethodBinding b = context.getApplication().createMethodBinding("#{" + callback + "}", new Class[] { int.class, String.class });
        SelectItem[] result = (SelectItem[]) b.invoke(context, new Object[] {0, query});
        
        if(result == null || result.length < 1) {
            return;
        }
        
        StringBuffer sb = new StringBuffer();
        for (SelectItem selectItem : result) {
            sb.append(selectItem.getLabel()).append("\n");
        }
        context.getResponseWriter().write(sb.toString());
    }

}
