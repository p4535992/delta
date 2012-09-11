package ee.webmedia.alfresco.workflow.search.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Riina Tens
 */
public interface TaskReportModel {

    String URI = "http://alfresco.webmedia.ee/model/task/report/1.0";
    String PREFIX = "taskreport:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "taskReportFilters";
    }

    interface Types {
        QName FILTER = QName.createQName(URI, "filter");
    }

    interface Assocs {
        QName FILTER = QName.createQName(URI, "filter");
        QName FILTERS = QName.createQName(URI, "filters");
    }

    interface Aspects {
        /** Object can contain 0 to many filters */
        QName TASK_REPORT_FILTERS_CONTAINER = QName.createQName(URI, "taskReportFiltersContainer");
        /** Object can contain 0 to 1 filters */
        QName TASK_REPORT_FILTER_CONTAINER = QName.createQName(URI, "taskReportFilterContainer");
    }

    interface Props {
        QName REPORT_TEMPLATE = QName.createQName(URI, "reportTemplate");
    }
}
