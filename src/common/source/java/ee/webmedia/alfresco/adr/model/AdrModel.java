package ee.webmedia.alfresco.adr.model;

import org.alfresco.service.namespace.QName;

public interface AdrModel {
    String URI = "http://alfresco.webmedia.ee/model/adr/1.0";
    String PREFIX = "adr:";

    public interface Repo {
        final static String ADR_PARENT = "/";
        final static String ADR_DELETED_DOCUMENTS = ADR_PARENT + PREFIX + "adrDeletedDocuments";
    }

    public interface Types {
        QName ADR_DELETED_DOCUMENT = QName.createQName(URI, "adrDeletedDocument");
    }

    public interface Props {
        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");
        QName DELETED_DATE_TIME = QName.createQName(URI, "deletedDateTime");
    }

}
