package ee.webmedia.alfresco.substitute.model;

import org.alfresco.service.namespace.QName;

<<<<<<< HEAD
/**
 * @author Romet Aidla
 */
=======
>>>>>>> develop-5.1
public interface SubstituteModel {
    String URI = "http://alfresco.webmedia.ee/model/substitute/1.0";
    String NAMESPACE_PREFFIX = "sub:";

    interface Repo {
        // final static String SUBSTITUTES_PARENT = "/";
        // final static String SUBSTITUTES_SPACE = SUBSTITUTES_PARENT + NAMESPACE_PREFFIX + Types.SUBSTITUTES_ROOT.getLocalName();
    }

    interface Types {
        QName SUBSTITUTES = QName.createQName(URI, "substitutes");
        QName SUBSTITUTE = QName.createQName(URI, "substitute");
    }

    interface Aspects {
        QName SUBSTITUTES = QName.createQName(URI, "substitutable");
    }

    interface Associations {
        QName SUBSTITUTES = QName.createQName(URI, "substitutes");
        QName SUBSTITUTE = QName.createQName(URI, "substitute");
    }

    public interface Props {
        QName SUBSTITUTE_NAME = QName.createQName(URI, "substituteName");
        QName SUBSTITUTE_ID = QName.createQName(URI, "substituteId");
        QName SUBSTITUTION_START_DATE = QName.createQName(URI, "substitutionStartDate");
        QName SUBSTITUTION_END_DATE = QName.createQName(URI, "substitutionEndDate");
    }
}
