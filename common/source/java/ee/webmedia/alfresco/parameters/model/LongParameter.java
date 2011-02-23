package ee.webmedia.alfresco.parameters.model;

import org.apache.commons.lang.StringUtils;

/**
 * @author Ats Uiboupin
 */
public class LongParameter extends Parameter<Long> {
    private static final long serialVersionUID = 1L;

    public LongParameter(String paramName) {
        super(paramName, "parameters_type_long", "parameters_validation_failed_integer");
    }

    @Override
    protected Long convertFromString(String paramValueString) {
        if (StringUtils.isBlank(paramValueString)) {
            return 0L;
        }
        return Long.valueOf(paramValueString);
    }
}
