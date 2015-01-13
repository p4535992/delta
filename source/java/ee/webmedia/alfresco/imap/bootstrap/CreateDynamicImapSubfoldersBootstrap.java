package ee.webmedia.alfresco.imap.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;

/**
 * Dynamically create subfolders for fixed type imap folders
 */
public class CreateDynamicImapSubfoldersBootstrap extends AbstractModuleComponent {

    private GeneralService generalService;
    private NodeService nodeService;
    private ImapServiceExt imapServiceExt;
    private FileService fileService;

    @Override
    protected void executeInternal() throws Throwable {
        addSubfolders(ImapModel.Repo.INCOMING_SPACE);
        addSubfolders(ImapModel.Repo.ATTACHMENT_SPACE);
        addSubfolders(ImapModel.Repo.SENT_SPACE);
        addSubfolders(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE);
    }

    private void addSubfolders(String parentNodeXPath) {
        NodeRef parentRef = generalService.getNodeRef(parentNodeXPath);
        if (imapServiceExt.isFixedFolder(parentRef)) {
            NodeRef parentFolderNodeRef = generalService.getNodeRef(parentNodeXPath);
            String behaviour = (String) nodeService.getProperty(parentFolderNodeRef, ImapModel.Properties.APPEND_BEHAVIOUR);
            for (String subfolderName : imapServiceExt.getFixedSubfolderNames(parentRef)) {
                NodeRef childFolder = fileService.findSubfolderWithName(parentRef, subfolderName, ImapModel.Types.IMAP_FOLDER);
                if (childFolder == null) {
                    imapServiceExt.createImapSubfolder(parentFolderNodeRef, behaviour, subfolderName, subfolderName);
                }
            }
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    public ImapServiceExt getImapServiceExt() {
        return imapServiceExt;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
