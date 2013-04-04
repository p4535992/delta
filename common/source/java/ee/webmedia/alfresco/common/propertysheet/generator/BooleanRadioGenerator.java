package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.convert.BooleanConverter;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
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
    public static final String ATTR_LABEL_TRUE = "labelTrue";
    /** show-property configuration element attribute - label used for false value */
    public static final String ATTR_LABEL_FALSE = "labelFalse";
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
        final HtmlSelectOneRadio selectComponent = new HtmlSelectOneRadio();

        UISelectItem selectTrue = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectTrue.setItemValue(true);
        selectTrue.setItemLabel(MessageUtil.getMessage(getCustomAttributes().get(ATTR_LABEL_TRUE)));

        UISelectItem selectFalse = (UISelectItem) context.getApplication().createComponent("javax.faces.SelectItem");
        selectFalse.setItemLabel(MessageUtil.getMessage(getCustomAttributes().get(ATTR_LABEL_FALSE)));
        selectFalse.setItemValue(false);

        ComponentUtil.addChildren(selectComponent, selectTrue, selectFalse);
        // setValue
        Node node = propertySheet.getNode();
        QName qName = QName.resolveToQName(node.getNamespacePrefixResolver(), item.getAttributes().get("name").toString());

        Boolean value = (Boolean) node.getProperties().get(qName);
        if (value == null) {
            String nullValueStr = getCustomAttributes().get(ATTR_NULL_VALUE);
            if (StringUtils.isNotBlank(nullValueStr)) {
                value = Boolean.valueOf(nullValueStr);
            }
        }
        selectComponent.setValue(value);

        FacesHelper.setupComponentId(context, selectComponent, item.getName());
        selectComponent.setConverter(new BooleanConverter());
        selectComponent.setLayout("pageDirection");// radiobuttons positioned under each-other
        return selectComponent;
    }
}
