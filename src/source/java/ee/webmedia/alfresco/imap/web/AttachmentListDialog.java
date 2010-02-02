package ee.webmedia.alfresco.imap.web;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import java.util.List;

/**
 * Email attachments list dialog.
 *
 * @author Romet Aidla
 */
public class AttachmentListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient ImapServiceExt imapServiceExt;
    private transient GeneralService generalService;
    private FileService fileService;

    public void init(ActionEvent event) {
        readFiles();
    }
    
    @Override
    public void restored() {
        readFiles();
        super.restored();
    }

    private void readFiles() {
        Node node = getGeneralService().fetchNode(getImapServiceExt().getAttachmentRoot());
        Assert.notNull(node, "Attachment root not found");
        files = getFileService().getAllFilesExcludingDigidocSubitems(node.getNodeRef());
    }

    private List<File> files;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // Finish button is always hidden
        return null;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    public ImapServiceExt getImapServiceExt() {
        if (imapServiceExt == null) {
            imapServiceExt = (ImapServiceExt) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(ImapServiceExt.BEAN_NAME);
        }
        return imapServiceExt;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

    public List<File> getFiles() {
        return files;
    }

    public Node getNode() {
        return null;
    }
}
