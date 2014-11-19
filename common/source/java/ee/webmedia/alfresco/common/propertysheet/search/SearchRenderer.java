package ee.webmedia.alfresco.common.propertysheet.search;

import static ee.webmedia.alfresco.utils.MessageUtil.getMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.search.Search.SearchAddEvent;
import ee.webmedia.alfresco.common.propertysheet.search.Search.SearchRemoveEvent;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Render {@link Search} component. Child component of type {@link HtmlPanelGroup} is rendered as HTML table. Child component of type {@link UIGenericPicker} is
 * rendered as modal popup dialog.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class SearchRenderer extends BaseRenderer {

    public static final String SEARCH_RENDERER_TYPE = SearchRenderer.class.getCanonicalName();

    protected static final String ACTION_SEPARATOR = ";";
    protected static final String REMOVE_ROW_ACTION = "removeRow";
    protected static final String ADD_ROW_ACTION = "addRow";
    public static final String OPEN_DIALOG_ACTION = "openDialog";
    public static final String CLOSE_DIALOG_ACTION = "closeDialog";

    public static final String SEARCH_MSG = "search";
    public static final String DELETE_MSG = "delete";
    public static final String CLOSE_WINDOW_MSG = "close_window";

    @Override
    public void decode(FacesContext context, UIComponent component) {
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String value = requestMap.get(getActionId(context, component));

        String action = null;
        int index = 0;
        if (value == null || value.length() == 0) {
            return;
        }

        // break up the action into it's parts
        int sepIdx = value.indexOf(ACTION_SEPARATOR);
        if (sepIdx != -1) {
            action = value.substring(0, sepIdx);
            index = Integer.parseInt(value.substring(sepIdx + 1));
        } else {
            action = value;
        }

        Map<String, Object> attributes = component.getAttributes();
        if (action.equals(REMOVE_ROW_ACTION)) {
            component.queueEvent(new SearchRemoveEvent(component, index));
        } else if (action.equals(ADD_ROW_ACTION)) {
            component.queueEvent(new SearchAddEvent(component));
        } else if (action.equals(OPEN_DIALOG_ACTION)) {
            attributes.put(Search.OPEN_DIALOG_KEY, index);
            Utils.setRequestValidationDisabled(context);
        } else if (action.equals(CLOSE_DIALOG_ACTION)) {
            attributes.remove(Search.OPEN_DIALOG_KEY);
            Utils.setRequestValidationDisabled(context);
        } else {
            throw new RuntimeException("Unknown action: " + action);
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
<<<<<<< HEAD
=======
        if (Boolean.TRUE.equals(component.getAttributes().get(Search.RENDER_PLAIN))) {
            return;
        }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        ResponseWriter out = context.getResponseWriter();
        out.write("<div class=\"inline\" id=\"");
        out.write(((Search) component).getAjaxClientId(context));
        out.write("\">");
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        Search search = (Search) component;

        HtmlPanelGroup list = null;
        UIGenericPicker picker = null;

        List<UIComponent> children = component.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }
            if (child instanceof HtmlPanelGroup) {
                if (list != null) {
                    throw new RuntimeException("Must have exactly one HtmlPanelGroup child component");
                }
                list = (HtmlPanelGroup) child;
            } else if (child instanceof UIGenericPicker) {
                if (picker != null) {
                    throw new RuntimeException("Must have exactly one UIGenericPicker child component");
                }
                picker = (UIGenericPicker) child;
            } else {
                throw new RuntimeException("UIComponent not supported: " + component.getClass().getCanonicalName());
            }
        }
        if ((list == null || picker == null) && (!search.isDisabled())) {
            throw new RuntimeException("Child UIComponent is missing for " + component.getClass().getSimpleName() //
                    + " component with id '" + component.getId() + "'. list=" + list + "; picker=" + picker);
        }

        if (search.isMultiValued()) {
            renderMultiValued(context, out, search, list, picker);
        } else {
            renderSingleValued(context, out, search, list, picker);
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        Search search = (Search) component;

        // Mandatory validation
        out.write("<input id=\"");
        out.write(component.getClientId(context));
        out.write("\" type=\"hidden\" value=\"");
        if (!search.isEmpty()) {
            out.write("notEmpty");
        }
<<<<<<< HEAD
        out.write("\"/></div>");
=======
        out.write("\"/>");
        if (!search.isRenderPlain()) {
            out.write("</div>");
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    private void renderMultiValued(FacesContext context, ResponseWriter out, Search search, HtmlPanelGroup list, UIGenericPicker picker) throws IOException {
        out.write("<table class=\"recipient");
        if (!search.isEditable()) {
            out.write(" inline");
        }
        out.write("\" cellpadding=\"0\" cellspacing=\"0\"><tbody>");

        List<UIComponent> children = list.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }

            out.write("<tr><td>");
            setInputStyleClass(child, search);

            int rowIndex = getRowIndex(search);
            if (rowIndex < 0) {
                rowIndex = i;
            }
            child.setId(StringUtils.substringBeforeLast(child.getId(), "_") + "_" + rowIndex); // Replace random counter for multiple valued autocomplete

            Utils.encodeRecursive(context, child);
            renderExtraInfo(search, out);
            if (hasSearchSuggest(search)) {
                out.write(ComponentUtil.generateSuggestScript(context, child, (String) search.getAttributes().get(Search.PICKER_CALLBACK_KEY)));
            }
            out.write("</td><td>");
<<<<<<< HEAD
            boolean editable = search.isEditable();
            if (editable) {
=======
            if (search.isEditable()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                renderPicker(context, out, search, picker, i);
            }
            if (isRemoveLinkRendered(search)) {
                renderRemoveLink(context, out, search, i);
            }
<<<<<<< HEAD
            if (!editable && i == children.size() - 1) {
                renderPicker(context, out, search, picker, -1);
            }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            out.write("</td></tr>");
        }
        out.write("</tbody></table>");

<<<<<<< HEAD
        if (children.isEmpty()) {
=======
        if (children.isEmpty() || !search.isEditable()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            renderPicker(context, out, search, picker, -1);
        }
        renderAddLink(context, search, out);
    }

    private void renderSingleValued(FacesContext context, ResponseWriter out, Search search, HtmlPanelGroup list, UIGenericPicker picker) throws IOException {
<<<<<<< HEAD
        out.write("<table class=\"recipient inline\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr>");
=======
        boolean renderPlain = search.isRenderPlain();
        if (!renderPlain) {
            out.write("<table class=\"recipient inline\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr>");
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        List<UIComponent> children = list.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }

<<<<<<< HEAD
            out.write("<td>");
=======
            if (!renderPlain) {
                out.write("<td>");
            }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            setInputStyleClass(child, search);
            Utils.encodeRecursive(context, child);
            if (hasSearchSuggest(search)) {
                out.write(ComponentUtil.generateSuggestScript(context, child, (String) search.getAttributes().get(Search.PICKER_CALLBACK_KEY)));
            }
            renderExtraInfo(search, out);
<<<<<<< HEAD
            out.write("</td>");
            UIOutput ch = (UIOutput) child;
            Object val = ch.getValue();
            if (isRemoveLinkRendered(search) && val != null) {
                out.write("<td>");
                renderRemoveLink(context, out, search, i);
                out.write("</td>");
            }
        }
        out.write("<td>");
        renderPicker(context, out, search, picker, -1);
        out.write("</td></tr></tbody></table>");
=======
            if (!renderPlain) {
                out.write("</td>");
            }
            UIOutput ch = (UIOutput) child;
            Object val = ch.getValue();
            if (isRemoveLinkRendered(search) && val != null) {
                if (!renderPlain) {
                    out.write("<td>");
                }
                renderRemoveLink(context, out, search, i);
                if (!renderPlain) {
                    out.write("</td>");
                }
            }
        }
        if (!renderPlain) {
            out.write("<td>");
        }
        renderPicker(context, out, search, picker, -1);
        if (!renderPlain) {
            out.write("</td></tr></tbody></table>");
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    private boolean hasSearchSuggest(Search search) {
        return !Boolean.TRUE.equals(search.getAttributes().get(Search.SEARCH_SUGGEST_DISABLED));
    }

    @SuppressWarnings("unused")
    protected void renderExtraInfo(Search search, ResponseWriter out) throws IOException {
        // UserSearchRenderer overrides this
    }

    private void setInputStyleClass(UIComponent child, Search search) {
        Map<String, Object> searchAttributes = search.getAttributes();
        if (child instanceof UIInput && searchAttributes.containsKey(Search.STYLE_CLASS_KEY)) {
            Map<String, Object> childAttributes = child.getAttributes();
            childAttributes.put(Search.STYLE_CLASS_KEY, searchAttributes.get(Search.STYLE_CLASS_KEY));
        }
    }

    /**
     * Before calling this method, verify that remove link is needed/allowed with SearchRenderer.isRemoveLinkRendered()
     */
    private void renderRemoveLink(FacesContext context, ResponseWriter out, Search search, int index) throws IOException {
        out.write("<a class=\"icon-link delete\" onclick=\"");
        Integer ajaxParentLevel = (Integer) search.getAttributes().get(Search.AJAX_PARENT_LEVEL_KEY);
        out.write(ComponentUtil //
                .generateAjaxFormSubmit(context, search, getActionId(context, search), REMOVE_ROW_ACTION + ACTION_SEPARATOR + index, ajaxParentLevel));
        out.write("\" title=\"" + Application.getMessage(context, DELETE_MSG) + "\">");
        out.write("</a>");
    }

    /**
     * @param search
     */
    private boolean isRemoveLinkRendered(Search search) {
        return search.isRemoveLinkRendered();
    }

    private void renderAddLink(FacesContext context, Search search, ResponseWriter out) throws IOException {
        if (!search.isEditable()) {
            return;
        }

        final Map<String, Object> attributes = search.getAttributes();
        String addLabelId = (String) attributes.get(MultiValueEditor.ADD_LABEL_ID);
        if (StringUtils.isBlank(addLabelId)) {
            addLabelId = "add_contact";
        }

        if (!ComponentUtil.isComponentDisabledOrReadOnly(search)) { // don't render adding link when disabled
            out.write("<a class=\"icon-link add-person\" onclick=\"");
            // TODO: optimeerimise võimalus (vt ka AjaxSearchBean)
            // siin seatakse ajaxParentLevel=1 ainult selle pärast, et ajax'iga uut rida lisades renderdataks ka valideerimise skriptid,
            // mis praegu lisatakse propertySheet'ile, aga mitte komponendile endale.
            // Kui valideerimine teha nii ümber, et komponentide valideerimine delegeerida propertySheet'ide poolt komponentidele
            // ja komponendid renderdaksid ise(propertySheet'i asemel) oma valideerimise funktsioonid, siis võiks ajaxParentLevel'i muuta tagasi 0 peale.
            // Kui ajaxParentLevel=0, siis poleks vaja kogu propertysheet'i koos kõigi tema alamkomponentidega (sh alam propertySheet'idega) vaja uuesti renderdada!
            int ajaxParentLevel = 1;
            out.write(ComponentUtil.generateAjaxFormSubmit(context, search, getActionId(context, search), ADD_ROW_ACTION, null, ajaxParentLevel));
            out.write("\">");
            out.write(Application.getMessage(context, addLabelId));
            out.write("</a>");
        }
    }

    private void renderPicker(FacesContext context, ResponseWriter out, Search search, UIGenericPicker picker, int index) throws IOException {
        if (search.isDisabled()) {
            return;
        }
        int rowIndex = getRowIndex(search);
        if (rowIndex < 0) {
            rowIndex = index;
        }

        out.write("<a class=\"icon-link margin-left-4 search\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, search, getActionId(context, search), OPEN_DIALOG_ACTION + ACTION_SEPARATOR + rowIndex));
        out.write("return showModal('");
        out.write(getDialogId(context, search));
        String toolTip = search.getSearchLinkTooltip();
        if (toolTip == null) {
            toolTip = SEARCH_MSG;
        }
        out.write("');\" title=\"" + getMessage(toolTip) + "\">");
        // out.write(Application.getMessage(context, SEARCH_MSG));
        String searchLinkLabel = search.getSearchLinkLabel();
        if (searchLinkLabel != null) {
            out.write(getMessage(searchLinkLabel));
        }
        out.write("</a>");

        Integer openDialog = (Integer) search.getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (openDialog != null) {
            out.write("<div id=\"overlay\" style=\"display: block;\"></div>");
        }

        out.write("<div id=\"");
        out.write(getDialogId(context, search));
        out.write("\" class=\"modalpopup modalwrap\"");
        if (openDialog != null) {
            out.write(" style=\"display: block;\"");
        }
        out.write("><div class=\"modalpopup-header clear\"><h1>");

        String searchMessage = (String) search.getAttributes().get(Search.DIALOG_TITLE_ID_KEY);
        out.write(Application.getMessage(context, searchMessage != null ? searchMessage : SEARCH_MSG));

        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, search, getActionId(context, search), CLOSE_DIALOG_ACTION));
        out.write("hideModal();");
        out.write(ComponentUtil.generateAjaxFormSubmit(context, picker, picker.getClientId(context), "1" /* ACTION_CLEAR */));
        out.write("\">");
        out.write(Application.getMessage(context, CLOSE_WINDOW_MSG));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner");
        Map<String, Object> searchAttributes = search.getAttributes();
        if (searchAttributes.containsKey(Search.STYLE_CLASS_KEY)) {
            out.write(" ");
            out.write((String) searchAttributes.get(Search.STYLE_CLASS_KEY));
        }
        out.write("\">");

        Map<String, Object> attributes = picker.getAttributes();
        attributes.put(Search.PICKER_CALLBACK_KEY, search.getAttributes().get(Search.PICKER_CALLBACK_KEY));
        Utils.encodeRecursive(context, picker);

        out.write("</div></div></div>");

        if (openDialog != null) { // Used when full submit is done, but AJAX deprecates it
            search.getAttributes().remove(Search.OPEN_DIALOG_KEY);
            out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
            out.write(ComponentUtil.generateFieldSetter(context, search, getActionId(context, search), OPEN_DIALOG_ACTION + ACTION_SEPARATOR + openDialog));
            out.write("showModal('");
            out.write(getDialogId(context, search));
            out.write("');");
            out.write("});</script>");
        }
    }

    private static int getRowIndex(Search search) {
        UIComponent comp = search.getParent().getParent();
        if (comp instanceof UIRichList) {
            UIRichList list = (UIRichList) comp;
            return list.getRowIndex();
        }
        return -1;
    }

    private String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    private String getActionId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_action";
    }

}
