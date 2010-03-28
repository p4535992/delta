package ee.webmedia.alfresco.workflow.search.model;

import org.alfresco.service.namespace.QName;

/**
 * Task search filter model QNames.
 * 
 * @author Erko Hansar
 */
public interface TaskSearchModel {
    
    String URI = "http://alfresco.webmedia.ee/model/task/search/1.0";
    String PREFIX = "tasksearch:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "taskSearchFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Aspects {
        QName TASK_SEARCH_FILTERS_CONTAINER = QName.createQName(URI, "taskSearchFiltersContainer");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName STARTED_DATE_TIME_BEGIN = QName.createQName(URI, "startedDateTimeBegin");
        QName STARTED_DATE_TIME_END = QName.createQName(URI, "startedDateTimeEnd");
        QName TASK_TYPE = QName.createQName(URI, "taskType");
        QName OWNER_NAME = QName.createQName(URI, "ownerName");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName ORGANIZATION_NAME = QName.createQName(URI, "organizationName");
        QName JOB_TITLE = QName.createQName(URI, "jobTitle");
        QName DUE_DATE_TIME_BEGIN = QName.createQName(URI, "dueDateTimeBegin");
        QName DUE_DATE_TIME_END = QName.createQName(URI, "dueDateTimeEnd");
        QName ONLY_RESPONSIBLE = QName.createQName(URI, "onlyResponsible");
        QName COMPLETED_DATE_TIME_BEGIN = QName.createQName(URI, "completedDateTimeBegin");
        QName COMPLETED_DATE_TIME_END = QName.createQName(URI, "completedDateTimeEnd");
        QName COMMENT = QName.createQName(URI, "comment");
        QName RESOLUTION = QName.createQName(URI, "resolution");
        QName STATUS = QName.createQName(URI, "status");
        QName COMPLETED_OVERDUE = QName.createQName(URI, "completedOverdue");
        QName STOPPED_DATE_TIME_BEGIN = QName.createQName(URI, "stoppedDateTimeBegin");
        QName STOPPED_DATE_TIME_END = QName.createQName(URI, "stoppedDateTimeEnd");
    }

}
