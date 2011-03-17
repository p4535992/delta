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

    public void receiveDocuments(@SuppressWarnings("unused") ActionEvent event) {
        Collection<String> docs = getDvkService().receiveDocuments();
        // XXX: kui tahta, et listi sisu ka värskenduks, peaks lisama document-list-dialog.jsp refreshOnBind="true" - mida praegu performance pärast ei tee
        // (kasutuses paljudes teistes dokumentide vaadetes, kus refreshimist ei ole vaja)
        // DvkDocumentListDialog dialog = (DvkDocumentListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(),
        // DvkDocumentListDialog.BEAN_NAME);
        // dialog.restored();

        FacesContext context = FacesContext.getCurrentInstance();
        String msg = MessageUtil.getMessage(context, "dvk_received_documents", docs.size());
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));

        UIContextService.getInstance(context).notifyBeans();
    }

    public void updateDocSendStatuses(@SuppressWarnings("unused") ActionEvent event) {
        final int updateDocSendStatuses = getDvkService().updateDocSendStatuses();
        MessageUtil.addInfoMessage("dvk_updateDocSendStatuses_success", updateDocSendStatuses);
    }

    public void updateOrganizationsDvkCapability(@SuppressWarnings("unused") ActionEvent event) {
        final int dvkCapableOrgs = getDvkService().updateOrganizationsDvkCapability();
        MessageUtil.addInfoMessage("dvk_updateOrganizationsDvkCapability_success", dvkCapableOrgs);
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