package ee.webmedia.alfresco.imap.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Email attachments list dialog.
 * 
 * @author Romet Aidla
 */
public class AttachmentListDialog extends BaseDialogBean implements FolderListDialog {
    private static final long serialVersionUID = 1L;

    private List<File> files;
    private List<ImapFolder> folders;
    private NodeRef parentRef;

    public void setup(ActionEvent event) {
        init(event);
    }

    public void init(ActionEvent event) {
        parentRef = null;
        if (ActionUtil.hasParam(event, PARAM_PARENT_NODEREF)) {
            String folderRef = ActionUtil.getParam(event, PARAM_PARENT_NODEREF);
            if (StringUtils.isNotBlank(folderRef)) {
                parentRef = new NodeRef(folderRef);
            }
        }
        if (parentRef == null) {
            parentRef = getMainFolderRef();
        }
        readFiles();
    }

    private NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.ATTACHMENT_SPACE);
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        Node node = BeanHelper.getGeneralService().fetchNode(parentRef);
        Assert.notNull(node, "Attachment root not found");
        List<File> temp = BeanHelper.getFileService().getAllFilesExcludingDigidocSubitems(node.getNodeRef());
        files = new ArrayList<File>();
        for (int i = temp.size(); i > 0; i--) {
            files.add(temp.get(i - 1));
        }
        folders = BeanHelper.getFileService().getImapSubfolders(parentRef);
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // Finish button is always hidden
        return null;
    }

    public Node getNode() {
        return null;
    }

    @Override
    public boolean isShowFolderList() {
        return folders != null && !folders.isEmpty();
    }

    @Override
    public List<ImapFolder> getFolders() {
        return folders;
    }

    @Override
    public String getFolderListTitle() {
        return MessageUtil.getMessage("document_incoming_emails_folders");
    }
}
