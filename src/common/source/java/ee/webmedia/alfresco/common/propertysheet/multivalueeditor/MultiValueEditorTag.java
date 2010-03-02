package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.webapp.UIComponentTag;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.search.Search;

/**
 * Tag for MultiValueEditor
 * 
 * @author Erko Hansar
 */
public class MultiValueEditorTag extends UIComponentTag {

    @Override
    public String getComponentType() {
        return MultiValueEditor.MULTI_VALUE_EDITOR_FAMILY;
    }

    @Override
    public String getRendererType() {
        return MultiValueEditorRenderer.MULTI_VALUE_EDITOR_RENDERER_TYPE;
    }

    private String varName;
    private String propsGeneration;
    private String pickerCallback;
    private String setterCallback;
    private String dialogTitleId;
    private String titles;

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public void setPropsGeneration(String propsGeneration) {
        this.propsGeneration = propsGeneration;
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

    public String getPropsGeneration() {
        return propsGeneration;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        List<String> propNames = new ArrayList<String>();
        for (String prop : propsGeneration.split(",")) {
            if (StringUtils.isNotBlank(prop)) {
                propNames.add(prop);
            }
        }
        final List<ComponentPropVO> propsVOs = CombinedPropReader.readProperties(propsGeneration, null, "¤", titles);

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put(PROP_GENERATOR_DESCRIPTORS, propsVOs);

        attributes.put(MultiValueEditor.PROPERTY_SHEET_VAR, varName);
        attributes.put(Search.PICKER_CALLBACK_KEY, pickerCallback);
        attributes.put(Search.DIALOG_TITLE_ID_KEY, dialogTitleId);
        attributes.put("setterCallback", setterCallback);
    }

    @Override
    public void release() {
        super.release();
        varName = null;
        propsGeneration = null;
        pickerCallback = null;
        setterCallback = null;
        dialogTitleId = null;
        titles = null;
    }

}
