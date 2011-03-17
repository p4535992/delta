package ee.webmedia.alfresco.common.propertysheet.validator;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.validator.ValidatorException;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Validator that can be used to make UIInput mandatory based on value of some other input.
 * It support two types of expressions to be used to decide whether UIInput being validated is required or not:
 * 1) equality expression separated by "!=" or "=" where left side refers to another UIInput on the same UIPropertySheet and right side can be string. <br>
 * For example <br>
 * <code>docspec:substituteName!=null</code> to make this component mandatory if input with clientId suffix "docspec:substituteName" is not left empty. <br>
 * Another example: <br>
 * <code>docspec:substituteName=String value that makes this component mandatory</code> <br>
 * to make this component mandatory if other component value is equal to given text.
 * 
 * @author Ats Uiboupin
 */
public class MandatoryIfValidator extends ForcedMandatoryValidator implements StateHolder {
    private static final long serialVersionUID = 1L;
    
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MandatoryIfValidator.class);

    private static final String MESSAGE_ID = "common_propertysheet_validator_mandatoryIf";
    public static final String ATTR_MANDATORY_IF = "mandatoryIf";
    public static final String ATTR_MANDATORY_IF_LABEL_ID = "mandatoryIfLabelId";
    private static final List<String> SELECT_VALUES_INDICATING_MANDATORY = Arrays.asList("Jah", "true", "yes", "AK");

    /**
     * Expression to be used to decide whether UIInput being validated is required or not.
     */
    private String evaluationExpression;
    private String mandatoryIfLabelId;
    private boolean _transient;

    public MandatoryIfValidator() {
        // used when restoring state
    }

    /**
     * @param otherPropertyNameSuffix
     */
    public MandatoryIfValidator(String evaluationExpression) {
        this.evaluationExpression = evaluationExpression;
    }

    public void setMandatoryIfLabelId(String mandatoryIfLabelId) {
        this.mandatoryIfLabelId = mandatoryIfLabelId;
    }

    @Override
    public void validate(final FacesContext context, final UIComponent component, Object value) throws ValidatorException {
        if (Utils.isRequestValidationDisabled(context) || isFilled(value)) {
            return; // we don't want to validate before for example Search component is starting searching
        }
        // because model values are not jet applied to all components
        // it seems to be much easier to get correct values after PhaseId.UPDATE_MODEL_VALUES has bee executed
        delayValidation(context, component);
    }

    private void delayValidation(final FacesContext context, final UIComponent component) {
        final UIPropertySheet propSheet = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class);
        ActionEvent event = new ActionEvent(propSheet) {
            private static final long serialVersionUID = 1L;
            boolean notExecuted = true;

            @Override
            public void processListener(FacesListener faceslistener) {
                notExecuted = false;
                validateInternal(context, component);
            }

            @Override
            public boolean isAppropriateListener(FacesListener faceslistener) {
                return notExecuted; // process listeners only once
            }
        };
        event.setPhaseId(PhaseId.UPDATE_MODEL_VALUES);
        propSheet.queueEvent(event);
    }

    private void validateInternal(FacesContext context, UIComponent component) {
        UIInput input = (UIInput) component;

        String propName = evaluationExpression;
        String propExpectedVal = null;
        Boolean checkEquals = null;
        final int neIndex = evaluationExpression.indexOf("!=");
        final int eqIndex;
        if (neIndex >= 0) {
            checkEquals = false;
            propName = evaluationExpression.substring(0, neIndex);
            propExpectedVal = evaluationExpression.substring(neIndex + 2);
        } else if (0 <= (eqIndex = evaluationExpression.indexOf("="))) {
            checkEquals = true;
            propName = evaluationExpression.substring(0, eqIndex);
            propExpectedVal = evaluationExpression.substring(eqIndex + 1);
        }
        propName = StringUtils.replace(propName, ":", "x003a_"); // colon is encoded, as it is also used as it is used in id to separate parent components
        UIInput otherInput = findOtherInputComponent(context, component, propName);
        if(otherInput==null) {
            return;
        }
        // boolean required = true;
        if (checkEquals != null) {

            final boolean required = isRequired(propExpectedVal, checkEquals, otherInput);
            input.setRequired(required);
            if (required) {
                final FacesMessage msg = new FacesMessage(MessageUtil.getMessage(context, mandatoryIfLabelId));
                handleValidationException(input, msg, context);
            }
        } else {
            boolean mustBeFilled = isOtherFilledAndMandatory(otherInput);
            if (mustBeFilled) {
                String label = (String) input.getAttributes().get(ComponentUtil.ATTR_DISPLAY_LABEL);
                if (label == null) {
                    UIProperty thisUIProperty = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
                    label = ComponentUtil.getPropertyLabel(thisUIProperty, component.getId());
                }
                String msg = MessageUtil.getMessage(context, MESSAGE_ID, label);
                handleValidationException(input, new FacesMessage(msg), context);
            }
            input.setRequired(false);
        }
    }

    private void handleValidationException(UIInput input, FacesMessage facesMessage, FacesContext context) {
        // can't throw new ValidatorException(facesMessage), because the JSF phase is not PhaseId.PROCESS_VALIDATIONS;
        input.setValid(false);
        if (facesMessage != null) {
            facesMessage.setSeverity(FacesMessage.SEVERITY_ERROR);
            context.addMessage(input.getClientId(context), facesMessage);
        }
        throw new AbortProcessingException("MandatoryIf validation failed");
    }

    private boolean isRequired(String propExpectedVal, boolean checkEquals, UIInput otherInput) {
        final String otherValue = DefaultTypeConverter.INSTANCE.convert(String.class, otherInput.getValue());
        final boolean equals = StringUtils.equals(propExpectedVal, otherValue) || StringUtils.equals(nullToEmpty(propExpectedVal), nullToEmpty(otherValue));
        final boolean expressionValue;
        if (!checkEquals) {
            expressionValue = !equals;
        } else {
            expressionValue = equals;
        }
        return expressionValue;
    }

    protected boolean isFilled(Object value) {
        final String strValue = DefaultTypeConverter.INSTANCE.convert(String.class, value);
        return StringUtils.isNotBlank(strValue);
    }

    private String nullToEmpty(String val) {
        if (val == null || StringUtils.equals(val, "null")) {
            return "";
        }
        return val;
    }

    private UIInput findOtherInputComponent(FacesContext context, UIComponent component, String otherPropertyName) {
        UIPropertySheet propSheetComponent = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class, true);
        if(propSheetComponent==null) {
            log.info("No parent propSheetComponent found for component '"+component.getId()+"'");
            return null;
        }
        List<UIInput> inputs = new ArrayList<UIInput>();
        ComponentUtil.getChildrenByClass(inputs, propSheetComponent, UIInput.class, otherPropertyName);
        if (inputs.size() != 1) {
            StringBuilder ids = new StringBuilder();
            for (UIInput wrongInput : inputs) {
                if (ids.length() > 0) {
                    ids.append(", ");
                }
                ids.append(wrongInput.getClientId(context));
            }
            // component might be multiValued (let's search for component with the same multiValue index)
            final Integer multivalueIndex = ComponentUtil.gettAttribute(component, VALUE_INDEX_IN_MULTIVALUED_PROPERTY, Integer.class);
            if (multivalueIndex != null) {
                return findOtherInputComponent(context, propSheetComponent, otherPropertyName + "_" + multivalueIndex);
            }
            throw new RuntimeException("There must be one UIInput component with id suffix '" + otherPropertyName + "', but found "
                    + inputs.size() + " components: " + ids.toString());
        }
        UIInput propertyInput = inputs.get(0);
        return propertyInput;
    }

    public Object saveState(FacesContext context) {
        Object values[] = new Object[3];
        values[0] = evaluationExpression;
        values[1] = mandatoryIfLabelId;
        return values;
    }

    public void restoreState(FacesContext context,
            Object state) {
        Object values[] = (Object[]) state;
        evaluationExpression = (String) values[0];
        mandatoryIfLabelId = (String) values[1];
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
        this.evaluationExpression = otherPropertyName;
    }

    // END: getters / setters

}