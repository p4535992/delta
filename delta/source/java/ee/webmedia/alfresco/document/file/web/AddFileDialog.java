package ee.webmedia.alfresco.document.file.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.utils.FilenameUtil.getFilenameFromDisplayname;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectManyMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.einvoice.generated.EInvoice;
import ee.webmedia.alfresco.document.einvoice.generated.Invoice;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Dmitri Melnikov
 * @author Kaarel Jõgeva
 */
public class AddFileDialog extends BaseDialogBean implements Validator {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddFileDialog";

    private static final String ERR_EXISTING_FILE = "add_file_existing_file";

    private transient UserService userService;
    private transient ImapServiceExt imapServiceExt;
    private transient FileService fileService;
    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient GeneralService generalService;
    private transient EInvoiceService eInvoiceService;

    private transient HtmlPanelGroup uploadedFilesPanelGroup;
    private transient HtmlSelectManyMenu attachmentSelect;
    private transient HtmlSelectManyMenu scannedSelect;

    private boolean isFileSelected = false;
    private List<NodeRef> selectedFileNodeRef;
    private List<String> selectedFileName;
    private List<String> selectedFileNameWithoutExtension;
    private DocumentDialog documentDialog;

    private NodeRef attachmentParentRef;
    private NodeRef scannedParentRef;
    private boolean inactiveFileDialog;

    @Override
    public String cancel() {
        reset();
        return "dialog:close#files-panel";
    }

