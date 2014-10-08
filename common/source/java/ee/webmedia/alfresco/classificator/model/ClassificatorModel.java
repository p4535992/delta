<<<<<<< HEAD
package ee.webmedia.alfresco.classificator.model;

import org.alfresco.service.namespace.QName;

public interface ClassificatorModel {
    String URI = "http://alfresco.webmedia.ee/model/classificator/1.0";
    String NAMESPACE_PREFFIX = "cl:";

    interface Repo {
        final static String CLASSIFICATORS_PARENT = "/";
        final static String CLASSIFICATORS_SPACE = CLASSIFICATORS_PARENT + NAMESPACE_PREFFIX + Types.CLASSIFICATOR_ROOT.getLocalName();
    }

    interface Types {
        QName CLASSIFICATOR_ROOT = QName.createQName(URI, "classificators");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
    }

    interface Associations {
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
    }

    interface Props {
        /** description of the classificator */
        QName DESCRIPTION = QName.createQName(URI, "description");
        /** description of the classificator value */
        QName CL_VALUE_DESCRIPTION = QName.createQName(URI, "classificatorDescription");
        QName CL_VALUE_NAME = QName.createQName(URI, "valueName");
        /** should classificator values be sorted alfabetically? */
        QName ALFABETIC_ORDER = QName.createQName(URI, "alfabeticOrder");
        QName DELETE_ENABLED = QName.createQName(URI, "deleteEnabled");
        QName ADD_REMOVE_VALUES = QName.createQName(URI, "addRemoveValues");
        QName CLASSIFICATOR_NAME = QName.createQName(URI, "name");
    }
}
=======
package ee.webmedia.alfresco.classificator.model;

import org.alfresco.service.namespace.QName;

public interface ClassificatorModel {
    String URI = "http://alfresco.webmedia.ee/model/classificator/1.0";
    String NAMESPACE_PREFFIX = "cl:";

    interface Repo {
        final static String CLASSIFICATORS_PARENT = "/";
        final static String CLASSIFICATORS_SPACE = CLASSIFICATORS_PARENT + NAMESPACE_PREFFIX + Types.CLASSIFICATOR_ROOT.getLocalName();
    }

    interface Types {
        QName CLASSIFICATOR_ROOT = QName.createQName(URI, "classificators");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
    }

    interface Associations {
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
    }

    interface Props {
        /** description of the classificator */
        QName DESCRIPTION = QName.createQName(URI, "description");
        /** description of the classificator value */
        QName CL_VALUE_DESCRIPTION = QName.createQName(URI, "classificatorDescription");
        QName CL_VALUE_NAME = QName.createQName(URI, "valueName");
        /** should classificator values be sorted alfabetically? */
        QName ALFABETIC_ORDER = QName.createQName(URI, "alfabeticOrder");
        QName DELETE_ENABLED = QName.createQName(URI, "deleteEnabled");
        QName ADD_REMOVE_VALUES = QName.createQName(URI, "addRemoveValues");
        QName CLASSIFICATOR_NAME = QName.createQName(URI, "name");
    }
}
>>>>>>> develop-5.1
