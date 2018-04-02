package ee.webmedia.alfresco.volume.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public interface VolumeModel {
    String VOLUME_MODEL_URI = "http://alfresco.webmedia.ee/model/volume/1.0";
    String URI = DocumentDynamicModel.URI;

    interface Aspects {
        QName VOLUME_ASSOCIATIONS = QName.createQName(VOLUME_MODEL_URI, "volumeAssociations");
    }

    interface Types {
        QName VOLUME = QName.createQName(VOLUME_MODEL_URI, "volume");
        QName DELETED_DOCUMENT = QName.createQName(VOLUME_MODEL_URI, "deletedDocument");
    }

    interface Associations {
        QName VOLUME = QName.createQName(VOLUME_MODEL_URI, "volume");
        QName DELETED_DOCUMENT = QName.createQName(VOLUME_MODEL_URI, "deletedDocument");
        QName VOLUME_VOLUME = QName.createQName(VOLUME_MODEL_URI, "volumeVolume");
        QName VOLUME_DOCUMENT = QName.createQName(VOLUME_MODEL_URI, "volumeDocument");
        QName VOLUME_CASE = QName.createQName(VOLUME_MODEL_URI, "volumeCase");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName TITLE = QName.createQName(URI, "title");
        QName DESCRIPTION = QName.createQName(URI, "description");
        QName VOLUME_TYPE = QName.createQName(URI, "volumeType");
        QName VOLUME_MARK = QName.createQName(URI, "volumeMark");
        QName MARK = QName.createQName(URI, "volumeMark");
        QName VALID_FROM = QName.createQName(URI, "validFrom");
        QName VALID_TO = QName.createQName(URI, "validTo");
        QName CONTAINING_DOCS_COUNT = QName.createQName(URI, "containingDocsCount");
        QName CONTAINS_CASES = QName.createQName(URI, "containsCases");
        QName CASES_CREATABLE_BY_USER = QName.createQName(URI, "casesCreatableByUser");
        QName LOCATION = QName.createQName(URI, "location");

        QName ACTOR = QName.createQName(URI, "actor");
        QName DELETED_DATE_TIME = QName.createQName(URI, "deletedDateTime");
        QName DOCUMENT_DATA = QName.createQName(URI, "documentData");
        QName COMMENT = QName.createQName(URI, "comment");
        QName ORIGINAL_VOLUME = QName.createQName(URI, "originalVolume");
        QName MARKED_FOR_ARCHIVING = QName.createQName(URI, "markedForArchiving");
        QName MARKED_FOR_DESTRUCTING = QName.createQName(URI, "markedForDestructing");
        QName DELETION_TYPE = QName.createQName(URI, "deletionType");

        QName VOL_SHORT_REG_NUMBER = QName.createQName(URI, "volShortRegNumber");
    }
}
