package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;

/**
 * @author Dmitri Melnikov
 */
public class AddFileDialog extends AddContentDialog {
    private static final long serialVersionUID = 1L;

    private static final String ERR_EXISTING_FILE = "add_file_existing_file";

    private transient UserService userService;
    private transient ImapServiceExt imapServiceExt;
    private transient FileService fileService;
    private transient DocumentService documentService;

    private boolean isFileSelected = false;
    private NodeRef selectedFileNodeRef;
    private String selectedFileName;

    @Override
    public void start(ActionEvent event) {
        reset();
    }

    public String reset() {
        isFileSelected = false;
        selectedFileNodeRef = null;
        selectedFileName = null;
        return removeUploadedFile();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        try {
            if (isFileSelected) {
                NodeRef documentNodeRef = new NodeRef(Repository.getStoreRef(), navigator.getCurrentNodeId());
                this.createdNode = getFileService().addFileToDocument(selectedFileName, selectedFileNodeRef, documentNodeRef);
            } else {
                outcome = super.finishImpl(context, outcome);
            }
            // XXX Should probably be refactored to a single service method to add all the aspects there.
            addVersionModifiedAspect(this.createdNode);
            NodeRef document = getNodeService().getPrimaryParent(this.createdNode).getParentRef();
            getDocumentService().updateSearchableFiles(document);
            getDocumentService().getDocumentLogService().addDocumentLog(document,
                    MessageUtil.getMessage(context, "document_log_status_fileAdded", new Object[] { getFileName() }));
            return outcome;
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }
    }
    
    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        super.doPostCommitProcessing(context, outcome);
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    public String getFileNameWithoutExtension() {
        return FilenameUtils.removeExtension(getFileName());
    }

    public void setFileNameWithoutExtension(String name) {
        String fileName = name + "." + FilenameUtils.getExtension(getFileName());
        if (isFileSelected) {
            selectedFileName = fileName;
        }
        else {
            setFileName(fileName);
        }
    }
    
    private void addVersionModifiedAspect(NodeRef nodeRef) {
        if (getNodeService().hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED) == false) {
            Map<QName, Serializable> properties = getNodeService().getProperties(nodeRef);
            
            String user = (String)properties.get(ContentModel.PROP_CREATOR);
            Map<QName, Serializable> personProps = getUserService().getUserProperties(user);
            String first = (String) personProps.get(ContentModel.PROP_FIRSTNAME);
            String last = (String) personProps.get(ContentModel.PROP_LASTNAME);
            Date modified = DefaultTypeConverter.INSTANCE.convert(Date.class, properties.get(ContentModel.PROP_CREATED));

            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, modified );
            aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, first);
            aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, last);

            getNodeService().addAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED, aspectProperties);
        }
    }

    @Override
    public String getFileName() {
        if (isFileSelected) {
            return selectedFileName;
        }
        else {
            return super.getFileName();
        }
    }

    @Override
    public String getFileUploadSuccessMsg() {
        if (isFileSelected) {
            String msg = Application.getMessage(FacesContext.getCurrentInstance(), "file_upload_success");
            return MessageFormat.format(msg, Utils.encode(getFileName())); 
        }
        else {
            return super.getFileUploadSuccessMsg();
        }
    }

    public void fileSelected(ValueChangeEvent event) {
        String newVal = (String) event.getNewValue();
        Assert.notNull(newVal, "Node reference must be provided when file is selected.");
        selectedFileNodeRef = new NodeRef(newVal);
        File file = getFileService().getFile(selectedFileNodeRef);
        Assert.notNull(file, "Selected file was not found.");
        isFileSelected = true;
        selectedFileName = file.getName();
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

    public List<SelectItem> getAttachments() {
        List<SelectItem> attachments = new ArrayList<SelectItem>();
        List<File> files = getFileService().getAllFilesExcludingDigidocSubitems(getImapServiceExt().getAttachmentRoot());
        for (File file : files) {
            attachments.add(new SelectItem(file.getNodeRef().toString(), file.getName()));
        }
        return attachments;        
    }

    public List<SelectItem> getScannedFiles() {
        List<SelectItem> scannedFiles = new ArrayList<SelectItem>();
        List<File> files = getFileService().getScannedFiles();
        for (File file : files) {
            scannedFiles.add(new SelectItem(file.getNodeRef().toString(), file.getName()));
        }
        return scannedFiles;
    }
    // END: getters / setters
}
