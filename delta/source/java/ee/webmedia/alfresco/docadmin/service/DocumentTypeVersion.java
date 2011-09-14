package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class DocumentTypeVersion extends BaseObject implements MetadataContainer {
    private static final long serialVersionUID = 1L;

    // When this object is create-added under some documentType, then versionNr property needs to be set

    public DocumentTypeVersion(BaseObject parent) {
        super(parent, DocumentAdminModel.Types.DOCUMENT_TYPE_VERSION);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public DocumentTypeVersion(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    @Override
    protected DocumentType getParent() {
        return (DocumentType) super.getParent();
    }

    @Override
    protected QName getAssocName() {
        return QName.createQName(DocumentAdminModel.URI, getVersionNr().toString());
    }

    // ChildrenList

    @Override
    protected Class<MetadataItem> getChildGroupingClass(BaseObject child) {
        return MetadataItem.class;
    }

    // return type ChildrenList<? extends MetadataItem> would be incorrect, because then ChildrenList#getList() should contain items of only one concrete subType of MetadataItem
    @Override
    public ChildrenList<MetadataItem> getMetadata() {
        return getChildren(MetadataItem.class);
    }

    // Properties

    public Integer getVersionNr() {
        return getProp(DocumentAdminModel.Props.VERSION_NR);
    }

    public void setVersionNr(Integer versionNr) {
        setProp(DocumentAdminModel.Props.VERSION_NR, versionNr);
    }

    public String getCreatorId() {
        return getProp(DocumentAdminModel.Props.CREATOR_ID);
    }

    public void setCreatorId(String creatorId) {
        setProp(DocumentAdminModel.Props.CREATOR_ID, creatorId);
    }

    public String getCreatorName() {
        return getProp(DocumentAdminModel.Props.CREATOR_NAME);
    }

    public void setCreatorName(String creatorName) {
        setProp(DocumentAdminModel.Props.CREATOR_NAME, creatorName);
    }

    public Date getCreatedDateTime() {
        return getProp(DocumentAdminModel.Props.CREATED_DATE_TIME);
    }

    public void setCreatedDateTime(Date createdDateTime) {
        setProp(DocumentAdminModel.Props.CREATED_DATE_TIME, createdDateTime);
    }

    @Override
    public DocumentTypeVersion clone() {
        return (DocumentTypeVersion) super.clone(); // just return casted type
    }

    @Override
    public DocumentTypeVersion cloneAndResetBaseState() {
        return (DocumentTypeVersion) super.cloneAndResetBaseState(); // just return casted type
    }

    // Utilities

    // FIXME DLSeadist - pole vist enam vaja
//@formatter:off
//    /**
//     * Find first {@link Field} that matches given {@code fieldId}. Traverses all child {@link Field}s and {@link FieldGroup}'s {@link Field}s.
//     * 
//     * @param fieldId fieldId to match by, cannot be {@code null}.
//     * @return {@code null} if not found; otherwise the found field.
//     */
//    public Field getFieldById(QName fieldId) {
//        Assert.notNull(fieldId, "fieldId cannot be null");
//        for (MetadataItem metadataItem : getMetadata()) {
//            if (metadataItem instanceof Field) {
//                if (((Field) metadataItem).getFieldId().equals(fieldId)) {
//                    return (Field) metadataItem;
//                }
//            } else if (metadataItem instanceof FieldGroup) {
//                Field field = ((FieldGroup) metadataItem).getFieldById(fieldId);
//                if (field != null) {
//                    return field;
//                }
//            }
//        }
//        return null;
//    }
//@formatter:on

    /**
     * return fields added directly or indirectly (trough {@link FieldGroup}) to this {@link DocumentTypeVersion}
     */
    @Override
    public Collection<Field> getFieldsById(Set<String> fieldIdLocalNames) {
        HashSet<Field> matchingFields = new HashSet<Field>();
        for (Field field : getFieldsDeeply()) {
            if (fieldIdLocalNames.contains(field.getFieldId().getLocalName())) {
                matchingFields.add(field);
            }
        }
        return matchingFields;
    }

    public List<Field> getFieldsDeeply() {
        List<Field> fields = new ArrayList<Field>();
        for (MetadataItem metadataItem : getMetadata()) {
            if (metadataItem instanceof Field) {
                fields.add((Field) metadataItem);
            } else if (metadataItem instanceof FieldGroup) {
                fields.addAll(((FieldGroup) metadataItem).getFields());
            }
        }
        return fields;
    }

}
