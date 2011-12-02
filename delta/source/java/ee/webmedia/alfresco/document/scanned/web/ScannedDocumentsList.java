package ee.webmedia.alfresco.document.scanned.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.web.Subfolder;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.imap.web.FolderListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for scanned documents list
 * 
 * @author Romet Aidla
 */
public class ScannedDocumentsList extends BaseDialogBean implements FolderListDialog {
    private static final long serialVersionUID = 0L;

    private List<File> files;
    private NodeRef folderRef;
    private List<Subfolder> folders;

    public void setup(ActionEvent event) {
        init(event);
    }

    public void init(ActionEvent event) {
        folderRef = ActionUtil.getParentNodeRefParam(event);
        if (folderRef == null) {
            folderRef = getMainFolderRef();
        }
        readFiles();
    }

    private NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ScannedModel.Repo.SCANNED_SPACE);
    }

    @Override
    public void restored() {
        readFiles();
    }

    private void readFiles() {
        files = BeanHelper.getFileService().getScannedFiles(folderRef);
        Collections.sort(files, new Comparator<File>() {

            @Override
            public int compare(File f1, File f2) {
                if (f1 == null || f1.getCreated() == null) {
                    return (f2 == null || f2.getCreated() == null) ? 0 : -1;
                }
                if (f2 == null || f2.getCreated() == null) {
                    return 1;
                }
                return f2.getCreated().compareTo(f1.getCreated());
            }
        });
        folders = BeanHelper.getFileService().getSubfolders(folderRef, ContentModel.TYPE_FOLDER, ContentModel.TYPE_CONTENT);
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public String cancel() {
        folderRef = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null; // Finish button is always hidden
    }

    public boolean isShowFileList() {
        return (files != null && !files.isEmpty()) || !isShowFolderList();
    }

    @Override
    public boolean isShowFolderList() {
        return folders != null && !folders.isEmpty();
    }

    @Override
    public List<Subfolder> getFolders() {
        return folders;
    }

    @Override
    public String getFolderListTitle() {
        return MessageUtil.getMessage("document_incoming_emails_folders");
    }
}
