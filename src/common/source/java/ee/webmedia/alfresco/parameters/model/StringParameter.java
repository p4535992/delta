package ee.webmedia.alfresco.parameters.model;

/**
 * @author Ats Uiboupin
 */
public class StringParameter extends Parameter<String> {
    private static final long serialVersionUID = 1L;

    public StringParameter(String paramName) {
        super(paramName, "parameters_type_string", "parameters_validation_failed");
    }

    @Override
    protected String convertFromString(String paramValueString) {
        return paramValueString;
    }

}
