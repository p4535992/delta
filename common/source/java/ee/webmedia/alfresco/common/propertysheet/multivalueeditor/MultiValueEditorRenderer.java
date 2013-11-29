package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;
import static ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor.NO_ADD_LINK_LABEL;
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
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor.MultiValueEditorEvent;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.help.web.HelpTextUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

/**
 * Render {@link MultiValueEditor} as HTML table. Direct children of {@link MultiValueEditor} must be {@link HtmlPanelGroup} components.
 * 
 * @author Alar Kvell
 */
// Extends BaseMultiValueRenderer, because only decode method implementation is needed from there.
public class MultiValueEditorRenderer extends BaseRenderer {

    public static final String MULTI_VALUE_EDITOR_RENDERER_TYPE = MultiValueEditorRenderer.class.getCanonicalName();
    public static final String GROUP_CONTROL_SEND_OUT = "sendOut";

    @Override
    public void decode(FacesContext context, UIComponent component) {
        superDecode(context, component);

        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String value = requestMap.get(getActionId(context, component));

        if (value == null || value.length() == 0) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        if (value.startsWith(SearchRenderer.OPEN_DIALOG_ACTION + ";")) {
            attributes.put(Search.OPEN_DIALOG_KEY, value.substring((SearchRenderer.OPEN_DIALOG_ACTION + ";").length()));
        } else if (value.equals(SearchRenderer.CLOSE_DIALOG_ACTION)) {
            attributes.remove(Search.OPEN_DIALOG_KEY);
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
            Utils.setRequestValidationDisabled(context);
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
        out.write("<div id=\"");
        out.write(((MultiValueEditor) component).getAjaxClientId(context));
        out.write("\"><table class=\"recipient multiE cells" + propVOs.size() + "\" cellpadding=\"0\" cellspacing=\"0\">");

        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        String showHeaders = (String) attributes.get(MultiValueEditor.SHOW_HEADERS);
        if (StringUtils.isBlank(showHeaders) || Boolean.parseBoolean(showHeaders)) {
            boolean disabledOrReadOnly = ComponentUtil.isComponentDisabledOrReadOnly(component);
            out.write("<thead><tr>");
            for (ComponentPropVO propVO : propVOs) {
                out.write("<th>");

                // Render mandatory marker if needed
                String mandatory = propVO.getCustomAttributes().get(ValidatingModalLayerComponent.ATTR_MANDATORY);
                if (!disabledOrReadOnly && Boolean.valueOf(mandatory)) {
                    Utils.encodeRecursive(context, ComponentUtil.createMandatoryMarker(context));
                }

                out.writeText(propVO.getPropertyLabel(), null);

                // Field help:
                String property = StringUtils.substringAfter(propVO.getPropertyName(), ":");
                if (HelpTextUtil.hasHelpText(context, HelpTextUtil.TYPE_FIELD, property)) {
                    Utils.encodeRecursive(context, HelpTextUtil.createHelpTextLink(context, HelpTextUtil.TYPE_FIELD, property));
                }
                out.write("</th>");
            }
            out.write("</tr></thead>");
        }
        out.write("<tbody>");
    }

    private List<ComponentPropVO> getPropVOs(UIComponent component) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        @SuppressWarnings("unchecked")
        final List<ComponentPropVO> propsVOs = (List<ComponentPropVO>) attributes.get(PROP_GENERATOR_DESCRIPTORS);
        return propsVOs;
    }

    private String getAddLinkId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_multivalue-add-link";
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.write("</tbody></table>");

        renderAddLink(context, component, out);

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