    /**
     * Used by scanned and attachment lists
     * 
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String[] values = (String[]) value;
        for (String val : values) {
            NodeRef nodeRef = new NodeRef(val);
            NodeRef parentRef = getNodeService().getPrimaryParent(nodeRef).getParentRef();
            boolean hasAspect = getNodeService().hasAspect(parentRef, DocumentCommonModel.Aspects.COMMON);
            if (parentRef != null && hasAspect) {
                String msg = MessageUtil.getMessage("file_file_already_added_to_document", getFileService().getFile(nodeRef).getName());
                throw new ValidatorException(new FacesMessage(msg));
            }
        }
    }

    public void start(@SuppressWarnings("unused") ActionEvent event) {
        reset();
    }

    public void startInactive(ActionEvent event) {
        start(event);
        setInactiveFileDialog(true);
    }

    public String reset() {
        isFileSelected = false;
        selectedFileNodeRef = null;
        selectedFileName = null;
        selectedFileNameWithoutExtension = null;
        uploadedFilesPanelGroup = null;
        attachmentSelect = null;
        scannedSelect = null;
        attachmentParentRef = null;
        scannedParentRef = null;
        inactiveFileDialog = false;
        clearUpload();
        return null;
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        if (getFileUploadBean() == null && getSelectedFileNodeRef() == null) {
            buttons.add(new DialogButtonConfig("confirmFileSelectionButton", null, "file_confirm_selected_files",
                    "#{AddFileDialog.confirmSelectedFiles}", "false", null));
        }
        return buttons;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        try {
            try {
                NodeRef documentNodeRef = getDocumentDialogHelperBean().getNodeRef();
                validatePermission(documentNodeRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
                List<String> existingDisplayNames = getFileService().getDocumentFileDisplayNames(documentNodeRef);
                Map<Integer, EInvoice> attachmentInvoices = new HashMap<Integer, EInvoice>();
                Map<Integer, EInvoice> fileInvoices = new HashMap<Integer, EInvoice>();
                boolean isParseInvoice = getEInvoiceService().isEinvoiceEnabled()
                        && DocumentSubtypeModel.Types.INVOICE.equals(getNodeService().getType(documentNodeRef));
                boolean invoiceAdded = false;
                if (isFileSelected) {
                    for (int i = 0; i < selectedFileNodeRef.size(); i++) {
                        checkEncryptedFile(selectedFileName.get(i));
                    }
                    for (int i = 0; i < selectedFileNodeRef.size(); i++) {
                        Pair<String, String> filenames = getAttachmentFilenames(documentNodeRef, existingDisplayNames, i);
                        NodeRef fileRef = selectedFileNodeRef.get(i);
                        if (isParseInvoice) {
                            EInvoice einvoice = EInvoiceUtil.unmarshalEInvoice(getFileFolderService().getReader(fileRef).getContentInputStream());
                            if (einvoice != null) {
                                if (!invoiceAdded) {
                                    getEInvoiceService().setDocPropsFromInvoice(einvoice.getInvoice().get(0), documentNodeRef, null, false);
                                    addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, fileRef, existingDisplayNames);
                                    invoiceAdded = true;
                                }
                                attachmentInvoices.put(i, einvoice);
                            } else {
                                addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, fileRef, existingDisplayNames);
                            }
                        } else {
                            addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, fileRef, existingDisplayNames);
                        }
                    }
                }
                if (getFileUploadBean() != null) {
                    List<java.io.File> files = getFileUploadBean().getFiles();
                    List<String> fileNames = getFileUploadBean().getFileNames();
                    List<String> fileNameWithoutExtension = getFileUploadBean().getFileNameWithoutExtension();
                    for (int i = 0; i < files.size(); i++) {
                        checkEncryptedFile(fileNames.get(i));
                    }
                    for (int i = 0; i < files.size(); i++) {
                        Pair<String, String> filenames = getFileFilenames(documentNodeRef, existingDisplayNames, fileNames, fileNameWithoutExtension, i);
                        java.io.File file = files.get(i);
                        String mimeType = getFileUploadBean().getContentTypes().get(i);
                        if (isParseInvoice) {
                            EInvoice einvoice = EInvoiceUtil.unmarshalEInvoice(file);
                            if (einvoice != null) {
                                if (!invoiceAdded) {
                                    getEInvoiceService().setDocPropsFromInvoice(einvoice.getInvoice().get(0), documentNodeRef, null, false);
                                    addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, existingDisplayNames, file, mimeType);
                                    invoiceAdded = true;
                                }
                                fileInvoices.put(i, einvoice);
                            } else {
                                addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, existingDisplayNames, file, mimeType);
                            }
                        } else {
                            addFileAndFilename(filenames.getFirst(), filenames.getSecond(), documentNodeRef, existingDisplayNames, file, mimeType);
                        }
                    }
                }

                getDocumentService().updateSearchableFiles(documentNodeRef);

                if (invoiceAdded) {
                    documentDialog.reloadDocAndClearPropertySheet(true);
                }

                // generate new invoice files in case of multiple uploaded invoice xmls
                createAdditionalInvoices(attachmentInvoices, documentNodeRef, !documentDialog.isDraft());
                createAdditionalInvoices(fileInvoices, documentNodeRef, !documentDialog.isDraft());

            } catch (NodeLockedException e) {
                BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_addFile_error_docLocked");
                return outcome;
            }
            return outcome;
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }
    }

    private void checkEncryptedFile(String fileName) {
        if (FilenameUtil.isEncryptedFile(fileName)) {
            throw new UnableToPerformException("file_encrypted_forbidden");
        }
    }

    public Pair<String, String> getFileFilenames(NodeRef documentNodeRef, List<String> existingFilenames
            , List<String> fileNames, List<String> fileNameWithoutExtension, int i) {
        String displayName = fileNameWithoutExtension.get(i) + "." + FilenameUtils.getExtension(fileNames.get(i));
        return getFilenameFromDisplayname(documentNodeRef, existingFilenames, displayName, BeanHelper.getGeneralService());
    }

    public Pair<String, String> getAttachmentFilenames(NodeRef documentNodeRef, List<String> existingDisplayNames, int i) {
        String displayName = selectedFileNameWithoutExtension.get(i) + "." + FilenameUtils.getExtension(selectedFileName.get(i));
        return getFilenameFromDisplayname(documentNodeRef, existingDisplayNames, displayName, BeanHelper.getGeneralService());
    }

    public void addFileAndFilename(String name, String displayName, NodeRef documentNodeRef, NodeRef fileRef, List<String> existingFilenames) {
        getFileService().addFileToDocument(name, displayName, documentNodeRef, fileRef, isActiveFileDialog());
        existingFilenames.add(name);
    }

    public void addFileAndFilename(String name, String displayName, NodeRef documentNodeRef, List<String> existingFilenames, java.io.File file, String mimeType) {
        getFileService().addFileToDocument(name, displayName, documentNodeRef, file, mimeType, isActiveFileDialog());
        existingFilenames.add(name);
    }

    public void createAdditionalInvoices(Map<Integer, EInvoice> attachmentInvoices, NodeRef originalDocument, boolean save) {
        boolean isFirstInvoice = true;
        for (Map.Entry<Integer, EInvoice> entry : attachmentInvoices.entrySet()) {
            for (Invoice invoice : entry.getValue().getInvoice()) {
                if (isFirstInvoice) {
                    isFirstInvoice = false;
                    continue;
                }
                Node doc = getDocumentService().createDocument(DocumentSubtypeModel.Types.INVOICE);
                NodeRef docRef = doc.getNodeRef();
                getEInvoiceService().setDocPropsFromInvoice(invoice, docRef, null, false);
                getDocumentService().setTransientProperties(doc, getDocumentService().getAncestorNodesByDocument(originalDocument));
                List<String> existingFilenames = new ArrayList<String>();
                if (isFileSelected) {
                    for (int i = 0; i < selectedFileNodeRef.size(); i++) {
                        if (i == entry.getKey() || !attachmentInvoices.containsKey(new Integer(i))) {
                            Pair<String, String> filenames = getAttachmentFilenames(docRef, existingFilenames, i);
                            addFileAndFilename(filenames.getFirst(), filenames.getSecond(), docRef, selectedFileNodeRef.get(i), existingFilenames);
                        }
                    }
                }
                if (getFileUploadBean() != null) {
                    List<java.io.File> files = getFileUploadBean().getFiles();
                    List<String> fileNames = getFileUploadBean().getFileNames();
                    List<String> fileNameWithoutExtension = getFileUploadBean().getFileNameWithoutExtension();
                    for (int i = 0; i < files.size(); i++) {
                        if (i == entry.getKey() || !attachmentInvoices.containsKey(new Integer(i))) {
                            Pair<String, String> filenames = getFileFilenames(docRef, existingFilenames, fileNames, fileNameWithoutExtension, i);
                            addFileAndFilename(filenames.getFirst(), filenames.getSecond(), docRef
                                    , existingFilenames, files.get(i), getFileUploadBean().getContentTypes().get(i));
                        }
                    }
                }
                if (save) {
                    // E-invoice functionality is not yet enabled in Delta 3.x
                    // When enabling e-invoice functionality in Delta 3.x, update the following code:
                    if (true) {
                        throw new RuntimeException("Update code");
                    }
                    // Use documentDynamicService.update... instead
                    // getDocumentService().updateDocument(doc);
                    getDocumentService().updateSearchableFiles(docRef);
                } else {
                    documentDialog.getNewInvoiceDocuments().add(docRef);
                }
            }
        }
    }

    // TODO: optimize, store unmarshalled invoices for later use?
    public boolean isNeedMultipleInvoiceConfirmation() {
        if (!getEInvoiceService().isEinvoiceEnabled()) {
            return false;
        }
        if (SystematicDocumentType.INVOICE.getId().equals(getDocTypeIdAndVersionNr(getDocumentDialogHelperBean().getNode()).getFirst())) {
            return false;
        }
        boolean hasEinvoice = false;
        if (isFileSelected) {
            for (NodeRef fileRef : selectedFileNodeRef) {
                EInvoice einvoice = EInvoiceUtil.unmarshalEInvoice(getFileFolderService().getReader(fileRef).getContentInputStream());
                if (einvoice != null) {
                    if (hasEinvoice) {
                        return true;
                    }
                    if (einvoice.getInvoice().size() > 1) {
                        return true;
                    }
                    hasEinvoice = true;
                }
            }
        }
        if (getFileUploadBean() != null && getFileUploadBean().getFiles() != null) {
            for (java.io.File file : getFileUploadBean().getFiles()) {
                EInvoice einvoice = EInvoiceUtil.unmarshalEInvoice(file);
                if (einvoice != null) {
                    if (hasEinvoice) {
                        return true;
                    }
                    if (einvoice.getInvoice().size() > 1) {
                        return true;
                    }
                    hasEinvoice = true;
                }
            }
        }
        return false;
    }

    public boolean isShowAttachmentFolderSelect() {
        return getAttachmentFolders().size() > 1;
    }

    public boolean isShowScannedFolderSelect() {
        return getScannedFolders().size() > 1;
    }

    @Override
    public void restored() {
        refreshUploadedFilesPanelGroup();
        super.restored();
    }

    @Override
    protected String getErrorOutcome(Throwable exception) {
        if (exception instanceof IntegrityException) {
            Utils.addErrorMessage(MessageUtil.getMessage(FacesContext.getCurrentInstance(), FilenameUtil.ERR_INVALID_FILE_NAME));
            return ""; // Don't return null!
        }
        return super.getErrorOutcome(exception);
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME + "#files-panel";
    }

    protected void clearUpload() {
        // remove the file upload bean from the session
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
    }

    public String getFileNameWithoutExtension() {
        return FilenameUtils.removeExtension(getFileName());
    }

    public void setFileNameWithoutExtension(String name, int index) {
        getFileUploadBean().getFileNameWithoutExtension().set(index, name);
    }

    public String getFileName() {
        return getFileName(-1);
    }

    public String getFileName(int index) {
        index = (index < 0) ? 0 : index;

        if (isFileSelected) {
            return selectedFileName.get(index);
        }
        FileUploadBean fileUploadBean = getFileUploadBean();

        return (fileUploadBean != null) ? fileUploadBean.getFileNames().get(index) : "";
    }

    public String getFileUploadSuccessMsg() {
        String msg = MessageUtil.getMessage(FacesContext.getCurrentInstance(), "file_upload_success");
        Object[] uploaded = new Object[0];
        if (getFileUploadBean() != null) {
            uploaded = getFileUploadBean().getFileNames().toArray();
        }
        Object[] names = ArrayUtils.addAll(
                uploaded,
                selectedFileName == null ? null : selectedFileName.toArray());
        String namesStr = "";
        if (names.length > 2) {
            namesStr = StringUtils.join(names, ", ", 0, names.length - 1);
            namesStr += " " + MessageUtil.getMessage("file_and") + " " + names[names.length - 1];
        } else if (names.length == 2) {
            namesStr = StringUtils.join(names, " " + MessageUtil.getMessage("file_and") + " ");
        } else if (names.length == 1) {
            namesStr = names[0].toString();
        }
        return MessageFormat.format(msg, namesStr);
    }

    public HtmlPanelGroup getUploadedFilesPanelGroup() {
        return uploadedFilesPanelGroup;
    }

    public void setUploadedFilesPanelGroup(HtmlPanelGroup panelGroup) {
        if (uploadedFilesPanelGroup == null) {
            uploadedFilesPanelGroup = panelGroup;
            refreshUploadedFilesPanelGroup();
        } else {
            uploadedFilesPanelGroup = panelGroup;
        }
    }

    /**
     * @param app
     */
    private void refreshUploadedFilesPanelGroup() {
        @SuppressWarnings("unchecked")
        List<UIComponent> groupChildren = uploadedFilesPanelGroup.getChildren();
        groupChildren.clear();

        Application app = FacesContext.getCurrentInstance().getApplication();
        HtmlPanelGrid uploadedFilesGrid = (HtmlPanelGrid) app.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        uploadedFilesGrid.setStyleClass("table-padding");
        uploadedFilesGrid.setColumnClasses("propertiesLabel,");
        uploadedFilesGrid.setColumns(3);

        groupChildren.add(uploadedFilesGrid);

        @SuppressWarnings("unchecked")
        List<UIComponent> gridChildren = uploadedFilesGrid.getChildren();
        int rowCount = 0, size = 0;
        FileUploadBean fileBean = getFileUploadBean();
        if (fileBean != null) {
            size = fileBean.getFiles().size();
        }

        for (int i = 0; i < size; i++, rowCount++) { // Uploaded files
            gridChildren.add(createLabel(app, rowCount));

            String nameValueBinding = "#{AddFileDialog.fileUploadBean.fileNameWithoutExtension[" + i + "]}";
            gridChildren.add(createInput(app, rowCount, nameValueBinding));

            String deleteMethodBinding = "#{AddFileDialog.removeUploadedFile}";
            gridChildren.add(createDelete(app, rowCount, i, deleteMethodBinding));
        }

        if (selectedFileNodeRef == null) {
            return; // We can skip further processing
        }

        for (int i = 0; i < selectedFileNodeRef.size(); i++, rowCount++) { // Scanned files and email-attachments
            gridChildren.add(createLabel(app, rowCount));

            String nameValueBinding = "#{AddFileDialog.selectedFileNameWithoutExtension[" + i + "]}";
            gridChildren.add(createInput(app, rowCount, nameValueBinding));

            String deleteMethodBinding = "#{AddFileDialog.removeSelectedFile}";
            gridChildren.add(createDelete(app, rowCount, i, deleteMethodBinding));
        }
    }

