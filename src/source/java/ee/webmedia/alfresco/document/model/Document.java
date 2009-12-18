package ee.webmedia.alfresco.document.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentCommonModel.URI)
public class Document implements Serializable, Comparable<Document> {

    private static final long serialVersionUID = 1L;

    private String regNumber;
    private String title;
    private String docName;
    // START: not mappable fields
    @AlfrescoModelProperty(isMappable = false)
    private DocumentType documentType;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef volumeNodeRef;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public NodeRef getVolumeNodeRef() {
        return volumeNodeRef;
    }

    public void setVolumeNodeRef(NodeRef volumeNodeRef) {
        this.volumeNodeRef = volumeNodeRef;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getDocType() {
        return documentType != null ? documentType.getName() : null;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getSender() {
        final QName id = documentType.getId();
        if (id.equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            return "TODO: unimplemented:: kui dokumendi tüüp = incomingLetter, siis dokumendi senderName väärtus;";
        } else {
            return (String) getNode().getProperties().get(DocumentCommonModel.Props.OWNER_NAME.toString());
        }
    }

    // END: not mappable fields
    
    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    @Override
    public int compareTo(Document other) {
        if (getRegNumber() == other.getRegNumber()) {
            return 0;// TODO: 3.1.5. Nimekiri on sorteeritud kõigepealt regNumber alusel kasvavalt, seejärel regDateTime alusel kasvavalt.
        }
        return getRegNumber().compareTo(other.getRegNumber());
    }

    @Override
    public String toString() {
        return new StringBuilder("Document:")//
                .append("\n\tvolumeMark = " + regNumber)
                .append("\n\ttitle = " + title)
                .toString();
    }

}
