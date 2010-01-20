package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.renderer.BaseMultiValueRenderer;

import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Render {@link MultiValueEditor} as HTML table. Direct children of {@link MultiValueEditor} must be {@link HtmlPanelGroup} components.
 * 
 * @author Alar Kvell
 */
// Extends BaseMultiValueRenderer, because only decode method implementation is needed from there.
public class MultiValueEditorRenderer extends BaseMultiValueRenderer {

    public static final String MULTI_VALUE_EDITOR_RENDERER_TYPE = MultiValueEditorRenderer.class.getCanonicalName();

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        
        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String value = requestMap.get(getActionId(context, component));

        if (value == null || value.length() == 0) {
            return;
        }

        if (value.startsWith(SearchRenderer.OPEN_DIALOG_ACTION + ";")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put(Search.OPEN_DIALOG_KEY, value.substring((SearchRenderer.OPEN_DIALOG_ACTION + ";").length()));
        } else {
            throw new RuntimeException("Unknown action: " + value);
        }
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> propTitles = (List<String>) component.getAttributes().get("propTitles");
        ResponseWriter out = context.getResponseWriter();
        out.write("<table class=\"recipient cells"+ propTitles.size() +"\" cellpadding=\"0\" cellspacing=\"0\">");
        out.write("<thead><tr>");
        for (String propTitle : propTitles) {
            out.write("<th>");
            out.writeText(propTitle, null);
            out.write("</th>");
        }
        out.write("</tr></thead><tbody>");
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.write("</tbody></table>");

        if (!Utils.isComponentDisabledOrReadOnly(component)) { // don't render adding link when disabled
            out.write("<a class=\"icon-link add-person\" onclick=\"");
            out.write(Utils.generateFormSubmit(context, component, component.getClientId(context), Integer.toString(UIMultiValueEditor.ACTION_ADD)));
            out.write("\">");
            out.write(Application.getMessage(context, "add_contact"));
            out.write("</a>");
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }
            if (child instanceof UIGenericPicker) {
                renderPicker(context, out, component, (UIGenericPicker) child);
            }
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent multiValueEditor) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        boolean hasPicker = ((MultiValueEditor) multiValueEditor).getPickerCallback() != null;
        int rowIndex = 0;

        @SuppressWarnings("unchecked")
        List<UIComponent> children = multiValueEditor.getChildren();
        for (UIComponent child : children) {
            if (!child.isRendered()) {
                continue;
            }

            if (child instanceof HtmlPanelGroup) {
                out.write("<tr>");

                @SuppressWarnings("unchecked")
                List<UIComponent> columns = child.getChildren();
                for (UIComponent column : columns) {
                    if (!column.isRendered()) {
                        continue;
                    }
                    out.write("<td>");
                    Utils.encodeRecursive(context, column);
                    out.write("</td>");
                }

                out.write("<td>");
                if (!Utils.isComponentDisabledOrReadOnly(multiValueEditor)) { // don't render removing link
                    
                    out.write("<a class=\"icon-link margin-left-4 delete\" onclick=\"");
                    out.write(Utils //
                            .generateFormSubmit(context, multiValueEditor, multiValueEditor.getClientId(context), Integer.toString(UIMultiValueEditor.ACTION_REMOVE) + ";" + rowIndex));
                    out.write("\" title=\""+Application.getMessage(context, "delete")+"\">");
                    out.write("</a>");

                    if (hasPicker) {

                        out.write("<a class=\"icon-link search\" onclick=\"");
                        out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor), SearchRenderer.OPEN_DIALOG_ACTION + ";" + rowIndex));
                        out.write("return showModal('");
                        out.write(getDialogId(context, multiValueEditor));
                        out.write("');\">");
                        out.write(Application.getMessage(context, SearchRenderer.SEARCH_MSG));
                        out.write("</a>");

                    }
                }

                out.write("</td></tr>");
                rowIndex++;
            }
        }
    }

    protected void renderPicker(FacesContext context, ResponseWriter out, UIComponent multiValueEditor, UIGenericPicker picker) throws IOException {
        out.write("<div id=\"");
        out.write(getDialogId(context, multiValueEditor));
        out.write("\" class=\"modalpopup modalwrap\">");
        out.write("<div class=\"modalpopup-header clear\"><h1>");

        String searchMessage = (String) multiValueEditor.getAttributes().get(Search.DIALOG_TITLE_ID_KEY);
        out.write(Application.getMessage(context, searchMessage != null ? searchMessage : SearchRenderer.SEARCH_MSG));

        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor), ""));
        out.write(Utils.generateFormSubmit(context, picker, picker.getClientId(context), "1" /* ACTION_CLEAR */));
        out.write("\">");
        out.write(Application.getMessage(context, SearchRenderer.CLOSE_WINDOW_MSG));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner\">");

        Utils.encodeRecursive(context, picker);

        out.write("</div></div></div>");

        String openDialog = (String) multiValueEditor.getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (openDialog != null) {
            multiValueEditor.getAttributes().remove(Search.OPEN_DIALOG_KEY);
            out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
            out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor), SearchRenderer.OPEN_DIALOG_ACTION + ";" + openDialog));
            out.write("showModal('");
            out.write(getDialogId(context, multiValueEditor));
            out.write("');");
            out.write("});</script>");
        }

    }

    @Override
    protected void renderPostWrappedComponent(FacesContext context, ResponseWriter out, UIMultiValueEditor editor) throws IOException {
        // Do nothing
    }

    @Override
    protected void renderPreWrappedComponent(FacesContext context, ResponseWriter out, UIMultiValueEditor editor) throws IOException {
        // Do nothing
    }

    protected String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    protected String getActionId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_action";
    }

}
