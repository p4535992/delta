package ee.webmedia.alfresco.docdynamic.model;

import org.alfresco.service.namespace.QName;

public interface DocumentChildModel {
    String URI = "http://alfresco.webmedia.ee/model/document/child/1.0";
    String PREFIX = "docchild:";

    QName MODEL = QName.createQName(URI, "documentChildModel");

    /**
     * NB! child-association and it's target type must have exactly the same name
     */
    interface Assocs {
        QName CONTRACT_PARTY = QName.createQName(URI, "contractParty");
        QName APPLICANT_ABROAD = QName.createQName(URI, "applicantAbroad");
        QName ERRAND_ABROAD = QName.createQName(URI, "errandAbroad");
        QName APPLICANT_DOMESTIC = QName.createQName(URI, "applicantDomestic");
        QName ERRAND_DOMESTIC = QName.createQName(URI, "errandDomestic");
        QName APPLICANT_TRAINING = QName.createQName(URI, "applicantTraining");
        QName APPLICANT_ERRAND = QName.createQName(URI, "applicantErrand");
        QName ERRAND = QName.createQName(URI, "errand");
        QName[] ALL_ASSOCS = new QName[] { CONTRACT_PARTY, APPLICANT_ABROAD, ERRAND_ABROAD, APPLICANT_DOMESTIC, ERRAND_DOMESTIC, APPLICANT_TRAINING, APPLICANT_ERRAND, ERRAND };
    }

    interface Aspects {
        QName CONTRACT_PARTY_CONTAINER = QName.createQName(URI, "contractPartyContainer");
    }

}
