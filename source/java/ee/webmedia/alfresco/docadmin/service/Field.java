package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.utils.TextUtil.joinNonBlankStringsWithComma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.BooleanUtils;
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
 */
public class Field extends FieldAndGroupBase {
    private static long serialVersionUID = 1L;
    /** this temp. property is added to Field that is created based on fieldDefinition */
    private static final QName COPY_OF_FIELD_DEF_NODE_REF = RepoUtil.createTransientProp("copyOfFieldDefNodeRef");
    private static final QName FOR_SEARCH = RepoUtil.createTransientProp("forSearch");
    private static final QName DATAFIELD_PARAM_NAME = RepoUtil.createTransientProp("datafieldParamName");
    private static final Map<String, QName> FIELD_ID_TO_ASSOC_QNAME = new HashMap<>();

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
        return RepoUtil.getFromQNamePool(getFieldId(), DocumentDynamicModel.URI, FIELD_ID_TO_ASSOC_QNAME);
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
        List<Object> messageValueHolders = new ArrayList<Object>(Arrays.asList(MessageUtil.getMessage(getFieldTypeEnum()) // type
                , additionalInfoDefaultValue // defaultValue
                , new MessageDataImpl("docType_metadataList_additInfo_field_systematic_" + isSystematic())
                , new MessageDataImpl("docType_metadataList_additInfo_field_mandatory_" + isMandatory())));
        List<String> relatedIncomingDecElement = getRelatedIncomingDecElement();
        if (relatedIncomingDecElement != null) {
            String relatedIncomingDecElementStr = joinNonBlankStringsWithComma(relatedIncomingDecElement);
            if (StringUtils.isNotBlank(relatedIncomingDecElementStr)) {
                messageValueHolders.add(new MessageDataImpl("docType_metadataList_additInfo_related_incoming_dec", relatedIncomingDecElementStr));
            } else {
                messageValueHolders.add("");
            }
        } else {
            messageValueHolders.add("");
        }
        List<String> relatedOutgoingDecElement = getRelatedOutgoingDecElement();
        if (relatedOutgoingDecElement != null) {
            String relatedOutgoingDecElementStr = joinNonBlankStringsWithComma(relatedOutgoingDecElement);
            if (StringUtils.isNotBlank(relatedOutgoingDecElementStr)) {
                messageValueHolders.add(new MessageDataImpl("docType_metadataList_additInfo_related_outgoing_dec", relatedOutgoingDecElementStr));
            } else {
                messageValueHolders.add("");
            }
        } else {
            messageValueHolders.add("");
        }
        MessageData msgData = new MessageDataImpl("docType_metadataList_additInfo_field", messageValueHolders);
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
        return RepoUtil.getFromQNamePool(fieldId, DocumentDynamicModel.URI, FIELD_ID_TO_ASSOC_QNAME);
    }

    // Properties

    public void setFieldId(String fieldId) {
        setProp(DocumentAdminModel.Props.FIELD_ID, fieldId);
    }

    public boolean isMandatory() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY);
    }

    public void setMandatory(boolean mandatory) {
        setProp(DocumentAdminModel.Props.MANDATORY, mandatory);
    }

    public String getChangeableIf() {
        return getProp(DocumentAdminModel.Props.CHANGEABLE_IF);
    }

    public void setChangeableIf(String changeableIf) {
        setProp(DocumentAdminModel.Props.CHANGEABLE_IF, changeableIf);
    }

    public FieldChangeableIf getChangeableIfEnum() {
        return getEnumFromValue(FieldChangeableIf.class, getChangeableIf());
    }

    public void setChangeableIfEnum(FieldChangeableIf changeableIf) {
        setChangeableIf(getValueFromEnum(changeableIf));
    }

    public String getClassificator() {
        return getProp(DocumentAdminModel.Props.CLASSIFICATOR);
    }

    public void setClassificator(String classificator) {
        setProp(DocumentAdminModel.Props.CLASSIFICATOR, classificator);
    }

    public String getDefaultValue() {
        return getProp(DocumentAdminModel.Props.DEFAULT_VALUE);
    }

    public void setDefaultValue(String defaultValue) {
        setProp(DocumentAdminModel.Props.DEFAULT_VALUE, defaultValue);
    }

    public String getClassificatorDefaultValue() {
        return getProp(DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE);
    }

    public void setClassificatorDefaultValue(String classificatorDefaultValue) {
        setProp(DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE, classificatorDefaultValue);
    }

    public boolean isDefaultDateSysdate() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_DATE_SYSDATE);
    }

    public void setDefaultDateSysdate(boolean defaultDateSysdate) {
        setProp(DocumentAdminModel.Props.DEFAULT_DATE_SYSDATE, defaultDateSysdate);
    }

    public boolean isDefaultUserLoggedIn() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN);
    }

    public void setDefaultUserLoggedIn(boolean defaultUserLoggedIn) {
        setProp(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN, defaultUserLoggedIn);
    }

    public boolean isDefaultSelected() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_SELECTED);
    }

    public void setDefaultSelected(boolean defaultSelected) {
        setProp(DocumentAdminModel.Props.DEFAULT_SELECTED, defaultSelected);
    }

    public String getFieldType() {
        return getProp(DocumentAdminModel.Props.FIELD_TYPE);
    }

    public void setFieldType(String fieldType) {
        setProp(DocumentAdminModel.Props.FIELD_TYPE, fieldType);
    }

    public FieldType getFieldTypeEnum() {
        return getEnumFromValue(FieldType.class, getFieldType());
    }

    public void setFieldTypeEnum(FieldType fieldType) {
        setFieldType(getValueFromEnum(fieldType));
    }

    public boolean isOnlyInGroup() {
        return getPropBoolean(DocumentAdminModel.Props.ONLY_IN_GROUP);
    }

    public void setOnlyInGroup(boolean onlyInGroup) {
        setProp(DocumentAdminModel.Props.ONLY_IN_GROUP, onlyInGroup);
    }

    public boolean isMandatoryChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY_CHANGEABLE);
    }

    public void setMandatoryChangeable(boolean mandatoryChangeable) {
        setProp(DocumentAdminModel.Props.MANDATORY_CHANGEABLE, mandatoryChangeable);
    }

    public boolean isChangeableIfChangeable() {
        return getPropBoolean(DocumentAdminModel.Props.CHANGEABLE_IF_CHANGEABLE);
    }

    public void setChangeableIfChangeable(boolean changeableIfChangeable) {
        setProp(DocumentAdminModel.Props.CHANGEABLE_IF_CHANGEABLE, changeableIfChangeable);
    }

    public boolean isComboboxNotRelatedToClassificator() {
        return getPropBoolean(DocumentAdminModel.Props.COMBOBOX_NOT_RELATED_TO_CLASSIFICATOR);
    }

    public boolean isRemovableFromSystematicFieldGroup() {
        return getPropBoolean(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP);
    }

    public void setRemovableFromSystematicFieldGroup(boolean removableFromSystematicFieldGroup) {
        setProp(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, removableFromSystematicFieldGroup);
    }

    public String getMappingRestriction() {
        return getProp(DocumentAdminModel.Props.MAPPING_RESTRICTION);
    }

    public void setMappingRestriction(String mappingRestriction) {
        setProp(DocumentAdminModel.Props.MAPPING_RESTRICTION, mappingRestriction);
    }

    public String getOriginalFieldId() {
        return getProp(DocumentAdminModel.Props.ORIGINAL_FIELD_ID);
    }

    public void setOriginalFieldId(String originalFieldId) {
        setProp(DocumentAdminModel.Props.ORIGINAL_FIELD_ID, originalFieldId);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRelatedIncomingDecElement() {
        List<String> list = (List<String>) getNode().getProperties().get(DocumentAdminModel.Props.RELATED_INCOMING_DEC_ELEMENT);
        return Utils.removeNulls(list);
    }

    public void setRelatedIncomingDecElement(List<String> relatedIncomingDecElement) {
        setProp(DocumentAdminModel.Props.RELATED_INCOMING_DEC_ELEMENT, (Serializable) relatedIncomingDecElement);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRelatedOutgoingDecElement() {
        List<String> list = (List<String>) getNode().getProperties().get(DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT);
        return Utils.removeNulls(list);
    }

    public void setRelatedOutgoingDecElement(List<String> relatedOutgoingDecElement) {
        setProp(DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT, (Serializable) relatedOutgoingDecElement);
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
    
    public boolean isParameterInDocSearch() {
        return BooleanUtils.isTrue((Boolean) getProp(DocumentAdminModel.Props.IS_PARAMETER_IN_DOC_SEARCH));
    }
}
