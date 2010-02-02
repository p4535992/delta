package ee.webmedia.alfresco.common.propertysheet.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class MandatoryIfValidator extends ForcedMandatoryValidator implements StateHolder {
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_ID = "common_propertysheet_validator_mandatoryIf";
    public static final String ATTR_MANDATORY_IF = "mandatoryIf";
    public static final String DISABLE_VALIDATION = "DISABLE_MANDATORY_IF_VALIDATOR";
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
        if (isValidationDisabled(context)) {
            return; // we don't want to validate before for example Search component is starting searching
        }

        UIInput input = (UIInput) component;

        // find otherPropertyName
        UIPropertySheet propSheetComponent = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class, true);
        List<UIInput> inputs = new ArrayList<UIInput>();
        ComponentUtil.getChildrenByClass(inputs, propSheetComponent, UIInput.class, otherPropertyName);
        if (inputs.size() != 1) {
            StringBuilder ids = new StringBuilder();
            for (UIInput wrongInput : inputs) {
                if (ids.length() > 0) {
                    ids.append(", ");
                }
                ids.append(wrongInput.getId());
            }
            throw new RuntimeException("There must be only one UIInput component with id suffix '" + otherPropertyName + "', but found " + inputs.size() + " components: " + ids.toString());
        }
        UIInput propertyInput = inputs.get(0);

        boolean mustBeFilled = isOtherFilledAndMandatory(propertyInput);
        if (mustBeFilled && !isFilled(input)) {
            String label = (String) input.getAttributes().get(ComponentUtil.ATTR_DISPLAY_LABEL);
            if (label == null) {
                UIProperty thisUIProperty = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
                label = ComponentUtil.getPropertyLabel(thisUIProperty, component.getId());
            }
            String msg = MessageUtil.getMessage(context, MESSAGE_ID, label);
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

    private boolean isValidationDisabled(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        return (Boolean.TRUE == requestMap.get(DISABLE_VALIDATION));
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
            }
            if (StringUtils.isNotBlank(stringValue)) {
                stringValue = stringValue.trim();
            }
            for (String allowedValue : SELECT_VALUES_INDICATING_MANDATORY) {
                if (stringValue.equalsIgnoreCase(allowedValue)) {
                    return true;
                }
            }
            return false;
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