    private UIActionLink createDelete(Application app, int rowCount, int i, String deleteBinding) {
        UIActionLink delete = (UIActionLink) app.createComponent("org.alfresco.faces.ActionLink");
        delete.setId("file-remove-link-" + rowCount);
        delete.setImage("/images/icons/delete.gif");
        delete.setValue("");
        delete.setTooltip(MessageUtil.getMessage("delete"));
        delete.setActionListener(app.createMethodBinding(deleteBinding, UIActions.ACTION_CLASS_ARGS));
        delete.setShowLink(false);
        putAttribute(delete, "styleClass", "icon-link");

        UIParameter index = (UIParameter) app.createComponent(UIParameter.COMPONENT_TYPE);
        index.setName("index");
        index.setValue(i);
        addChildren(delete, index);
        return delete;
    }

    private UIComponent createInput(Application app, int rowCount, String nameValueBinding) {
        UIInput nameInput = (UIInput) app.createComponent(HtmlInputTextarea.COMPONENT_TYPE);
        nameInput.setValueBinding("value", app.createValueBinding(nameValueBinding));
        nameInput.setId("uploaded-file-input-" + rowCount);
        nameInput.setRequired(true);
        nameInput.setValidator(app.createMethodBinding("#{AddFileDialog.validateFileName}", new Class[] { FacesContext.class, UIComponent.class,
                Object.class }));
        putAttribute(nameInput, "styleClass", "expand19-200");
        return nameInput;
    }