        out.write("</div>");
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
        boolean deleteEnabled = !ComponentUtil.isComponentDisabledOrReadOnly(multiValueEditor);
        String previousGroupingValue = null;
        boolean inGroup = false;
        String groupRowControls = generateGroupRowControls(context, multiValueEditor);
        List<UIComponent> children = multiValueEditor.getChildren();
        int renderedRowCount = ComponentUtil.getRenderedChildrenCount(multiValueEditor);
        for (UIComponent child : children) {
            if (!child.isRendered()) {
                continue;
            }

            if (child instanceof HtmlPanelGroup) {
                int renderedColumnCount = ComponentUtil.getRenderedChildrenCount(child);
                String groupByColumnValue = (String) child.getAttributes().get(MultiValueEditor.GROUP_BY_COLUMN_VALUE);
                if (StringUtils.isNotBlank(groupByColumnValue) && !groupByColumnValue.equals(previousGroupingValue)) {
                    if (previousGroupingValue != null) {
                        out.write("</tbody>");
                        inGroup = false;
                    }

                    generateGroupRow(out, context, (MultiValueEditor) multiValueEditor, groupByColumnValue, rowIndex, renderedColumnCount, deleteEnabled, groupRowControls);
                    out.write("<tbody class=\"hidden\">");
                    inGroup = true;
                    previousGroupingValue = groupByColumnValue;
                } else if (StringUtils.isBlank(groupByColumnValue) && previousGroupingValue != null) {
                    out.write("</tbody>");
                    inGroup = false;
                }

                out.write("<tr>");

                List<UIComponent> columns = child.getChildren();
                int columnCount = 0;
                for (UIComponent column : columns) {
                    if (!column.isRendered()) {
                        continue;
                    }
                    out.write("<td>");
                    if ((rowIndex == renderedRowCount - 1) && (columnCount == renderedColumnCount - 1) && ((MultiValueEditor) multiValueEditor).isAutomaticallyAddRows()) {
                        String addLinkId = getAddLinkId(context, multiValueEditor);
                        // component has to implement actual link clicking
                        ComponentUtil.putAttribute(column, MultiValueEditor.ATTR_CLICK_LINK_ID, addLinkId);
                    } else {
                        column.getAttributes().remove(MultiValueEditor.ATTR_CLICK_LINK_ID);
                    }
                    Utils.encodeRecursive(context, column);
                    if (hasSearchSuggest(multiValueEditor, column)) {
                        out.write(ComponentUtil.generateSuggestScript(context, column, (String) multiValueEditor.getAttributes().get(Search.PICKER_CALLBACK_KEY)));
                    }
                    out.write("</td>");
                    columnCount++;
                }

                out.write("<td>");
                if (!ComponentUtil.isComponentDisabledOrReadOnly(multiValueEditor) && (!((MultiValueEditor) multiValueEditor).isForcedMandatory_() || renderedRowCount > 1)) {

                    out.write("<a class=\"icon-link margin-left-4 delete\" onclick=\"");
                    out.write(ComponentUtil //
                            .generateAjaxFormSubmit(context, multiValueEditor, multiValueEditor.getClientId(context), Integer
                                    .toString(UIMultiValueEditor.ACTION_REMOVE) + ";" + rowIndex));
                    out.write("\" title=\"" + Application.getMessage(context, "delete") + "\">");
                    out.write("</a>");

                    if (hasPicker && !inGroup) {
                        out.write("<a class=\"icon-link search\" onclick=\"");
                        out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor),
                                SearchRenderer.OPEN_DIALOG_ACTION + ";" + rowIndex));
                        out.write("return showModal('");
                        out.write(getDialogId(context, multiValueEditor));
                        out.write("');\"");
                        out.write(" title=\"" + Application.getMessage(context, SearchRenderer.SEARCH_MSG) + "\">");
                        // out.write(Application.getMessage(context, SearchRenderer.SEARCH_MSG));
                        out.write("</a>");
                    }
                }

                out.write("</td></tr>");
                rowIndex++;
            }
        }
    }

    private String generateGroupRowControls(FacesContext context, UIComponent multiValueEditor) {
        String html = null;
        String rowControls = (String) multiValueEditor.getAttributes().get(MultiValueEditor.GROUP_ROW_CONTROLS);
        if (StringUtils.isBlank(rowControls)) {
            return html;
        }

        if (GROUP_CONTROL_SEND_OUT.equals(rowControls)) {
            ClassificatorService classificatorService = BeanHelper.getClassificatorService();
            List<ClassificatorValue> activeClassificatorValues = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName("sendMode"));

            StringBuilder s = new StringBuilder("<select class=\"changeSendOutMode width120\">");
            s.append("<option value=\"\">").append(MessageUtil.getMessage("select_default_label")).append("</option>");
            for (ClassificatorValue classificatorValue : activeClassificatorValues) {
                s.append("<option value=\"").append(classificatorValue.getValueName()).append("\">").append(classificatorValue.getValueName()).append("</option>");
            }
            s.append("</select>");
            html = s.toString();
        }

        return html;
    }

    private void generateGroupRow(ResponseWriter out, FacesContext context, MultiValueEditor multiValueEditor, String groupByColumnValue, int rowIndex, int columnCount,
            boolean deleteEnabled, String rowControlComponent) throws IOException {

        if (rowControlComponent != null) {
            columnCount -= 1;
        }
        if (!deleteEnabled) {
            columnCount += 1;
        }

        out.write("<tr><td");
        if (columnCount > 0) {
            out.write(" colspan=\"");
            out.write(Integer.toString(columnCount));
            out.write("\"");
        }
        out.write("><a href=\"#\" onclick=\"return false;\" class=\"icon-link toggle-tbody plus\"></a>");
        out.write(groupByColumnValue);
        out.write("</td>");

        if (rowControlComponent != null) {
            out.write("<td>");
            out.write(rowControlComponent);
            out.write("</td>");
        }

        if (deleteEnabled) {
            out.write("<td class=\"actions\"><a class=\"icon-link margin-left-4 delete\" onclick=\"");
            out.write(ComponentUtil.generateAjaxFormSubmit(context, multiValueEditor, multiValueEditor.getClientId(context), Integer.toString(MultiValueEditor.ACTION_REMOVE_GROUP)
                    + ";" + rowIndex));
            out.write("\" title=\"");
            out.write(Application.getMessage(context, "delete"));
            out.write("\"></a></td>");
        }

        out.write("</tr>");
    }

    private boolean hasSearchSuggest(UIComponent multiValueEditor, UIComponent column) {
        if (Boolean.TRUE.equals(multiValueEditor.getAttributes().get(Search.SEARCH_SUGGEST_DISABLED))) {
            return false;
        }
        String id = column.getId();
        return StringUtils.startsWith(id, FacesHelper.makeLegalId(DocumentCommonModel.PREFIX + DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName()))
                || StringUtils.startsWith(id,
                        FacesHelper.makeLegalId(DocumentCommonModel.PREFIX + DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName()))
                || StringUtils.startsWith(id,
                        FacesHelper.makeLegalId(DocumentSpecificModel.PREFIX + DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME.getLocalName()))
                || StringUtils.startsWith(id, FacesHelper.makeLegalId(DocumentDynamicModel.PREFIX + DocumentDynamicModel.Props.USER_NAME.getLocalName()))
                || StringUtils.startsWith(id, FacesHelper.makeLegalId(TaskSearchModel.PREFIX + DocumentDynamicModel.Props.OWNER_NAME.getLocalName()))
                || StringUtils.startsWith(id, FacesHelper.makeLegalId(CompoundWorkflowSearchModel.PREFIX + DocumentDynamicModel.Props.OWNER_NAME.getLocalName()));
    }

    private void renderAddLink(FacesContext context, UIComponent component, ResponseWriter out) throws IOException {
        @SuppressWarnings("unchecked")
        final Map<String, Object> attributes = component.getAttributes();
        String addLabelId = (String) attributes.get(MultiValueEditor.ADD_LABEL_ID);
        boolean noAddLinkLabel = attributes.containsKey(NO_ADD_LINK_LABEL) && Boolean.valueOf((String) attributes.get(MultiValueEditor.NO_ADD_LINK_LABEL));
        if (StringUtils.isBlank(addLabelId)) {
            addLabelId = "add_contact";
        }

        if (!ComponentUtil.isComponentDisabledOrReadOnly(component)) { // don't render adding link when disabled
            String styleClass = (String) attributes.get(STYLE_CLASS);
            if (StringUtils.isBlank(styleClass)) {
                styleClass = "add-person";
            }
            String addLabel = Application.getMessage(context, addLabelId);
            String titleAttr = "";
            if (noAddLinkLabel) {
                titleAttr = "title=\"" + addLabel + "\"";
            }
            out.write("<a id=\"" + getAddLinkId(context, component) + "\" class=\"icon-link " + styleClass + "\" " + titleAttr + " onclick=\"");
            // TODO: optimeerimise võimalus (vt ka AjaxSearchBean)
            // siin seatakse ajaxParentLevel=1 ainult selle pärast, et ajax'iga uut rida lisades renderdataks ka valideerimise skriptid,
            // mis praegu lisatakse propertySheet'ile, aga mitte komponendile endale.
            // Kui valideerimine teha nii ümber, et komponentide valideerimine delegeerida propertySheet'ide poolt komponentidele
            // ja komponendid renderdaksid ise(propertySheet'i asemel) oma valideerimise funktsioonid, siis võiks ajaxParentLevel'i muuta tagasi 0 peale.
            // Kui ajaxParentLevel=0, siis poleks vaja kogu propertysheet'i koos kõigi tema alamkomponentidega (sh alam propertySheet'idega) vaja uuesti renderdada!
            int ajaxParentLevel = 1;
            out.write(ComponentUtil.generateAjaxFormSubmit(context, component, component.getClientId(context)
                    , Integer.toString(UIMultiValueEditor.ACTION_ADD), null, ajaxParentLevel));
            out.write("\">");
            if (!noAddLinkLabel) {
                out.write(Application.getMessage(context, addLabelId));
            }
            out.write("</a>");
        }
    }

    protected void renderPicker(FacesContext context, ResponseWriter out, UIComponent multiValueEditor, UIGenericPicker picker) throws IOException {
        String openDialog = (String) multiValueEditor.getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (openDialog != null) {
            out.write("<div id=\"overlay\" style=\"display: block;\"></div>");
        }

        out.write("<div id=\"");
        out.write(getDialogId(context, multiValueEditor));
        out.write("\" class=\"modalpopup modalwrap\"");
        if (openDialog != null) {
            out.write(" style=\"display: block;\"");
        }
        out.write("><div class=\"modalpopup-header clear\"><h1>");

        String searchMessage = (String) multiValueEditor.getAttributes().get(Search.DIALOG_TITLE_ID_KEY);
        out.write(Application.getMessage(context, searchMessage != null ? searchMessage : SearchRenderer.SEARCH_MSG));

        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, multiValueEditor, getActionId(context, multiValueEditor), SearchRenderer.CLOSE_DIALOG_ACTION));
        out.write("hideModal();");
        out.write(ComponentUtil.generateAjaxFormSubmit(context, picker, picker.getClientId(context), "1" /* ACTION_CLEAR */));
        out.write("\">");
        out.write(Application.getMessage(context, SearchRenderer.CLOSE_WINDOW_MSG));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner\">");

        Map<String, Object> attributes = picker.getAttributes();
        attributes.put(Search.PICKER_CALLBACK_KEY, multiValueEditor.getAttributes().get(Search.PICKER_CALLBACK_KEY));
        Utils.encodeRecursive(context, picker);

        out.write("</div></div></div>");

        if (openDialog != null) { // Used when full submit is done, but AJAX deprecates it
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
