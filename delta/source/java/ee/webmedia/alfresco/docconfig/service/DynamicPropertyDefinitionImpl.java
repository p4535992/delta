package ee.webmedia.alfresco.docconfig.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Alar Kvell
 */
public class DynamicPropertyDefinitionImpl implements DynamicPropertyDefinition {

    private final QName name;
    private final String originalFieldId;
    private final String title;
    private final FieldType fieldType;
    private final boolean mandatory;
    private final Boolean multiValuedOverride;
    private final QName[] childAssocTypeQNameHierarchy;

    DynamicPropertyDefinitionImpl(Field field, Boolean multiValuedOverride, QName[] childAssocTypeQNameHierarchy) {
        Assert.notNull(field, "field");
        name = field.getQName();
        originalFieldId = field.getOriginalFieldId();
        title = field.getName();
        fieldType = field.getFieldTypeEnum();
        mandatory = field.isMandatory();
        this.multiValuedOverride = multiValuedOverride;
        this.childAssocTypeQNameHierarchy = childAssocTypeQNameHierarchy;
    }

    DynamicPropertyDefinitionImpl(String hiddenFieldId, DynamicPropertyDefinitionImpl originalPropertyDefinition) {
        Assert.notNull(hiddenFieldId, "hiddenFieldId");
        name = Field.getQName(hiddenFieldId);
        originalFieldId = null;
        title = null;
        fieldType = hiddenFieldId.contains("_") ? originalPropertyDefinition.fieldType : FieldType.TEXT_FIELD;
        mandatory = originalPropertyDefinition.mandatory;
        multiValuedOverride = originalPropertyDefinition.multiValuedOverride;
        childAssocTypeQNameHierarchy = originalPropertyDefinition.childAssocTypeQNameHierarchy;
    }

    @Override
    public ModelDefinition getModel() {
        return getDictionaryService().getModel(DocumentDynamicModel.MODEL); // TODO FIXME WARNING!! uses BeanHelper
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public DataTypeDefinition getDataType() {
        return getDictionaryService().getDataType(getDataTypeQName()); // TODO FIXME WARNING!! uses BeanHelper
    }

    @Override
    public QName getDataTypeQName() {
        if (originalFieldId != null && Arrays.asList(DocumentLocationGenerator.NODE_REF_FIELD_IDS).contains(originalFieldId)) {
            return DataTypeDefinition.NODE_REF;
        }
        switch (fieldType) {
        case DOUBLE:
            return DataTypeDefinition.DOUBLE;
        case LONG:
            return DataTypeDefinition.LONG;
        case DATE:
            return DataTypeDefinition.DATE;
        case CHECKBOX:
            return DataTypeDefinition.BOOLEAN;
        default:
            return DataTypeDefinition.TEXT;
        }
    }

    @Override
    public ClassDefinition getContainerClass() {
        return getDictionaryService().getClass(DocumentCommonModel.Types.DOCUMENT);
    }

    @Override
    public boolean isOverride() {
        return false;
    }

    @Override
    public boolean isMultiValued() {
        if (multiValuedOverride != null) {
            return multiValuedOverride;
        }
        switch (fieldType) {
        case USERS:
        case USERS_CONTACTS:
        case CONTACTS:
        case LISTBOX:
        case STRUCT_UNIT:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public boolean isMandatoryEnforced() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public boolean isStoredInIndex() {
        return false;
    }

    @Override
    public IndexTokenisationMode getIndexTokenisationMode() {
        return IndexTokenisationMode.TRUE;
    }

    @Override
    public boolean isIndexedAtomically() {
        return true;
    }

    @Override
    public List<ConstraintDefinition> getConstraints() {
        return Collections.emptyList();
    }

    @Override
    public QName[] getChildAssocTypeQNameHierarchy() {
        return childAssocTypeQNameHierarchy;
    }

    @Override
    public Boolean getMultiValuedOverride() {
        return multiValuedOverride;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[" +
                "name=" + name.toPrefixString(getNamespaceService()) +
                " originalFieldId=" + originalFieldId +
                " fieldType=" + fieldType +
                " mandatory=" + mandatory +
                " multiValuedOverride=" + multiValuedOverride +
                " tile=" + title +
                "]";
    }

}