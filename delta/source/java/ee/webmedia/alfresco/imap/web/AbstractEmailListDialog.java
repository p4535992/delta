package ee.webmedia.alfresco.imap.web;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for incoming emails list.
 * 
 * @author Romet Aidla
 */
public abstract class AbstractEmailListDialog extends BaseDocumentListDialog implements FolderListDialog {
    private static final long serialVersionUID = 0L;

    private List<ImapFolder> folders;
    private NodeRef parentRef;

    /** @param event */
    public void setup(ActionEvent event) {
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
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getIncomingDocuments(parentRef);
        folders = BeanHelper.getFileService().getImapSubfolders(parentRef);
    }

    protected abstract NodeRef getMainFolderRef();

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("document_incoming_emails_documents");
    }

    @Override
    public String getFolderListTitle() {
        return MessageUtil.getMessage("document_incoming_emails_folders");
    }

    @Override
    public List<ImapFolder> getFolders() {
        return folders;
    }

    @Override
    public boolean isShowFolderList() {
        return folders != null && !folders.isEmpty();
    }

}
