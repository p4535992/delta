package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDvkService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Add permissions to all folders related to arrived/sent documents/files
 * 
 * @author Ats Uiboupin
 */
public class ArrivedDocumentsPermissionsUpdateBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                executeInTransaction();
                return null;
            }
        }, false, true);
    }

    private void executeInTransaction() {
        addPermissions(getAllFolderRefs());
    }

    protected void addPermissions(List<NodeRef> folderRefs) {
        PrivilegeService privilegeService = BeanHelper.getPrivilegeService();
        for (NodeRef folderRef : folderRefs) {
            LOG.info("Adding permissions to " + folderRef + " - " + getNodeService().getPath(folderRef).toPrefixString(getNamespaceService()));
            privilegeService.setPermissions(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Privileges.EDIT_DOCUMENT);
        }
    }

    protected List<NodeRef> getAllFolderRefs() {
        DvkService dvkService = getDvkService();
        ArrayList<NodeRef> folderRefs = new ArrayList<NodeRef>();
        folderRefs.addAll(getImapChildrenRefs());
        folderRefs.addAll(getOtherFolderRefs(dvkService));
        return folderRefs;
    }

    protected List<NodeRef> getImapChildrenRefs() {
        GeneralService generalService = getGeneralService();
        return Arrays.asList(
                // 1) Sissetulevad e-kirjad
                generalService.getNodeRef(ImapModel.Repo.INCOMING_SPACE)
                // 2) E-kirja manused
                , generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE)
                // 3) Väljasaadetud e-kirjad
                , generalService.getNodeRef(ImapModel.Repo.SENT_SPACE)
                // 4) Ebaõnnestunud saatmised
                , generalService.getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE)
                );
    }

    private List<NodeRef> getOtherFolderRefs(DvkService dvkService) {
        GeneralService generalService = getGeneralService();
        return Arrays.asList(
                // 5) Praak DVK'st
                generalService.getNodeRef(dvkService.getCorruptDvkDocumentsPath())
                // 6) Skanneeritud dokumendid
                , generalService.getNodeRef(ScannedModel.Repo.SCANNED_SPACE)
                // 7) DVK dokumendid
                , generalService.getNodeRef(dvkService.getReceivedDvkDocumentsPath())
                // 8) einvoice
                , generalService.getNodeRef(getDocumentService().getReceivedInvoicePath())
                );
    }

}
