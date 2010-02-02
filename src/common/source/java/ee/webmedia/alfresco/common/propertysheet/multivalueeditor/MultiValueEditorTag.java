package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.webapp.UIComponentTag;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Tag for MultiValueEditor
 * 
 * @author Erko Hansar
 */
public class MultiValueEditorTag extends UIComponentTag {

    public String getComponentType() {
        return MultiValueEditor.MULTI_VALUE_EDITOR_FAMILY;
    }

    public String getRendererType() {
        return MultiValueEditorRenderer.MULTI_VALUE_EDITOR_RENDERER_TYPE;
    }

    private String varName;
    private String props;
    private String pickerCallback;
    private String setterCallback;
    private String dialogTitleId;
    private String titles;
    private String propTypes;

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public void setProps(String props) {
        this.props = props;
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

    public void setTitles(String titles) {
        this.titles = titles;
    }
    
    public void setPropTypes(String propTypes) {
        this.propTypes = propTypes;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        List<String> propNames = new ArrayList<String>();
        for (String prop : props.split(",")) {
            if (StringUtils.isNotBlank(prop)) {
                propNames.add(prop);
            }
        }
        List<String> propTitles = new ArrayList<String>();
        for (String title : titles.split(",")) {
            if (StringUtils.isNotBlank(title)) {
                propTitles.add(MessageUtil.getMessage(FacesContext.getCurrentInstance(), title));
            }
            else {
                propTitles.add("");
            }
        }
        List<String> componentTypes = new ArrayList<String>();
        for (String propType : propTypes.split(",")) {
            componentTypes.add(propType);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("propNames", propNames);
        attributes.put("propTitles", propTitles);
        attributes.put("componentTypes", componentTypes);
        attributes.put("propertySheetVar", varName);
        attributes.put(Search.PICKER_CALLBACK_KEY, pickerCallback);
        attributes.put(Search.DIALOG_TITLE_ID_KEY, dialogTitleId);
        attributes.put("setterCallback", setterCallback);
    }
    
    @Override
    public void release() {
        super.release();
        varName = null;
        props = null;
        pickerCallback = null;
        setterCallback = null;
        dialogTitleId = null;
        titles = null;
        propTypes = null;
    }
    
}
