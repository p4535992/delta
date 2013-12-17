package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Dynamic document type
 * 
 * @author Ats Uiboupin
 */
public class DocumentType extends DynamicType {
    private static final long serialVersionUID = 1L;

    public DocumentType(NodeRef parentRef) {
        super(parentRef, DocumentAdminModel.Types.DOCUMENT_TYPE);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public DocumentType(NodeRef parentNodeRef, WmNode docTypeNode) {
        super(parentNodeRef, docTypeNode);
    }

    // ChildrenList

    public List<? extends AssociationModel> getAssociationModels(DocTypeAssocType associationTypeEnum) {
        if (associationTypeEnum == null) {
            List<AssociationModel> allAssocsToDocType = new ArrayList<AssociationModel>();
            allAssocsToDocType.addAll(getFollowupAssociations());
            allAssocsToDocType.addAll(getReplyAssociations());
            return allAssocsToDocType;
        }
        return DocTypeAssocType.FOLLOWUP == associationTypeEnum ? getFollowupAssociations() : getReplyAssociations();
    }

    public ChildrenList<FollowupAssociation> getFollowupAssociations() {
        return getChildren(FollowupAssociation.class);
    }

    public ChildrenList<ReplyAssociation> getReplyAssociations() {
        return getChildren(ReplyAssociation.class);
    }

    // Properties

    public boolean isPublicAdr() {
        return getPropBoolean(DocumentAdminModel.Props.PUBLIC_ADR);
    }

    public void setPublicAdr(boolean publicAdr) {
        setProp(DocumentAdminModel.Props.PUBLIC_ADR, publicAdr);
    }

    public String getSystematicComment() {
        return getProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT);
    }

    public void setSystematicComment(String systematicComment) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT, systematicComment);
    }

    public void setSystematic(boolean systematic) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC, systematic);
    }

    public boolean isSystematic() {
        return getPropBoolean(DocumentAdminModel.Props.SYSTEMATIC);
    }

    public boolean isChangeByNewDocumentEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.CHANGE_BY_NEW_DOCUMENT_ENABLED);
    }

    public void setChangeByNewDocumentEnabled(boolean changeByNewDocumentEnabled) {
        setProp(DocumentAdminModel.Props.CHANGE_BY_NEW_DOCUMENT_ENABLED, changeByNewDocumentEnabled);
    }

    public boolean isRegistrationEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.REGISTRATION_ENABLED);
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        setProp(DocumentAdminModel.Props.REGISTRATION_ENABLED, registrationEnabled);
    }

    public boolean isFinishDocByRegistration() {
        return getPropBoolean(DocumentAdminModel.Props.FINISH_DOC_BY_REGISTRATION);
    }

    public void setFinishDocByRegistration(boolean finishDocByRegistration) {
        setProp(DocumentAdminModel.Props.FINISH_DOC_BY_REGISTRATION, finishDocByRegistration);
    }

    public boolean isSendUnregistratedDocEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.SEND_UNREGISTRATED_DOC_ENABLED);
    }

    public void setSendUnregistratedDocEnabled(boolean sendUnregistratedDocEnabled) {
        setProp(DocumentAdminModel.Props.SEND_UNREGISTRATED_DOC_ENABLED, sendUnregistratedDocEnabled);
    }

    public boolean isEditFilesOfFinishedDocEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.EDIT_FILES_OF_FINISHED_DOC_ENABLED);
    }

    public void setEditFilesOfFinishedDocEnabled(boolean editFilesOfFinishedDocEnabled) {
        setProp(DocumentAdminModel.Props.EDIT_FILES_OF_FINISHED_DOC_ENABLED, editFilesOfFinishedDocEnabled);
    }

    public boolean isAddReplyToUnregistratedDocEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.ADD_REPLY_TO_UNREGISTRATED_DOC_ENABLED);
    }

    public void setAddReplyToUnregistratedDocEnabled(boolean addReplyToUnregistratedDocEnabled) {
        setProp(DocumentAdminModel.Props.ADD_REPLY_TO_UNREGISTRATED_DOC_ENABLED, addReplyToUnregistratedDocEnabled);
    }

    public boolean isAddFollowUpToUnregistratedDocEnabled() {
        return getPropBoolean(DocumentAdminModel.Props.ADD_FOLLOW_UP_TO_UNREGISTRATED_DOC_ENABLED);
    }

    public void setAddFollowUpToUnregistratedDocEnabled(boolean addFollowUpToUnregistratedDocEnabled) {
        setProp(DocumentAdminModel.Props.ADD_FOLLOW_UP_TO_UNREGISTRATED_DOC_ENABLED, addFollowUpToUnregistratedDocEnabled);
    }

    public boolean isShowUnvalued() {
        return getPropBoolean(DocumentAdminModel.Props.SHOW_UNVALUED);
    }

    @Override
    public DocumentType clone() {
        return (DocumentType) super.clone(); // just return casted type
    }

}
