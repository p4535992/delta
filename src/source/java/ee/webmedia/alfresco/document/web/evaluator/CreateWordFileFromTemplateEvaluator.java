package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;

public class CreateWordFileFromTemplateEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    private UserService userService;
    private NodeService nodeService;

    @Override
    public boolean evaluate(Node node) {

        FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);

        return (!bean.isInEditMode())
                && bean.getDocument().getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(
                        DocumentStatus.WORKING.getValueName())
                && ((DocumentTemplateService) FacesHelper.getManagedBean(context, DocumentTemplateService.BEAN_NAME)).getDocumentsTemplate(node.getNodeRef()) != null
                && isOwnerOrDocManagerOrAdmin(node);
    }

    private boolean isOwnerOrDocManagerOrAdmin(Node node) {
        final String userName = getUserService().getCurrentUserName();
        if (getUserService().isDocumentManager()) {
            return true;
        }
        String ownerId = (String) getNodeService().getProperty(node.getNodeRef(), DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            return true;
        }
        return false;
    }

    private NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = (NodeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean("NodeService");
        }
        return nodeService;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }
}