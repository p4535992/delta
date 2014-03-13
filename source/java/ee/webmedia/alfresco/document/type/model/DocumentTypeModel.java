package ee.webmedia.alfresco.document.type.model;

import org.alfresco.service.namespace.QName;

public interface DocumentTypeModel {
    String URI = "http://alfresco.webmedia.ee/model/documenttype/1.0";
    String PREFIX = "doctype:";

    public interface Repo {
        String DOCUMENT_TYPES_PARENT = "/";
        String DOCUMENT_TYPES_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "documentTypes";
    }

    public interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName PUBLIC_ADR = QName.createQName(URI, "publicAdr");
        QName SELECTED = QName.createQName(URI, "selected");
    }

    public interface Types {
        QName SELECTOR = QName.createQName(URI, "selector");
    }

}
