package ee.webmedia.alfresco.series.model;

import org.alfresco.service.namespace.QName;

public interface SeriesModel {
    String URI = "http://alfresco.webmedia.ee/model/series/1.0";

    interface Types {
        QName SERIES = QName.createQName(URI, "series");
    }

    interface Associations {
        QName SERIES = QName.createQName(URI, "series");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName ORDER = QName.createQName(URI, "order");
        QName SERIES_IDENTIFIER = QName.createQName(URI, "seriesIdentifier");
        QName TITLE = QName.createQName(URI, "title");
    }
}
