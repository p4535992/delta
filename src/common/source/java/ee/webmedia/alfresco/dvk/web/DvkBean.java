package ee.webmedia.alfresco.dvk.web;

import java.io.Serializable;
import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.context.UIContextService;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DvkBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient DvkService dvkService;

    public void receiveDocuments(ActionEvent event) {
        Collection<String> docs = getDvkService().receiveDocuments();

        FacesContext context = FacesContext.getCurrentInstance();
        String msg = MessageUtil.getMessage(context, "dvk_received_documents", docs.size());
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));

        UIContextService.getInstance(context).notifyBeans();
    }

    // START: getters / setters
    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public DvkService getDvkService() {
        if (dvkService == null) {
            dvkService = (DvkService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(DvkService.BEAN_NAME);
        }
        return dvkService;
    }
    // END: getters / setters
}
