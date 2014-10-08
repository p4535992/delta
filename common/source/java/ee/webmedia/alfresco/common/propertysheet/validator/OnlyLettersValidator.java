package ee.webmedia.alfresco.common.propertysheet.validator;

import static ee.webmedia.alfresco.utils.ComponentUtil.getDisplayLabel;
import static ee.webmedia.alfresco.utils.ComponentUtil.getPanelLabel;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageCreatorHelper;
import ee.webmedia.alfresco.utils.MessageCreatorHelper.ErrorMsgFormat;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Validates that input string only contains letters
 */
public class OnlyLettersValidator extends ForcedMandatoryValidator implements StateHolder {
    private static final long serialVersionUID = 1L;

    private boolean trans;
    private String customErrorMsg;
    private ErrorMsgFormat customErrorMsgFormat;

    @Override
    public void validate(final FacesContext context, final UIComponent component, Object value) throws ValidatorException {
        String localName = value != null ? value.toString() : null;
        MessageCreatorHelper errMsgCreator = validateOnlyLetters(localName, component, new MessageCreatorHelper(customErrorMsg, customErrorMsgFormat));
        if (errMsgCreator.isMessageSet()) {
            throw new ValidatorException(MessageUtil.getFacesMessage(errMsgCreator));
        }
    }

    public static MessageCreatorHelper validateOnlyLetters(String localName, UIComponent localNameInput, MessageCreatorHelper errMsgCreator) {
        if (StringUtils.isBlank(localName)) {
            errMsgCreator.createMessage("validator_onlyLetters", "_mandatory", ErrorMsgFormat.FULL);
        } else if (!localName.matches("^[a-zA-Z]+$")) { // only letters
            errMsgCreator.createMessage("validator_onlyLetters", "_constraint", ErrorMsgFormat.FULL);
        }
        if (errMsgCreator.getMessageKey() != null) {
            errMsgCreator.setMessageValuesForHolders(getPanelLabel(localNameInput), getDisplayLabel(localNameInput), localName);
        }
        return errMsgCreator;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        trans = (Boolean) values[0];
        customErrorMsg = (String) values[1];
        customErrorMsgFormat = (ErrorMsgFormat) values[2];
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[4];
        values[0] = trans;
        values[1] = customErrorMsg;
        values[2] = customErrorMsgFormat;
        return values;
    }

    @Override
    public boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        trans = newTransientValue;
    }

    // START: getters / setters
    public void setCustomErrorMsg(String customErrorMsg) {
        this.customErrorMsg = customErrorMsg;
    }

    public void setCustomErrorMsgFormat(String customErrorMsgFormat) {
        if (StringUtils.isNotBlank(customErrorMsgFormat)) {
            this.customErrorMsgFormat = ErrorMsgFormat.valueOf(customErrorMsgFormat);
        }
    }
    // END: getters / setters

}
