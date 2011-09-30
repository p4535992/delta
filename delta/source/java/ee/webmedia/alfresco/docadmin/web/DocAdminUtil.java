package ee.webmedia.alfresco.docadmin.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Util methods used by several classes
 * 
 * @author Ats Uiboupin
 */
public class DocAdminUtil {
    /**
     * Instead of persisting metadataItem to repository metadataItem will be added to (or replaced by previous version of metadataItem in) metadataContainer held in memory
     * 
     * @param metadataItem - field or fieldGroup
     * @param metadataContainer - metadataItem will be replaced in there (when updating) or added there(when creating new)
     */
    static void commitToMetadataContainer(MetadataItem metadataItem, MetadataContainer metadataContainer) {
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
        MessageUtil.addWarningMessage("fieldOrFieldGroup_details_affirm_changes_warning"); // FIXME DLSeadist msg key
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

    public// FIXME DLSeadist Vladimir peaks navigate meetodi WebUtil'isse liigutama
    static void navigate(String navigationOutcome) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getNavigationHandler()
                .handleNavigation(context, null, navigationOutcome);
    }

    static <T extends MetadataItem> List<T> reorderAndMarkBaseState(List<T> metadata, BaseObjectOrderModifier<T> reorderHelper) {
        List<T> reordered = ListReorderHelper.reorder(metadata, reorderHelper);
        reorderHelper.markBaseState(reordered);
        // order property has been changed, now reorder list items based on that property
        @SuppressWarnings("unchecked")
        Comparator<T> byOrderComparator = new TransformingComparator(new ComparableTransformer<T>() {
            @Override
            public Comparable<?> tr(T input) {
                return input.getOrder();
            }
        }, new NullComparator());
        Collections.sort(reordered, byOrderComparator);
        return reordered;
    }

    static <T extends MetadataItem> BaseObjectOrderModifier<T> getMetadataItemReorderHelper(QName orderProp) {
        return new BaseObjectOrderModifier<T>(orderProp);
    }
}
