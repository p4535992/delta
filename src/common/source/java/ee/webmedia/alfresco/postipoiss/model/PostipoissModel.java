package ee.webmedia.alfresco.postipoiss.model;

import org.alfresco.service.namespace.QName;

public interface PostipoissModel {
    String URI = "http://alfresco.webmedia.ee/tk/model/postipoiss/1.0";

    interface Aspects {
        QName FUNCTION = QName.createQName(URI, "function");
        QName SERIES = QName.createQName(URI, "series");
    }

    interface Props {
        QName FUNCTION_ID = QName.createQName(URI, "functionId");
        QName SERIES_ID = QName.createQName(URI, "seriesId");
        QName SERIES_MULTIPLE_YEARS = QName.createQName(URI, "seriesMultipleYears");
        QName SERIES_VALID_FROM = QName.createQName(URI, "seriesValidFrom");
        QName SERIES_VALID_TO = QName.createQName(URI, "seriesValidTo");
    }

}