    private HtmlOutputText createLabel(Application app, int rowCount) {
        HtmlOutputText label = (HtmlOutputText) app.createComponent(HtmlOutputText.COMPONENT_TYPE);
        label.setValue("<span class=\"red\">* </span>" + MessageUtil.getMessage("name"));
        label.setEscape(false);
        label.setId("uploaded-file-label-" + rowCount);
        return label;
    }

    public void removeUploadedFile(ActionEvent event) {
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        getFileUploadBean().removeFile(index);
        refreshUploadedFilesPanelGroup();
    }

    public void removeSelectedFile(ActionEvent event) {
        int index = Integer.parseInt(ActionUtil.getParam(event, "index"));
        selectedFileNodeRef.remove(index);
        selectedFileName.remove(index);
        refreshUploadedFilesPanelGroup();
    }

    public FileUploadBean getFileUploadBean() {
        FileUploadBean fileBean = (FileUploadBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().
                get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        return fileBean;
    }

    public List<NodeRef> getSelectedFileNodeRef() {
        return selectedFileNodeRef;
    }

    public List<String> getSelectedFileName() {
        return selectedFileName;
    }

    public void validateFileName(@SuppressWarnings("unused") FacesContext context, UIComponent component, Object value) {
        if (component instanceof HtmlInputText) {
            boolean isValid = true;
            if (!value.toString().equals(FilenameUtil.stripForbiddenWindowsCharacters(value.toString()))) {
                isValid = false;
            }

            String styleClass = ((HtmlInputText) component).getStyleClass();
            styleClass = (styleClass == null) ? "" : styleClass;
            if (isValid) {
                styleClass = styleClass.replaceAll("[\\s]error", "");
            } else {
                if (!styleClass.contains("error")) {
                    styleClass += styleClass + " error";
                }
            }

            ((HtmlInputText) component).setStyleClass(styleClass);
        }
    }

    public void confirmSelectedFiles() {
        String[] attachments = getAttachmentSelect() == null ? new String[0] : (String[]) getAttachmentSelect().getValue();
        String[] scanned = getScannedSelect() == null ? new String[0] : (String[]) getScannedSelect().getValue();

        String[] selected = (String[]) ArrayUtils.addAll(attachments, scanned);

        if (selectedFileNodeRef == null) {
            selectedFileNodeRef = new ArrayList<NodeRef>(selected.length);
        }

        if (selectedFileName == null) {
            selectedFileName = new ArrayList<String>(selected.length);
        }

        if (selectedFileNameWithoutExtension == null) {
            selectedFileNameWithoutExtension = new ArrayList<String>(selected.length);
        }

        for (String nodeRefStr : selected) {
            NodeRef nodeRef = new NodeRef(nodeRefStr);
            File file = getFileService().getFile(nodeRef);
            Assert.notNull(file, "Selected file was not found.");

            selectedFileNodeRef.add(nodeRef);
            selectedFileName.add(file.getName());
            selectedFileNameWithoutExtension.add(FilenameUtils.getBaseName(file.getName()));
        }

        isFileSelected = (selectedFileNodeRef.size() > 0);
    }

    public void attachmentFolderSelected(ActionEvent event) {
        String folderNodeRefStr = (String) ((UIInput) event.getComponent().findComponent("attachment-folder-select")).getValue();
        if (StringUtils.isBlank(folderNodeRefStr)) {
            attachmentParentRef = null;
        } else {
            attachmentParentRef = new NodeRef(folderNodeRefStr);
        }
    }

    public void scannedFolderSelected(ActionEvent event) {
        String folderNodeRefStr = (String) ((UIInput) event.getComponent().findComponent("scanned-folder-select")).getValue();
        if (StringUtils.isBlank(folderNodeRefStr)) {
            scannedParentRef = null;
        } else {
            scannedParentRef = new NodeRef(folderNodeRefStr);
        }
    }

    // START: getters / setters

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected ImapServiceExt getImapServiceExt() {
        if (imapServiceExt == null) {
            imapServiceExt = (ImapServiceExt) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ImapServiceExt.BEAN_NAME);
        }
        return imapServiceExt;
    }

