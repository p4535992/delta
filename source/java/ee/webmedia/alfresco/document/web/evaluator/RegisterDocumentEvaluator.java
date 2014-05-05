package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
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
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.workflow.service.HasNoStoppedOrInprogressWorkflowsEvaluator;

/**
 * Evaluator, that is used to decide if we should show registrDoc button or not
 */
public class RegisterDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node docNode) {
        NodeRef docTypeRef = getDocTypeRef(docNode);
        return docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)
                && new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)
                && new ViewStateActionEvaluator().evaluate(docNode) && canRegister(docNode, true)
                && getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, Boolean.class);
    }

    public boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows) {
        return canRegister(docNode, checkStoppedOrInprogressWorkflows, getDocTypeRef(docNode));
    }

    private boolean canRegister(Node docNode, boolean checkStoppedOrInprogressWorkflows, NodeRef docTypeRef) {
        if (!canRegister(docNode, docTypeRef)) {
            return false;
        }
        if (checkStoppedOrInprogressWorkflows && !new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(docNode)) {
            return false;
        }
        final FacesContext context = FacesContext.getCurrentInstance();
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        if (!documentService.isSaved(docNode.getNodeRef())) {
            return false;
        }
        return docNode.hasPermission(Privilege.EDIT_DOCUMENT);
    }

    public boolean canRegister(Node docNode) {
        return canRegister(docNode, getDocTypeRef(docNode));
    }

    private boolean canRegister(Node docNode, NodeRef docTypeRef) {
        if (isRegistered(docNode)) {
            return false;
        }
        BeanHelper.getDocumentService().throwIfNotDynamicDoc(docNode);
        if (!getDocumentAdminService().getTypeProperty(docTypeRef, DocumentAdminModel.Props.REGISTRATION_ENABLED, Boolean.class)) {
            return false;
        }
        return true;
    }

    public NodeRef getDocTypeRef(Node docNode) {
        NodeRef docTypeRef = getDocumentAdminService().getDocumentTypeRef((String) docNode.getProperties().get(Props.OBJECT_TYPE_ID));
        return docTypeRef;
    }

    public static boolean isNotRegistered(Node docNode) {
        return !isRegistered(docNode);
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

    /** Deprecated - for deciding if document is registered or not, use isRegistered/isNotRegistered methods. */
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
