package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.workflow.service.HasNoStoppedOrInprogressWorkflowsEvaluator;

/**
 * Evaluator, that is used to decide if we should show registrDoc button or not
 * 
 * @author Ats Uiboupin
 */
public class RegisterDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node docNode) {
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && new ViewStateActionEvaluator().evaluate(docNode) && canRegister(docNode, true);
    }

    public boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows) {
        final FacesContext context = FacesContext.getCurrentInstance();
        if (!isNotRegistered(docNode)) {
            return false;
        }
        BeanHelper.getDocumentService().throwIfNotDynamicDoc(docNode);
        String docTypeId = (String) docNode.getProperties().get(Props.OBJECT_TYPE_ID);
        if (!getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.REGISTRATION_ENABLED, Boolean.class)) {
            return false;
        }
        if (checkStoppedOrInprogressWorkflows && !new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(docNode)) {
            return false;
        }
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        if (!documentService.isSaved(docNode.getNodeRef())) {
            return false;
        }
        return docNode.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
    }

    public static boolean isNotRegistered(Node docNode) {
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        final Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (StringUtils.isBlank(regNumber) && regDateTime == null) {
            return true;
        }
        return false;
    }

    public static boolean isRegistered(Node docNode) {
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        final Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (StringUtils.isNotBlank(regNumber) && regDateTime != null) {
            return true;
        }
        return false;
    }

    public static boolean isRegNumFilled(Node docNode) {
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        if (StringUtils.isNotBlank(regNumber)) {
            return true;
        }
        return false;
    }

    public boolean evaluateAdditionalButton(Node docNode) {
        PermissionService permissionService = (PermissionService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean("PermissionService");
        if (!permissionService.hasPermission(docNode.getNodeRef(), PermissionService.WRITE_PROPERTIES).equals(AccessStatus.ALLOWED)) {
            return false;
        }
        return isNotRegistered(docNode);
    }
}
