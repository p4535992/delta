package ee.webmedia.alfresco.cases.model;

import org.alfresco.service.namespace.QName;

/**
 * Constants of caseModel.xml
 * 
 * @author Ats Uiboupin
 */
public interface CaseModel {
    String URI = "http://alfresco.webmedia.ee/model/case/1.0";

    interface Types {
        QName CASE = QName.createQName(URI, "case");
    }
    
    interface Associations {
        QName CASE = QName.createQName(URI, "case");
    }

    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
    }
}
