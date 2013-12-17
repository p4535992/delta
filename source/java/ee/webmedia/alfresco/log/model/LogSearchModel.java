package ee.webmedia.alfresco.log.model;

import org.alfresco.service.namespace.QName;

public interface LogSearchModel {

    String URI = "http://alfresco.webmedia.ee/model/log/1.0";

    interface Types {
        QName LOG_FILTER = QName.createQName(URI, "logFilter");
    }

    interface Props {
        QName LOG_ENTRY_ID = QName.createQName(URI, "logEntryId");
        QName DATE_CREATED_START = QName.createQName(URI, "dateCreatedStart");
        QName DATE_CREATED_END = QName.createQName(URI, "dateCreatedStart_EndDate");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName CREATOR_ID = QName.createQName(URI, "creatorId");
        QName OBJECT_NAME = QName.createQName(URI, "objectName");
        QName OBJECT_ID = QName.createQName(URI, "objectId");
        QName COMPUTER_ID = QName.createQName(URI, "computerId");
        QName DESCRIPTION = QName.createQName(URI, "description");
    }
}
