package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDvkService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Add permissions to all folders related to arrived/sent documents/files
 * 
 * @author Ats Uiboupin
 */
public class ArrivedDocumentsPermissionsUpdateBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        GeneralService generalService = getGeneralService();
        DvkService dvkService = getDvkService();
        List<NodeRef> folderRefs = Arrays.asList(
                // 1) Sissetulevad e-kirjad
                generalService.getNodeRef(ImapModel.Repo.INCOMING_SPACE)
                // 2) E-kirja manused
                , generalService.getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE)
                // 3) Väljasaadetud e-kirjad
                , generalService.getNodeRef(ImapModel.Repo.SENT_SPACE)
                // 4) Praak DVK'st
                , generalService.getNodeRef(dvkService.getCorruptDvkDocumentsPath())
                // 5) Skanneeritud dokumendid
                , generalService.getNodeRef(ScannedModel.Repo.SCANNED_SPACE)
                // 6) Ebaõnnestunud saatmised
                , generalService.getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE)
                // 7) DVK dokumendid
                , generalService.getNodeRef(dvkService.getReceivedDvkDocumentsPath())
                // 8) einvoice
                , generalService.getNodeRef(getDocumentService().getReceivedInvoicePath())
                );

        for (NodeRef folderRef : folderRefs) {
            for (String permission : Arrays.asList(Privileges.VIEW_DOCUMENT_META_DATA, Privileges.EDIT_DOCUMENT, Privileges.VIEW_DOCUMENT_FILES)) {
                BeanHelper.getPermissionService().setPermission(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, permission, true);
            }
        }
    }
}
