<<<<<<< HEAD
package ee.webmedia.alfresco.document.einvoice.model;

import org.alfresco.service.namespace.QName;

public interface DimensionModel {
    String URI = "http://alfresco.webmedia.ee/model/dimension/1.0";
    String NAMESPACE_PREFFIX = "dim:";

    public interface Repo {
        final static String DIMENSIONS_PARENT = "/";
        final static String DIMENSIONS_SPACE = DIMENSIONS_PARENT + NAMESPACE_PREFFIX + "dimensions";
    }

    interface Types {
        QName DIMENSION_ROOT = QName.createQName(URI, "dimensions");
        QName DIMENSION = QName.createQName(URI, "dimension");
        QName DIMENSION_VALUE = QName.createQName(URI, "dimensionValue");
    }

    interface Associations {
        QName DIMENSION = QName.createQName(URI, "dimension");
        QName DIMENSION_VALUE = QName.createQName(URI, "dimensionValue");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName COMMENT = QName.createQName(URI, "comment");
        QName VALUE_NAME = QName.createQName(URI, "valueName");
        QName VALUE = QName.createQName(URI, "value");
        QName VALUE_COMMENT = QName.createQName(URI, "valueComment");
        QName BEGIN_DATE = QName.createQName(URI, "beginDate");
        QName END_DATE = QName.createQName(URI, "endDate");
        QName ACTIVE = QName.createQName(URI, "active");
        QName DEFAULT_VALUE = QName.createQName(URI, "defaultValue");
    }
}
=======
package ee.webmedia.alfresco.document.einvoice.model;

import org.alfresco.service.namespace.QName;

public interface DimensionModel {
    String URI = "http://alfresco.webmedia.ee/model/dimension/1.0";
    String NAMESPACE_PREFFIX = "dim:";

    public interface Repo {
        final static String DIMENSIONS_PARENT = "/";
        final static String DIMENSIONS_SPACE = DIMENSIONS_PARENT + NAMESPACE_PREFFIX + "dimensions";
    }

    interface Types {
        QName DIMENSION_ROOT = QName.createQName(URI, "dimensions");
        QName DIMENSION = QName.createQName(URI, "dimension");
        QName DIMENSION_VALUE = QName.createQName(URI, "dimensionValue");
    }

    interface Associations {
        QName DIMENSION = QName.createQName(URI, "dimension");
        QName DIMENSION_VALUE = QName.createQName(URI, "dimensionValue");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName COMMENT = QName.createQName(URI, "comment");
        QName VALUE_NAME = QName.createQName(URI, "valueName");
        QName VALUE = QName.createQName(URI, "value");
        QName VALUE_COMMENT = QName.createQName(URI, "valueComment");
        QName BEGIN_DATE = QName.createQName(URI, "beginDate");
        QName END_DATE = QName.createQName(URI, "endDate");
        QName ACTIVE = QName.createQName(URI, "active");
        QName DEFAULT_VALUE = QName.createQName(URI, "defaultValue");
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
