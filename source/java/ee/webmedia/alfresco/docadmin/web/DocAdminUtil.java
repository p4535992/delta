package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.utils.RepoUtil.toQNameProperties;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Util methods used by several classes
 */
public class DocAdminUtil {
    /**
     * Instead of persisting metadataItem to repository metadataItem will be added to (or replaced by previous version of metadataItem in) metadataContainer held in memory
     * 
     * @param metadataItem - field or fieldGroup
     * @param metadataContainer - metadataItem will be replaced in there (when updating) or added there(when creating new)
     */
    static void commitToMetadataContainer(MetadataItem metadataItem, MetadataContainer metadataContainer, String dynTypeName) {
        // Don't persist changes to repository - field should be changed when parent documentType is changed
        @SuppressWarnings("unchecked")
        ChildrenList<MetadataItem> metadata = (ChildrenList<MetadataItem>) metadataContainer.getMetadata();
        if (isSavedInPreviousDocTypeVersionOrFieldDefinitions(metadataItem)) {
            // field that user came to edit was saved in previous version of DocumentType
            metadata.replaceChild(metadataItem); // replace original with clone created for editing in this dialog
        } else if (!metadata.contains(metadataItem)) {
            // field has not been added to the DocumentType
            metadataItem.setOrder(metadata.size() + 1);
            metadata.addExisting(metadataItem);
        } else {
            // field has been added to the DocumentType but not previously persisted
            metadata.replaceChild(metadataItem); // but since we cloned it, we need to replace
        }
        MessageUtil.addWarningMessage("fieldOrFieldGroup_details_affirm_changes_warning_" + dynTypeName);
    }

    /**
     * @return true if this metadataItem(field/fieldGroup) object is saved to the repository(applies when opening fieldDefinition from {@link FieldDefinitionListDialog})
     *         or when field is opened from {@link FieldsListBean} and field is a new version of field already saved in repository under previous {@link DocumentTypeVersion}
     */
    static boolean isSavedInPreviousDocTypeVersionOrFieldDefinitions(MetadataItem metadataItem) {
        return metadataItem.isCopyFromPreviousDocTypeVersion() || metadataItem.isSaved();
    }

    static Set<String> getDuplicateFieldIds(Collection<Field> fieldsToAdd, MetadataContainer metadataContainer) {
        Map<String, Field> fieldsById = new HashMap<String, Field>();
        for (Field field : fieldsToAdd) {
            fieldsById.put(field.getFieldId(), field);
        }
        Set<String> duplicateFieldIds = new LinkedHashSet<String>();
        for (Field duplicateField : metadataContainer.getFieldsById(fieldsById.keySet())) {
            String fieldIdLocalName = duplicateField.getFieldId();
            Field fieldToAdd = fieldsById.get(fieldIdLocalName);
            NodeRef cloneOfNodeRef = fieldToAdd.getCloneOfNodeRef();
            boolean cloneOfTheSameField = cloneOfNodeRef != null && cloneOfNodeRef.equals(duplicateField.getNodeRef());
            if (!cloneOfTheSameField && (!fieldToAdd.isCopyFromPreviousDocTypeVersion() || !fieldToAdd.getCopyFromPreviousDocTypeVersion().equals(duplicateField.getNodeRef()))) {
                duplicateFieldIds.add(fieldIdLocalName);
            }
        }
        if (metadataContainer instanceof FieldGroup) {
            FieldGroup parentFieldGroup = (FieldGroup) metadataContainer;
            DocumentTypeVersion docTypeVersion = parentFieldGroup.getParent();
            duplicateFieldIds.addAll(getDuplicateFieldIds(fieldsToAdd, docTypeVersion));
        }
        return duplicateFieldIds;
    }

    public static <T extends MetadataItem> List<T> reorderAndMarkBaseState(List<T> metadata, BaseObjectOrderModifier<T> reorderHelper) {
        List<T> reordered = ListReorderHelper.reorder(metadata, reorderHelper);
        reorderHelper.markBaseState(reordered);
        return reordered;
    }

    public static <T extends MetadataItem> BaseObjectOrderModifier<T> getMetadataItemReorderHelper(QName orderProp) {
        return new BaseObjectOrderModifier<T>(orderProp);
    }

    public static Pair<String, Integer> getDocTypeIdAndVersionNr(Node documentDynamicNode) {
        return getDocTypeIdAndVersionNr(toQNameProperties(documentDynamicNode.getProperties()));
    }

    public static Pair<String, Integer> getDocTypeIdAndVersionNr(Map<QName, Serializable> props) {
        String docTypeId = (String) props.get(Props.OBJECT_TYPE_ID);
        Integer docTypeVersionNr = (Integer) props.get(Props.OBJECT_TYPE_VERSION_NR);
        Pair<String, Integer> docTypeIdAndVersionNr = new Pair<String, Integer>(docTypeId, docTypeVersionNr);
        return docTypeIdAndVersionNr;
    }

    public static Pair<String, Integer> getDocTypeIdAndVersionNr(DocumentTypeVersion docVer) {
        Pair<String, Integer> docTypeIdAndVersionNr = Pair.newInstance(docVer.getParent().getId(), docVer.getVersionNr());
        return docTypeIdAndVersionNr;
    }

    public static PropDefCacheKey getPropDefCacheKey(Map<String, Object> props, Class<? extends DynamicType> dynamicTypeClass) {
        return new PropDefCacheKey(dynamicTypeClass, (String) props.get(Props.OBJECT_TYPE_ID), (Integer) props.get(Props.OBJECT_TYPE_VERSION_NR));
    }

    public static PropDefCacheKey getPropDefCacheKey(Class<? extends DynamicType> typeClass, DocumentTypeVersion docVer) {
        Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(docVer);
        return new PropDefCacheKey(typeClass, docTypeIdAndVersionNr.getFirst(), docTypeIdAndVersionNr.getSecond());
    }

    public static PropDefCacheKey getPropDefCacheKey(Class<? extends DynamicType> typeClass, Map<QName, Serializable> props) {
        Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(props);
        return new PropDefCacheKey(typeClass, docTypeIdAndVersionNr.getFirst(), docTypeIdAndVersionNr.getSecond());
    }

    public static Class<? extends DynamicType> getDynamicTypeClass(Node node) {
        if (node == null) {
            return null;
        }

        Class<? extends DynamicType> typeClass = null;
        QName type = node.getType();
        if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            typeClass = DocumentType.class;
        } else if (CaseFileModel.Types.CASE_FILE.equals(type)) {
            typeClass = CaseFileType.class;
        }

        return typeClass;
    }
}
