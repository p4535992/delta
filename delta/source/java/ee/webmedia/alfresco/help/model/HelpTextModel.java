package ee.webmedia.alfresco.help.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface HelpTextModel {

    String URI = "http://alfresco.webmedia.ee/model/helpText/1.0";

    QName ROOT = QName.createQName(URI, "helpTexts");

    public interface Types {
        QName HELP_TEXT = QName.createQName(URI, "helpText");
    }

    public interface Props {
        QName TYPE = QName.createQName(URI, "type");
        QName CODE = QName.createQName(URI, "code");
        QName CONTENT = QName.createQName(URI, "content");
    }
}
