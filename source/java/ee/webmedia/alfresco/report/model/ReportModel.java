package ee.webmedia.alfresco.report.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Riina Tens
 */
public interface ReportModel {
    String URI = "http://alfresco.webmedia.ee/model/report/1.0";
    String PREFIX = "report:";

    interface Repo {
        String REPORTS_PARENT = "/";
        String REPORTS_SPACE = REPORTS_PARENT + PREFIX + "reportsQueue";
    }

    interface Types {
        QName REPORTS_QUEUE_ROOT = QName.createQName(URI, "reportsQueue");
        QName REPORT_RESULT = QName.createQName(URI, "reportResult");
    }

    interface Assocs {
        QName REPORTS_QUEUE = QName.createQName(URI, "reportsQueue");
        QName REPORT_RESULT = QName.createQName(URI, "reportResult");
    }

    interface Aspects {
        QName REPORTS_QUEUE_CONTAINER = QName.createQName(URI, "reportsQueueContainer");
    }

    interface Props {
        QName USERNAME = QName.createQName(URI, "userName");
        QName REPORT_NAME = QName.createQName(URI, "reportName");
        QName REPORT_TYPE = QName.createQName(URI, "reportType");
        QName REPORT_OUTPUT_TYPE = QName.createQName(URI, "reportOutputType");
        QName USER_START_DATE_TIME = QName.createQName(URI, "userStartDateTime");
        QName RUN_START_DATE_TIME = QName.createQName(URI, "runStartDateTime");
        QName RUN_FINISH_START_TIME = QName.createQName(URI, "runFinishDateTime");
        QName CANCEL_DATE_TIME = QName.createQName(URI, "cancelDateTime");
        QName FIRST_DOWNLOAD_DATE_TIME = QName.createQName(URI, "firstDownloadDateTime");
        QName CSV_FUNCTION_STORE_NODE_REF = QName.createQName(URI, "nodeRef");
        /**
         * All values for this property are defined in Enum {@link ReportStatus}
         */
        QName STATUS = QName.createQName(URI, "status");
    }

}
