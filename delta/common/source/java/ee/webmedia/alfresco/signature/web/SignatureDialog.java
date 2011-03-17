package ee.webmedia.alfresco.signature.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;

public class SignatureDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(SignatureDialog.class);

    private transient SignatureService signatureService;

    private NodeRef nodeRef;
    private SignatureDigest signatureDigest;
    private List<DataItem> dataItems;
    private List<SignatureItem> signatures;
    /**
     * All available items in the list.
     */
    private List<SelectItem> allItems;
    /**
     * Selected items from the list to create the .ddoc from.
     */
    private List<String> selectedItems;
    /**
     * Suggested filename for the new .ddoc
     */
    private String filename = "";
    /**
     * True when we create a DigiDoc from files and folders.
     * Becomes false when files have been selected.
     * Always false if we have an existing .ddoc file (digiDoc is true).
     */
    private Boolean editMode;
    /**
     * Single file, no need to select anything.
     */
    private Boolean singleFile;
    /**
     * True if it's a .ddoc file.
     */
    private Boolean digiDoc;
    private boolean error;

    protected SignatureService getSignatureService() {
        if (signatureService == null) {
            signatureService = (SignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    SignatureService.BEAN_NAME);
        }
        return signatureService;
    }

    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public List<DataItem> getDataFiles() {
        if (dataItems == null) {
            getDataFilesAndSignatures();
        }
        return dataItems;
    }

    public List<SignatureItem> getSignatures() {
        if (signatures == null) {
            getDataFilesAndSignatures();
        }
        return signatures;
    }

    public boolean isDigiDoc() {
        if (digiDoc == null) {
            digiDoc = getSignatureService().isDigiDocContainer(nodeRef);
        }
        return digiDoc;
    }

    public boolean isEditMode() {
        if (editMode == null) {
            editMode = !isDigiDoc();
        }
        return editMode;
    }

    public boolean isError() {
        return error;
    }

    public boolean isSingleFile() {
        if (singleFile == null) {
            singleFile = Boolean.FALSE;
        }
        return singleFile;
    }

    public void setSelectedItems(List<String> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<String> getSelectedItems() {
        if (selectedItems == null) {
            selectedItems = new ArrayList<String>(getAllItems().size());
            for (SelectItem item : getAllItems()) {
                selectedItems.add(item.getValue().toString());
            }
        }
        return selectedItems;
    }

    public List<SelectItem> getAllItems() {
        if (allItems == null) {
            allItems = new ArrayList<SelectItem>();
            for (NodeRef n : getAllNodeRefs()) {
                SelectItem item = new SelectItem();
                String name = getFileFolderService().getFileInfo(n).getName();
                item.setValue(n.toString());
                item.setLabel(name);
                allItems.add(item);
            }
            if (allItems.size() == 1) {
                singleFile = Boolean.TRUE;
                editMode = Boolean.FALSE;
            } else {
                singleFile = Boolean.FALSE;
                editMode = Boolean.TRUE;
            }
        }
        return allItems;
    }

    /**
     * Used in JSP pages.
     */
    public boolean isContainerDataFiles() {
        return !error && !isEditMode() && getDataFiles() != null;
    }

    /**
     * Used in JSP pages.
     */
    public boolean isContainerSignatures() {
        return !error && isDigiDoc() && getSignatures() != null && !getSignatures().isEmpty();
    }

    /**
     * Used in JSP pages.
     */
    public boolean isSelectFilename() {
        return (isSingleFile() || isEditMode()) && !isDigiDoc();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        return outcome;
    }

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        resetData();
        nodeRef = browseBean.getDocument().getNodeRef();
        // a suggested name for the new DigiDoc
        filename = getFileFolderService().getFileInfo(nodeRef).getName();
        if (!isDigiDoc()) {
            filename = FilenameUtils.removeExtension(filename) + ".ddoc";
            // load the selected files and set singleFile and editMode flags
            getSelectedItems();
        } else {
            getDataFilesAndSignatures();
        }
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        session.setAttribute("operation", "PREPARE");
    }

    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }

    public void processCert() {
        if (!isDigiDoc() && selectedItems.size() == 0) {
            Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_file_empty"));
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<?, ?> params = facesContext.getExternalContext().getRequestParameterMap();
        String certHex = (String) params.get("cert");
        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            if (isDigiDoc()) {
                signatureDigest = getSignatureService().getSignatureDigest(nodeRef, certHex);
            } else {
                if (!validateFilename()) {
                    return;
                }
                editMode = Boolean.FALSE;
                singleFile = Boolean.FALSE;
                signatureDigest = getSignatureService().getSignatureDigest(getSelectedNodeRefs(), certHex);
            }
            session.setAttribute("digest", signatureDigest.getDigestHex());
            session.setAttribute("operation", "FINALIZE");
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
        }
    }

    public String signDocument() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<?, ?> params = facesContext.getExternalContext().getRequestParameterMap();
        String signatureHex = (String) params.get("signature");

        if (isDigiDoc()) {
            try {
                getSignatureService().addSignature(nodeRef, signatureDigest, signatureHex);
            } catch (SignatureRuntimeException e) {
                SignatureBlockBean.addSignatureError(e);
                return null;
            }
        } else {
            List<NodeRef> nodeRefs = getSelectedNodeRefs();
            NodeRef parentRef = getNodeService().getPrimaryParent(nodeRefs.get(0)).getParentRef();
            try {
                // update the reference to the newly created DigiDoc
                nodeRef = getSignatureService().createContainer(parentRef, nodeRefs, filename, signatureDigest, signatureHex);
            } catch (SignatureRuntimeException e) {
                SignatureBlockBean.addSignatureError(e);
                return null;
            } catch (FileExistsException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to create ddoc, file with same name already exists, filename = " + filename + " and parentRef = " + parentRef, e);
                }
                Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_file_exists"));
                return null;
            }
            // now it's a DigiDoc
            digiDoc = Boolean.TRUE;
        }

        String msg = Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_signature_signed");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
        return getDefaultFinishOutcome();
    }

    /**
     * TODO: move to separate service class later.
     */
    protected boolean validateFilename() {
        if (StringUtils.isBlank(filename)) {
            Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_filename_empty"));
            return false;
        }
        try {
            for (ConstraintDefinition c : getDictionaryService().getProperty(ContentModel.PROP_NAME).getConstraints()) {
                c.getConstraint().evaluate(filename);
            }
        } catch (ConstraintException e) {
            Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_filename_invalid"));
            return false;
        }
        return true;
    }

    protected List<NodeRef> getSelectedNodeRefs() {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        for (String s : selectedItems) {
            nodeRefs.add(new NodeRef(s));
        }
        return nodeRefs;
    }

    protected List<NodeRef> getAllNodeRefs() {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();

        if (isFolder()) {
            for (FileInfo fi : getFileFolderService().listFiles(nodeRef)) {
                nodeRefs.add(fi.getNodeRef());
            }
        } else {
            nodeRefs.add(nodeRef);
        }
        return nodeRefs;
    }

    protected Boolean isFolder() {
        return getFileFolderService().getFileInfo(nodeRef).isFolder();
    }

    protected void resetData() {
        signatureDigest = null;
        nodeRef = null;
        filename = null;

        signatures = null;
        dataItems = null;

        allItems = null;
        selectedItems = null;

        digiDoc = null;
        editMode = null;
        singleFile = null;
        error = false;

        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        session.removeAttribute("digest");
        session.removeAttribute("operation");
    }

    private void getDataFilesAndSignatures() {
        if (isDigiDoc()) {
            // get the data from the container
            try {
                SignatureItemsAndDataItems values = getSignatureService().getDataItemsAndSignatureItems(nodeRef, false);
                signatures = values.getSignatureItems();
                dataItems = values.getDataItems();
            } catch (SignatureException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
                Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), "ddoc_container_fail"));
                error = true;
                return;
            }
        } else {
            // not a DigiDoc, so no signatures present
            signatures = Collections.emptyList();
            // get the data from selected files
            dataItems = new ArrayList<DataItem>(selectedItems.size());
            for (int i = 0; i < selectedItems.size(); i++) {
                NodeRef ref = new NodeRef(selectedItems.get(i));
                String name = getFileFolderService().getFileInfo(ref).getName();
                DataItem d = new DataItem(ref, i, name, null, null,
                        getFileFolderService().getFileInfo(ref).getContentData().getSize());
                d.setDownloadUrl(DownloadContentServlet.generateDownloadURL(ref, name));
                dataItems.add(d);
            }
        }
    }

}
