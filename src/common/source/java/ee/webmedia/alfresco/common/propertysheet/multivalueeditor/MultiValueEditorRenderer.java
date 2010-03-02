package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor.MultiValueEditorEvent;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Render {@link MultiValueEditor} as HTML table. Direct children of {@link MultiValueEditor} must be {@link HtmlPanelGroup} components.
 * 
 * @author Alar Kvell
 */
// Extends BaseMultiValueRenderer, because only decode method implementation is needed from there.
public class MultiValueEditorRenderer extends BaseRenderer {

    public static final String MULTI_VALUE_EDITOR_RENDERER_TYPE = MultiValueEditorRenderer.class.getCanonicalName();

    @Override
    public void decode(FacesContext context, UIComponent component) {
        superDecode(context, component);

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

    /** Method copied from BaseMultiValueRenderer, but added one line to change even processing phase */
    private void superDecode(FacesContext context, UIComponent component) {
        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String fieldId = getHiddenFieldName(component);
        String value = requestMap.get(fieldId);

        int action = UIMultiValueEditor.ACTION_NONE;
        int removeIndex = -1;
        if (value != null && value.length() != 0) {
            // break up the action into it's parts
            int sepIdx = value.indexOf(UIMultiValueEditor.ACTION_SEPARATOR);
            if (sepIdx != -1) {
                action = Integer.parseInt(value.substring(0, sepIdx));
                removeIndex = Integer.parseInt(value.substring(sepIdx + 1));
            } else {
                action = Integer.parseInt(value);
            }
        }

        if (action != UIMultiValueEditor.ACTION_NONE) {
            MultiValueEditorEvent event = new MultiValueEditorEvent(component, action, removeIndex);
            event.setPhaseId(PhaseId.UPDATE_MODEL_VALUES); // addition to BaseMultiValueRenderer.decode()
            component.queueEvent(event);
        }
    }
    
    /**
     * We use a hidden field per picker instance on the page.
     * 
     * @return hidden field name
     */
    protected String getHiddenFieldName(UIComponent component) {
        return component.getClientId(FacesContext.getCurrentInstance());
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        final List<ComponentPropVO> propVOs = getPropVOs(component);
        ResponseWriter out = context.getResponseWriter();
        // class "recipient" should not be hard-coded, i guess
        out.write("<table class=\"recipient multiE cells" + propVOs.size() + "\" cellpadding=\"0\" cellspacing=\"0\">");
        out.write("<thead><tr>");
        for (ComponentPropVO propVO : propVOs) {
            out.write("<th>");
            out.writeText(propVO.getPropertyLabel(), null);
            out.write("</th>");
        }
        out.write("</tr></thead><tbody>");
    }

    private List<ComponentPropVO> getPropVOs(UIComponent component) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        @SuppressWarnings("unchecked")
        final List<ComponentPropVO> propsVOs = (List<ComponentPropVO>) attributes.get(PROP_GENERATOR_DESCRIPTORS);
        return propsVOs;
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.write("</tbody></table>");

        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        String addLabelId = (String) attributes.get(MultiValueEditor.ADD_LABEL_ID);
        if (StringUtils.isBlank(addLabelId)) {
            addLabelId = "add_contact";
        }

        if (!Utils.isComponentDisabledOrReadOnly(component)) { // don't render adding link when disabled
            String styleClass = (String) attributes.get(STYLE_CLASS);
            if (StringUtils.isBlank(styleClass)) {
                styleClass = "add-person";
            }
            out.write("<a class=\"icon-link " + styleClass + "\" onclick=\"");
            out.write(Utils.generateFormSubmit(context, component, component.getClientId(context), Integer.toString(UIMultiValueEditor.ACTION_ADD)));
            out.write("\">");
            out.write(Application.getMessage(context, addLabelId));
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
                            .generateFormSubmit(context, multiValueEditor, multiValueEditor.getClientId(context), Integer
                                    .toString(UIMultiValueEditor.ACTION_REMOVE) + ";" + rowIndex));
                    out.write("\" title=\"" + Application.getMessage(context, "delete") + "\">");
                    out.write("</a>");

                    if (hasPicker) {

                        out.write("<a class=\"icon-link search\" onclick=\"");
                        out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor),
                                SearchRenderer.OPEN_DIALOG_ACTION + ";" + rowIndex));
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
            out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor), SearchRenderer.OPEN_DIALOG_ACTION
                    + ";" + openDialog));
            out.write("showModal('");
            out.write(getDialogId(context, multiValueEditor));
            out.write("');");
            out.write("});</script>");
        }

    }

    protected String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    protected String getActionId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_action";
    }

}
