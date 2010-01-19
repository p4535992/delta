package ee.webmedia.alfresco.document.file.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;

/**
 * @author Dmitri Melnikov
 */
public class File implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String downloadUrl;
    private String creator;
    private String modifier;
    private long size;
    private Date created;
    private Date modified;
    private NodeRef nodeRef;
    private Node node;
    private boolean digiDocItem;
    private boolean digiDocContainer;
    private boolean versionable;
    private SignatureItemsAndDataItems ddocItems;

    public File() {
    }

    public File(FileInfo fileInfo) {
        name = fileInfo.getName();
        created = fileInfo.getCreatedDate();
        modified = fileInfo.getModifiedDate();
        size = fileInfo.getContentData().getSize();
        nodeRef = fileInfo.getNodeRef();
        node = new Node(nodeRef);
        digiDocItem = false;
        versionable = fileInfo.getProperties().get(ContentModel.PROP_VERSION_LABEL) != null;
        creator = "";
        modifier = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String url) {
        this.downloadUrl = url;
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
}
