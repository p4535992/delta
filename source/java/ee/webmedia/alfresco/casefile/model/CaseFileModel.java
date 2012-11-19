package ee.webmedia.alfresco.casefile.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Kaarel Jõgeva
 */
public interface CaseFileModel {
    String URI = "http://alfresco.webmedia.ee/model/casefile/1.0";

    interface Types {
        QName CASE_FILE = QName.createQName(URI, "caseFile");
    }

    interface Assocs {
        QName CASE_FILE = QName.createQName(URI, "caseFile");
        QName CASE = QName.createQName(URI, "case");
        QName FAVORITE = QName.createQName(URI, "favorite");
        QName CASE_FILE_DOCUMENT = QName.createQName(URI, "caseFileDocument");
        QName CASE_FILE_VOLUME = QName.createQName(URI, "caseFileVolume");
        QName CASE_FILE_CASE_FILE = QName.createQName(URI, "caseFileCaseFile");
        QName CASE_FILE_CASE = QName.createQName(URI, "caseFileCase");
    }

    interface Aspects {
        QName CASE_FILE_CONTAINER = QName.createQName(URI, "caseFileContainer");
        QName FAVORITE = QName.createQName(URI, "favorite");
    }
}