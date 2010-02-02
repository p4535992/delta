package ee.webmedia.alfresco.dvk.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 *
 */
public interface DvkModel {
    String URI = "http://alfresco.webmedia.ee/model/dvk/1.0";

    interface Types {
        QName ORG_LIST_ROOT = QName.createQName(URI, "orgListRoot");
        QName ORGANIZATION = QName.createQName(URI, "organization");
    }

    interface Aspects {
        QName RECEIVED_DVK_DOCUMENT = QName.createQName(URI, "receivedDvkDocument");
        QName ACCESS_RIGHTS = QName.createQName(URI, "accessRights");
    }

    interface Props {
        QName DVK_ID = QName.createQName(URI, "dvkId");
    }
}
