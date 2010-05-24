package ee.webmedia.alfresco.common.propertysheet.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.BaseRenderer;

import ee.webmedia.alfresco.common.propertysheet.search.Search.SearchRemoveEvent;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Render {@link Search} component. Child component of type {@link HtmlPanelGroup} is rendered as HTML table. Child component of type {@link UIGenericPicker} is
 * rendered as modal popup dialog.
 * 
 * @author Alar Kvell
 */
public class SearchRenderer extends BaseRenderer {

    public static final String SEARCH_RENDERER_TYPE = SearchRenderer.class.getCanonicalName();

    protected static final String ACTION_SEPARATOR = ";";
    protected static final String REMOVE_ROW_ACTION = "removeRow";
    public static final String OPEN_DIALOG_ACTION = "openDialog";
    public static final String CLOSE_DIALOG_ACTION = "closeDialog";

    public static final String SEARCH_MSG = "search";
    public static final String DELETE_MSG = "delete";
    public static final String CLOSE_WINDOW_MSG = "close_window";

    @Override
    public void decode(FacesContext context, UIComponent component) {
        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        if (action.equals(REMOVE_ROW_ACTION)) {
            component.queueEvent(new SearchRemoveEvent(component, index));
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
        
        @SuppressWarnings("unchecked")
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
        out.write("\"/></div>");
    }

    private void renderMultiValued(FacesContext context, ResponseWriter out, Search search, HtmlPanelGroup list, UIGenericPicker picker) throws IOException {
        out.write("<table class=\"recipient inline\" cellpadding=\"0\" cellspacing=\"0\"><tbody>");

        @SuppressWarnings("unchecked")
        List<UIComponent> children = list.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }

            out.write("<tr><td>");
            setInputStyleClass(child, search);
            Utils.encodeRecursive(context, child);
            out.write("</td><td>");
            renderRemoveLink(context, out, search, i);
            out.write("</td></tr>");

        }
        out.write("</tbody></table>");

        renderPicker(context, out, search, picker);
    }

    private void renderSingleValued(FacesContext context, ResponseWriter out, Search search, HtmlPanelGroup list, UIGenericPicker picker) throws IOException {
        out.write("<table class=\"recipient inline\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr>");

        @SuppressWarnings("unchecked")
        List<UIComponent> children = list.getChildren();
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            if (!child.isRendered()) {
                continue;
            }

            out.write("<td>");
            setInputStyleClass(child, search);
            Utils.encodeRecursive(context, child);
            out.write("</td>");
            if(isRemoveLinkRendered(search)) {
                out.write("<td>");
                renderRemoveLink(context, out, search, i);
                out.write("</td>");
            }
        }
        out.write("<td>");
        renderPicker(context, out, search, picker);
        out.write("</td></tr></tbody></table>");
    }

    private void setInputStyleClass(UIComponent child, Search search) {
        @SuppressWarnings("unchecked")
        Map<String, Object> searchAttributes = search.getAttributes();
        if(child instanceof UIInput && searchAttributes.containsKey(Search.STYLE_CLASS_KEY)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> childAttributes = child.getAttributes();
            childAttributes.put(Search.STYLE_CLASS_KEY, searchAttributes.get(Search.STYLE_CLASS_KEY));
        }
    }
    
    /**
     * Before calling this method, verify that remove link is needed/allowed with SearchRenderer.isRemoveLinkRendered()
     */
    private void renderRemoveLink(FacesContext context, ResponseWriter out, Search search, int index) throws IOException {
        out.write("<a class=\"icon-link delete\" onclick=\"");
        out.write(ComponentUtil //
                .generateAjaxFormSubmit(context, search, getActionId(context, search), REMOVE_ROW_ACTION + ACTION_SEPARATOR + index));
        out.write("\" title=\"" + Application.getMessage(context, DELETE_MSG) + "\">");
        out.write("</a>");
    }

    /**
     * @param search
     */
    private boolean isRemoveLinkRendered(Search search) {
        // don't render removing link
        if (search.isDisabled()) {
            return false;
        }
        if (!search.isMultiValued()) {
            return false;
        }
        
        return true;
    }

    private void renderPicker(FacesContext context, ResponseWriter out, Search search, UIGenericPicker picker) throws IOException {
        if(search.isDisabled()) {
            return;
        }
        out.write("<a class=\"icon-link margin-left-4 search\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, search, getActionId(context, search), OPEN_DIALOG_ACTION + ACTION_SEPARATOR + getRowIndex(search)));
        out.write("return showModal('");
        out.write(getDialogId(context, search));
        out.write("');\" title=\"" + Application.getMessage(context, SEARCH_MSG) + "\">");
        // out.write(Application.getMessage(context, SEARCH_MSG));
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
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner\">");

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
