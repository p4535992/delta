package ee.webmedia.alfresco.workflow.search.model;

import org.alfresco.service.namespace.QName;

/**
 * Compound Workflow search filter model QNames.
 */
public interface CompoundWorkflowSearchModel {

    String URI = "http://alfresco.webmedia.ee/model/cw/search/1.0";
    String PREFIX = "cwsearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "cwSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName CW_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "cwSearchFiltersContainer");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName TYPE = QName.createQName(URI, "type");
        QName TITLE = QName.createQName(URI, "title");
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName STRUCT_UNIT = QName.createQName(URI, "structUnit");
        QName JOB_TITLE = QName.createQName(URI, "jobTitle");
        QName CREATED_DATE = QName.createQName(URI, "createdDate");
        QName CREATED_DATE_END = QName.createQName(URI, "createdDate_EndDate");
        QName IGNITION_DATE = QName.createQName(URI, "ignitionDate");
        QName IGNITION_DATE_END = QName.createQName(URI, "ignitionDate_EndDate");
        QName STOPPED_DATE = QName.createQName(URI, "stoppedDate");
        QName STOPPED_DATE_END = QName.createQName(URI, "stoppedDate_EndDate");
        QName ENDING_DATE = QName.createQName(URI, "endingDate");
        QName ENDING_DATE_END = QName.createQName(URI, "endingDate_EndDate");
        QName STATUS = QName.createQName(URI, "status");
        /**
         * Not used in search any more (compound workflow comments were moved to delta_compound_workflow_comment table)
         */
        @Deprecated
        QName COMMENT = QName.createQName(URI, "comment");
    }

}
