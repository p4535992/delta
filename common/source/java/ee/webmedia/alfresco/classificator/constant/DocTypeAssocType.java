package ee.webmedia.alfresco.classificator.constant;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Constant for different associations between documents
 * 
 * @author Ats Uiboupin
 */
public enum DocTypeAssocType {
    FOLLOWUP(DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP),
    REPLY(DocumentAdminModel.Types.REPLY_ASSOCIATION, DocumentCommonModel.Assocs.DOCUMENT_REPLY);

    /** association type between {@link DocumentType} and {@link AssociationModel} that determines whether association model describes reply or followup association */
    private QName assocBetweenDocTypeAndAssocModel;
    /** association type used to create assocaition between documents */
    private QName assocBetweenDocs;

    private DocTypeAssocType(QName qName, QName assocBetweenDocs) {
        assocBetweenDocTypeAndAssocModel = qName;
        this.assocBetweenDocs = assocBetweenDocs;
    }

    public static DocTypeAssocType valueOf(QName replyOrFollowUp) {
        for (DocTypeAssocType docTypeAssocType : DocTypeAssocType.values()) {
            if (docTypeAssocType.getAssocBetweenDocTypeAndAssocModel().equals(replyOrFollowUp)) {
                return docTypeAssocType;
            }
        }
        throw new IllegalArgumentException("DocTypeAssocType enum for QName " + replyOrFollowUp);
    }

    public QName getAssocBetweenDocTypeAndAssocModel() {
        return assocBetweenDocTypeAndAssocModel;
    }

    public QName getAssocBetweenDocs() {
        return assocBetweenDocs;
    }
}
