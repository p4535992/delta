package ee.webmedia.alfresco.template.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;

/**
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient DocumentTemplateService documentTemplateService;
    
    public List<DocumentTemplate> getTemplates() {
        return getDocumentTemplateService().getTemplates();
    }
    
    /*
     * This is a read-only dialog, so we have nothing to do here (Save/OK button isn't displayed)
     */
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // not used
        return null;
    }
    
    @Override
    public Object getActionsContext() {
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
