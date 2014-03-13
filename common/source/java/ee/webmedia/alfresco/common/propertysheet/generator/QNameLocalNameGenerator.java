package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.TextFieldGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.converter.QNameLocalNameConverter;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.utils.MessageCreatorHelper.ErrorMsgFormat;

/**
 * Generates a input that is converted to QName. QName namespace is given with namespace attribute
 */
public class QNameLocalNameGenerator extends TextFieldGenerator implements HandlesViewMode {
    private static final String ATTR_NAMESPACE = "namespace";
    /** optional format(see {@link ErrorMsgFormat}) */
    private static final String ATTR_ERROR_MSG_FORMAT = "customErrorMsgFormat";
    private static final String ATTR_ERROR_MSG_PREFIX = "customErrorMsgPrefix";

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        UIComponent comp = super.createComponent(context, propertySheet, item);
        addConverter(comp);
        return comp;
    }

    private void addConverter(UIComponent comp) {
        QNameLocalNameConverter qNameLocalNameConverter = new QNameLocalNameConverter();
        qNameLocalNameConverter.setNamespace(getCustomAttributes().get(ATTR_NAMESPACE));
        qNameLocalNameConverter.setCustomErrorMsgFormat(getCustomAttributes().get(ATTR_ERROR_MSG_FORMAT));
        qNameLocalNameConverter.setCustomErrorMsgPrefix(getCustomAttributes().get(ATTR_ERROR_MSG_PREFIX));
        if (comp instanceof UIInput) {
            ((UIOutput) comp).setConverter(qNameLocalNameConverter);
        } else if (comp instanceof UIOutput) {
            ((UIOutput) comp).setConverter(qNameLocalNameConverter);
        } else {
            throw new IllegalArgumentException("component is neither UIInput nor UIOutput");
        }
    }

}
