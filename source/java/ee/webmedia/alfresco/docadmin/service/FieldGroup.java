package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * container for {@link Field}. Stored under /fieldGroupDefinitions or {@link DocumentTypeVersion}
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class FieldGroup extends FieldAndGroupBase implements MetadataContainer {
    private static final long serialVersionUID = 1L;

    public FieldGroup(BaseObject parent) {
        super(parent, DocumentAdminModel.Types.FIELD_GROUP);
    }

    /** Used by {@link BaseService#getObject(NodeRef, Class)} through reflection */
    public FieldGroup(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public FieldGroup(NodeRef parentRef, WmNode fieldDefNode) {
        super(parentRef, fieldDefNode);
    }

    @Override
    public DocumentTypeVersion getParent() {
        return (DocumentTypeVersion) super.getParent();
    }

    public ChildrenList<Field> getFields() {
        return getChildren(Field.class);
    }

    @Override
    public Collection<Field> getFieldsById(Set<String> fieldIdLocalNames) {
        Set<Field> matchingFields = new HashSet<Field>();
        for (Field existingField : getFields()) {
            if (fieldIdLocalNames.contains(existingField.getFieldId())) {
                matchingFields.add(existingField);
            }
        }
        return matchingFields;
    }

    public Map<String, Field> getFieldsByOriginalId() {
        Map<String, Field> fieldsByOriginalId = new HashMap<String, Field>();
        for (Field field : getFields()) {
            fieldsByOriginalId.put(field.getOriginalFieldId(), field);
        }
        return fieldsByOriginalId;
    }

    @Override
    public ChildrenList<Field> getMetadata() {
        return getFields();
    }

    /** @return „name (id, fieldTypeTranslated), ..., name (id, fieldTypeTranslated)" */
    @Override
    public String getAdditionalInfo() {
        List<String> fieldNames = new ArrayList<String>();
        List<? extends Field> fields;
        List<String> fieldDefinitionIds = getFieldDefinitionIds();
        if (!fieldDefinitionIds.isEmpty()) {
            // fieldGroup is under fieldGroupDefinitions where fields are referenced using multivalued property not child-assoc
            fields = getDocumentAdminService().getFieldDefinitions(fieldDefinitionIds);
        } else {
            fields = getFields();
        }
        for (Field field : fields) {
            fieldNames.add(field.getFieldNameWithIdAndType());
        }
        return StringUtils.join(fieldNames, ", ");
    }

    protected void nextSaveToParent(DocumentTypeVersion newParent) {
        super.nextSaveToParent(newParent, MetadataItem.class);
    }

    // START: properties
    public final String getReadonlyFieldsName() {
        return getProp(DocumentAdminModel.Props.READONLY_FIELDS_NAME);
    }

    public final void setReadonlyFieldsName(String readonlyFieldsName) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_NAME, readonlyFieldsName);
    }

    public final String getReadonlyFieldsRule() {
        return getProp(DocumentAdminModel.Props.READONLY_FIELDS_RULE);
    }

    public final void setReadonlyFieldsRule(String readonlyFieldsRule) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_RULE, readonlyFieldsRule);
    }

    public final void setShowInTwoColumns(boolean showInTwoColumns) {
        setProp(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS, showInTwoColumns);
    }

    public final boolean isShowInTwoColumns() {
        return getPropBoolean(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS);
    }

    public final String getThesaurus() {
        return getProp(DocumentAdminModel.Props.THESAURUS);
    }

    public final void setThesaurus(String thesaurus) {
        setProp(DocumentAdminModel.Props.THESAURUS, thesaurus);
    }

    public final boolean isReadonlyFieldsNameChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.READONLY_FIELDS_NAME_CHANGEABLE);
    }

    public final void setReadonlyFieldsNameChangeable(boolean readonlyFieldsNameChangeable) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_NAME_CHANGEABLE, readonlyFieldsNameChangeable);
    }

    public final boolean isReadonlyFieldsRuleChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.READONLY_FIELDS_RULE_CHANGEABLE);
    }

    public final void setReadonlyFieldsRuleChangeable(boolean readonlyFieldsRuleChangeable) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_RULE_CHANGEABLE, readonlyFieldsRuleChangeable);
    }

    public final boolean isShowInTwoColumnsChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS_CHANGEABLE);
    }

    public final void setShowInTwoColumnsChangeable(boolean showInTwoColumnsChangeable) {
        setProp(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS_CHANGEABLE, showInTwoColumnsChangeable);
    }

    // START: properties needed only for fieldGroup stored under fieldGroupDefinitions
    public List<String> getFieldDefinitionIds() {
        return getPropList(DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
    }

    public boolean isInapplicableForDoc() {
        return getPropBoolean(DocumentAdminModel.Props.INAPPLICABLE_FOR_DOC);
    }

    public boolean isInapplicableForVol() {
        return getPropBoolean(DocumentAdminModel.Props.INAPPLICABLE_FOR_VOL);
    }

    // END: properties needed only for fieldGroup stored under fieldGroupDefinitions
    // END: properties

    @Override
    public FieldGroup clone() {
        return (FieldGroup) super.clone(); // just return casted type
    }

    List<Field> getRemovedFields() {
        Map<Class<? extends BaseObject>, List<? extends BaseObject>> removedChildren = super.getRemovedChildren();
        @SuppressWarnings("unchecked")
        List<Field> removedFields = (List<Field>) removedChildren.get(Field.class);
        return removedFields != null ? removedFields : Collections.<Field> emptyList();
    }

    // Utilities

    /**
     * Find first {@link Field} that matches given {@code fieldId}. Traverses all child {@link Field}s.
     * 
     * @param fieldId fieldId to match by, cannot be {@code null}.
     * @return {@code null} if not found; otherwise the found field.
     */
    public Field getFieldById(String fieldId) {
        Assert.notNull(fieldId, "fieldId cannot be null");
        for (Field field : getFields()) {
            if (field.getFieldId().equals(fieldId)) {
                return field;
            }
        }
        return null;
    }

    public Set<String> getOriginalFieldIds() {
        HashSet<String> originalFieldIds = new HashSet<String>();
        for (Field field : getFields()) {
            originalFieldIds.add(field.getOriginalFieldId());
        }
        return originalFieldIds;
    }

}
