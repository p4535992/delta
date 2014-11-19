<<<<<<< HEAD
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
=======
package ee.webmedia.alfresco.parameters.model;

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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
