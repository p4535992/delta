package ee.webmedia.alfresco.parameters.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface ParametersModel {
    String URI = "http://alfresco.webmedia.ee/model/parameters/1.0";
    String PREFIX = "param:";

    public interface Repo {
        final static String PARAMETERS_PARENT = "/";
        final static String PARAMETERS_SPACE = PARAMETERS_PARENT + PREFIX + "parameters";
    }
    
    public interface Types {
        QName PARAMETER_STRING = QName.createQName(URI, "stringParameter");
        QName PARAMETER_INT = QName.createQName(URI, "intParameter");
        QName PARAMETER_DOUBLE = QName.createQName(URI, "doubleParameter");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        /**
         * Properties of "param:parameter" type
         */
        public interface Parameter {
            QName VALUE = QName.createQName(ParametersModel.URI, "value");
        }

    }

}
