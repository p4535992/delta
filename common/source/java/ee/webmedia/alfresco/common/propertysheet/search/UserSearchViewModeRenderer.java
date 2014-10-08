package ee.webmedia.alfresco.common.propertysheet.search;

import static ee.webmedia.alfresco.common.propertysheet.search.UserSearchGenerator.EXTRA_INFO_TRANSFORMER;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> develop-5.1
public class UserSearchViewModeRenderer extends BaseRenderer {
    public static final String RENDERER_TYPE = UserSearchViewModeRenderer.class.getCanonicalName();

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        UIOutput output = (UIOutput) component;
        ResponseWriter out = context.getResponseWriter();
        out.write("<div class=\"inline\" id=\"");
        out.write(output.getClientId(context));
        out.write("\">");
        if (output.getValue() != null) {
            out.write(output.getValue().toString());
        }
        out.write("</div>");
        renderExtraInfo(component, out);
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        super.encodeChildren(context, component);
        // never used
    }

    public static void renderExtraInfo(UIComponent search, ResponseWriter out) throws IOException {
        Object transformer = search.getAttributes().get(EXTRA_INFO_TRANSFORMER);
        if (transformer == null || !(transformer instanceof SubstituteInfoTransformer)) {
            return;
        }
        String substInfo = ((SubstituteInfoTransformer) transformer).tr(search);
        if (!StringUtils.isBlank(substInfo)) {
            out.write("<span class=\"fieldExtraInfo\">");
            out.write(substInfo);
            out.write("</span>");
        }
    }
}
