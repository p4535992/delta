package ee.webmedia.alfresco.docadmin.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.collections.comparators.FixedOrderComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.constant.MappingRestriction;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.FieldMapping;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Bean related to showing list of {@link FieldMapping} objects bellow {@link AssociationModel}
 */
public class FieldMappingsListBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private AssociationModel associationModel;
    private List<FieldMappingListItem> listItems;
    private Map<String/* fieldId */, Field> relatedDocTypeFieldsById;

    void init(AssociationModel assocModel) {
        reset();
        associationModel = assocModel;
    }

    void reset() {
        associationModel = null;
        listItems = null;
        relatedDocTypeFieldsById = null;
    }

    void save() {
        ChildrenList<FieldMapping> fieldMappings = associationModel.getFieldMappings();
        for (FieldMappingListItem listItem : listItems) {
            String toField = listItem.getToField();
            FieldMapping fieldMapping = listItem.getFieldMapping();
            boolean saved = fieldMapping.isSaved();
            boolean fieldMappingAddedToAssocModel = fieldMappings.contains(fieldMapping);
            if (StringUtils.isBlank(toField)) {
                if (saved || fieldMappingAddedToAssocModel) {
                    fieldMappings.remove(fieldMapping);
                }
                continue;
            }
            if (!saved && !fieldMappingAddedToAssocModel) {
                fieldMappings.addExisting(fieldMapping);
            }
        }
    }

    boolean validate() {
        Set<String> usedFields = new HashSet<String>(listItems.size());
        Set<String> duplicateFields = new LinkedHashSet<String>(4);
        for (FieldMappingListItem listItem : listItems) {
            String toField = listItem.getToField();
            Assert.isTrue(StringUtils.isNotBlank(listItem.getFromField()), "fromField must not be blank");
            Assert.isTrue(null != toField, "toField must not be null");
            if (StringUtils.isBlank(toField)) {
                continue;
            }
            if (usedFields.contains(toField)) {
                duplicateFields.add(toField);
            }
            usedFields.add(toField);
        }
        boolean valid = true;
        if (!duplicateFields.isEmpty()) {
            valid = false;
            MessageUtil.addErrorMessage("associationModel_details_panel_fieldMappings_error_duplicateFields", TextUtil.collectionToString(duplicateFields));
        }
        return valid;
    }

    public List<FieldMappingListItem> getAssociatedFieldsList() {
        return getFieldMappingListItems();
    }

    private List<FieldMappingListItem> getFieldMappingListItems() {
        if (listItems == null) {
            String relatedDocTypeId = associationModel.getDocType();
            if (StringUtils.isNotBlank(relatedDocTypeId)) {
                DocumentType relatedDocType = BeanHelper.getDocumentAdminService().getDocumentType(relatedDocTypeId
                        , DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
                relatedDocTypeFieldsById = relatedDocType.getLatestDocumentTypeVersion().getFieldsDeeplyById();
            }
            List<Field> allFields = associationModel.getParent().getLatestDocumentTypeVersion().getFieldsDeeply();
            List<FieldMapping> persistedFieldMappings = associationModel.getFieldMappings();
            listItems = new ArrayList<FieldMappingListItem>(allFields.size());

            Set<String> associatedFieldsByFieldId = new HashSet<String>();
            Map<String, Field> fieldsByIds = getFieldsByIds(allFields);
            for (FieldMapping fieldMapping : persistedFieldMappings) {
                createFieldMappingListItem(null, fieldMapping, listItems, associatedFieldsByFieldId, fieldsByIds);
            }
            for (Field field : allFields) {
                if (!associatedFieldsByFieldId.contains(field.getFieldId())) {
                    createFieldMappingListItem(field.getFieldId(), new FieldMapping(associationModel), listItems, associatedFieldsByFieldId, fieldsByIds);
                }
            }
            sortByFieldOrder(allFields);
        }
        return listItems;
    }

    private void sortByFieldOrder(List<Field> allFields) {
        List<String> fieldIdsOrdered = new ArrayList<String>(allFields.size());
        for (Field field : allFields) {
            fieldIdsOrdered.add(field.getFieldId());
        }
        @SuppressWarnings("unchecked")
        Comparator<FieldMappingListItem> byOrderInMetadataListomparator = new TransformingComparator(new ComparableTransformer<FieldMappingListItem>() {
            @Override
            public Comparable<?> tr(FieldMappingListItem input) {
                return input.getFieldId();
            }
        }, new FixedOrderComparator(fieldIdsOrdered));
        Collections.sort(listItems, byOrderInMetadataListomparator);
    }

    private String getFieldGroupName(Field field) {
        BaseObject parent = field.getParent();
        if (parent instanceof FieldGroup) {
            return ((FieldGroup) parent).getName();
        }
        Assert.notNull(parent, "unexpected: field parent is null");
        return null;
    }

    private Map<String, Field> getFieldsByIds(List<Field> fields) {
        Map<String, Field> fieldIds = new HashMap<String, Field>(fields.size());
        for (Field field : fields) {
            fieldIds.put(field.getFieldId(), field);
        }
        return fieldIds;
    }

    private void createFieldMappingListItem(String fieldId, FieldMapping fieldMapping
            , List<FieldMappingListItem> items, Set<String> associatedFieldsByFieldId, Map<String, Field> fieldsByIds) {
        if (fieldId == null) {
            fieldId = fieldMapping.getFromField();
        }
        Field field = fieldsByIds.get(fieldId);
        if (field == null) { // this shouldn't happen
            throw new IllegalStateException("Was field removed and fieldMapping not removed? Missing field for fieldMapping: field==null, fieldId="
                    + fieldId + "; fieldMapping=" + fieldMapping);
        }
        String groupName = getFieldGroupName(field);
        items.add(new FieldMappingListItem(fieldMapping, field, groupName));
        associatedFieldsByFieldId.add(fieldId);
    }

    private static final Set<FieldType> TO_TEXT_FIELD_COMPATIBLE_TYPES = new HashSet<FieldType>(
            Arrays.asList(
                    FieldType.COMBOBOX,
                    FieldType.COMBOBOX_EDITABLE,
                    FieldType.USER,
                    FieldType.CONTACT,
                    FieldType.USER_CONTACT,
                    FieldType.USERS,
                    FieldType.CONTACTS,
                    FieldType.USERS_CONTACTS,
                    FieldType.COMBOBOX_AND_TEXT,
                    FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE
                    ));
    private static final Set<FieldType> FROM_USER_CONTACT_COMPATIBLE_TYPES = new HashSet<FieldType>(
            Arrays.asList(FieldType.USERS, FieldType.USER_CONTACT, FieldType.USERS_CONTACTS));

    /**
     * ListItem that provides some extra information about the object, but uses internally node of the object that is saved to repository
     */
    public class FieldMappingListItem extends FieldMapping {
        private static final long serialVersionUID = 1L;
        private final String group;
        private final Field field;
        private List<SelectItem> relatedFieldSelectItems;
        private final FieldMapping wrapped;

        public FieldMappingListItem(FieldMapping fieldMapping, Field field, String groupName) {
            super(fieldMapping.getParent(), fieldMapping.getNode());
            wrapped = fieldMapping;
            group = groupName;
            setFromField(field.getFieldId());
            this.field = field;
            if (getToField() == null) {
                setToField("");
            }
        }

        public FieldMapping getFieldMapping() {
            return wrapped;
        }

        public String getFieldId() {
            return field.getFieldId();
        }

        public String getNameAndFieldId() {
            return field.getNameAndFieldId();
        }

        public String getGroup() {
            return group;
        }

        public List<SelectItem> getRelatedFieldSelectItems() {
            return relatedFieldSelectItems != null ? relatedFieldSelectItems : loadRelatedFieldSelectItems();
        }

        protected List<SelectItem> loadRelatedFieldSelectItems() {
            relatedFieldSelectItems = new ArrayList<SelectItem>();
            if (relatedDocTypeFieldsById == null) {
                relatedFieldSelectItems.add(new SelectItem("", ""));
                return relatedFieldSelectItems;
            }
            MappingRestriction mappingRestriction = field.getMappingRestrictionEnum();
            if (mappingRestriction == null) {
                ComponentUtil.addDefault(relatedFieldSelectItems, FacesContext.getCurrentInstance());
                SelectItem sameIdSelectItem = null;
                for (Field relatedField : relatedDocTypeFieldsById.values()) {
                    if (relatedField.getMappingRestrictionEnum() == null && isFieldTypeCompatible(relatedField)) {
                        SelectItem selectItem = new SelectItem(relatedField.getFieldId(), getFieldMappingLabel(relatedField));
                        if (field.getFieldId().equals(relatedField.getFieldId())) {
                            sameIdSelectItem = selectItem;
                        } else {
                            relatedFieldSelectItems.add(selectItem);
                        }
                    }
                }
                WebUtil.sort(relatedFieldSelectItems);
                if (sameIdSelectItem != null) {
                    relatedFieldSelectItems.add(0, sameIdSelectItem);
                }
            } else {
                String value = "";
                String label = MessageUtil.getMessage(mappingRestriction);
                if (MappingRestriction.IDENTICAL_FIELD_MAPPING_ONLY.equals(mappingRestriction)) {
                    Field relatedField = relatedDocTypeFieldsById.get(field.getFieldId());
                    if (relatedField == null) {
                        label = MessageUtil.getMessage("select_default_label");
                    } else {
                        label = getFieldMappingLabel(relatedField);
                    }
                    value = getFieldId();
                }
                setToField(value);
                final SelectItem selectItem = new SelectItem(value, label);
                relatedFieldSelectItems.add(selectItem);
            }
            return relatedFieldSelectItems;
        }

        private String getFieldMappingLabel(Field f) {
            BaseObject parent = f.getParent();
            if (parent instanceof FieldGroup) {
                FieldGroup fGroup = (FieldGroup) parent;
                return MessageUtil.getMessage("field_nameIdAndGroup", f.getName(), f.getFieldId(), fGroup.getName());
            }
            return f.getNameAndFieldId();
        }

        private boolean isFieldTypeCompatible(Field relatedDocField) {
            FieldType relatedDocFieldType = relatedDocField.getFieldTypeEnum();
            FieldType docFieldType = field.getFieldTypeEnum();
            boolean fieldTypesCompatible = docFieldType.equals(relatedDocFieldType);
            if (!fieldTypesCompatible) {
                if (relatedDocFieldType.equals(FieldType.TEXT_FIELD) && TO_TEXT_FIELD_COMPATIBLE_TYPES.contains(docFieldType)) {
                    fieldTypesCompatible = true;
                } else if ((FieldType.USER.equals(docFieldType) || FieldType.CONTACT.equals(docFieldType)) && FROM_USER_CONTACT_COMPATIBLE_TYPES.contains(relatedDocFieldType)) {
                    // allow mapping from (user or contact) to (users, contacts, users/contacts)
                    fieldTypesCompatible = true;
                } else if (FieldType.USERS.equals(docFieldType) && FieldType.USER.equals(relatedDocFieldType)) {
                    fieldTypesCompatible = true;
                } else if (FieldType.CONTACTS.equals(docFieldType) && FieldType.CONTACT.equals(relatedDocFieldType)) {
                    fieldTypesCompatible = true;
                }
            }
            return fieldTypesCompatible;
        }

    }

}
