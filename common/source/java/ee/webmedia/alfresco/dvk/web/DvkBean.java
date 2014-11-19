package ee.webmedia.alfresco.dvk.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDvkService;

import java.io.Serializable;
import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.context.UIContextService;

import ee.webmedia.alfresco.utils.MessageUtil;

public class DvkBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DvkBean.class);

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
        final int updateDocSendStatuses = getDvkService().updateDocAndTaskSendStatuses();
        MessageUtil.addInfoMessage("dvk_updateDocSendStatuses_success", updateDocSendStatuses);
    }

    public void updateOrganizationsDvkCapability(@SuppressWarnings("unused") ActionEvent event) {
        int dvkCapableOrgs;
        try {
            dvkCapableOrgs = getDvkService().updateOrganizationsDvkCapability();
            MessageUtil.addInfoMessage("dvk_updateOrganizationsDvkCapability_success", dvkCapableOrgs);
        } catch (Exception e) {
            MessageUtil.addErrorMessage("dvk_updateOrganizationsDvkCapability_error", e.getMessage());
            LOG.error(e.getMessage(), e);
        }
    }

}
