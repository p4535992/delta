package ee.webmedia.alfresco.imap.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.web.Subfolder;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for incoming emails list.
 * 
 * @author Romet Aidla
 */
public abstract class AbstractEmailListDialog extends BaseDocumentListDialog implements FolderListDialog {
    private static final long serialVersionUID = 0L;

    private List<Subfolder> folders;
    private NodeRef parentRef;

    /** @param event */
    public void setup(ActionEvent event) {
        parentRef = ActionUtil.getParentNodeRefParam(event);
        if (parentRef == null) {
            parentRef = getMainFolderRef();
        }
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getIncomingDocuments(parentRef);
        folders = BeanHelper.getImapServiceExt().getImapSubfolders(parentRef, DocumentCommonModel.Types.DOCUMENT);
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
    public List<Subfolder> getFolders() {
        return folders;
    }

    @Override
    public boolean isShowFolderList() {
        return folders != null && !folders.isEmpty();
    }

    public boolean isShowFileList() {
        return (documents != null && !documents.isEmpty()) || !isShowFolderList();
    }

    public CustomChildrenCreator getDocumentRowFileGenerator() {
        return ComponentUtil.getDocumentRowFileGenerator(FacesContext.getCurrentInstance().getApplication());
    }
}
