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
        /** n:m relation (case:case -> doccom:document) */
        QName CASE_DOCUMENT = QName.createQName(URI, "caseDocument");
    }

    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName TITLE = QName.createQName(URI, "title");
        QName CONTAINING_DOCS_COUNT = QName.createQName(URI, "containingDocsCount");
    }
}