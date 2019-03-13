package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.*;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithViewDocPermission;

import java.util.*;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.listener.RefreshEventListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class FileBlockBean implements DocumentDynamicBlock, RefreshEventListener {
    private static final long serialVersionUID = 1L;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FileBlockBean.class);
    public static final String BEAN_NAME = "FileBlockBean";
    public static final String PDF_OVERWRITE_CONFIRMED = "pdfOverwriteConfirmed";

    transient private MultilingualContentService multilingualContentService;
    private NavigationBean navigationBean;
    private List<File> files;
    private int activeFilesCount;
    private int notActiveFilesCount;
    private NodeRef docRef;
    private String pdfUrl;
    private Map<NodeRef, Boolean> listFileCheckboxes = new HashMap<>();
    private Map<NodeRef, Boolean> listInactiveCheckboxes = new HashMap<>();

    public void unlock(ActionEvent event) {
        String filenodeRefStr = ActionUtil.getParam(event, "nodeRef");
        if (StringUtils.isNotBlank(filenodeRefStr)) {
            BeanHelper.getDocLockService().unlockFile(new NodeRef(filenodeRefStr));
            refresh();
        }
    }

    public void toggleActive(ActionEvent event) {
        NodeRef fileNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        try {
            BaseDialogBean.validatePermission(docRef, Privilege.EDIT_DOCUMENT);
            final boolean active = getFileService().toggleActive(fileNodeRef);
            if (LOG.isDebugEnabled()) {
                LOG.debug("changed file active status, nodeRef=" + fileNodeRef + ", new status=" + active);
            }
            getFileService().reorderFiles(docRef, fileNodeRef, !active);
            restore(); // refresh the files list
            MessageUtil.addInfoMessage(active ? "file_toggle_active_success" : "file_toggle_deactive_success", getFileName(fileNodeRef));
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("file_inactive_toggleFailed", e);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            refresh(); // file might have been deleted
        }
    }

    public void updateFilesProperties() {
        // Perform this operation as administrator, because some files may be locked for editing.
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                NodeService nodeService = BeanHelper.getNodeService();
                for (File file : files) {
                    NodeRef fileRef = file.getNodeRef();

                    if (file != null && fileRef != null) {
                        Boolean convertToPdfRepoValue = (Boolean) nodeService.getProperty(fileRef, FileModel.Props.CONVERT_TO_PDF_IF_SIGNED);
                        boolean convertToPdfNewValue = file.isConvertToPdfIfSigned();
                        if (convertToPdfRepoValue == null || convertToPdfRepoValue != convertToPdfNewValue) {
                            nodeService.setProperty(fileRef, FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, convertToPdfNewValue);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("changed file" + ", nodeRef=" + fileRef + ", convertToPdfIfSigned=" + convertToPdfNewValue);
                            }
                        }
                    }
                }
                return null;
            }

        }, AuthenticationUtil.getSystemUserName());
    }

    private String getFileName(NodeRef fileNodeRef) {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService().getFileInfo(fileNodeRef).getName();
    }

    public void confirmDeleteInactiveFiles(ActionEvent event) {
        List<String> fileNames = getSelectedFileNames(listInactiveCheckboxes);
        if (CollectionUtils.isNotEmpty(fileNames)) {
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl("file_delete_multiple_confirm", StringUtils.join(fileNames, ", ")), null,
                    "#{FileBlockBean.deleteInactiveFiles}", null, null, null, null);
        } else {
            MessageUtil.addInfoMessage("file_toggle_no_file_selected");
        }
    }

    public void confirmDeleteActiveFiles(ActionEvent event) {
        List<String> fileNames = getSelectedFileNames(listFileCheckboxes);
        if (CollectionUtils.isNotEmpty(fileNames)) {
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl("file_delete_multiple_confirm", StringUtils.join(fileNames, ", ")), null,
                    "#{FileBlockBean.deleteActiveFiles}", null, null, null, null);
        } else {
            MessageUtil.addInfoMessage("file_toggle_no_file_selected");
        }
    }

    private List<String> getSelectedFileNames(Map<NodeRef, Boolean> selectedCheckboxes) {
        List<String> fileNames = new ArrayList<String>();
        if (MapUtils.isNotEmpty(selectedCheckboxes)) {
            for (NodeRef nodeRef : selectedCheckboxes.keySet()) {
                if (BooleanUtils.isTrue(selectedCheckboxes.get(nodeRef))) {
                    fileNames.add(getFileName(nodeRef));
                }
            }
        }
        return fileNames;
    }

    public void deleteInactiveFiles(ActionEvent event) {
        if (MapUtils.isNotEmpty(listInactiveCheckboxes)) {
            deleteMultiple(listInactiveCheckboxes);
        }
    }

    public void deleteActiveFiles(ActionEvent event) {
        if (MapUtils.isNotEmpty(listFileCheckboxes)) {
            deleteMultiple(listFileCheckboxes);
        }
    }

    private void deleteMultiple(Map<NodeRef, Boolean> listCheckboxes) {
        List<String> deletedFiles = new ArrayList<String>();
        boolean hasSelectedCheckboxes = false;
        for (NodeRef nodeRef : listCheckboxes.keySet()) {
            if (BooleanUtils.isTrue(listCheckboxes.get(nodeRef))) {
                hasSelectedCheckboxes = true;
                deleteFile(nodeRef, deletedFiles);
            }
        }
        if (!hasSelectedCheckboxes) {
            MessageUtil.addInfoMessage("file_toggle_no_file_selected");
        }
        if (CollectionUtils.isNotEmpty(deletedFiles)) {
            MessageUtil.addInfoMessage("file_delete_multiple", StringUtils.join(deletedFiles, ", "));
        }
        restore(); // refresh the files list
    }

    private void deleteFile(NodeRef documentNodeRef, List<String> deletedFiles) {
        Node file = new Node(documentNodeRef);
        NodeRef document = BeanHelper.getNodeService().getPrimaryParent(documentNodeRef).getParentRef();
        if (file.getType().equals(ContentModel.TYPE_CONTENT)) {
            document = getNodeService().getPrimaryParent(documentNodeRef).getParentRef();
        }
        String fileName = file.getName();
        if (getDocLockService().getLockStatus(document) == LockStatus.LOCKED) {
            addLockedMessage(fileName, document);
        } else if (getDocLockService().getLockStatus(documentNodeRef) == LockStatus.LOCKED) {
            addLockedMessage(fileName, documentNodeRef);
        } else { // could be locked: LockStatus: LOCK_OWNER | NO_LOCK | LOCK_EXPIRED
            String displayName = (String) file.getProperties().get(FileModel.Props.DISPLAY_NAME);
            if (ContentModel.TYPE_MULTILINGUAL_CONTAINER.equals(file.getType())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to delete multilingual container: " + file.getId() + " and its translations");
                }
                // delete the mlContainer and its translations
                getMultilingualContentService().deleteTranslationContainer(documentNodeRef);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to delete content node: " + file.getId());
                }
                // delete the node
                getNodeService().deleteNode(documentNodeRef);
            }
            
            if (document != null && getDictionaryService().isSubClass(getNodeService().getType(document), DocumentCommonModel.Types.DOCUMENT)) {
                BeanHelper.getFileService().reorderFiles(document);
                if (StringUtils.isNotBlank(displayName)) {
                    fileName = displayName;
                }
                getDocumentLogService().addDocumentLog(document, MessageUtil.getMessage("document_log_status_fileDeleted", fileName));
                getDocumentService().updateSearchableFiles(document);
            }
            
            NodeRef previouslyGeneratedPdf = BeanHelper.getFileService().getPreviouslyGeneratedPdf(documentNodeRef);
            if (previouslyGeneratedPdf != null) {
                getNodeService().setProperty(previouslyGeneratedPdf, FileModel.Props.PDF_GENERATED_FROM_FILE, null);
            }
            if (document != null && BeanHelper.getConstantNodeRefsBean().getTemplateRoot().equals(document)) {
                BeanHelper.getDocumentTemplateService().removeTemplateFromCache(documentNodeRef);
            }
            deletedFiles.add(fileName);
        }
    }

    private void addLockedMessage(String fileName, NodeRef nodeRef) {
        String lockOwner = getDocLockService().getLockOwnerIfLocked(nodeRef);
        MessageUtil.addErrorMessage("file_delete_multiple_failed", fileName, getUserService().getUserFullName(lockOwner));
    }

    public void deactivateFiles(ActionEvent event) {
        if (MapUtils.isNotEmpty(listFileCheckboxes)) {
            toggleActiveMultiple(listFileCheckboxes, false);
        }
    }

    public void activateFiles(ActionEvent event) {
        if (MapUtils.isNotEmpty(listInactiveCheckboxes)) {
            toggleActiveMultiple(listInactiveCheckboxes, true);
        }
    }
    
    private void toggleActiveMultiple(Map<NodeRef, Boolean> listCheckboxes, boolean makeActive) {
        List<String> succeededFiles = new ArrayList<String>();
        boolean hasSelectedCheckboxes = false;
        for (NodeRef nodeRef : listCheckboxes.keySet()) {
            if (BooleanUtils.isTrue(listCheckboxes.get(nodeRef))) {
                hasSelectedCheckboxes = true;
                try {
                    getFileService().toggleActive(nodeRef);
                    BaseDialogBean.validatePermission(docRef, Privilege.EDIT_DOCUMENT);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("changed file active status, nodeRef=" + nodeRef + ", new status=" + makeActive);
                    }
                    getFileService().reorderFiles(docRef, nodeRef, !makeActive);
                    succeededFiles.add(getFileName(nodeRef));
                } catch (NodeLockedException e) {
                    BeanHelper.getDocumentLockHelperBean().handleLockedFileNode("file_inactive_multiple_toggleFailed", e);
                } catch (UnableToPerformException e) {
                    MessageUtil.addStatusMessage(e);
                }
            }
        }
        if (!hasSelectedCheckboxes) {
            MessageUtil.addInfoMessage("file_toggle_no_file_selected");
        }
        if (CollectionUtils.isNotEmpty(succeededFiles)) {
            MessageUtil.addInfoMessage(makeActive ? "file_toggle_active_multiple_success" : "file_toggle_deactive_multiple_success", StringUtils.join(succeededFiles, ", "));
        }
        restore(); // refresh the files list
    }

    public void transformToPdf(ActionEvent event) {
        NodeRef fileRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        FileInfo pdfFileInfo = null;
        NodeRef previouslyGeneratedPdf = getFileService().getPreviouslyGeneratedPdf(fileRef);
        boolean overwritePdf = ActionUtil.hasParam(event, PDF_OVERWRITE_CONFIRMED);
        if (!overwritePdf && getFileService().isPdfUpToDate(fileRef, previouslyGeneratedPdf)) {
            Map<String, String> params = new HashMap<String, String>(2);
            params.put(PDF_OVERWRITE_CONFIRMED, Boolean.TRUE.toString());
            params.put("nodeRef", fileRef.toString());
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl("file_transform_pdf_info_existing_uptodate"), null, "#{FileBlockBean.transformToPdf}",
                    params, null, null, null);
            return;
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("starting to generated pdf from FileBlockBean call, fileRef=" + fileRef);
            }
            pdfFileInfo = getFileService().transformToPdf(docRef, fileRef, true);
            if (!overwritePdf && pdfFileInfo != null) {
                BeanHelper.getNodeService().setProperty(pdfFileInfo.getNodeRef(), FileModel.Props.FILE_ORDER_IN_LIST, activeFilesCount + 1);
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("file_transform_pdf_error_docLocked", docRef);
            return;
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            refresh(); // file might have been deleted
            return;
        }
        restore(); // refresh the files list
        if (pdfFileInfo != null) {
            MessageUtil.addInfoMessage(previouslyGeneratedPdf == null ? "file_generate_pdf_success" : "file_generate_pdf_version_success", pdfFileInfo.getName(), BeanHelper
                    .getNodeService().getProperty(fileRef, FileModel.Props.DISPLAY_NAME));
            if (LOG.isDebugEnabled()) {
                LOG.debug("generated pdf from FileBlockBean call, file" + ", fileRef=" + fileRef + ", pdfFileRef=" + pdfFileInfo.getNodeRef());
            }
        } else {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_generate_pdf_failed");
            if (LOG.isDebugEnabled()) {
                LOG.debug("generating pdf from FileBlockBean call failed, file" + ", fileRef=" + fileRef);
            }
        }
    }

    public void viewPdf(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        if (!BeanHelper.getNodeService().exists(nodeRef)) {
            MessageUtil.addErrorMessage("file_toggle_failed");
            pdfUrl = null;
            refresh(); // file might have been deleted
            return;
        }
        pdfUrl = DownloadContentServlet.generateBrowserURL(nodeRef, getFileName(nodeRef));
    }

    public void hidePdfBlock(@SuppressWarnings("unused") ActionEvent event) {
        pdfUrl = null;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getNode());
        }
    }

    public void init(Node node) {
        docRef = node.getNodeRef();
        Assert.notNull(docRef, "nodeRef is null - node: " + node);
        restore();
        // Alfresco's AddContentDialog.saveContent uses
        // navigationBean.getCurrentNodeId() for getting the folder to save to
        navigationBean.setCurrentNodeId(node.getId());
        pdfUrl = null;
    }

    public void reset() {
        navigationBean.setCurrentNodeId(BeanHelper.getConstantNodeRefsBean().getDraftsRoot().getId());
        clean();
    }

    @Override
    public void clean() {
        listFileCheckboxes = new HashMap<>();
        listInactiveCheckboxes = new HashMap<>();
        files = null;
        docRef = null;
        pdfUrl = null;
        activeFilesCount = 0;
        notActiveFilesCount = 0;
    }

    @Override
    public void refresh() {
        restore();
    }

    public void restore() {
        files = getFileService().getAllFiles(docRef);
        setListFileCheckboxes(new HashMap<NodeRef, Boolean>());
        setListInactiveCheckboxes(new HashMap<NodeRef, Boolean>());
        countFiles();
    }

    public boolean moveAllFiles(NodeRef toRef) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("starting to move all files from FileBlockBean call, docRef=" + docRef + ", toRef=" + toRef);
            }
            getFileService().moveAllFiles(docRef, toRef);
            return true;
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        } catch (DuplicateChildNodeNameException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "add_file_existing_file", e.getName());
        } catch (FileExistsException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "add_file_existing_file", e.getName());
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "file_not_found");
        }
        return false;
    }

    private void countFiles() {
        activeFilesCount = 0;
        notActiveFilesCount = 0;
        for (File file : files) {
            if (file.isActiveAndNotDigiDoc()) {
                activeFilesCount++;
            } else if (file.isNotActiveAndNotDigiDoc()) {
                notActiveFilesCount++;
            }
        }
    }

    /**
     * Used in JSP page.
     */
    public List<File> getFiles() {
        if (files != null) {
            Collections.sort(files);
        }
        return files;
    }

    public List<File> getActiveFiles() {
        List<File> activeFiles = new ArrayList<>();
        List<File> files = getFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isActive()) {
                    activeFiles.add(file);
                }
            }
        }
        return activeFiles;
    }

    public List<File> getInactiveFiles() {
        List<File> inactiveFiles = new ArrayList<>();
        List<File> files = getFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isActive()) {
                    inactiveFiles.add(file);
                }
            }
        }
        return inactiveFiles;
    }

    public int getActiveFilesCount() {
        return activeFilesCount;
    }

    public int getNotActiveFilesCount() {
        return notActiveFilesCount;
    }

    public boolean isToggleActive() {
        return isToggleActive(true);
    }

    public boolean isToggleInActive() {
        return isToggleActive(false);
    }

    public boolean isToggleActive(boolean fileIsActive) {
        DocumentDialogHelperBean documentDialogHelperBean = BeanHelper.getDocumentDialogHelperBean();
        Node docNode = documentDialogHelperBean.getNode();
        return (!fileIsActive || !documentDialogHelperBean.isNotEditable()) && isOwnerOrManagerWithPermissions(docNode);
    }

    public boolean isDeleteFileAllowed() {
        return isDeleteFileAllowed(true);
    }

    public boolean isDeleteInactiveFileAllowed() {
        return isDeleteFileAllowed(false);
    }

    private boolean isDeleteFileAllowed(boolean activeFile) {
        DocumentDialogHelperBean documentDialogHelperBean = getDocumentDialogHelperBean();
        Node docNode = documentDialogHelperBean.getNode();
        return !documentDialogHelperBean.isNotEditable() && isOwnerOrManagerWithPermissions(docNode);
    }

    private boolean isOwnerOrManagerWithPermissions(Node docNode) {
        return AuthenticationUtil.getRunAsUser().equals(docNode.getProperties().get(DocumentCommonModel.Props.OWNER_ID.toString()))
                || isAdminOrDocmanagerWithViewDocPermission(docNode);
    }

    public boolean isInWorkspace() {
        if (docRef == null) {
            LOG.error("isInWorkspace(): docRef is NULL!");
            return false;
        }
        LOG.debug("docRef: " + docRef.toString());
        if (docRef.getStoreRef() == null) {
            LOG.error("isInWorkspace(): docRef.getStoreRef() is NULL!");
            return false;
        }
        if (docRef.getStoreRef().getProtocol() == null) {
            LOG.error("isInWorkspace(): docRef.getStoreRef() is NULL!");
            return false;
        }
        if (StoreRef.PROTOCOL_WORKSPACE == null) {
            LOG.error("isInWorkspace(): StoreRef.PROTOCOL_WORKSPACE is NULL!");
            return false;
        }
        return docRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE);
    }

    public boolean isGuest() {
        return BeanHelper.getUserService().isGuest();
    }

    // START: getters / setters

    public void setNavigationBean(NavigationBean navigationBean) {
        this.navigationBean = navigationBean;
    }

    public Map<NodeRef, Boolean> getListFileCheckboxes() {
        return listFileCheckboxes;
    }

    public void setListFileCheckboxes(Map<NodeRef, Boolean> listFileCheckboxes) {
        this.listFileCheckboxes = listFileCheckboxes;
    }

    public Map<NodeRef, Boolean> getListInactiveCheckboxes() {
        return listInactiveCheckboxes;
    }

    public void setListInactiveCheckboxes(Map<NodeRef, Boolean> listInactiveCheckboxes) {
        this.listInactiveCheckboxes = listInactiveCheckboxes;
    }
    // END: getters / setters

    public void setMultilingualContentService(MultilingualContentService multilingualContentService)
    {
        this.multilingualContentService = multilingualContentService;
    }

    protected MultilingualContentService getMultilingualContentService()
    {
        if (multilingualContentService == null)
        {
            multilingualContentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMultilingualContentService();
        }
        return multilingualContentService;
    }
}
