package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CHANCELLORS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.DECREE;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MANAGEMENTS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MINISTERS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SMIT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TENDERING_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER_SMIT;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.workflow.service.HasNoStoppedOrInprogressWorkflowsEvaluator;

/**
 * Evaluator, that is used to decide if we should show registrDoc button or not
 */
public class RegisterDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;
    /** Document types for which registration button must not be displayed */
    private static List<QName> deniedDocTypes = Arrays.asList(MANAGEMENTS_ORDER, CHANCELLORS_ORDER, MINISTERS_ORDER, DECREE, TRAINING_APPLICATION,
            TENDERING_APPLICATION
            , LEAVING_LETTER, INTERNAL_APPLICATION, ERRAND_APPLICATION_DOMESTIC, ERRAND_ORDER_ABROAD, PERSONELLE_ORDER_SIM
            , PERSONELLE_ORDER_SMIT, VACATION_ORDER, VACATION_ORDER_SMIT);

    @Override
    public boolean evaluate(Node docNode) {
        final FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean metadataBlock = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);
        if (metadataBlock.isInEditMode()) {
            return false;
        }
        if (deniedDocTypes.contains(docNode.getType())) {
            return false;
        }
        if (!new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(docNode)) {
            return false;
        }
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        if (!documentService.isSaved(docNode.getNodeRef())) {
            return false;
        }
        if (!docNode.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA)) {
            return false;
        }
        return isNotRegistered(docNode);
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
