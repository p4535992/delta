package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class ChangeFileDialog extends BaseDialogBean {
    public static final String BEAN_NAME = "ChangeFileDialog";
    private static final long serialVersionUID = 1L;

    private NodeRef docRef;
    private List<String> fileNamesWithoutExtension;
    private List<String> fileNames;
    private List<String> fileOrdersInList;
    private List<Long> fileOrderValues;
    private List<FileVO> files;
    private boolean isFileUpload = false;
    private boolean attatchmentsOrScannedFilesAdded = false;

    @Override
    public String cancel() {
        handleLock(false);
        init();
        return super.cancel();
    }

    private void init() {
        fileNamesWithoutExtension = null;
        fileNames = null;
        fileOrdersInList = null;
        fileOrderValues = null;
        files = null;
        isFileUpload = false;
        attatchmentsOrScannedFilesAdded = false;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        if (isFileUpload) {
            fileNames.clear();
            fileOrderValues.clear();
            fileNamesWithoutExtension.clear();

            if (!validate()) {
                return null;
            }
            setFileValues();
            FileUploadBean fileUploadBean = getFileUploadBean();
            if (fileUploadBean != null) {
                fileUploadBean.setFileName(fileNames);
                fileUploadBean.setFileNameWithoutExtension(fileNamesWithoutExtension);
                fileUploadBean.setOrderNumbers(fileOrderValues);
            }
            if (attatchmentsOrScannedFilesAdded) {
                AddFileDialog dialog = BeanHelper.getAddFileDialog();
                List<NodeRef> tempNodeRefs = new ArrayList<>();
                List<String> tempNames = new ArrayList<>();
                List<String> tempNamesWithoutExtensions = new ArrayList<>();
                List<Long> tempOrdersInList = new ArrayList<>();
                List<Boolean> tempSelectedWithMetadata = new ArrayList<>();

                for (FileVO file : files) {
                    if (file.getFileRef() == null) {
                        continue;
                    }
                    tempNodeRefs.add(file.getFileRef());
                    tempNames.add(file.getFileNameWithExtension());
                    tempNamesWithoutExtensions.add(file.getFileNameWithoutExtension());
                    tempOrdersInList.add(Long.valueOf(file.getFileOrderInList()));
                    tempSelectedWithMetadata.add(AddFileDialog.BOUND_METADATA_EXTENSIONS.contains(file.getExtension()));
                }
                dialog.setSelectedFileNodeRef(tempNodeRefs);
                dialog.setSelectedFileName(tempNames);
                dialog.setSelectedFileNameWithoutExtension(tempNamesWithoutExtensions);
                dialog.setSelectedFileOrderInList(tempOrdersInList);
                dialog.setSelectedAssociatedWithMetaData(tempSelectedWithMetadata);
            }
            BeanHelper.getAddFileDialog().save(context, outcome, false);
            return "dialog:close[2]";
        }
        validatePermission(docRef, Privilege.EDIT_DOCUMENT);
        if (!validate()) {
            return null;
        }
        setFileValues();
        // Map<Old name, New name>
        Map<String, String> changedFileNames = new HashMap<>();
        Map<NodeRef, Long> originalOrders = new HashMap<>();
        for (int i = 0; i < files.size(); i++) {
            String newDisplayName = files.get(i).getFileNameWithExtension();
            String newOrderNr = files.get(i).getFileOrderInList();
            boolean nameChanged = false;
            if (!StringUtils.equals(newDisplayName, fileNames.get(i))) { // display name changed
                changedFileNames.put(fileNames.get(i), newDisplayName);
                nameChanged = true;
            }
            if (!nameChanged && StringUtils.equals(newOrderNr, fileOrdersInList.get(i))) { // nothing changed
                continue;
            }
            Map<QName, Serializable> properties = getNodeService().getProperties(files.get(i).getFileRef());

            if (nameChanged) {
                Pair<String, String> filenames = FilenameUtil.getFilenameFromDisplayname(docRef, getFileService().getDocumentFileDisplayNames(docRef),
                        newDisplayName, BeanHelper.getGeneralService());
                properties.put(ContentModel.PROP_NAME, filenames.getFirst());
                properties.put(FileModel.Props.DISPLAY_NAME, filenames.getSecond());
            }
            Long originalOrderNr = (Long) properties.get(FileModel.Props.FILE_ORDER_IN_LIST);
            originalOrders.put(files.get(i).getFileRef(), originalOrderNr);

            properties.put(FileModel.Props.FILE_ORDER_IN_LIST, Long.valueOf(newOrderNr));
            getNodeService().addProperties(files.get(i).getFileRef(), properties);
            getDocumentLogService().addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_fileNameChanged", fileNames.get(i), newDisplayName));
        }
        getFileService().reorderFiles(docRef, originalOrders);
        MessageUtil.addInfoMessage("save_success");
        handleLock(false);
        return "dialog:close#files-panel";
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void open() {
        open(null);
    }

    public FileUploadBean getFileUploadBean() {
        FileUploadBean fileBean = (FileUploadBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
                get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        return fileBean;
    }

    public void open(ActionEvent event) {
        init();
        try {
            files = new ArrayList<>();
            docRef = getDocumentDialogHelperBean().getNodeRef();
            if (event == null) { // file upload, must display only newly uploaded files
                AddFileDialog addFileDialog = BeanHelper.getAddFileDialog();
                attatchmentsOrScannedFilesAdded = addFileDialog.addAttatchmentsAndScannedFiles();
                isFileUpload = true;
                int maxOrderNr = 0;
                List<File> docFiles = getFileService().getAllActiveAndInactiveFiles(docRef);
                Boolean isActive = addFileDialog.isActiveFileDialog();
                for (File f : docFiles) {
                    if (isActive.equals(f.isActive())) {
                        maxOrderNr++;
                    }
                }
                FileUploadBean fuBean = getFileUploadBean();
                if (fuBean != null) {
                    List<String> fileNamez = fuBean.getFileNames();
                    for (String fileName : fileNamez) {
                        files.add(new FileVO(String.valueOf(++maxOrderNr), FilenameUtils.removeExtension(fileName), FilenameUtils.getExtension(fileName), null));
                    }
                }
                if (attatchmentsOrScannedFilesAdded) {
                    List<String> fileNames = addFileDialog.getSelectedFileName();
                    List<NodeRef> fileRefs = addFileDialog.getSelectedFileNodeRef();
                    for (int i = 0; i < fileNames.size(); i++) {
                        String fileName = fileNames.get(i);
                        files.add(new FileVO(String.valueOf(++maxOrderNr), FilenameUtils.removeExtension(fileName), FilenameUtils.getExtension(fileName), fileRefs.get(i)));
                    }
                }
            } else { // started from document dialog
                String type = ActionUtil.getParam(event, "type");
                List<SimpleFile> docFiles = null;
                if ("active".equals(type)) {
                    docFiles = BeanHelper.getBulkLoadNodeService().loadActiveFiles(docRef, null);
                } else {
                    docFiles = BeanHelper.getBulkLoadNodeService().loadInactiveFiles(docRef);
                }

                for (SimpleFile f : docFiles) {
                    String orderInList = String.valueOf(f.getFileOrderInList());
                    files.add(new FileVO(orderInList, FilenameUtils.removeExtension(f.getDisplayName()), FilenameUtils.getExtension(f.getDisplayName()),
                            f.getFileRef()));
                }
            }

            Collections.sort(files);

            fileNames = new ArrayList<>();
            fileOrderValues = new ArrayList<>();
            fileOrdersInList = new ArrayList<>();
            fileNamesWithoutExtension = new ArrayList<>();
            setFileValues();
            handleLock(true);
            WebUtil.navigateTo("dialog:changeFile", FacesContext.getCurrentInstance());
        } catch (RuntimeException e) {
            if (e instanceof NodeLockedException) {
                BeanHelper.getDocumentLockHelperBean().handleLockedNode("file_change_file_validation_alreadyLocked", ((NodeLockedException) e).getNodeRef());
            } else {
                MessageUtil.addErrorMessage("file_locking_failed");
            }
        }
    }

    private void setFileValues() {
        for (FileVO sf : files) {
            fileNames.add(sf.getFileNameWithExtension());
            fileOrderValues.add(Long.valueOf(sf.getFileOrderInList()));
            fileOrdersInList.add(sf.getFileOrderInList());
            fileNamesWithoutExtension.add(sf.getFileNameWithoutExtension());
        }
    }

    private boolean handleLock(boolean lock4Edit) {
        if (isFileUpload) {
            return true;
        }
        List<NodeRef> fileRefs = getFileRefs();
        if (lock4Edit) {
            for (NodeRef fileRef : fileRefs) {
                try {
                    if (getDocLockService().setLockIfFree(fileRef) == LockStatus.LOCKED) {
                        throw new NodeLockedException(fileRef);
                    }
                } catch (RuntimeException e) {
                    getDocLockService().unlockFiles(fileRefs, docRef);
                    if (e instanceof NodeLockedException) {
                        throw e;
                    }
                    throw new RuntimeException();
                }
            }
            return true;
        }
        getDocLockService().unlockFiles(fileRefs, docRef);
        return false;
    }

    private List<NodeRef> getFileRefs() {
        List<NodeRef> fileRefs = new ArrayList<>(files.size());
        for (FileVO f : files) {
            fileRefs.add(f.getFileRef());
        }
        return fileRefs;
    }

    private boolean validate() {
        boolean isValid = true;
        for (int i = 0; i < files.size(); i++) {

            if (StringUtils.isEmpty(files.get(i).getFileNameWithoutExtension())) {
                MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("name"));
                isValid = false;
                break;
            }
            if (isValid && StringUtils.isEmpty(files.get(i).getFileOrderInList())) {
                MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("file_order"));
                isValid = false;
                break;
            }
            if (isValid && !files.get(i).getFileNameWithoutExtension().equals(FilenameUtil.stripForbiddenWindowsCharacters(files.get(i).getFileNameWithoutExtension()))) {
                MessageUtil.addErrorMessage("add_file_invalid_file_name");
                isValid = false;
                break;
            }
            if (isValid) {
                try {
                    fileOrderValues.add(Long.parseLong(files.get(i).getFileOrderInList()));
                } catch (Exception e) {
                    MessageUtil.addErrorMessage("file_order_invalid", MessageUtil.getMessage("file_order"));
                    isValid = false;
                    break;
                }
            }
        }
        return isValid;
    }

    public List<String> getFileOrderInList() {
        return fileOrdersInList;
    }

    public List<FileVO> getFiles() {
        return files;
    }

    public static class FileVO implements Serializable, Comparable<FileVO> {

        private static final long serialVersionUID = 1L;
        private String fileOrderInList;
        private String fileNameWithoutExtension;
        private final String extension;
        private final NodeRef fileRef;

        public FileVO(String fileOrderInList, String fileNameWithoutExtension, String extension, NodeRef fileRef) {
            this.fileOrderInList = fileOrderInList;
            this.fileNameWithoutExtension = fileNameWithoutExtension;
            this.extension = extension;
            this.fileRef = fileRef;
        }

        public String getFileOrderInList() {
            return fileOrderInList;
        }

        public void setFileOrderInList(String fileOrderInList) {
            this.fileOrderInList = fileOrderInList;
        }

        public String getFileNameWithoutExtension() {
            return fileNameWithoutExtension;
        }

        public void setFileNameWithoutExtension(String fileNameWithoutExtension) {
            this.fileNameWithoutExtension = fileNameWithoutExtension;
        }

        public String getExtension() {
            return extension;
        }

        public String getFileNameWithExtension() {
            return getFileNameWithoutExtension() + (StringUtils.isNotBlank(extension) ? ("." + extension) : "");
        }

        public NodeRef getFileRef() {
            return fileRef;
        }

        @Override
        public int compareTo(FileVO o) {
            Long long1 = Long.valueOf(fileOrderInList);
            Long long2 = Long.valueOf(o.fileOrderInList);
            if (long1 < long2) {
                return -1;
            } else if (long1 > long2) {
                return 1;
            }
            return 0;
        }

    }

}
