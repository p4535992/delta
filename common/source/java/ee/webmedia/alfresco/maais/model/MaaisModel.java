package ee.webmedia.alfresco.maais.model;

import org.alfresco.service.namespace.QName;

public interface MaaisModel {
    String URI = "http://alfresco.webmedia.ee/model/maais/1.0";

    public interface Types {
        QName MAAIS_CASE = QName.createQName(URI, "maaisCase");
    }

    interface Associations {
        /** n:m relation (maais:maaisCase -> doccom:document) */
        QName MAAIS_CASE_DOCUMENT = QName.createQName(URI, "maaisCaseDocument");
    }

    interface Repo {
        String MAAIS_CASES_XPATH = "/maaisCases";
    }

    interface Aspects {
        QName MAAIS_NOTIFY_ASSOC = QName.createQName(URI, "maaisNotifyAssoc");
    }

    public interface Props {
        QName CASE_NUMBER = QName.createQName(URI, "caseNumber");
        QName CASE_RELATED_PERSON = QName.createQName(URI, "caseRelatedPerson");
        QName LAND_NUMBER = QName.createQName(URI, "landNumber");
        QName LAND_NAME = QName.createQName(URI, "landName");
    }
}
