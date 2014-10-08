<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ForcedMandatoryValidator implements Validator {
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_ID = "common_propertysheet_validator_mandatory";

    public ForcedMandatoryValidator() {
        // used when restoring state
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (Utils.isRequestValidationDisabled(context)) {
            return; // we don't want to validate before for example Search component is starting searching
        }

        UIInput input = (UIInput) component;
        if (!isFilled(input)) {
            UIProperty thisUIProperty = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
            String msg = MessageUtil.getMessage(context, MESSAGE_ID, ComponentUtil.getPropertyLabel(thisUIProperty, component.getId()));
            throw new ValidatorException(new FacesMessage(msg));
        }
        input.setRequired(false);
    }

    protected boolean isFilled(UIInput dependant) {
        // XXX: võiks selle ka sarnaselt MandatoryIfValidator'ile ümber teha, et valideerimine lükataks edasi PhaseId.UPDATE_MODEL_VALUES, et vältida probleeme
        // teise komponendi väärtuse lugemisel
        Object submittedValue = dependant.getSubmittedValue();
        if (submittedValue instanceof String) {
            return StringUtils.isNotBlank((String) submittedValue);
        } else if (submittedValue == null) {
            return false; // dropDown select may submit null
        } else {
            throw new RuntimeException("Not implemented to handing submited value with type: " + submittedValue.getClass().getCanonicalName());
        }
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ForcedMandatoryValidator implements Validator {
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_ID = "common_propertysheet_validator_mandatory";

    public ForcedMandatoryValidator() {
        // used when restoring state
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (Utils.isRequestValidationDisabled(context)) {
            return; // we don't want to validate before for example Search component is starting searching
        }

        UIInput input = (UIInput) component;
        if (!isFilled(input)) {
            UIProperty thisUIProperty = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
            String msg = MessageUtil.getMessage(context, MESSAGE_ID, ComponentUtil.getPropertyLabel(thisUIProperty, component.getId()));
            throw new ValidatorException(new FacesMessage(msg));
        }
        input.setRequired(false);
    }

    protected boolean isFilled(UIInput dependant) {
        // XXX: võiks selle ka sarnaselt MandatoryIfValidator'ile ümber teha, et valideerimine lükataks edasi PhaseId.UPDATE_MODEL_VALUES, et vältida probleeme
        // teise komponendi väärtuse lugemisel
        Object submittedValue = dependant.getSubmittedValue();
        if (submittedValue instanceof String) {
            return StringUtils.isNotBlank((String) submittedValue);
        } else if (submittedValue == null) {
            return false; // dropDown select may submit null
        } else {
            throw new RuntimeException("Not implemented to handing submited value with type: " + submittedValue.getClass().getCanonicalName());
        }
    }

}
>>>>>>> develop-5.1
