package ee.webmedia.alfresco.document.sendout.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class ForwardDecDocumentDialog extends BaseDialogBean {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ForwardDecDocumentDialog.class);

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ForwardDecDocumentDialog";
    public static final String RECIPIENTS = "recipientName";
    public static final String RECIPIENT_EMAILS = "recipientEmail";

    private ForwardingModel model;
    private NodeRef docRef;
    private Node docNode;

    public String init() {
        resetState();
        FacesContext context = FacesContext.getCurrentInstance();
        docNode = BeanHelper.getDocumentDialogHelperBean().getNode();
        docNode.clearPermissionsCache();
        docRef = docNode.getNodeRef();

        if (docRef == null || !getNodeService().exists(docRef)) {
            MessageUtil.addErrorMessage("document_forward_dec_document_error_doc_deleted");
            return null;
        }
        NodeRef decContainerRef = (NodeRef) docNode.getProperties().get(DvkModel.Props.DEC_CONTAINER);
        if (decContainerRef == null) {
            MessageUtil.addErrorMessage("document_forward_dec_document_error_container_v1_5");
            return null;
        }

        model = new ForwardingModel();
        List<String> recipientNames = new ArrayList<>();
        List<String> recipientEmails = new ArrayList<>();
        recipientNames.add("");
        recipientEmails.add("");
        Map<String, List<String>> properties = new HashMap<>();
        properties.put(RECIPIENTS, recipientNames);
        properties.put(RECIPIENT_EMAILS, recipientEmails);
        model.setProperties(properties);
        try {
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed());
            BaseDialogBean.validatePermission(docNode, Privilege.EDIT_DOCUMENT);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_forward_dec_document_error_doc_locked");
            return null;
        } catch (UnableToPerformException e) {
            LOG.warn("Cannot open dialog", e);
            MessageUtil.addStatusMessage(context, e);
            return null;
        }
        return "dialog:forwardDecDocumentDialog";
    }

    @Override
    public String cancel() {
        BeanHelper.getDocumentLockHelperBean().lockOrUnlockIfNeeded(false);
        resetState();
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (BeanHelper.getDocumentLockHelperBean().isLockReleased(docRef)) {
            MessageUtil.addErrorMessage("lock_send_out_administrator_released");
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }

        List<String> recipients = model.getProperties().get(RECIPIENTS);
        List<String> recipientEmails = model.getProperties().get(RECIPIENT_EMAILS);
        List<String> filteredRecipients = new ArrayList<>();
        List<String> filteredEmails = new ArrayList<>();
        List<String> modes = new ArrayList<>();
        for (int i = 0; i < recipients.size(); i++) {
            String recipient = recipients.get(i);
            if (StringUtils.isNotBlank(recipient)) {
                filteredRecipients.add(recipient);
                filteredEmails.add(recipientEmails.get(i));
                modes.add(SendMode.DVK.getValueName());
            }
        }

        if (filteredRecipients.isEmpty()) {
            MessageUtil.addWarningMessage("document_forward_dec_document_name_is_mandatory");
            return null;
        }

        try {
            String fromEmail = BeanHelper.getParametersService().getStringParameter(Parameters.DOC_SENDER_EMAIL);

            List<Pair<String, String>> forwarded = BeanHelper.getSendOutService().forward(docRef, filteredRecipients, filteredEmails, modes, fromEmail, "", getFileRefs());
            if(forwarded == null){
                LOG.error("Forwarding failed!");
            } else {
                BeanHelper.getDocumentDynamicService().moveNodeToForwardedDecDocuments(docNode, forwarded);

                MessageUtil.addInfoMessage("document_forward_dec_document_success");
                return outcome;
            }

        } catch (Exception e) {
            MessageUtil.addErrorMessage("document_send_failed");
            LOG.error("Forwarding dec document failed", e);
        }
        return null;
    }

    private List<NodeRef> getFileRefs() {
        return BeanHelper.getFileService().getAllFileRefsExcludingDecContainer(docRef);
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("document_forward_dec_document_ok");
    }

    private void resetState() {
        model = null;
        docNode = null;
        docRef = null;
    }

    @Override
    public void clean() {
        resetState();
    }

    public ForwardingModel getModel() {
        return model;
    }

    public List<String> addContactData(String nodeRef) {
        return AddressbookUtil.getContactData(nodeRef);
    }

    public static class ForwardingModel implements Serializable {
        private static final long serialVersionUID = 1L;

        private Map<String, List<String>> properties;

        public Map<String, List<String>> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, List<String>> properties) {
            this.properties = properties;
        }
    }

}
