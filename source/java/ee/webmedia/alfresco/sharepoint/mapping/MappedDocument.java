package ee.webmedia.alfresco.sharepoint.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.dom4j.Element;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

/**
 * A mapped document is the document after it has been read from the XML according to the provided mapping.
 * 
 * @author Martti Tamm
 */
public class MappedDocument {

    private final Map<QName, Serializable> propertyValues = new HashMap<QName, Serializable>();

    private final Map<QName, MappedDocument> children = new HashMap<QName, MappedDocument>();

    private final QName type;

    private final DocumentTypeVersion documentTypeVersion;

    /**
     * Maps a document using the given resolved mapping, which may be <code>null</code>.
     * 
     * @param docRoot The current document XML root element.
     * @param mapping The (sub)mapping to use (may be <code>null</code>).
     */
    public MappedDocument(Element docRoot, Mapping mapping) {
        if (mapping == null) {
            type = null;
            documentTypeVersion = null;
            return;
        }

        type = mapping.getTo();
        documentTypeVersion = mapping.getDocumentVersion();

        for (PropMapping propMapping : mapping.getPropertyMapping()) {
            propMapping.addPropValues(docRoot, propertyValues);
        }

        if (documentTypeVersion != null) {
            propertyValues.put(DocumentAdminModel.Props.OBJECT_TYPE_ID, type.getLocalName());
            propertyValues.put(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR, documentTypeVersion.getVersionNr());
        }

        for (Mapping submapping : mapping.getSubMappings()) {
            MappedDocument child = new MappedDocument(docRoot, submapping);
            if (!child.isEmpty()) {
                children.put(submapping.getTo(), child);
            }
        }
    }

    public QName getType() {
        return type;
    }

    public String getDocumentType() {
        return documentTypeVersion.getParent().getId();
    }

    public DocumentTypeVersion getDocumentTypeVersion() {
        return documentTypeVersion;
    }

    public Map<QName, Serializable> getPropertyValues() {
        return propertyValues;
    }

    public Map<QName, MappedDocument> getChildren() {
        return children;
    }

    public boolean isEmpty() {
        return propertyValues.isEmpty() && children.isEmpty();
    }
}
