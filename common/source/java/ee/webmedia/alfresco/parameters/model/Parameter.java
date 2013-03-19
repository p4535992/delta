package ee.webmedia.alfresco.parameters.model;

import static ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus.PARAM_CHANGED;
import static ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus.PARAM_DESC_CHANGED;
import static ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus.PARAM_NEW;
import static ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus.PARAM_NOT_CHANGED;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

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
    protected String paramDescription;
    /** Used only for importing */
    protected T previousParamValue;
    private String previousParamDescription;
    private String validationFailedMsgId;
    private ImportStatus statusOfValueChange;

    private Date nextFireTime;

    protected Parameter(String paramName, String typeMsg, String defaultValidationFailedMsg) {
        this.paramName = paramName;
        this.defaultValidationFailedMsgId = defaultValidationFailedMsg;
        this.typeMsg = typeMsg;
    }

    public static Parameter<? extends Serializable> newInstance(String paramName, Serializable property, QName nodeType) {
        return newInstance(paramName, property, nodeType, null);
    }

    public static Parameter<? extends Serializable> newInstance(String paramName, Serializable property, QName nodeType, String paramDescription) {
        if (nodeType.equals(ParametersModel.Types.PARAMETER_STRING)) {
            return newInstance(paramName, DefaultTypeConverter.INSTANCE.convert(String.class, property), paramDescription);
        }
        if (property instanceof String && StringUtils.isBlank((String) property)) {
            return null;
        }
        final Class<? extends Serializable> paramClass = getParamClass(nodeType);
        return newInstance(paramName, DefaultTypeConverter.INSTANCE.convert(paramClass, property), paramDescription);
    }

    public static Class<? extends Serializable> getParamClass(QName nodeType) {
        if (nodeType.equals(ParametersModel.Types.PARAMETER_STRING)) {
            return String.class;
        } else if (nodeType.equals(ParametersModel.Types.PARAMETER_INT)) {
            return Long.class;
        } else if (nodeType.equals(ParametersModel.Types.PARAMETER_DOUBLE)) {
            return Double.class;
        }
        throw new IllegalArgumentException("Unimplemented parameter value type: " + nodeType.toString());
    }

    public T getParamValue() {
        return paramValue;
    }

    public void setParamValue(T paramValue) {
        this.paramValue = paramValue;
        this.statusOfValueChange = null;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public void setParamDescription(String paramDescription) {
        this.paramDescription = paramDescription;
    }

    public Date getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public void setParamValueFromString(String paramValueString) {
        setParamValue(convertFromString(paramValueString));
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
        return this.getClass().getSimpleName() + " " + paramName + "=" + paramValue + "(" + paramDescription + ")";
    }

    // START: private methods

    @SuppressWarnings("unchecked")
    // can't put SuppressWarnings annotation to each place with reasonable amount of code
    private static <G extends Serializable> Parameter<G> newInstance(String paramName, G paramValue, String paramDescription) {
        Parameter<G> parameter;
        if (paramValue instanceof String) {
            parameter = (Parameter<G>) new StringParameter(paramName);
        } else if (paramValue instanceof Long) {
            parameter = (Parameter<G>) new LongParameter(paramName);
        } else if (paramValue instanceof Double) {
            parameter = (Parameter<G>) new DoubleParameter(paramName);
        } else {
            throw new IllegalArgumentException("Unimplemented nodeType: " + paramValue.getClass().getCanonicalName());
        }
        parameter.setParamDescription(paramDescription);
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

    public enum ImportStatus {
        PARAM_NEW, PARAM_CHANGED, PARAM_DESC_CHANGED, PARAM_NOT_CHANGED;
    }

    public void setPreviousParamValue() {
        this.previousParamValue = paramValue;
        statusOfValueChange = null;
    }

    public void setPreviousParamDescription() {
        this.previousParamDescription = paramDescription;
        statusOfValueChange = null;
    }

    public T getPreviousParamValue() {
        return previousParamValue;
    }

    public ImportStatus getStatus() {
        if (statusOfValueChange == null) {
            final String previousVal = getStringValOrNull(getPreviousParamValue());
            final String nvlParamVal = getNvlStringVal(getParamValue());
            if ((previousVal == null && StringUtils.isNotBlank(nvlParamVal))) {
                statusOfValueChange = PARAM_NEW;
            } else if (!StringUtils.equals(previousVal, nvlParamVal)) {
                statusOfValueChange = PARAM_CHANGED;
            } else if (!StringUtils.equals(StringUtils.trimToNull(previousParamDescription), StringUtils.trimToNull(getParamDescription()))) {
                statusOfValueChange = PARAM_DESC_CHANGED;
            } else {
                statusOfValueChange = PARAM_NOT_CHANGED;
            }
        }
        return statusOfValueChange;
    }

    private String getStringValOrNull(final T val) {
        return val == null ? null : val.toString().trim();
    }

    private String getNvlStringVal(final T val) {
        return val == null ? "" : val.toString().trim();
    }
    // END: getters / setters

}
