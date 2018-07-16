package ee.webmedia.alfresco.help.model;

import org.alfresco.service.namespace.QName;

public interface HelpTextModel {

    String URI = "http://alfresco.webmedia.ee/model/helpText/1.0";

    QName ROOT = QName.createQName(URI, "helpTexts");

    interface Types {
        QName HELP_TEXT = QName.createQName(URI, "helpText");
    }

    interface Props {
        QName TYPE = QName.createQName(URI, "type");
        QName CODE = QName.createQName(URI, "code");
        QName NAME = QName.createQName(URI, "name");
        QName CONTENT = QName.createQName(URI, "content");
    }
}
