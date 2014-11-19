<<<<<<< HEAD
package ee.webmedia.alfresco.ocr.model;

import org.alfresco.service.namespace.QName;

public interface OcrModel {
    String URI = "http://alfresco.webmedia.ee/model/ocr/1.0";
    String PREFIX = "ocr:";

    public interface Aspects {
        QName OCR_COMPLETED = QName.createQName(URI, "ocrCompleted");
    }

    public interface Props {
        QName OCR_LOG = QName.createQName(URI, "ocrLog");
        QName OCR_STARTED_DATE_TIME = QName.createQName(URI, "ocrStartedDateTime");
        QName OCR_COMPLETED_DATE_TIME = QName.createQName(URI, "ocrCompletedDateTime");
    }

}
=======
package ee.webmedia.alfresco.ocr.model;

import org.alfresco.service.namespace.QName;

public interface OcrModel {
    String URI = "http://alfresco.webmedia.ee/model/ocr/1.0";
    String PREFIX = "ocr:";

    public interface Aspects {
        QName OCR_COMPLETED = QName.createQName(URI, "ocrCompleted");
    }

    public interface Props {
        QName OCR_LOG = QName.createQName(URI, "ocrLog");
        QName OCR_STARTED_DATE_TIME = QName.createQName(URI, "ocrStartedDateTime");
        QName OCR_COMPLETED_DATE_TIME = QName.createQName(URI, "ocrCompletedDateTime");
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
