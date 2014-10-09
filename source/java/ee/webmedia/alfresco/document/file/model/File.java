package ee.webmedia.alfresco.document.file.model;

import static ee.webmedia.alfresco.document.file.model.FileModel.Props.ACTIVE;
import static ee.webmedia.alfresco.document.file.model.FileModel.Props.DISPLAY_NAME;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.service.IClonable;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;

public class File implements Serializable, IClonable<File> {

    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String downloadUrl;
    private String readOnlyUrl;
    private String creator;
    private String modifier;
    private String encoding;
    private String mimeType;
    private String comment;
    private long size;
    private Date created;
    private Date modified;
    private NodeRef nodeRef;
    private Node node;
    private boolean digiDocItem;
    private boolean digiDocContainer;
    private boolean bdoc;
    private boolean versionable;
    private SignatureItemsAndDataItems ddocItems;
    private boolean generated;
    private boolean active;
    private boolean isTransformableToPdf;
    private boolean convertToPdfIfSigned; //
    private long nrOfChildren; // used to show childCount if the file represents folder
    private boolean isPdf;
    private String activeLockOwner;
    private boolean decContainer;
    public static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    public File() {
    }

    public File(FileInfo fileInfo) {
        name = fileInfo.getName();
        Map<QName, Serializable> fileProps = fileInfo.getProperties();
        displayName = (fileProps.get(DISPLAY_NAME) == null) ? name : fileProps.get(DISPLAY_NAME).toString();
        created = fileInfo.getCreatedDate();
        modified = fileInfo.getModifiedDate();
        // fileInfo.getContentData() != null is here for testing purposes only; normally fileInfo.getContentData() shouldn't be null
        if (!fileInfo.isFolder() && fileInfo.getContentData() != null) {
            encoding = fileInfo.getContentData().getEncoding();
            mimeType = fileInfo.getContentData().getMimetype();
            size = fileInfo.getContentData().getSize();
        }
        nodeRef = fileInfo.getNodeRef();
        node = new Node(nodeRef);
        digiDocItem = false;
        versionable = fileProps.get(ContentModel.PROP_VERSION_LABEL) != null;
        creator = "";
        modifier = "";
        generated = fileProps.get(FileModel.Props.GENERATED_FROM_TEMPLATE) != null || fileProps.get(FileModel.Props.GENERATION_TYPE) != null;
        active = (fileProps.get(ACTIVE) == null) ? true : Boolean.parseBoolean(fileProps.get(ACTIVE).toString());
        convertToPdfIfSigned = Boolean.TRUE.equals(fileProps.get(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED));
        decContainer = fileProps.containsKey(DvkModel.Props.DVK_ID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String url) {
        downloadUrl = url;
    }

    public String getReadOnlyUrl() {
        return readOnlyUrl;
    }

    public void setReadOnlyUrl(String readOnlyUrl) {
        this.readOnlyUrl = readOnlyUrl;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getModifier() {
        return modifier;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedTimeStr() {
        return getCreated() != null ? dateFormat.format(getCreated()) : "";
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return true if this item/file is contained in digiDoc container
     */
    public boolean isDigiDocItem() {
        return digiDocItem;
    }

    public void setDigiDocItem(boolean digiDocItem) {
        this.digiDocItem = digiDocItem;
    }

    /**
     * @return true, if this document is in digiDocItem format and may contain other documents
     */
    public boolean isDigiDocContainer() {
        return digiDocContainer;
    }

    public void setDigiDocContainer(boolean digiDocContainer) {
        this.digiDocContainer = digiDocContainer;
    }

    public NodeRef getCompoundWorkflowRef() {
        return (NodeRef) getNode().getProperties().get(FileModel.Props.COMPOUND_WORKFLOW);
    }

    public NodeRef getGeneratedFileRef() {
        return (NodeRef) getNode().getProperties().get(FileModel.Props.GENERATED_FILE);
    }

    public boolean isVersionable() {
        return versionable;
    }

    public void setVersionable(boolean versionable) {
        this.versionable = versionable;
    }

    public void setDdocItems(SignatureItemsAndDataItems ddocItems) {
        this.ddocItems = ddocItems;
    }

    public List<SignatureItem> getSignatureItems() {
        return ddocItems.getSignatureItems();
    }

    public List<DataItem> getDataItems() {
        return ddocItems.getDataItems();
    }

    /**
     * @return true if file has been generated by system and mark has been set
     */
    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isTransformableToPdf() {
        return isTransformableToPdf;
    }

    public void setTransformableToPdf(boolean isTransformableToPdf) {
        this.isTransformableToPdf = isTransformableToPdf;
    }

    public void setNrOfChildren(long nrOfChildren) {
        this.nrOfChildren = nrOfChildren;
    }

    public long getNrOfChildren() {
        return nrOfChildren;
    }

    /**
     * Used to specify icon.
     */
    public String getFileType16() {
        return FileTypeImageUtils.getFileTypeImage(getName(), true);
    }

    /**
     * Used as a parameter.
     */
    public String getId() {
        return nodeRef.getId();
    }

    /* JSP is evil! */
    public boolean isActiveAndNotDigiDoc() {
        return isActive() && !isDigiDocItem();
    }

    public boolean isActiveDigiDoc() {
        return isActive() && isDigiDocItem();
    }

    public boolean isNotActiveAndDigiDoc() {
        return !isActive() && isDigiDocItem();
    }

    public boolean isNotActiveAndNotDigiDoc() {
        return !isActive() && !isDigiDocItem();
    }

    public void setPdf(boolean isPdf) {
        this.isPdf = isPdf;
    }

    public boolean isPdf() {
        return isPdf;
    }

    public boolean isDecContainer() {
        return decContainer;
    }

    public String getActiveLockOwner() {
        return activeLockOwner;
    }

    public void setActiveLockOwner(String activeLockOwner) {
        this.activeLockOwner = activeLockOwner;
    }

    @Override
    public File clone() {
        File file;
        try {
            file = (File) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        file.setCreated((Date) created.clone());
        file.setModified((Date) modified.clone());
        file.setNode(new Node(nodeRef));
        return file;
    }

    public void setConvertToPdfIfSigned(boolean isConvertToPdfIfSigned) {
        convertToPdfIfSigned = isConvertToPdfIfSigned;
    }

    public boolean isConvertToPdfIfSigned() {
        return convertToPdfIfSigned;
    }

    public boolean getConvertToPdfIfSignedFromProps() {
        Boolean convertToPdf = node != null ? Boolean.TRUE.equals(node.getProperties().get(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED)) : false;
        return convertToPdf != null ? convertToPdf : false;
    }

    public boolean isBdoc() {
        return bdoc;
    }

    public void setBdoc(boolean isBdoc) {
        bdoc = isBdoc;
    }
}
