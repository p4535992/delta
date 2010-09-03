package ee.webmedia.alfresco.document.service;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

/**
 * Helps to add/remove applicant childNodes to document and errands to application
 * Helper/service class that doesn't use repository.
 * 
 * @author Ats Uiboupin
 */
public class InMemoryChildNodeHelper {
    public static final String BEAN_NAME = "inMemoryChildNodeHelper";
    private GeneralService generalService;

    public void addApplicant(final Node docNode) {
        final WmNode newApplicant = generalService.createNewUnSaved(getApplicantType(docNode), null);
        docNode.addChildAssociations(getApplicantAssoc(docNode), newApplicant);
        addErrand(newApplicant, docNode);
    }

    public void removeApplicant(final Node docNode, final int assocIndex) {
        final QName applicantAssoc = getApplicantAssoc(docNode);
        docNode.removeChildAssociations(applicantAssoc, assocIndex);
    }

    public void removeErrand(final Node applicantNode, final int assocIndex) {
        applicantNode.removeChildAssociations(getErrandAssocType(applicantNode), assocIndex);
    }

    public void addErrand(final Node applicantNode, Node docNode) {
        final QName errandType = getErrandType(applicantNode, docNode);
        if (errandType == null) {
            return;
        }
        final WmNode newErrand = generalService.createNewUnSaved(errandType, null);
        applicantNode.addChildAssociations(getErrandAssocType(applicantNode), newErrand);
    }

    private QName getApplicantType(Node docNode) {
        final QName applicantType;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType())) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + docNode.getType() + "'");
        }
        return applicantType;
    }

    private QName getApplicantAssoc(Node document) {
        final QName applicantAssoc;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + document.getType() + "'");
        }
        return applicantAssoc;
    }

    private QName getErrandAssocType(final Node applicantNode) {
        final QName errandAssocType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandAssocType;
    }

    private QName getErrandType(final Node applicantNode, Node document) {
        final QName errandType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())) {
            return null; // trainingApplication document has applicant block, but not errand
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandType;
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: getters / setters
}