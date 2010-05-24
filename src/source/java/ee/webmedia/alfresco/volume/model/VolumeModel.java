package ee.webmedia.alfresco.volume.model;

import org.alfresco.service.namespace.QName;

public interface VolumeModel {
    String URI = "http://alfresco.webmedia.ee/model/volume/1.0";

    interface Types {
        QName VOLUME = QName.createQName(URI, "volume");
    }
    
    interface Associations {
        QName VOLUME = QName.createQName(URI, "volume");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName DISPOSITION_DATE = QName.createQName(URI, "dispositionDate");
        QName TITLE = QName.createQName(URI, "title");
        QName VOLUME_TYPE = QName.createQName(URI, "volumeType");
        QName VOLUME_MARK = QName.createQName(URI, "volumeMark");
        QName MARK = QName.createQName(URI, "volumeMark");
        QName VALID_FROM = QName.createQName(URI, "validFrom");
        QName VALID_TO = QName.createQName(URI, "validTo");
        QName ARCHIVING_NOTE = QName.createQName(URI, "archivingNote");
        QName SEND_TO_DESTRUCTION = QName.createQName(URI, "sendToDestruction");
        QName CONTAINING_DOCS_COUNT = QName.createQName(URI, "containingDocsCount");
    }
}
