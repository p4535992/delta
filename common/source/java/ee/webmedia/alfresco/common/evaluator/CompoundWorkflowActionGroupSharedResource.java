package ee.webmedia.alfresco.common.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;

public class CompoundWorkflowActionGroupSharedResource extends EvaluatorSharedResource<CompoundWorkflow> {
    private static final long serialVersionUID = 1L;

    private Boolean fullAccess;
    private Boolean ownerOrDocManager;
    private Boolean parentOwner;
    private Boolean adminOrDocManagerWithPermission;
    private Boolean saved;
    private Boolean admin;
    private Boolean independentWorkflow;
    private Boolean notificationAssocExists;
    private String currentUser;

    public String getCurrentUser() {
        if (currentUser == null) {
            currentUser = AuthenticationUtil.getRunAsUser();
        }
        return currentUser;
    }

    public boolean isParentOwner() {
        if (parentOwner == null) {
            parentOwner = BeanHelper.getDocumentDynamicService().isOwner(getObject().getParent(), getCurrentUser());
        }
        return parentOwner;
    }

    public boolean isAdminOrDocManagerWithPermission() {
        if (adminOrDocManagerWithPermission == null) {
            adminOrDocManagerWithPermission = PrivilegeUtil.isAdminOrDocmanagerWithPermission(getObject().getParent(), Privilege.VIEW_CASE_FILE);
        }
        return adminOrDocManagerWithPermission;
    }

    public boolean isSaved() {
        if (saved == null) {
            saved = getObject().isSaved();
        }
        return saved;
    }

    @Override
    public boolean isFavourite() {
        if (favourite == null) {
            favourite = BeanHelper.getCompoundWorkflowFavoritesService().isFavorite(getObject().getNodeRef()) != null;
        }
        return favourite;
    }

    public boolean isAdmin() {
        if (admin == null) {
            admin = getUserService().isAdministrator();
        }
        return admin;
    }

    public boolean isIndependentWorkflow() {
        if (independentWorkflow == null) {
            independentWorkflow = getObject().isIndependentWorkflow();
        }
        return independentWorkflow;
    }

    public boolean isNotificationAssocsExists(QName assocType) {
        if (notificationAssocExists == null) {
            NodeRef currentUserRef = BeanHelper.getUserService().getCurrentUser();
            notificationAssocExists = BeanHelper.getNotificationService().isNotificationAssocExists(currentUserRef, getObject().getNodeRef(), assocType);
        }
        return notificationAssocExists;
    }

    public Boolean isFullAccess() {
        return fullAccess;
    }

    public void setFullAccess(boolean fullAccess) {
        this.fullAccess = fullAccess;
    }

    public Boolean isOwnerOrDocManager() {
        return ownerOrDocManager;
    }

    public void setOwnerOrDocManager(boolean ownerOrDocManager) {
        this.ownerOrDocManager = ownerOrDocManager;
    }

}
