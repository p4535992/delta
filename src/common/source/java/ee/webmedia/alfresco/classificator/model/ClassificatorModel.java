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
}
