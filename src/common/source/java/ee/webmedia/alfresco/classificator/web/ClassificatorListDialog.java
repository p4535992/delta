package ee.webmedia.alfresco.classificator.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;

public class ClassificatorListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    
    private transient ClassificatorService classificatorService;
    private List<Classificator> classificators;
    

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    protected ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }
    
    /**
     * Used in JSP pages.
     */
    public List<Classificator> getClassificators() {
        return classificators;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button not shown or used
        return null;
    }
    
    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        classificators = getClassificatorService().getAllClassificators();
    }
    
    @Override
    public String cancel() {
        classificators = null;
        return super.cancel();
    }
}
