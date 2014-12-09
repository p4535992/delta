package ee.webmedia.alfresco.common.service;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.thesaurus.model.ThesaurusModel;

public class ConstantNodeRefsBean implements InitializingBean, Serializable {

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
    private NodeRef receivedincoiceRoot;
    private NodeRef sentEmailRoot;
    private NodeRef forwardedDecDocumentsRoot;

    private String corruptDvkDocumentsPath;
    private String receivedDvkDocumentsPath;

    @Override
    public void afterPropertiesSet() throws Exception {
        GeneralService generalService = BeanHelper.getGeneralService();
        sendFailureNoticeSpaceRoot = generalService.getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE);
        webServiceDocumentsRoot = BeanHelper.getGeneralService().getNodeRef(DocumentCommonModel.Repo.WEB_SERVICE_SPACE);
        attachmentSpaceRoot = generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
        addressbookRoot = generalService.getNodeRef(AddressbookModel.Repo.ADDRESSBOOK_SPACE);
        thesauriRoot = generalService.getNodeRef(ThesaurusModel.Repo.THESAURI_SPACE);
        templateRoot = generalService.getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE);
        draftsRoot = generalService.getNodeRef(DocumentCommonModel.Repo.DRAFTS_SPACE);
        dvkCorruptRoot = generalService.getNodeRef(corruptDvkDocumentsPath);
        receivedDvkDocumentsRoot = generalService.getNodeRef(receivedDvkDocumentsPath);
    }

    public NodeRef getDvkCorruptRoot() {
        return dvkCorruptRoot;
    }

    public NodeRef getScannedFilesRoot() {
        return scannedFilesRoot;
    }

    public NodeRef getSendFailureNoticeSpaceRoot() {
        return sendFailureNoticeSpaceRoot;
    }

    public NodeRef getWebServiceDocumentsRoot() {
        return webServiceDocumentsRoot;
    }

    public NodeRef getAttachmentRoot() {
        return attachmentSpaceRoot;
    }

    public NodeRef getAddressbookRoot() {
        return addressbookRoot;
    }

    public NodeRef getReceivedDvkDocumentsRoot() {
        return receivedDvkDocumentsRoot;
    }

    public NodeRef getThesauriRoot() {
        return thesauriRoot;
    }

    public NodeRef getTemplateRoot() {
        return templateRoot;
    }

    public NodeRef getDraftsRoot() {
        return draftsRoot;
    }

    public void setCorruptDvkDocumentsPath(String corruptDvkDocumentsPath) {
        this.corruptDvkDocumentsPath = corruptDvkDocumentsPath;
    }

    public void setReceivedDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.receivedDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    public void setScannedFilesRoot(NodeRef scannedFilesRoot) {
        this.scannedFilesRoot = scannedFilesRoot;
    }

    public NodeRef getFromDvkRoot() {
        return fromDvkRoot;
    }

    public void setFromDvkRoot(NodeRef fromDvkRoot) {
        this.fromDvkRoot = fromDvkRoot;
    }

    public NodeRef getIncomingEmailRoot() {
        return incomingEmailRoot;
    }

    public void setIncomingEmailRoot(NodeRef incomingEmailRoot) {
        this.incomingEmailRoot = incomingEmailRoot;
    }

    public NodeRef getReceivedincoiceRoot() {
        return receivedincoiceRoot;
    }

    public void setReceivedincoiceRoot(NodeRef receivedincoiceRoot) {
        this.receivedincoiceRoot = receivedincoiceRoot;
    }

    public NodeRef getSentEmailRoot() {
        return sentEmailRoot;
    }

    public void setSentEmailRoot(NodeRef sentEmailRoot) {
        this.sentEmailRoot = sentEmailRoot;
    }

    public NodeRef getForwardedDecDocumentsRoot() {
        return forwardedDecDocumentsRoot;
    }

    public void setForwardedDecDocumentsRoot(NodeRef forwardedDecDocumentsRoot) {
        this.forwardedDecDocumentsRoot = forwardedDecDocumentsRoot;
    }

}
