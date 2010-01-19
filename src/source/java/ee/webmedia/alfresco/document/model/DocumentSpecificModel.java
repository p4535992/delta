package ee.webmedia.alfresco.document.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface DocumentSpecificModel {
    String URI = "http://alfresco.webmedia.ee/model/document/specific/1.0";
    String PREFIX = "docspec:";

    QName MODEL = QName.createQName(URI, "documentSpecificModel");
    
    interface Aspects {
        QName SENDER = QName.createQName(URI, "sender");
        QName SENDER_DETAILS = QName.createQName(URI, "senderDetails");
        QName WHOM = QName.createQName(URI, "whom");
        QName WHOSE = QName.createQName(URI, "whose");
        QName TEMPLATE = QName.createQName(URI, "template");
        QName COMPLIENCE = QName.createQName(URI, "complience");
        QName DELIVERER = QName.createQName(URI, "deliverer");
        QName RECEIVER = QName.createQName(URI, "receiver");
        QName SECOND_PARTY_REG = QName.createQName(URI, "secondPartyReg");
        QName LICENCE_DETAILS = QName.createQName(URI, "licenceDetails");
        QName MANAGEMENTS_ORDER_DETAILS = QName.createQName(URI, "managementsOrderDetails");
        QName VACATION_ORDER = QName.createQName(URI, "vacationOrder");
        QName VACATION_ORDER_SMIT = QName.createQName(URI, "vacationOrderSmit");
    }

    interface Props {
        QName SENDER_REG_NUMBER = QName.createQName(URI, "senderRegNumber");
        QName SENDER_REG_DATE = QName.createQName(URI, "senderRegDate");
        
        QName SENDER_DETAILS_NAME = QName.createQName(URI, "senderName");
        QName SENDER_DETAILS_EMAIL = QName.createQName(URI, "senderEmail");
        
        QName WHOM_NAME = QName.createQName(URI, "whomName");
        QName WHOM_JOB_TITLE = QName.createQName(URI, "whomJobTitle");
        
        QName WHOSE_NAME = QName.createQName(URI, "whose");
        
        QName TEMPLATE_NAME = QName.createQName(URI, "templateName");
        QName RAPPORTEUR_NAME = QName.createQName(URI, "rapporteur");

        QName COMPLIENCE_DATE = QName.createQName(URI, "complienceDate");
        QName DUE_DATE = QName.createQName(URI, "dueDate");
        
        QName DELIVERER_NAME = QName.createQName(URI, "delivererName");
        QName DELIVERER_JOB_TITLE = QName.createQName(URI, "delivererJobTitle");
        QName DELIVERER_STRUCT_UNIT = QName.createQName(URI, "delivererStructUnit");
        
        QName RECEIVER_NAME = QName.createQName(URI, "receiverName");
        QName RECEIVER_JOB_TITLE = QName.createQName(URI, "receiverJobTitle");
        QName RECEIVER_STRUCT_UNIT = QName.createQName(URI, "receiverStructUnit");
        
        QName SECOND_PARTY_REG_NUMBER = QName.createQName(URI, "secondPartyRegNumber");
        QName SECOND_PARTY_REG_DATE = QName.createQName(URI, "secondPartyRegDate");
        
        QName RESPONSIBLE_NAME = QName.createQName(URI, "responsibleName");
        QName RESPONSIBLE_STRUCT_UNIT = QName.createQName(URI, "responsibleStructUnit");
        QName RESPONSIBLE_ORGANIZATION = QName.createQName(URI, "responsibleOrganization");
        
        QName FIRST_PARTY_NAME = QName.createQName(URI, "firstPartyName");
        QName RECIPIENT_NAME1 = QName.createQName(URI, "recipientName1");
        QName RECIPIENT_EMAIL1 = QName.createQName(URI, "recipientEmail1");
        QName RECIPIENT_NAME2 = QName.createQName(URI, "recipientName2");
        QName RECIPIENT_EMAIL2 = QName.createQName(URI, "recipientEmail2");
        QName COST_MANAGER = QName.createQName(URI, "costManager");
                
        QName VACATION_ADD = QName.createQName(URI, "vacationAdd");
        QName VACATION_CHANGE = QName.createQName(URI, "vacationChange");
        QName VACATION_SUBSTITUTE = QName.createQName(URI, "vacationSubstitute");
        
        QName LEAVE_ANNUAL = QName.createQName(URI, "leaveAnnual");
        QName LEAVE_ANNUAL_BEGIN_DATE = QName.createQName(URI, "leaveAnnualBeginDate");
        QName LEAVE_ANNUAL_END_DATE = QName.createQName(URI, "leaveAnnualEndDate");
        QName LEAVE_ANNUAL_DAYS = QName.createQName(URI, "leaveAnnualDays");
        
        QName LEAVE_WITHOUT_PAY = QName.createQName(URI, "leaveWithoutPay");
        QName LEAVE_WITHOUT_PAY_BEGIN_DATE = QName.createQName(URI, "leaveWithoutPayBeginDate");
        QName LEAVE_WITHOUT_PAY_END_DATE = QName.createQName(URI, "leaveWithoutPayEndDate");
        QName LEAVE_WITHOUT_PAY_DAYS = QName.createQName(URI, "leaveWithoutPayDays");
        
        QName LEAVE_CHILD = QName.createQName(URI, "leaveChild");
        QName LEAVE_CHILD_BEGIN_DATE = QName.createQName(URI, "leaveChildBeginDate");
        QName LEAVE_CHILD_END_DATE = QName.createQName(URI, "leaveChildEndDate");
        QName LEAVE_CHILD_DAYS = QName.createQName(URI, "leaveChildDays");
        
        QName LEAVE_STUDY = QName.createQName(URI, "leaveStudy");
        QName LEAVE_STUDY_BEGIN_DATE = QName.createQName(URI, "leaveStudyBeginDate");
        QName LEAVE_STUDY_END_DATE = QName.createQName(URI, "leaveStudyEndDate");
        QName LEAVE_STUDY_DAYS = QName.createQName(URI, "leaveStudyDays");
        
        QName LEAVE_CHANGE = QName.createQName(URI, "leaveChange");
        QName LEAVE_INITIAL_BEGIN_DATE = QName.createQName(URI, "leaveInitialBeginDate");
        QName LEAVE_INITIAL_END_DATE = QName.createQName(URI, "leaveInitialEndDate");
        QName LEAVE_NEW_BEGIN_DATE = QName.createQName(URI, "leaveNewBeginDate");
        QName LEAVE_NEW_END_DATE = QName.createQName(URI, "leaveNewEndDate");
        QName LEAVE_NEW_DAYS = QName.createQName(URI, "leaveNewDays");
        
        QName LEAVE_CANCEL = QName.createQName(URI, "leaveCancel");
        QName LEAVE_CANCEL_BEGIN_DATE = QName.createQName(URI, "leaveCancelBeginDate");
        QName LEAVE_CANCEL_END_DATE = QName.createQName(URI, "leaveCancelEndDate");
        QName LEAVE_CANCEL_DAYS = QName.createQName(URI, "leaveCancelDays");
        
        QName SUBSTITUTE_NAME = QName.createQName(URI, "substituteName");
        QName SUBSTITUTION_BEGIN_DATE = QName.createQName(URI, "substitutionBeginDate");
        QName SUBSTITUTION_END_DATE = QName.createQName(URI, "substitutionEndDate");
    }

}
