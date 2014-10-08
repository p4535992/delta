<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.convert.BooleanConverter;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator for boolean properties that generates two radiobuttons instead of checkbox. <br>
 * Follwing show-property configuration element attributes are used to display lables for true and false radiobutton:<br>
 * 1) {@link BooleanRadioGenerator#ATTR_LABEL_TRUE} <br>
 * 2) {@link BooleanRadioGenerator#ATTR_LABEL_FALSE}
 * 
 * @author Ats Uiboupin
 */
public class BooleanRadioGenerator extends BaseComponentGenerator {

    /** show-property configuration element attribute - label used for true value */
    private static final String ATTR_LABEL_TRUE = "labelTrue";
    /** show-property configuration element attribute - label used for false value */
    private static final String ATTR_LABEL_FALSE = "labelFalse";
    /**
     * Optional show-property configuration element attribute: if Boolean property value is null, then: <br>
     * if this value is missing(or blank) then no radiobutton is selected<br>
     * if this value is "true", then radiobutton corresponsing to true value will be selected<br>
     * otherwise radiobutton corresponsing to false value will be selected
     */
    public static final String ATTR_NULL_VALUE = "nullValue";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("Not called");
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Boolean value = getValue(propertySheet, item);
        if (value == null) {
            String nullValueStr = getCustomAttributes().get(ATTR_NULL_VALUE);
            if (StringUtils.isNotBlank(nullValueStr)) {
                value = Boolean.valueOf(nullValueStr);
            }
        }
        String trueLabel = getLabel(ATTR_LABEL_TRUE, "yes");
        String falseLabel = getLabel(ATTR_LABEL_FALSE, "no");
        if (item.isReadOnly()) {
            UIComponent component = createReadOnlyComponent(context, value, trueLabel, falseLabel);
            FacesHelper.setupComponentId(context, component, getDefaultId(item));
            return component;
        }
        final HtmlSelectOneRadio selectComponent = new HtmlSelectOneRadio();

        UISelectItem selectTrue = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectTrue.setItemValue(true);
        selectTrue.setItemLabel(trueLabel);

        UISelectItem selectFalse = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectFalse.setItemLabel(falseLabel);
        selectFalse.setItemValue(false);

        ComponentUtil.addChildren(selectComponent, selectTrue, selectFalse);

        selectComponent.setValue(value);

        FacesHelper.setupComponentId(context, selectComponent, item.getName());
        selectComponent.setConverter(new BooleanConverter());
        selectComponent.setLayout("pageDirection");// radiobuttons positioned under each-other
        return selectComponent;
    }

    private UIComponent createReadOnlyComponent(FacesContext context, Boolean value, String trueLabel, String falseLabel) {
        HtmlOutputText outputText = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        // FacesHelper.setupComponentId(context, outputText, id);
        // outputText.setEscape(false);
        String displayValue;
        if (value == null) {
            displayValue = "null";
        } else if (value) {
            displayValue = trueLabel;
        } else {
            displayValue = falseLabel;
        }
        outputText.setValue(displayValue);
        return outputText;
    }

    private String getLabel(String attributeName, String defaultMsgKey) {
        String trueLabelMsg = getCustomAttributes().get(attributeName);
        if (StringUtils.isBlank(trueLabelMsg)) {
            trueLabelMsg = defaultMsgKey;
        }
        return MessageUtil.getMessage(trueLabelMsg);
    }
}
=======
package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.convert.BooleanConverter;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator for boolean properties that generates two radiobuttons instead of checkbox. <br>
 * Follwing show-property configuration element attributes are used to display lables for true and false radiobutton:<br>
 * 1) {@link BooleanRadioGenerator#ATTR_LABEL_TRUE} <br>
 * 2) {@link BooleanRadioGenerator#ATTR_LABEL_FALSE}
 */
public class BooleanRadioGenerator extends BaseComponentGenerator {

    /** show-property configuration element attribute - label used for true value */
    private static final String ATTR_LABEL_TRUE = "labelTrue";
    /** show-property configuration element attribute - label used for false value */
    private static final String ATTR_LABEL_FALSE = "labelFalse";
    /**
     * Optional show-property configuration element attribute: if Boolean property value is null, then: <br>
     * if this value is missing(or blank) then no radiobutton is selected<br>
     * if this value is "true", then radiobutton corresponsing to true value will be selected<br>
     * otherwise radiobutton corresponsing to false value will be selected
     */
    public static final String ATTR_NULL_VALUE = "nullValue";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("Not called");
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Boolean value = getValue(propertySheet, item);
        if (value == null) {
            String nullValueStr = getCustomAttributes().get(ATTR_NULL_VALUE);
            if (StringUtils.isNotBlank(nullValueStr)) {
                value = Boolean.valueOf(nullValueStr);
            }
        }
        String trueLabel = getLabel(ATTR_LABEL_TRUE, "yes");
        String falseLabel = getLabel(ATTR_LABEL_FALSE, "no");
        if (item.isReadOnly()) {
            UIComponent component = createReadOnlyComponent(context, value, trueLabel, falseLabel);
            FacesHelper.setupComponentId(context, component, getDefaultId(item));
            return component;
        }
        final HtmlSelectOneRadio selectComponent = new HtmlSelectOneRadio();

        UISelectItem selectTrue = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectTrue.setItemValue(true);
        selectTrue.setItemLabel(trueLabel);

        UISelectItem selectFalse = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectFalse.setItemLabel(falseLabel);
        selectFalse.setItemValue(false);

        ComponentUtil.addChildren(selectComponent, selectTrue, selectFalse);

        selectComponent.setValue(value);

        FacesHelper.setupComponentId(context, selectComponent, item.getName());
        selectComponent.setConverter(new BooleanConverter());
        selectComponent.setLayout("pageDirection");// radiobuttons positioned under each-other
        return selectComponent;
    }

    private UIComponent createReadOnlyComponent(FacesContext context, Boolean value, String trueLabel, String falseLabel) {
        HtmlOutputText outputText = (HtmlOutputText) context.getApplication().createComponent("javax.faces.HtmlOutputText");
        // FacesHelper.setupComponentId(context, outputText, id);
        // outputText.setEscape(false);
        String displayValue;
        if (value == null) {
            displayValue = "null";
        } else if (value) {
            displayValue = trueLabel;
        } else {
            displayValue = falseLabel;
        }
        outputText.setValue(displayValue);
        return outputText;
    }

    private String getLabel(String attributeName, String defaultMsgKey) {
        String trueLabelMsg = getCustomAttributes().get(attributeName);
        if (StringUtils.isBlank(trueLabelMsg)) {
            trueLabelMsg = defaultMsgKey;
        }
        return MessageUtil.getMessage(trueLabelMsg);
    }
}
>>>>>>> develop-5.1
