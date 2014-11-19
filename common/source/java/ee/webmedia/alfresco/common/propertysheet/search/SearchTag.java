package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;

import org.apache.commons.lang.StringUtils;

/**
 * JSF tag for Search.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class SearchTag extends UIComponentTag {
    private String value;
    private Boolean dataMultiValued;
    private Boolean dataMandatory;
    private String pickerCallback;
    private String setterCallback;
    private String dialogTitleId;
    private Boolean editable;
<<<<<<< HEAD
    private Boolean textarea;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private String readonly;
    private Boolean showFilter;
    private String filters;
    private Integer ajaxParentLevel;
    private String searchLinkLabel;
    private String searchLinkTooltip;
    private String converter;
<<<<<<< HEAD
=======
    private Boolean renderPlain;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public String getComponentType() {
        return Search.SEARCH_FAMILY;
    }

    @Override
    public String getRendererType() {
        return SearchRenderer.SEARCH_RENDERER_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        FacesContext context = FacesContext.getCurrentInstance();
        Application app = context.getApplication();

        Map<String, Object> attributes = component.getAttributes();
        if (StringUtils.isNotEmpty(value)) {
            ValueBinding vb = FacesContext.getCurrentInstance().getApplication().createValueBinding(value);
            component.setValueBinding(Search.VALUE_KEY, vb);
        }
        attributes.put(Search.DATA_TYPE_KEY, String.class);
        attributes.put(Search.DATA_MULTI_VALUED, dataMultiValued != null ? dataMultiValued : Boolean.FALSE);
        if (searchLinkLabel != null) {
            attributes.put(Search.SEARCH_LINK_LABEL, searchLinkLabel);
        }
        if (searchLinkTooltip != null) {
            attributes.put(Search.SEARCH_LINK_TOOLTIP, searchLinkTooltip);
        }
        attributes.put("dataMandatory", dataMandatory != null ? dataMandatory : Boolean.TRUE);
        attributes.put(Search.PICKER_CALLBACK_KEY, pickerCallback);
        if (setterCallback != null) {
            attributes.put("setterCallback", setterCallback);
        }
        if (ajaxParentLevel != null) {
            attributes.put(Search.AJAX_PARENT_LEVEL_KEY, ajaxParentLevel);
        }
        if (dialogTitleId != null) {
            attributes.put(Search.DIALOG_TITLE_ID_KEY, dialogTitleId);
        }
        attributes.put("editable", editable != null ? editable : Boolean.FALSE);
<<<<<<< HEAD
        attributes.put("textarea", textarea != null ? textarea : Boolean.FALSE);
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        if (readonly != null) {
            if (isValueReference(readonly)) {

                component.setValueBinding("readonly", app.createValueBinding(readonly));
            } else {
                attributes.put("readonly", readonly);
            }

        }
        if (converter != null) {
            attributes.put(Search.CONVERTER_KEY, converter);
        }
<<<<<<< HEAD
        if (showFilter != null) {
            attributes.put(Search.SHOW_FILTER_KEY, showFilter);
        }
        if (filters != null) {
            attributes.put(Search.FILTERS_KEY, filters);
=======
        if (renderPlain != null) {
            attributes.put(Search.RENDER_PLAIN, renderPlain);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    @Override
    public void release() {
        super.release();
        value = null;
        dataMultiValued = null;
        dataMandatory = null;
        pickerCallback = null;
        setterCallback = null;
        dialogTitleId = null;
        editable = null;
        readonly = null;
        ajaxParentLevel = null;
        converter = null;
<<<<<<< HEAD
=======
        renderPlain = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDataMultiValued(Boolean dataMultiValued) {
        this.dataMultiValued = dataMultiValued;
    }

    public void setDataMandatory(Boolean dataMandatory) {
        this.dataMandatory = dataMandatory;
    }

    public void setPickerCallback(String pickerCallback) {
        this.pickerCallback = pickerCallback;
    }

    public void setSetterCallback(String setterCallback) {
        this.setterCallback = setterCallback;
    }

    public void setDialogTitleId(String dialogTitleId) {
        this.dialogTitleId = dialogTitleId;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public void setReadonly(String readonly) {
        this.readonly = readonly;
    }

    public void setShowFilter(Boolean showFilter) {
        this.showFilter = showFilter;
    }

    public Boolean getShowFilter() {
        return showFilter;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public String getFilters() {
        return filters;
    }

    public void setAjaxParentLevel(Integer ajaxParentLevel) {
        this.ajaxParentLevel = ajaxParentLevel;
    }

    public Integer getAjaxParentLevel() {
        return ajaxParentLevel;
    }

    public void setSearchLinkLabel(String searchLinkLabel) {
        this.searchLinkLabel = searchLinkLabel;
    }

    public void setSearchLinkTooltip(String searchLinkTooltip) {
        this.searchLinkTooltip = searchLinkTooltip;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public String getConverter() {
        return converter;
    }

<<<<<<< HEAD
    public Boolean getTextarea() {
        return textarea;
    }

    public void setTextarea(Boolean textarea) {
        this.textarea = textarea;
    }
=======
    public Boolean getRenderPlain() {
        return renderPlain;
    }

    public void setRenderPlain(Boolean renderPlain) {
        this.renderPlain = renderPlain;
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
