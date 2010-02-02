package ee.webmedia.alfresco.document.web.evaluator;

import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Evaluator, that is used to decide if we should show registrDoc button or not
 * 
 * @author Ats Uiboupin
 */
public class RegisterDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node docNode) {
        final FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean metadataBlock = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);
        if(metadataBlock.isInEditMode()) {
            return false;
        }
        
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        if(!documentService.isSaved(docNode.getNodeRef())) {
            return false;
        }
        PermissionService permissionService = (PermissionService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean("PermissionService");
        if (!permissionService.hasPermission(docNode.getNodeRef(), PermissionService.WRITE_PROPERTIES).equals(AccessStatus.ALLOWED)) {
            return false;
        }
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        final Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (StringUtils.isBlank(regNumber) && regDateTime == null) {
            return true;
        }
        return false;
    }
    
    public boolean evaluateAdditionalButton(Node docNode) {
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);

        PermissionService permissionService = (PermissionService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean("PermissionService");
        if (!permissionService.hasPermission(docNode.getNodeRef(), PermissionService.WRITE_PROPERTIES).equals(AccessStatus.ALLOWED)) {
            return false;
        }
        final Map<String, Object> props = docNode.getProperties();
        final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        final Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (StringUtils.isBlank(regNumber) && regDateTime == null) {
            return true;
        }
        return false;
    }
}
