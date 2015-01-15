package ee.webmedia.alfresco.common.service;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.thesaurus.model.ThesaurusModel;

public class ConstantNodeRefsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "constantNodeRefsBean";

    private NodeRef dvkCorruptRoot;
    private NodeRef scannedFilesRoot;
    private NodeRef sendFailureNoticeSpaceRoot;
    private NodeRef webServiceDocumentsRoot;
    private NodeRef attachmentSpaceRoot;
    private NodeRef receivedDvkDocumentsRoot;
    private NodeRef addressbookRoot;
    private NodeRef thesauriRoot;
    private NodeRef templateRoot;
    private NodeRef draftsRoot;
    private NodeRef fromDvkRoot;
    private NodeRef incomingEmailRoot;
    private NodeRef receivedInvoiceRoot;
    private NodeRef sentEmailRoot;
    private NodeRef forwardedDecDocumentsRoot;

    private String corruptDvkDocumentsPath;
    private String receivedDvkDocumentsPath;
    private String scannedFilesRootPath;
    private String fromDvkRootPath;
    private String incomingEmailRootPath;
    private String receivedInvoiceRootPath;
    private String sentEmailRootPath;
    private String forwardedDecDocumentsRootPath;

    public NodeRef getDvkCorruptRoot() {
        if (dvkCorruptRoot == null) {
            dvkCorruptRoot = BeanHelper.getGeneralService().getNodeRef(corruptDvkDocumentsPath);
        }
        return dvkCorruptRoot;
    }

    public NodeRef getScannedFilesRoot() {
        if (scannedFilesRoot == null) {
            scannedFilesRoot = BeanHelper.getGeneralService().getNodeRef(scannedFilesRootPath);
        }
        return scannedFilesRoot;
    }

    public NodeRef getSendFailureNoticeSpaceRoot() {
        if (sendFailureNoticeSpaceRoot == null) {
            sendFailureNoticeSpaceRoot = BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE);
        }
        return sendFailureNoticeSpaceRoot;
    }

    public NodeRef getWebServiceDocumentsRoot() {
        if (webServiceDocumentsRoot == null) {
            webServiceDocumentsRoot = BeanHelper.getGeneralService().getNodeRef(DocumentCommonModel.Repo.WEB_SERVICE_SPACE);
        }
        return webServiceDocumentsRoot;
    }

    public NodeRef getAttachmentRoot() {
        if (attachmentSpaceRoot == null) {
            attachmentSpaceRoot = BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
        }
        return attachmentSpaceRoot;
    }

    public NodeRef getAddressbookRoot() {
        if (addressbookRoot == null) {
            addressbookRoot = BeanHelper.getGeneralService().getNodeRef(AddressbookModel.Repo.ADDRESSBOOK_SPACE);
        }
        return addressbookRoot;
    }

    public NodeRef getReceivedDvkDocumentsRoot() {
        if (receivedDvkDocumentsRoot == null) {
            receivedDvkDocumentsRoot = BeanHelper.getGeneralService().getNodeRef(receivedDvkDocumentsPath);
        }
        return receivedDvkDocumentsRoot;
    }

    public NodeRef getThesauriRoot() {
        if (thesauriRoot == null) {
            thesauriRoot = BeanHelper.getGeneralService().getNodeRef(ThesaurusModel.Repo.THESAURI_SPACE);
        }
        return thesauriRoot;
    }

    public NodeRef getTemplateRoot() {
        if (templateRoot == null) {
            templateRoot = BeanHelper.getGeneralService().getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
        }
        return templateRoot;
    }

    public NodeRef getDraftsRoot() {
        if (draftsRoot == null) {
            draftsRoot = BeanHelper.getGeneralService().getNodeRef(DocumentCommonModel.Repo.DRAFTS_SPACE);
        }
        return draftsRoot;
    }

    public NodeRef getFromDvkRoot() {
        if (fromDvkRoot == null) {
            fromDvkRoot = BeanHelper.getGeneralService().getNodeRef(fromDvkRootPath);
        }
        return fromDvkRoot;
    }

    public NodeRef getIncomingEmailRoot() {
        if (incomingEmailRoot == null) {
            incomingEmailRoot = BeanHelper.getGeneralService().getNodeRef(incomingEmailRootPath);
        }
        return incomingEmailRoot;
    }

    public NodeRef getReceivedInvoiceRoot() {
        if (receivedInvoiceRoot == null) {
            receivedInvoiceRoot = BeanHelper.getGeneralService().getNodeRef(receivedInvoiceRootPath);
        }
        return receivedInvoiceRoot;
    }

    public NodeRef getSentEmailRoot() {
        if (sentEmailRoot == null) {
            sentEmailRoot = BeanHelper.getGeneralService().getNodeRef(sentEmailRootPath);
        }
        return sentEmailRoot;
    }

    public NodeRef getForwardedDecDocumentsRoot() {
        if (forwardedDecDocumentsRoot == null) {
            forwardedDecDocumentsRoot = BeanHelper.getGeneralService().getNodeRef(forwardedDecDocumentsRootPath);
        }
        return forwardedDecDocumentsRoot;
    }

    public void setCorruptDvkDocumentsPath(String corruptDvkDocumentsPath) {
        this.corruptDvkDocumentsPath = corruptDvkDocumentsPath;
    }

    public void setReceivedDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.receivedDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    public void setScannedFilesRootPath(String scannedFilesRootPath) {
        this.scannedFilesRootPath = scannedFilesRootPath;
    }

    public void setFromDvkRootPath(String fromDvkRootPath) {
        this.fromDvkRootPath = fromDvkRootPath;
    }

    public void setIncomingEmailRootPath(String incomingEmailRootPath) {
        this.incomingEmailRootPath = incomingEmailRootPath;
    }

    public void setReceivedInvoiceRootPath(String receivedInvoiceRootPath) {
        this.receivedInvoiceRootPath = receivedInvoiceRootPath;
    }

    public void setSentEmailRootPath(String sentEmailRootPath) {
        this.sentEmailRootPath = sentEmailRootPath;
    }

    public void setForwardedDecDocumentsRootPath(String forwardedDecDocumentsRootPath) {
        this.forwardedDecDocumentsRootPath = forwardedDecDocumentsRootPath;
    }

}
