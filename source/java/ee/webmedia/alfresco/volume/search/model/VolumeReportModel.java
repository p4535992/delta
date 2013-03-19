package ee.webmedia.alfresco.volume.search.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Keit Tehvan
 */
public interface VolumeReportModel {

    String URI = "http://alfresco.webmedia.ee/model/volume/report/1.0";
    String PREFIX = "volreport:";

    public interface Repo {
        final static String FILTERS_PARENT = "/";
        final static String FILTERS_SPACE = FILTERS_PARENT + PREFIX + "volumeReportFilters";
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
        QName VOLUME_REPORT_FILTERS_CONTAINER = QName.createQName(URI, "volumeReportFiltersContainer");
        /** Object can contain 0 to 1 filters */
        QName VOLUME_REPORT_FILTER_CONTAINER = QName.createQName(URI, "volumeReportFilterContainer");
    }

    interface Props {
        QName REPORT_OUTPUT_TYPE = QName.createQName(URI, "reportOutputType");
        QName REPORT_TEMPLATE = QName.createQName(URI, "reportTemplate");
    }
}
