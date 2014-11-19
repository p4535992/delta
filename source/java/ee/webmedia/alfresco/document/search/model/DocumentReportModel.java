<<<<<<< HEAD
package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Riina Tens
 */
public interface DocumentReportModel {

    String URI = "http://alfresco.webmedia.ee/model/document/report/1.0";
    String PREFIX = "docreport:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "documentReportFilters";
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
        QName DOCUMENT_REPORT_FILTERS_CONTAINER = QName.createQName(URI, "documentReportFiltersContainer");
        /** Object can contain 0 to 1 filters */
        QName DOCUMENT_REPORT_FILTER_CONTAINER = QName.createQName(URI, "documentReportFilterContainer");
    }

    interface Props {
        QName REPORT_OUTPUT_TYPE = QName.createQName(URI, "reportOutputType");
        QName REPORT_TEMPLATE = QName.createQName(URI, "reportTemplate");
    }
}
=======
package ee.webmedia.alfresco.document.search.model;

import org.alfresco.service.namespace.QName;

public interface DocumentReportModel {

    String URI = "http://alfresco.webmedia.ee/model/document/report/1.0";
    String PREFIX = "docreport:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "documentReportFilters";
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
        QName DOCUMENT_REPORT_FILTERS_CONTAINER = QName.createQName(URI, "documentReportFiltersContainer");
        /** Object can contain 0 to 1 filters */
        QName DOCUMENT_REPORT_FILTER_CONTAINER = QName.createQName(URI, "documentReportFilterContainer");
    }

    interface Props {
        QName REPORT_OUTPUT_TYPE = QName.createQName(URI, "reportOutputType");
        QName REPORT_TEMPLATE = QName.createQName(URI, "reportTemplate");
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
