package ee.webmedia.alfresco.common.propertysheet.search;

import static ee.webmedia.alfresco.common.propertysheet.search.UserSearchGenerator.EXTRA_INFO_TRANSFORMER;

import java.io.IOException;

import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;

/**
 * @author Keit Tehvan
 */
public class UserSearchRenderer extends SearchRenderer {
    public static final String USER_SEARCH_RENDERER_TYPE = UserSearchRenderer.class.getCanonicalName();

    @Override
    protected void renderExtraInfo(Search search, ResponseWriter out) throws IOException {
        Object transformer = search.getAttributes().get(EXTRA_INFO_TRANSFORMER);
        if (transformer == null || !(transformer instanceof SubstituteInfoTransformer)) {
            return;
        }
        String substInfo = (String) ((SubstituteInfoTransformer) transformer).tr(search);
        if (!StringUtils.isBlank(substInfo)) {
            out.write("<span class=\"fieldExtraInfo\">");
            System.out.println(substInfo);
            out.write(substInfo);
            out.write("</span>");
        }
    }
}
