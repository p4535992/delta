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
    }

}
