package ee.webmedia.alfresco.parameters.model;

import org.apache.commons.lang.StringUtils;

public class DoubleParameter extends Parameter<Double> {
    private static final long serialVersionUID = 1L;

    public DoubleParameter(String paramName) {
        super(paramName, "parameters_type_double", "parameters_validation_failed_double");
    }

    @Override
    protected Double convertFromString(String paramValueString) {
        if (StringUtils.isBlank(paramValueString)) {
            return Double.valueOf(0);
        }
        return Double.valueOf(paramValueString);
    }
}
