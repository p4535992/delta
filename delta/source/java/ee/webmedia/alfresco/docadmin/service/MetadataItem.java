package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Base class for objects that can be stored under {@link DocumentTypeVersion}
 * 
 * @author Ats Uiboupin
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

    public final Integer getOrder() {
        return getProp(DocumentAdminModel.Props.ORDER);
    }

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

    @Override
    protected int getAssocIndex() {
        Integer order = getOrder();
        // TODO DLSeadist - test if index needs to start at 0, 1, 2, ... - if yes, then return order - 1
        // TODO DLSeadist - test what happens when multiple Metadataitems have same order value
        return order == null ? -1 : order;
    }

}
