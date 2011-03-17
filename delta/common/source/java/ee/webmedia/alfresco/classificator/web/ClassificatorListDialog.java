package ee.webmedia.alfresco.classificator.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
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

    /** @param event */
    public void export(ActionEvent event) {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        try {
            response.setContentType("text/xml; charset=" + CHARSET);
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-disposition", "attachment;filename=classificators.xml");
            outputStream = response.getOutputStream();
            writer = new OutputStreamWriter(outputStream, CHARSET);
            writer.write("<?xml version='1.0' encoding='" + CHARSET + "'?>\n");
            getClassificatorService().exportClassificators(writer);
            writer.flush();
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export classificators", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(writer);
            FacesContext.getCurrentInstance().responseComplete();
            
            // Erko hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
        }
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

    @Override
    public Object getActionsContext() {
        return null;
    }
}
