<<<<<<< HEAD
package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    public void addParty(final Node docNode) {
        addParty(docNode, null);
    }

    public void addParty(final Node docNode, Map<QName, Serializable> partyProps) {
        final QName partyType = getPartyType(docNode);
        final WmNode newParty = generalService.createNewUnSaved(partyType, partyProps);
        docNode.addChildAssociations(getPartyAssoc(docNode), newParty);
    }

    public void removeParty(final Node docNode, final int assocIndex) {
        final QName partyAssocs = getPartyAssoc(docNode);
        docNode.removeChildAssociations(partyAssocs, assocIndex);
    }

    public void removeParties(final Node docNode, List<Node> removedAssocs) {
        final QName partyAssocs = getPartyAssoc(docNode);
        docNode.removeChildAssociations(partyAssocs, removedAssocs);
    }

    private QName getApplicantType(Node docNode) {
        final QName applicantType;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType()) && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(docNode.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2;

        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + docNode.getType() + "'");
        }
        return applicantType;
    }

    private QName getPartyType(Node docNode) {
        final QName partyType;
        if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNode.getType())) {
            partyType = DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(docNode.getType())
                || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(docNode.getType())) {
            partyType = DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE;
        } else {
            throw new RuntimeException("Unimplemented adding party to document with type '" + docNode.getType() + "'");
        }
        return partyType;
    }

    private QName getApplicantAssoc(Node document) {
        final QName applicantAssoc;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType()) && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + document.getType() + "'");
        }
        return applicantAssoc;
    }

    private QName getPartyAssoc(Node docNode) {
        final QName partyAssoc;
        if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNode.getType())) {
            partyAssoc = DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(docNode.getType())
                || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(docNode.getType())) {
            partyAssoc = DocumentSpecificModel.Assocs.CONTRACT_PARTIES;
        } else {
            throw new RuntimeException("Unimplemented adding party to document with type '" + docNode.getType() + "'");
        }
        return partyAssoc;
    }

    private QName getErrandAssocType(final Node applicantNode) {
        final QName errandAssocType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2;
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandAssocType;
    }

    private QName getErrandType(final Node applicantNode, Node document) {
        final QName errandType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE_V2;
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
=======
package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

/**
 * Helps to add/remove applicant childNodes to document and errands to application
 * Helper/service class that doesn't use repository.
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

    public void addParty(final Node docNode) {
        addParty(docNode, null);
    }

    public void addParty(final Node docNode, Map<QName, Serializable> partyProps) {
        final QName partyType = getPartyType(docNode);
        final WmNode newParty = generalService.createNewUnSaved(partyType, partyProps);
        docNode.addChildAssociations(getPartyAssoc(docNode), newParty);
    }

    public void removeParty(final Node docNode, final int assocIndex) {
        final QName partyAssocs = getPartyAssoc(docNode);
        docNode.removeChildAssociations(partyAssocs, assocIndex);
    }

    public void removeParties(final Node docNode, List<Node> removedAssocs) {
        final QName partyAssocs = getPartyAssoc(docNode);
        docNode.removeChildAssociations(partyAssocs, removedAssocs);
    }

    private QName getApplicantType(Node docNode) {
        final QName applicantType;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType()) && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(docNode.getType())) {
            applicantType = DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
            applicantType = DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(docNode.getType())
                && docNode.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
            applicantType = DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2;

        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + docNode.getType() + "'");
        }
        return applicantType;
    }

    private QName getPartyType(Node docNode) {
        final QName partyType;
        if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNode.getType())) {
            partyType = DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(docNode.getType())
                || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(docNode.getType())) {
            partyType = DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE;
        } else {
            throw new RuntimeException("Unimplemented adding party to document with type '" + docNode.getType() + "'");
        }
        return partyType;
    }

    private QName getApplicantAssoc(Node document) {
        final QName applicantAssoc;
        if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType()) && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV.equals(document.getType())) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.ERRAND_APPLICATION_DOMESTIC_APPLICANTS_V2;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION)) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(document.getType())
                && document.hasAspect(DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2)) {
            applicantAssoc = DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2;
        } else {
            throw new RuntimeException("Unimplemented adding applicant to document with type '" + document.getType() + "'");
        }
        return applicantAssoc;
    }

    private QName getPartyAssoc(Node docNode) {
        final QName partyAssoc;
        if (DocumentSubtypeModel.Types.CONTRACT_MV.equals(docNode.getType())) {
            partyAssoc = DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES;
        } else if (DocumentSubtypeModel.Types.CONTRACT_SIM.equals(docNode.getType())
                || DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(docNode.getType())) {
            partyAssoc = DocumentSpecificModel.Assocs.CONTRACT_PARTIES;
        } else {
            throw new RuntimeException("Unimplemented adding party to document with type '" + docNode.getType() + "'");
        }
        return partyAssoc;
    }

    private QName getErrandAssocType(final Node applicantNode) {
        final QName errandAssocType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2.equals(applicantNode.getType())) {
            errandAssocType = DocumentSpecificModel.Assocs.ERRAND_DOMESTIC_V2;
        } else {
            throw new RuntimeException("Unimplemented adding errand to applicant with type '" + applicantNode.getType() + "'");
        }
        return errandAssocType;
    }

    private QName getErrandType(final Node applicantNode, Node document) {
        final QName errandType;
        if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2;
        } else if (DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE;
        } else if (DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2.equals(applicantNode.getType())) {
            errandType = DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE_V2;
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}