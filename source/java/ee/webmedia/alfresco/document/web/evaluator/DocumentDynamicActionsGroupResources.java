package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.evaluator.NodeBasedEvaluatorSharedResource;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.model.UserModel;

public class DocumentDynamicActionsGroupResources extends NodeBasedEvaluatorSharedResource {

    private static final long serialVersionUID = 1L;

    private Boolean workingStatus;
    private Boolean notInDraftsFunction;
    private Boolean addAssoc;
    private Boolean docOwner;
    private Boolean notificationAssocExists;
    private Boolean editPermission;
    private Boolean inForwardedDecDocuments;
    private String objectTypeId;
    private String docStatus;
    private String regNr;
    private NodeRef documentTypeRef;

    public boolean hasEditPermission() {
        if (editPermission == null) {
            editPermission = getObject().hasPermission(Privilege.EDIT_DOCUMENT);
        }
        return editPermission;
    }

    public String getObjectTypeId() {
        if (objectTypeId == null) {
            objectTypeId = (String) getObject().getProperties().get(Props.OBJECT_TYPE_ID);
        }
        return objectTypeId;
    }

    public NodeRef getDocumentTypeRef() {
        if (documentTypeRef == null) {
            documentTypeRef = getDocumentAdminService().getDocumentTypeRef(getObjectTypeId());
        }
        return documentTypeRef;
    }

    public boolean isInWorkingStatus() {
        if (workingStatus == null) {
            String docStatus = (String) getObject().getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
            workingStatus = EqualsHelper.nullSafeEquals(DocumentStatus.WORKING.getValueName(), docStatus);
        }
        return workingStatus;
    }

    public boolean isNotInDraftsFunction() {
        if (notInDraftsFunction == null) {
            notInDraftsFunction = new DocumentNotInDraftsFunctionActionEvaluator().evaluate(getObject());
        }
        return notInDraftsFunction;
    }

    public boolean isInStatus(DocumentStatus docStatusToCheck) {
        if (docStatus == null) {
            docStatus = (String) getObject().getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString());
        }
        return docStatusToCheck.getValueName().equals(docStatus);
    }

    public boolean isDocOwner() {
        if (docOwner == null) {
            docOwner = AuthenticationUtil.getRunAsUser().equals(getObject().getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()));
        }
        return docOwner;
    }

    public boolean isNotificationAssocExists() {
        if (notificationAssocExists == null) {
            notificationAssocExists = BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), getObject().getNodeRef(),
                    UserModel.Assocs.DOCUMENT_NOTIFICATION);
        }
        return notificationAssocExists;
    }

    public String getRegNr() {
        if (regNr == null) {
            regNr = (String) getObject().getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
            regNr = regNr == null ? "" : regNr;
        }
        return regNr;
    }

    public Boolean isAddAssoc() {
        return addAssoc;
    }

    public void setAddAssoc(boolean value) {
        addAssoc = value;
    }

    @Override
    public boolean isFavourite() {
        if (favourite == null) {
            favourite = BeanHelper.getDocumentFavoritesService().isFavorite(getObject().getNodeRef()) != null;
        }
        return favourite;
    }

    public boolean isInForwardedDecDocuments() {
        if (inForwardedDecDocuments == null) {
            inForwardedDecDocuments = StringUtils.contains(getObject().getPath(), DocumentDynamicDialog.FORWARDED_DEC_DOCUMENTS) ? true : false;
        }
        return inForwardedDecDocuments;
    }

}
