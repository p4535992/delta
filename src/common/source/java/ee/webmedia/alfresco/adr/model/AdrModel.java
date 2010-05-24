package ee.webmedia.alfresco.adr.model;

import org.alfresco.service.namespace.QName;

public interface AdrModel {
    String URI = "http://alfresco.webmedia.ee/model/adr/1.0";
    String PREFIX = "adr:";

    public interface Repo {
        final static String ADR_PARENT = "/";
        final static String ADR_DELETED_DOCUMENTS = ADR_PARENT + PREFIX + "adrDeletedDocuments";
        final static String ADR_DELETED_DOCUMENT_TYPES = ADR_PARENT + PREFIX + "adrDeletedDocumentTypes";
        final static String ADR_ADDED_DOCUMENT_TYPES = ADR_PARENT + PREFIX + "adrAddedDocumentTypes";
    }

    public interface Types {
        QName ADR_DELETED_DOCUMENT = QName.createQName(URI, "adrDeletedDocument");
        QName ADR_DELETED_DOCUMENT_TYPE = QName.createQName(URI, "adrDeletedDocumentType");
        QName ADR_ADDED_DOCUMENT_TYPE = QName.createQName(URI, "adrAddedDocumentType");
    }

    public interface Props {
        QName REG_NUMBER = QName.createQName(URI, "regNumber");
        QName REG_DATE_TIME = QName.createQName(URI, "regDateTime");
        QName DELETED_DATE_TIME = QName.createQName(URI, "deletedDateTime");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
    }

}
