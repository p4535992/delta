package ee.webmedia.alfresco.docadmin.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;

/**
 * Shielding class to prevent modifying encapsulated FieldDefinition object.
 * Used for field definitions cache.
 * If inner FieldDefinition needs to be changed, use getCopyOfFieldDefinition.
 */
public class UnmodifiableFieldDefinition implements Serializable {

    private static final long serialVersionUID = 1L;
    private final FieldDefinition fieldDefinition;
    private final List<String> unmodifiableVolTypes;
    private final List<String> unmodifiableDocTypes;

    /** Used by {@link BaseService#getObject(NodeRef, Class)} through reflection */
    public UnmodifiableFieldDefinition(FieldDefinition fieldDefinition) {
        this.fieldDefinition = fieldDefinition;
        unmodifiableDocTypes = Collections.unmodifiableList(fieldDefinition.getDocTypes());
        unmodifiableVolTypes = Collections.unmodifiableList(fieldDefinition.getVolTypes());
    }

    public List<String> getDocTypes() {
        return fieldDefinition.getDocTypes();
    }

    public <D extends DynamicType> List<String> getUsedTypes(Class<D> dynTypeClass) {
        return CaseFileType.class.isAssignableFrom(dynTypeClass) ? unmodifiableVolTypes : unmodifiableDocTypes;
    }

    public boolean isParameterInDocSearch() {
        return fieldDefinition.isParameterInDocSearch();
    }

    public boolean isParameterInVolSearch() {
        return fieldDefinition.isParameterInVolSearch();
    }

    public Integer getParameterOrderInDocSearch() {
        return fieldDefinition.getParameterOrderInDocSearch();
    }

    public Integer getParameterOrderInVolSearch() {
        return fieldDefinition.getParameterOrderInVolSearch();
    }

    public List<String> getVolTypes() {
        return unmodifiableVolTypes;
    }

    public boolean isFixedParameterInDocSearch() {
        return fieldDefinition.isFixedParameterInDocSearch();
    }

    public boolean isFixedParameterInVolSearch() {
        return fieldDefinition.isFixedParameterInVolSearch();
    }

    public boolean isInapplicableForDoc() {
        return fieldDefinition.isInapplicableForDoc();
    }

    public boolean isInapplicableForVol() {
        return fieldDefinition.isInapplicableForVol();
    }

    public FieldDefinition getCopyOfFieldDefinition() {
        return fieldDefinition.clone();
    }

    public final <D extends DynamicType> boolean isInapplicableForDynType(Class<D> dynTypeClass) {
        return fieldDefinition.isInapplicableForDynType(dynTypeClass);
    }

    protected QName getAssocName() {
        return fieldDefinition.getAssocName();
    }

    /** used by fields-list-bean.jsp */
    public final String getNameAndFieldId() {
        return fieldDefinition.getNameAndFieldId();
    }

    public String getFieldNameWithIdAndType() {
        return fieldDefinition.getFieldNameWithIdAndType();
    }

    public final String getFieldId() {
        return fieldDefinition.getFieldId();
    }

    public final QName getQName() {
        return fieldDefinition.getQName();
    }

    public boolean isMandatory() {
        return fieldDefinition.isMandatory();
    }

    public String getChangeableIf() {
        return fieldDefinition.getChangeableIf();
    }

    public FieldChangeableIf getChangeableIfEnum() {
        return fieldDefinition.getChangeableIfEnum();
    }

    public String getOriginalFieldId() {
        return fieldDefinition.getOriginalFieldId();
    }

    public String getName() {
        return fieldDefinition.getName();
    }

    public FieldType getFieldTypeEnum() {
        return fieldDefinition.getFieldTypeEnum();
    }

    public String getFieldType() {
        return fieldDefinition.getFieldType();
    }

    public String getClassificator() {
        return fieldDefinition.getClassificator();
    }

    public BaseObject getParent() {
        return fieldDefinition.getParent();
    }

}
