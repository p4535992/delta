package ee.webmedia.alfresco.common.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;

import java.util.Date;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public abstract class AbstarctCaseFileStatusEvaluator extends SharedResourceEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate((Node) obj);
    }

    protected boolean evaluateInternal(Node caseFileNode) {
        if (!StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(caseFileNode.getNodeRef().getStoreRef()) || BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        Map<String, Object> properties = caseFileNode.getProperties();
        return isValidStatus(properties) && isUserAllowedToView(caseFileNode, properties);
    }

    protected abstract boolean isValidStatus(Map<String, Object> properties);

    private boolean isUserAllowedToView(Node caseFileNode, Map<String, Object> properties) {
        return isAdminOrDocmanagerWithPermission(caseFileNode, Privilege.VIEW_CASE_FILE)
                || AuthenticationUtil.getRunAsUser().equals(properties.get(DocumentCommonModel.Props.OWNER_ID.toString()))
                || (validToIsExceeded(properties) && getUserService().isArchivist());
    }

    private static boolean validToIsExceeded(Map<String, Object> properties) {
        Date validTo = (Date) properties.get(VolumeModel.Props.VALID_TO);
        return validTo != null && CalendarUtil.getDaysBetweenSigned(validTo, new Date()) > 0;
    }

    @Override
    public boolean evaluate() {
        CaseFileActionsGroupResource resource = (CaseFileActionsGroupResource) sharedResource;
        if (!StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(resource.getObject().getNodeRef().getStoreRef()) || resource.isInEditMode()) {
            return false;
        }
        Map<String, Object> properties = resource.getObject().getProperties();
        return isValidStatus(resource.getStatus()) && (resource.isAdminOrOwner() || (validToIsExceeded(properties) && resource.isArchivist()));
    }

    protected abstract boolean isValidStatus(String status);
}
