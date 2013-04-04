package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;

public class CreateWordFileFromTemplateEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);

        return (!bean.isInEditMode())
                && bean.getDocument().getProperties().get(DocumentCommonModel.Props.DOC_STATUS.toString()).toString().equals(
                        DocumentStatus.WORKING.getValueName())
                && ((DocumentTemplateService) FacesHelper.getManagedBean(context, DocumentTemplateService.BEAN_NAME)).getDocumentsTemplate(node.getNodeRef()) != null
                && node.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
    }
}