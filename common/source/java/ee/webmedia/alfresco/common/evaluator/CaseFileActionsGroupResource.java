package ee.webmedia.alfresco.common.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import org.alfresco.repo.security.authentication.AuthenticationUtil;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.user.model.UserModel;

public class CaseFileActionsGroupResource extends NodeBasedEvaluatorSharedResource {

    private static final long serialVersionUID = 1L;

    private Boolean adminOrOwner;
    private Boolean archivist;
    private Boolean owner;
    private Boolean notificationAssocExists;
    private String status;

    public boolean isAdminOrOwner() {
        if (adminOrOwner == null) {
            adminOrOwner = PrivilegeUtil.isAdminOrDocmanagerWithPermission(getObject(), Privilege.VIEW_CASE_FILE) || isOwner();
        }
        return adminOrOwner;
    }

    public boolean isOwner() {
        if (owner == null) {
            owner = AuthenticationUtil.getRunAsUser().equals(getObject().getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()));
        }
        return owner;
    }

    public boolean isArchivist() {
        if (archivist == null) {
            archivist = getUserService().isArchivist();
        }
        return archivist;
    }

    public boolean isNotificationAssocExists() {
        if (notificationAssocExists == null) {
            notificationAssocExists = BeanHelper.getNotificationService().isNotificationAssocExists(BeanHelper.getUserService().getCurrentUser(), getObject().getNodeRef(),
                    UserModel.Assocs.CASE_FILE_NOTIFICATION);
        }
        return notificationAssocExists;
    }

    public String getStatus() {
        if (status == null) {
            status = (String) getObject().getProperties().get(DocumentDynamicModel.Props.STATUS.toString());
        }
        return status;
    }

    @Override
    public boolean isFavourite() {
        if (favourite == null) {
            favourite = BeanHelper.getCaseFileFavoritesService().isFavorite(getObject().getNodeRef()) != null;
        }
        return favourite;
    }
}
