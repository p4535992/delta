package ee.webmedia.alfresco.series.model;

import org.alfresco.service.namespace.QName;

public interface SeriesModel {
    String URI = "http://alfresco.webmedia.ee/model/series/1.0";

    interface Types {
        QName SERIES = QName.createQName(URI, "series");
    }

    interface Associations {
        QName SERIES = QName.createQName(URI, "series");
        QName SERIES_LOG = QName.createQName(URI, "seriesLog");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName ORDER = QName.createQName(URI, "order");
        QName SERIES_IDENTIFIER = QName.createQName(URI, "seriesIdentifier");
        QName TITLE = QName.createQName(URI, "title");
        QName REGISTER = QName.createQName(URI, "register");
        QName INDIVIDUALIZING_NUMBERS = QName.createQName(URI, "individualizingNumbers");
        QName STRUCT_UNIT = QName.createQName(URI, "structUnit");
        QName CONTAINING_DOCS_COUNT = QName.createQName(URI, "containingDocsCount");
        QName TYPE = QName.createQName(URI, "type");
        QName DOC_TYPE = QName.createQName(URI, "docType");
        QName RETENTION_PERIOD = QName.createQName(URI, "retentionPeriod");
    }
}
