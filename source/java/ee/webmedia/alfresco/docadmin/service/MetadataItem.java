package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Base class for objects that can be stored under {@link DocumentTypeVersion}
 */
public abstract class MetadataItem extends BaseObject {
    private static final long serialVersionUID = 1L;

    private final String type;

    protected MetadataItem(BaseObject parent, QName type) {
        super(parent, type);
        this.type = type.getLocalName();
    }

    protected MetadataItem(BaseObject parent, WmNode node) {
        super(parent, node);
        type = node.getType().getLocalName();
    }

    /** used only by subclass */
    protected MetadataItem(NodeRef parentRef, QName type) {
        super(parentRef, type);
        this.type = type.getLocalName();
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public MetadataItem(NodeRef parentRef, WmNode node) {
        super(parentRef, node);
        type = node.getType().getLocalName();
    }

    // START: Properties

    // actually fieldDefinition doesn't need order property, but we put it here to make model easier
    public final Integer getOrder() {
        return getProp(DocumentAdminModel.Props.ORDER);
    }

    // actually fieldDefinition doesn't need order property, but we put it here to make model easier
    public final void setOrder(Integer name) {
        setProp(DocumentAdminModel.Props.ORDER, name);
    }

    // END: Properties

    /** used in JSF */
    public String getType() {
        return "doc_types_" + type;
    }

    public abstract String getAdditionalInfo();

    public abstract boolean isRemovableFromList();

    public boolean isCopyFromPreviousDocTypeVersion() {
        return getCopyFromPreviousDocTypeVersion() != null;
    }

    public NodeRef getCopyFromPreviousDocTypeVersion() {
        return getCopyOfNodeRef();
    }

    @Override
    protected int getAssocIndex() {
        Integer order = getOrder();
        if (order == null) {
            return -1;
        }
        // childAssociationIndex starts at 0; other code ensures that order starts at 1
        return order - 1;
    }

}
