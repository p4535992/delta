package ee.webmedia.alfresco.volume.search.model;

import org.alfresco.service.namespace.QName;

public interface VolumeSearchModel {
    String URI = "http://alfresco.webmedia.ee/model/volume/search/1.0";
    String PREFIX = "volumesearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "volumeSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
        QName ARCHIVE_LIST_FILTER = QName.createQName(URI, "archiveListFilter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName VOLUME_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "volumeSearchFiltersContainer");
        QName PLANNED_REVIEW = QName.createQName(URI, "plannedReview");
        QName PLANNED_TRANSFER = QName.createQName(URI, "plannedTransfer");
        QName PLANNED_DESTRUCTION = QName.createQName(URI, "plannedDestruction");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName STORE = QName.createQName(URI, "store");
        QName INPUT = QName.createQName(URI, "input");
        QName VOLUME_TYPE = QName.createQName(URI, "volumeType");
        QName CASE_FILE_TYPE = QName.createQName(URI, "caseFileType");

        // archiveListFiler specific fields
        QName VALID_TO = QName.createQName(URI, "validTo");
        QName VALID_TO_END_DATE = QName.createQName(URI, "validTo_EndDate");
        QName EVENT_PLAN = QName.createQName(URI, "eventPlan");
        QName HAS_ARCHIVAL_VALUE = QName.createQName(URI, "hasArchivalValue");
        QName STATUS = QName.createQName(URI, "status");
        QName NEXT_EVENT_DATE = QName.createQName(URI, "nextEventDate");
        QName NEXT_EVENT_DATE_END_DATE = QName.createQName(URI, "nextEventDate_EndDate");
        QName RETAIN_PERMANENT = QName.createQName(URI, "retainPermanent");
        QName RETAIN_UNTIL_DATE = QName.createQName(URI, "retainUntilDate");
        QName RETAIN_UNTIL_DATE_END_DATE = QName.createQName(URI, "retainUntilDate_EndDate");
        QName MARKED_FOR_TRANSFER = QName.createQName(URI, "markedForTransfer");
        QName EXPORTED_FOR_UAM = QName.createQName(URI, "exportedForUam");
        QName EXPORTED_FOR_UAM_DATE_TIME = QName.createQName(URI, "exportedForUamDateTime");
        QName EXPORTED_FOR_UAM_DATE_TIME_END_DATE = QName.createQName(URI, "exportedForUamDateTime_EndDate");
        QName NEXT_EVENT = QName.createQName(URI, "nextEvent");
        QName MARKED_FOR_DESTRUCTION = QName.createQName(URI, "markedForDestruction");
        QName DISPOSAL_ACT_CREATED = QName.createQName(URI, "disposalActCreated");
        QName IS_APPRAISED = QName.createQName(URI, "isAppraised");
        QName TRANSFER_CONFIRMED = QName.createQName(URI, "transferConfirmed");
    }

}
