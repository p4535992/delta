package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.TextFieldGenerator;

import ee.webmedia.alfresco.common.propertysheet.converter.QNameLocalNameConverter;
import ee.webmedia.alfresco.utils.MessageCreatorHelper.ErrorMsgFormat;

/**
 * Generates a input that is converted to QName. QName namespace is given with namespace attribute
 * 
 * @author Ats Uiboupin
 */
public class QNameLocalNameGenerator extends TextFieldGenerator {
    private static final String ATTR_NAMESPACE = "namespace";
    /** optional format(see {@link ErrorMsgFormat}) */
    private static final String ATTR_ERROR_MSG_FORMAT = "customErrorMsgFormat";
    private static final String ATTR_ERROR_MSG_PREFIX = "customErrorMsgPrefix";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIInput input = (UIInput) super.generate(context, id);
        addConverter(input);
        return input;
    }

    private void addConverter(UIInput input) {
        QNameLocalNameConverter qNameLocalNameConverter = new QNameLocalNameConverter();
        qNameLocalNameConverter.setNamespace(getCustomAttributes().get(ATTR_NAMESPACE));
        qNameLocalNameConverter.setCustomErrorMsgFormat(getCustomAttributes().get(ATTR_ERROR_MSG_FORMAT));
        qNameLocalNameConverter.setCustomErrorMsgPrefix(getCustomAttributes().get(ATTR_ERROR_MSG_PREFIX));
        input.setConverter(qNameLocalNameConverter);
    }

}
