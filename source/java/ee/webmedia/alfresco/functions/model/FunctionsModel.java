package ee.webmedia.alfresco.functions.model;

import org.alfresco.service.namespace.QName;

public interface FunctionsModel {
    String URI = "http://alfresco.webmedia.ee/model/functions/1.0";
    String NAMESPACE_PREFFIX = "fn:";

    interface Types {
        QName FUNCTIONS_ROOT = QName.createQName(URI, "functions");
        QName FUNCTION = QName.createQName(URI, "function");
    }

    public interface Repo {
        String FUNCTIONS_PARENT = "/";
        String FUNCTIONS_ROOT = "documentList";
        String FUNCTIONS_SPACE = FUNCTIONS_PARENT + NAMESPACE_PREFFIX + FUNCTIONS_ROOT;
    }

    interface Associations {
        QName FUNCTION = QName.createQName(URI, "function");
    }

    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName ORDER = QName.createQName(URI, "order");
        QName TITLE = QName.createQName(URI, "title");
        QName MARK = QName.createQName(URI, "mark");
        QName TYPE = QName.createQName(URI, "type");
        QName DESCRIPTION = QName.createQName(URI, "description");
        QName DOCUMENT_ACTIVITIES_ARE_LIMITED = QName.createQName(URI, "documentActivitiesAreLimited");
    }

}
