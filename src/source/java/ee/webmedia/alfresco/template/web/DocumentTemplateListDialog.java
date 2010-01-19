package ee.webmedia.alfresco.template.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient DocumentTemplateService documentTemplateService;
    
    public List<DocumentTemplate> getTemplates() {
        return getDocumentTemplateService().getTemplates();
    }
    
    public void setupTemplateAction(ActionEvent event) {
        browseBean.setupContentAction((new NodeRef(ActionUtil.getParam(event, "nodeRef")).getId()), true);
    }

    /*
     * This is a read-only dialog, so we have nothing to do here (Save/OK button isn't displayed)
     */
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // not used
        return null;
    }

    protected DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

}
