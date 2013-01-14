package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithViewDocPermission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.lock.NodeLockedException;
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
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.listener.RefreshEventListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.web.evaluator.IsOwnerEvaluator;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Dmitri Melnikov
 */
public class FileBlockBean implements DocumentDynamicBlock, RefreshEventListener {
    private static final long serialVersionUID = 1L;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FileBlockBean.class);
    public static final String BEAN_NAME = "FileBlockBean";
    public static final String PDF_OVERWRITE_CONFIRMED = "pdfOverwriteConfirmed";

    private NavigationBean navigationBean;
    private List<File> files;
    private int activeFilesCount;
    private int notActiveFilesCount;
    private NodeRef docRef;
    private String pdfUrl;

    public void toggleActive(ActionEvent event) {
        NodeRef fileNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        try {
            BaseDialogBean.validatePermission(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
            final boolean active = getFileService().toggleActive(fileNodeRef);
            if (LOG.isDebugEnabled()) {
                LOG.debug("changed file active status, nodeRef=" + fileNodeRef + ", new status=" + active);
            }
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
                    if (file != null && file.getNodeRef() != null) {
                        nodeService.setProperty(file.getNodeRef(), FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, file.isConvertToPdfIfSigned());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("changed file" + ", nodeRef=" + file.getNodeRef() + ", convertToPdfIfSigned=" + file.isConvertToPdfIfSigned());
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

    public void transformToPdf(ActionEvent event) {
        NodeRef fileRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        FileInfo pdfFileInfo = null;
        NodeRef previouslyGeneratedPdf = getFileService().getPreviouslyGeneratedPdf(fileRef);
        if (!ActionUtil.hasParam(event, PDF_OVERWRITE_CONFIRMED) && getFileService().isPdfUpToDate(fileRef, previouslyGeneratedPdf)) {
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
        files = null;
        docRef = null;
        navigationBean.setCurrentNodeId(getDocumentService().getDrafts().getId());
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
     * 
     * @return
     */
    public List<File> getFiles() {
        return files;
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
        return (!fileIsActive || !documentDialogHelperBean.isNotEditable())
                && (new IsOwnerEvaluator().evaluate(docNode) || PrivilegeUtil.isAdminOrDocmanagerWithPermission(docNode, Privileges.VIEW_DOCUMENT_META_DATA));
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
        return !documentDialogHelperBean.isNotEditable()
                && (new IsOwnerEvaluator().evaluate(docNode) || isAdminOrDocmanagerWithViewDocPermission(docNode))
                && (!activeFile || !getWorkflowService().hasInprogressCompoundWorkflows(docRef));
    }

    public boolean isInWorkspace() {
        return docRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE);
    }

    // START: getters / setters

    public void setNavigationBean(NavigationBean navigationBean) {
        this.navigationBean = navigationBean;
    }
    // END: getters / setters

}
