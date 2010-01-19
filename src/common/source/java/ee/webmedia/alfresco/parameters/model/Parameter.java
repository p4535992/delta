package ee.webmedia.alfresco.parameters.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 *         Class representing abstract configurable system parameter of generic type <T>
 * @param <T>
 */
public abstract class Parameter<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String paramName;
    private final String defaultValidationFailedMsgId;
    private final String typeMsg;

    protected T paramValue;
    private String validationFailedMsgId;

    protected Parameter(String paramName, String typeMsg, String defaultValidationFailedMsg) {
        this.paramName = paramName;
        this.defaultValidationFailedMsgId = defaultValidationFailedMsg;
        this.typeMsg = typeMsg;
    }

    public static Parameter<? extends Serializable> newInstance(String paramName, Serializable property, QName nodeType) {
        if (nodeType.equals(ParametersModel.Types.PARAMETER_STRING)) {
            return newInstance(paramName, DefaultTypeConverter.INSTANCE.convert(String.class, property));
        } else if (nodeType.equals(ParametersModel.Types.PARAMETER_INT)) {
            return newInstance(paramName, DefaultTypeConverter.INSTANCE.convert(Integer.class, property));
        } else if (nodeType.equals(ParametersModel.Types.PARAMETER_DOUBLE)) {
            return newInstance(paramName, DefaultTypeConverter.INSTANCE.convert(Double.class, property));
        } else {
            throw new IllegalArgumentException("Unimplemented nodeType: " + nodeType.toString());
        }
    }

    public T getParamValue() {
        return paramValue;
    }

    public void setParamValue(T paramValue) {
        this.paramValue = paramValue;
    }

    /**
     * Value might be set using reflection (i.e. by JSF Dialog), so that type safety is not guaranteed. <br>
     * Delegating to concrete subclasses converting value from string to concrete type.
     */
    abstract protected T convertFromString(String paramValueString);

    /**
     * @return null if validation was successful, otherwise string containing validationFailedMsgId
     */
    public String validateValue() {
        try {
            convertToType();
            validationFailedMsgId = null;
        } catch (RuntimeException e) {
            validationFailedMsgId = defaultValidationFailedMsgId;
        }
        return validationFailedMsgId;
    }

    public boolean isLastValidationSuccessful() {
        return validationFailedMsgId == null;
    }

    @Override
    public String toString() {
        return "paramName: " + paramName + "; paramValue=" + paramValue + " (" + this.getClass().getSimpleName() + ")";
    }

    // START: private methods

    @SuppressWarnings("unchecked")
    // can't put SuppressWarnings annotation to each place with reasonable ammount of code
    private static <G extends Serializable> Parameter<G> newInstance(String paramName, G paramValue) {
        Parameter<G> parameter;
        if (paramValue instanceof String) {
            parameter = (Parameter<G>) new StringParameter(paramName);
        } else if (paramValue instanceof Integer) {
            parameter = (Parameter<G>) new LongParameter(paramName);
        } else if (paramValue instanceof Double) {
            parameter = (Parameter<G>) new DoubleParameter(paramName);
        } else {
            throw new IllegalArgumentException("Unimplemented nodeType: " + paramValue.getClass().getCanonicalName());
        }
        parameter.setParamValue(paramValue);
        return parameter;
    }

    private void convertToType() {
        Object paramValueObject = paramValue;
        if (paramValueObject instanceof String) {
            String paramValueString = (String) paramValueObject;
            setParamValue(convertFromString(paramValueString.trim()));
        }
    }

    // END: private methods

    // START: getters / setters
    public String getParamName() {
        return paramName;
    }

    public String getTypeMsg() {
        return typeMsg;
    }
    // END: getters / setters

}
