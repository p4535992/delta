package ee.webmedia.alfresco.classificator.model;

import org.alfresco.service.namespace.QName;

public interface ClassificatorModel {
    String URI = "http://alfresco.webmedia.ee/model/classificator/1.0";
    String NAMESPACE_PREFFIX = "cl:";

    interface Types {
        QName CLASSIFICATOR_ROOT = QName.createQName(URI, "classificators");
        QName CLASSIFICATOR = QName.createQName(URI, "classificator");
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
    }

    interface Associations {
        QName CLASSIFICATOR_VALUE = QName.createQName(URI, "classificatorValue");
    }

    interface Props {
        /** description of the classificator */
        QName DESCRIPTION = QName.createQName(URI, "description");
        /** description of the classificator value */
        QName CL_VALUE_DESCRIPTION = QName.createQName(URI, "classificatorDescription");
        /** should classificator values be sorted alfabetically? */
        QName ALFABETIC_ORDER = QName.createQName(URI, "alfabeticOrder");
    }
}
