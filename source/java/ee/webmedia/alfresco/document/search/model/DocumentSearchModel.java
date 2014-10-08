<<<<<<< HEAD
package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentSearchModel {
    String URI = "http://alfresco.webmedia.ee/model/document/search/1.0";
    String PREFIX = "docsearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "documentSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
        QName OBJECT_FILTER = QName.createQName(URI, "objectFilter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName DOCUMENT_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "documentSearchFiltersContainer");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName STORE = QName.createQName(URI, "store");
        QName INPUT = QName.createQName(URI, "input");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName SEND_MODE = QName.createQName(URI, "sendMode");
        QName FUND = QName.createQName(URI, "fund");
        QName FUNDS_CENTER = QName.createQName(URI, "fundsCenter");
        QName EA_COMMITMENT_ITEM = QName.createQName(URI, "eaCommitmentItem");
        QName DOCUMENT_CREATED = QName.createQName(URI, "documentCreated");
        QName DOCUMENT_CREATED_END_DATE = QName.createQName(URI, "documentCreated_EndDate");

        // objectFilter properties
        QName OBJECT_TYPE = QName.createQName(URI, "objectType");
        QName OBJECT_TITLE = QName.createQName(URI, "objectTitle");
    }

}
=======
package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

public interface DocumentSearchModel {
    String URI = "http://alfresco.webmedia.ee/model/document/search/1.0";
    String PREFIX = "docsearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "documentSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
        QName OBJECT_FILTER = QName.createQName(URI, "objectFilter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName DOCUMENT_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "documentSearchFiltersContainer");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName STORE = QName.createQName(URI, "store");
        QName INPUT = QName.createQName(URI, "input");
        QName DOCUMENT_TYPE = QName.createQName(URI, "documentType");
        QName SEND_MODE = QName.createQName(URI, "sendMode");
        QName FUND = QName.createQName(URI, "fund");
        QName FUNDS_CENTER = QName.createQName(URI, "fundsCenter");
        QName EA_COMMITMENT_ITEM = QName.createQName(URI, "eaCommitmentItem");
        QName DOCUMENT_CREATED = QName.createQName(URI, "documentCreated");
        QName DOCUMENT_CREATED_END_DATE = QName.createQName(URI, "documentCreated_EndDate");
        QName SEND_INFO_RECIPIENT = QName.createQName(URI, "sendInfoRecipient");
        QName SEND_INFO_SEND_DATE_TIME = QName.createQName(URI, "sendInfoDateTime");
        QName SEND_INFO_SEND_DATE_TIME_END = QName.createQName(URI, "sendInfoDateTime_EndDate");
        QName SEND_INFO_RESOLUTION = QName.createQName(URI, "sendInfoResolution");

        // objectFilter properties
        QName OBJECT_TYPE = QName.createQName(URI, "objectType");
        QName OBJECT_TITLE = QName.createQName(URI, "objectTitle");
    }

}
>>>>>>> develop-5.1
