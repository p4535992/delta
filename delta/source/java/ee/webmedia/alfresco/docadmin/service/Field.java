package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.constant.MappingRestriction;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Field that is stored under {@link DocumentTypeVersion} or {@link FieldGroup}, but not under /fieldDefinitions folder
 * 
 * @author Ats Uiboupin
 */
public class Field extends FieldAndGroupBase {
    private static final long serialVersionUID = 1L;
    /** this temp. property is added to Field that is created based on fieldDefinition */
    private static final QName COPY_OF_FIELD_DEF_NODE_REF = RepoUtil.createTransientProp("copyOfFieldDefNodeRef");
    private static final QName FOR_SEARCH = RepoUtil.createTransientProp("forSearch");
    private static final QName DATAFIELD_PARAM_NAME = RepoUtil.createTransientProp("datafieldParamName");

    /** used only by subclass */
    protected Field(BaseObject parent, QName type) {
        super(checkParentType(parent), type);
    }

    /** used only by subclass */
    protected Field(NodeRef parentRef, QName type) {
        super(parentRef, type);
    }

    public Field(BaseObject parent) {
        this(parent, DocumentAdminModel.Types.FIELD);
    }

    /** Used by {@link BaseService#getObjectObject(NodeRef, Class)} through reflection */
    public Field(BaseObject parent, WmNode node) {
        super(checkParentType(parent), node);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public Field(NodeRef parentRef, WmNode fieldNode) {
        super(parentRef, fieldNode);
    }

    @Override
    protected QName getAssocName() {
        return QName.createQName(DocumentDynamicModel.URI, getFieldId());
    }

    protected void nextSaveToParent(FieldGroup newParentFieldGroup) {
        nextSaveToParent(checkParentType(newParentFieldGroup), Field.class);
    }

    protected void nextSaveToParent(DocumentTypeVersion newParentFieldGroup) {
        nextSaveToParent(checkParentType(newParentFieldGroup), MetadataItem.class);
    }

    private static BaseObject checkParentType(BaseObject parent) {
        Assert.isTrue(parent instanceof MetadataContainer);
        return parent;
    }

    @Override
    /** used in JSP */
    public String getAdditionalInfo() {
        Object additionalInfoDefaultValue = getAdditionalInfoDefaultValue();
        if (additionalInfoDefaultValue == null) {
            additionalInfoDefaultValue = "";
        }
        MessageData msgData = new MessageDataImpl("docType_metadataList_additInfo_field"
                , MessageUtil.getMessage(getFieldTypeEnum()) // type
                , additionalInfoDefaultValue // defaultValue
                , new MessageDataImpl("docType_metadataList_additInfo_field_systematic_" + isSystematic())
                , new MessageDataImpl("docType_metadataList_additInfo_field_mandatory_" + isMandatory())
                );
        return MessageUtil.getMessage(msgData);
    }

    private MessageData getAdditionalInfoDefaultValue() {
        Object defaultValue;
        if (isDefaultUserLoggedIn()) {
            defaultValue = new MessageDataImpl("docType_metadataList_additInfo_field_default_user");
        } else if (StringUtils.isNotBlank(getClassificator())) {
            if (StringUtils.isNotBlank(getClassificatorDefaultValue())) {
                defaultValue = getClassificatorDefaultValue();
            } else {
                defaultValue = new MessageDataImpl("docType_metadataList_additInfo_field_default_fromClassificator", getClassificator());
            }
        } else if (isDefaultDateSysdate()) {
            defaultValue = new MessageDataImpl("docType_metadataList_additInfo_field_default_sysdate");
        } else if (FieldType.CHECKBOX.name().equals(getFieldType())) {
            if (isDefaultSelected()) {
                defaultValue = new MessageDataImpl("docType_metadataList_additInfo_field_default_checkBox_selected");
            } else {
                defaultValue = new MessageDataImpl("docType_metadataList_additInfo_field_default_checkBox_notSelected");
            }
        } else if (StringUtils.isNotBlank(getDefaultValue())) {
            defaultValue = getDefaultValue();
        } else {
            return null;
        }
        return new MessageDataImpl("docType_metadataList_additInfo_field_default", defaultValue);
    }

    /** used by fields-list-bean.jsp */
    public final String getNameAndFieldId() {
        return new StringBuilder(getName()).append(" (").append(getFieldId()).append(")").toString();
    }

    public String getFieldNameWithIdAndType() {
        return MessageUtil.getMessage("field_nameIdAndType", getName(), getFieldId(), MessageUtil.getMessage(getFieldTypeEnum()));
    }

    public final String getFieldId() {
        return getProp(DocumentAdminModel.Props.FIELD_ID);
    }

    public final QName getQName() {
        return getQName(getFieldId());
    }

    public static final QName getQName(String fieldId) {
        return QName.createQName(DocumentDynamicModel.URI, fieldId);
    }

    // Properties

    public final void setFieldId(String fieldId) {
        setProp(DocumentAdminModel.Props.FIELD_ID, fieldId);
    }

    public final boolean isMandatory() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY);
    }

    public final void setMandatory(boolean mandatory) {
        setProp(DocumentAdminModel.Props.MANDATORY, mandatory);
    }

    public final String getChangeableIf() {
        return getProp(DocumentAdminModel.Props.CHANGEABLE_IF);
    }

    public final void setChangeableIf(String changeableIf) {
        setProp(DocumentAdminModel.Props.CHANGEABLE_IF, changeableIf);
    }

    public FieldChangeableIf getChangeableIfEnum() {
        return getEnumFromValue(FieldChangeableIf.class, getChangeableIf());
    }

    public void setChangeableIfEnum(FieldChangeableIf changeableIf) {
        setChangeableIf(getValueFromEnum(changeableIf));
    }

    public final String getClassificator() {
        return getProp(DocumentAdminModel.Props.CLASSIFICATOR);
    }

    public final void setClassificator(String classificator) {
        setProp(DocumentAdminModel.Props.CLASSIFICATOR, classificator);
    }

    public final String getDefaultValue() {
        return getProp(DocumentAdminModel.Props.DEFAULT_VALUE);
    }

    public final void setDefaultValue(String defaultValue) {
        setProp(DocumentAdminModel.Props.DEFAULT_VALUE, defaultValue);
    }

    public final String getClassificatorDefaultValue() {
        return getProp(DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE);
    }

    public final void setClassificatorDefaultValue(String classificatorDefaultValue) {
        setProp(DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE, classificatorDefaultValue);
    }

    public final boolean isDefaultDateSysdate() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_DATE_SYSDATE);
    }

    public final void setDefaultDateSysdate(boolean defaultDateSysdate) {
        setProp(DocumentAdminModel.Props.DEFAULT_DATE_SYSDATE, defaultDateSysdate);
    }

    public final boolean isDefaultUserLoggedIn() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN);
    }

    public final void setDefaultUserLoggedIn(boolean defaultUserLoggedIn) {
        setProp(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN, defaultUserLoggedIn);
    }

    public final boolean isDefaultSelected() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_SELECTED);
    }

    public final void setDefaultSelected(boolean defaultSelected) {
        setProp(DocumentAdminModel.Props.DEFAULT_SELECTED, defaultSelected);
    }

    public final String getFieldType() {
        return getProp(DocumentAdminModel.Props.FIELD_TYPE);
    }

    public final void setFieldType(String fieldType) {
        setProp(DocumentAdminModel.Props.FIELD_TYPE, fieldType);
    }

    public FieldType getFieldTypeEnum() {
        return getEnumFromValue(FieldType.class, getFieldType());
    }

    public void setFieldTypeEnum(FieldType fieldType) {
        setFieldType(getValueFromEnum(fieldType));
    }

    public final boolean isOnlyInGroup() {
        return getPropBoolean(DocumentAdminModel.Props.ONLY_IN_GROUP);
    }

    public final void setOnlyInGroup(boolean onlyInGroup) {
        setProp(DocumentAdminModel.Props.ONLY_IN_GROUP, onlyInGroup);
    }

    public final boolean isMandatoryChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY_CHANGEABLE);
    }

    public final void setMandatoryChangeable(boolean mandatoryChangeable) {
        setProp(DocumentAdminModel.Props.MANDATORY_CHANGEABLE, mandatoryChangeable);
    }

    public final boolean isChangeableIfChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.CHANGEABLE_IF_CHANGEABLE);
    }

    public final void setChangeableIfChangeable(boolean changeableIfChangeable) {
        setProp(DocumentAdminModel.Props.CHANGEABLE_IF_CHANGEABLE, changeableIfChangeable);
    }

    public final boolean isComboboxNotRelatedToClassificator() {
        return getPropBoolean(DocumentAdminModel.Props.COMBOBOX_NOT_RELATED_TO_CLASSIFICATOR);
    }

    public final boolean isRemovableFromSystematicFieldGroup() {
        return getPropBoolean(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP);
    }

    public final void setRemovableFromSystematicFieldGroup(boolean removableFromSystematicFieldGroup) {
        setProp(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, removableFromSystematicFieldGroup);
    }

    public final String getMappingRestriction() {
        return getProp(DocumentAdminModel.Props.MAPPING_RESTRICTION);
    }

    public final void setMappingRestriction(String mappingRestriction) {
        setProp(DocumentAdminModel.Props.MAPPING_RESTRICTION, mappingRestriction);
    }

    public final String getOriginalFieldId() {
        return getProp(DocumentAdminModel.Props.ORIGINAL_FIELD_ID);
    }

    public final void setOriginalFieldId(String originalFieldId) {
        setProp(DocumentAdminModel.Props.ORIGINAL_FIELD_ID, originalFieldId);
    }

    public MappingRestriction getMappingRestrictionEnum() {
        return getEnumFromValue(MappingRestriction.class, getMappingRestriction());
    }

    public void setMappingRestrictionEnum(MappingRestriction mappingRestriction) {
        setMappingRestriction(getValueFromEnum(mappingRestriction));
    }

    @Override
    public boolean isRemovableFromList() {
        BaseObject parent = getParent();
        if (parent instanceof FieldGroup) {
            FieldGroup parentFieldGroup = (FieldGroup) parent;
            if (!parentFieldGroup.isSystematic() || isRemovableFromSystematicFieldGroup()) {
                return true;
            }
            return false;
        }
        return super.isRemovableFromList(); // parent is DocumentTypeVersion
    }

    @Override
    public Field clone() {
        return (Field) super.clone(); // just return casted type
    }

    public void setCopyOfFieldDefinition(FieldDefinition fieldDefinition) {
        setProp(COPY_OF_FIELD_DEF_NODE_REF, fieldDefinition.getNodeRef());
    }

    public boolean isCopyOfFieldDefinition() {
        return getProp(COPY_OF_FIELD_DEF_NODE_REF) != null;
    }

    public void setForSearch(Boolean bol) {
        setProp(FOR_SEARCH, bol);
    }

    public boolean isForSearch() {
        return getProp(FOR_SEARCH) != null;
    }

    public void setDatafieldParamName(String paramName) {
        setProp(DATAFIELD_PARAM_NAME, paramName);
    }

    public String getDatafieldParamName() {
        return getProp(DATAFIELD_PARAM_NAME);
    }

    public static List<String> getLocalNames(Collection<QName> qNameFieldDefinitionIds) {
        List<String> fieldDefinitionIds = new ArrayList<String>(qNameFieldDefinitionIds.size());
        for (QName qName : qNameFieldDefinitionIds) {
            fieldDefinitionIds.add(qName.getLocalName());
        }
        return fieldDefinitionIds;
    }

}
