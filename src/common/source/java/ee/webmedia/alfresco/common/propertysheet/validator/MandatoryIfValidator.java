package ee.webmedia.alfresco.common.propertysheet.validator;

import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class MandatoryIfValidator extends ForcedMandatoryValidator implements StateHolder {
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_ID = "common_propertysheet_validator_mandatoryIf";
    public static final String ATTR_MANDATORY_IF = "mandatoryIf";
    private static final List<String> SELECT_VALUES_INDICATING_MANDATORY = Arrays.asList("Jah", "true", "yes", "AK");

    private String otherPropertyName;
    private boolean _transient;

    public MandatoryIfValidator() {
        // used when restoring state
    }

    /**
     * @param otherPropertyNameSuffix
     */
    public MandatoryIfValidator(String otherPropertyNameSuffix) {
        this.otherPropertyName = otherPropertyNameSuffix;
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        UIInput input = (UIInput) component;
        UIInput propertyInput = ComponentUtil.getInputFromSamePropertySheet(component, otherPropertyName);
        boolean mustBeFilled = isOtherFilledAndMandatory(propertyInput);
        if (mustBeFilled && !isFilled(input)) {
            UIProperty thisUIProperty = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
            String msg = MessageUtil.getMessage(context, MESSAGE_ID, ComponentUtil.getPropertyLabel(thisUIProperty, component.getId()));
            throw new ValidatorException(new FacesMessage(msg));
        }
        input.setRequired(false);
    }

    public Object saveState(FacesContext context) {
        Object values[] = new Object[2];
        values[0] = otherPropertyName;
        return values;
    }

    public void restoreState(FacesContext context,
            Object state) {
        Object values[] = (Object[]) state;
        otherPropertyName = (String) values[0];
    }

    @Override
    public boolean isTransient() {
        return _transient;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        this._transient = newTransientValue;
    }

    private boolean isOtherFilledAndMandatory(UIInput propertyInput) {
        // Checkbox
        if (propertyInput instanceof UISelectBoolean) {
            UISelectBoolean selectBoolean = (UISelectBoolean) propertyInput;
            return selectBoolean.isSelected();
        }
        // dropDown (Jah/Ei)
        else if (propertyInput instanceof HtmlSelectOneMenu) {
            HtmlSelectOneMenu selector = (HtmlSelectOneMenu) propertyInput;
            String stringValue = (String) selector.getValue();
            if (stringValue == null) {
                return false;
            } else {
                if (StringUtils.isNotBlank(stringValue)) {
                    stringValue = stringValue.trim();
                }
                for (String allowedValue : SELECT_VALUES_INDICATING_MANDATORY) {
                    if (stringValue.equalsIgnoreCase(allowedValue)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            // text input
            String valueSubmitted = (String) propertyInput.getSubmittedValue();
            return StringUtils.isNotBlank(valueSubmitted);
        }

    }

    // START: getters / setters
    public void setOtherPropertyName(String otherPropertyName) {
        this.otherPropertyName = otherPropertyName;
    }

    // END: getters / setters

}
