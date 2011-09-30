package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    public DocumentType getParent() {
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
            if (fieldIdLocalNames.contains(field.getFieldId())) {
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

    List<String> getRemovedFieldIdsDeeply() {
        Map<Class<? extends BaseObject>, List<? extends BaseObject>> removedChildren = getRemovedChildren();
        List<String> removedFieldIds = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        List<MetadataItem> removedMetadataItems = (List<MetadataItem>) removedChildren.get(MetadataItem.class);
        if (removedMetadataItems != null) {
            for (MetadataItem metadataItem : removedMetadataItems) {
                if (!metadataItem.isCopyFromPreviousDocTypeVersion()) {
                    continue; // not interested in removed metadata items that haven't been saved jet
                }
                if (metadataItem instanceof Field) {
                    Field removedField = (Field) metadataItem;
                    removedFieldIds.add(removedField.getFieldId());
                } else if (metadataItem instanceof FieldGroup) {
                    FieldGroup fieldGroup = (FieldGroup) metadataItem;
                    ChildrenList<Field> fields = fieldGroup.getFields();
                    fields.addAll(fieldGroup.getRemovedFields()); // also inspect fields that have been removed before fieldGroup was removed
                    for (Field field : fields) {
                        if (field.isCopyFromPreviousDocTypeVersion()) {
                            removedFieldIds.add(field.getFieldId()); // only interested in fields that were saved
                        }
                    }
                }
            }
        }
        // inspect removed fields of existing fieldGroup
        for (MetadataItem metadataItem : getMetadata()) {
            if (metadataItem instanceof FieldGroup) {
                List<Field> removedFieldsOfGroup = ((FieldGroup) metadataItem).getRemovedFields();
                removedFieldIds.addAll(getFieldIds(removedFieldsOfGroup));
            }
        }
        // exclude fields that are removed and added back again with same fieldId
        Set<String> allExistingFields = getFieldIds(getFieldsDeeply());
        for (Iterator<String> it = removedFieldIds.iterator(); it.hasNext();) {
            String fieldId = it.next();
            if (allExistingFields.contains(fieldId)) {
                it.remove();
            }
        }
        return removedFieldIds;
    }

    private Set<String> getFieldIds(List<Field> fields) {
        Set<String> fieldIds = new HashSet<String>(fields.size());
        for (Field field : fields) {
            fieldIds.add(field.getFieldId());
        }
        return fieldIds;
    }
}