    public void setImapServiceExt(ImapServiceExt imapServiceExt) {
        this.imapServiceExt = imapServiceExt;
    }

    protected FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    protected EInvoiceService getEInvoiceService() {
        if (eInvoiceService == null) {
            eInvoiceService = (EInvoiceService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(EInvoiceService.BEAN_NAME);
        }
        return eInvoiceService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    public List<SelectItem> getAttachments() {
        List<SelectItem> attachments = new ArrayList<SelectItem>();
        List<File> files = getFileService().getAllFilesExcludingDigidocSubitems(getAttachmentParenNodeRef());
        for (File file : files) {
            attachments.add(new SelectItem(file.getNodeRef().toString(), file.getName()));
        }
        return attachments;
    }

    public NodeRef getAttachmentParenNodeRef() {
        if (attachmentParentRef == null) {
            attachmentParentRef = BeanHelper.getImapServiceExt().getAttachmentRoot();
        }
        return attachmentParentRef;
    }

    public List<SelectItem> getAttachmentFolders() {
        List<SelectItem> attachmentFolders = new ArrayList<SelectItem>();
        NodeRef attachmentRootFolderRef = BeanHelper.getImapServiceExt().getAttachmentRoot();
        attachmentFolders.add(new SelectItem(attachmentRootFolderRef.toString(), MessageUtil.getMessage("menu_email_attachments")));
        addFolderSelectItems(attachmentFolders, BeanHelper.getImapServiceExt().getImapSubfolders(attachmentRootFolderRef, ContentModel.TYPE_CONTENT), " ");
        return attachmentFolders;
    }

    public NodeRef getScannedParenNodeRef() {
        if (scannedParentRef == null) {
            scannedParentRef = BeanHelper.getGeneralService().getNodeRef(ScannedModel.Repo.SCANNED_SPACE);
        }
        return scannedParentRef;
    }

    public List<SelectItem> getScannedFolders() {
        List<SelectItem> attachmentFolders = new ArrayList<SelectItem>();
        NodeRef scannedRootFolderRef = BeanHelper.getGeneralService().getNodeRef(ScannedModel.Repo.SCANNED_SPACE);
        attachmentFolders.add(new SelectItem(scannedRootFolderRef.toString(), MessageUtil.getMessage("menu_scanned_documents")));
        addFolderSelectItems(attachmentFolders, BeanHelper.getFileService().getSubfolders(scannedRootFolderRef, ContentModel.TYPE_FOLDER, ContentModel.TYPE_CONTENT), " ");
        return attachmentFolders;
    }

    private void addFolderSelectItems(List<SelectItem> attachmentFolders, List<Subfolder> subfolders, String prefix) {
        for (Subfolder subfolder : subfolders) {
            String itemLabel = prefix + subfolder.getName();
            attachmentFolders.add(new SelectItem(subfolder.getNodeRef().toString(), itemLabel));
            addFolderSelectItems(attachmentFolders, BeanHelper.getImapServiceExt().getImapSubfolders(subfolder.getNodeRef(), ContentModel.TYPE_CONTENT), prefix + prefix);
        }
    }

    public List<SelectItem> getScannedFiles() {
        List<SelectItem> scannedFiles = new ArrayList<SelectItem>();
        List<File> files = getFileService().getScannedFiles(getScannedParenNodeRef());
        for (File file : files) {
            scannedFiles.add(new SelectItem(file.getNodeRef().toString(), file.getName()));
        }
        return scannedFiles;
    }

    public String getOnChangeStyleClass() {
        return ComponentUtil.getOnChangeStyleClass();
    }

    public HtmlSelectManyMenu getScannedSelect() {
        return scannedSelect;
    }

    public void setScannedSelect(HtmlSelectManyMenu scannedSelect) {
        this.scannedSelect = scannedSelect;
    }

    public HtmlSelectManyMenu getAttachmentSelect() {
        return attachmentSelect;
    }

    public void setAttachmentSelect(HtmlSelectManyMenu attachmentSelect) {
        this.attachmentSelect = attachmentSelect;
    }

    public List<String> getSelectedFileNameWithoutExtension() {
        return selectedFileNameWithoutExtension;
    }

    public void setSelectedFileNameWithoutExtension(List<String> selectedFileNameWithoutExtension) {
        this.selectedFileNameWithoutExtension = selectedFileNameWithoutExtension;
    }

    public void setDocumentDialog(DocumentDialog documentDialog) {
        this.documentDialog = documentDialog;
    }

    public void setInactiveFileDialog(boolean active) {
        inactiveFileDialog = active;
    }

    public boolean isActiveFileDialog() {
        return !inactiveFileDialog;
    }
    // END: getters / setters

}
