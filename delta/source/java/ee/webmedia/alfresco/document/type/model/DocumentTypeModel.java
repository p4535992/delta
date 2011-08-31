package ee.webmedia.alfresco.document.type.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentTypeModel {
    String URI = "http://alfresco.webmedia.ee/model/documenttype/1.0";
    String PREFIX = "doctype:";

    public interface Repo {
        String DOCUMENT_TYPES_PARENT = "/";
        String DOCUMENT_TYPES_SPACE = DOCUMENT_TYPES_PARENT + PREFIX + "documentTypes";
    }

    public interface Props {
        QName SELECTED = QName.createQName(URI, "selected");
        // START: properties of type documentType
        QName ID = QName.createQName(URI, "id");
        QName NAME = QName.createQName(URI, "name");
        QName USED = QName.createQName(URI, "used");
        QName COMMENT = QName.createQName(URI, "comment");
        QName SYSTEMATIC_COMMENT = QName.createQName(URI, "systematicComment");
        QName PUBLIC_ADR = QName.createQName(URI, "publicAdr");
        QName SHOW_UNVALUED = QName.createQName(URI, "showUnvalued");
        QName CHANGE_BY_NEW_DOCUMENT_ENABLED = QName.createQName(URI, "changeByNewDocumentEnabled");
        QName SYSTEMATIC = QName.createQName(URI, "systematic");
        QName DOCUMENT_TYPE_GROUP = QName.createQName(URI, "documentTypeGroup");
        QName REGISTRATION_ENABLED = QName.createQName(URI, "registrationEnabled");
        QName FINISH_DOC_BY_REGISTRATION = QName.createQName(URI, "finishDocByRegistration");
        QName SEND_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "sendUnregistratedDocEnabled");
        QName ADD_FOLLOW_UP_TO_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "addFollowUpToUnregistratedDocEnabled");
        QName ADD_REPLY_TO_UNREGISTRATED_DOC_ENABLED = QName.createQName(URI, "addReplyToUnregistratedDocEnabled");
        QName EDIT_FILES_OF_FINISHED_DOC_ENABLED = QName.createQName(URI, "editFilesOfFinishedDocEnabled");
        QName SHOW_VALIDITY_DATA = QName.createQName(URI, "showValidityData");
        QName LATEST_VERSION = QName.createQName(URI, "latestVersion");
        // END: properties of type documentType
    }

    public interface Assocs {
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
    }

    public interface Types {
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName SELECTOR = QName.createQName(URI, "selector");
    }

}
