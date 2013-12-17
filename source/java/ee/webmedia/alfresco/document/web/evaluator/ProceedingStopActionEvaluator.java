package ee.webmedia.alfresco.document.web.evaluator;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

public class ProceedingStopActionEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        FacesContext context = FacesContext.getCurrentInstance();
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        UserService userService = (UserService) FacesHelper.getManagedBean(context, UserService.BEAN_NAME);
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);
        Map<String, Object> props = bean.getDocument().getProperties();

        String status = (String) props.get(DocumentCommonModel.Props.DOC_STATUS.toString());
        String ownerId = (String) props.get(DocumentCommonModel.Props.OWNER_ID.toString());

        return viewStateEval.evaluate(node) && DocumentStatus.WORKING.getValueName().equals(status) &&
                (userService.isAdministrator() || userService.isDocumentManager() || AuthenticationUtil.getRunAsUser().equals(ownerId));
    }

}
