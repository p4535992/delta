package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static org.alfresco.web.ui.common.StringUtils.encode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.convert.LongConverter;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingCode;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingService;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class UserContactTableGenerator extends BaseSystematicFieldGenerator {

    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;

    private String[] originalFieldIds;

    @Override
    public void afterPropertiesSet() {
        Set<Map<String, UserContactMappingCode>> mappings = new HashSet<Map<String, UserContactMappingCode>>();

        // We register only the significant field for multiValuedOverride, because some other fields (e.g userJobTitle) are also used in other systematic groups
        // TODO Alar: but doesn't originalFieldIds mess that up?

        Map<String, UserContactMappingCode> recipientsMapping = new HashMap<String, UserContactMappingCode>();
        recipientsMapping.put(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName(), UserContactMappingCode.NAME);
        recipientsMapping.put(DocumentCommonModel.Props.RECIPIENT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME.getLocalName(), null);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        mappings.add(recipientsMapping);
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName());

        Map<String, UserContactMappingCode> additionalRecipientsMapping = new HashMap<String, UserContactMappingCode>();
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName(), UserContactMappingCode.NAME);
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME.getLocalName(), null);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        mappings.add(additionalRecipientsMapping);
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName());

        /*
         * Map<String, UserContactMappingCode> usersMapping = new HashMap<String, UserContactMappingCode>();
         * usersMapping.put(DocumentDynamicModel.Props.USER_NAMES.getLocalName(), UserContactMappingCode.NAME);
         * usersMapping.put(DocumentDynamicModel.Props.USER_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
         * usersMapping.put(DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
         * mappings.add(usersMapping);
         */

        Map<String, UserContactMappingCode> substituteMapping = new HashMap<String, UserContactMappingCode>();
        substituteMapping.put(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName(), UserContactMappingCode.NAME);
        substituteMapping.put(DocumentSpecificModel.Props.SUBSTITUTE_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
        substituteMapping.put(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE.getLocalName(), null);
        substituteMapping.put(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE.getLocalName(), null);
        substituteMapping.put(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), UserContactMappingCode.CODE);
        mappings.add(substituteMapping);
        userContactMappingService.registerMappingDependency(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName());

        Set<String> fields = new HashSet<String>();
        for (Map<String, UserContactMappingCode> mapping : mappings) {
            userContactMappingService.registerOriginalFieldIdsMapping(mapping);
            fields.addAll(mapping.keySet());
        }

        fields.add(DocumentSpecificModel.Props.LEAVE_TYPE.getLocalName());
        fields.add(DocumentDynamicModel.Props.LEAVE_BEGIN_DATE.getLocalName());
        fields.add(DocumentDynamicModel.Props.LEAVE_END_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_DAYS.getLocalName());
        fields.add(DocumentDynamicModel.Props.LEAVE_WORK_YEAR.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentDynamicModel.Props.LEAVE_BEGIN_DATE.getLocalName());

        fields.add(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE.getLocalName());
        fields.add(DocumentDynamicModel.Props.LEAVE_CHANGED_DAYS.getLocalName());
        fields.add(DocumentDynamicModel.Props.LEAVE_NEW_WORK_YEAR.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE.getLocalName());

        fields.add(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE.getLocalName());
        fields.add(DocumentSpecificModel.Props.LEAVE_CANCELLED_DAYS.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE.getLocalName());

        originalFieldIds = fields.toArray(new String[fields.size()]);
        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return originalFieldIds;
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        // TODO if substituteName is used outside systematic group, then (just like in UserContactRelatedGroupGenerator):
        // * set editable=false
        // * add substitution info

        Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMappingOrNull(field);

        // Ensure that exactly one USERS/CONTACTS/USERS_CONTACTS field is in this group;
        // and all other fields that have mapping, are TEXT_FIELD;
        // and all other fields that don't have mapping, are single-valued;
        FieldGroup group = (FieldGroup) field.getParent();
        Field foundField = null;
        List<FieldType> usersContactsFieldTypes = Arrays.asList(FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS, FieldType.USER, FieldType.CONTACT,
                FieldType.USER_CONTACT);
        for (Field child : group.getFields()) {
            if (usersContactsFieldTypes.contains(child.getFieldTypeEnum())) {
                Assert.isNull(foundField);
                foundField = child;
            } else {
                if (mapping != null && mapping.get(child.getQName()) != null) {
                    Assert.isTrue(child.getFieldTypeEnum() == FieldType.TEXT_FIELD);
                } else {
                    Assert.isTrue(child.getFieldTypeEnum() != FieldType.LISTBOX);
                }
            }
        }
        // TODO Alar: refactor UserContact functionality to subclass
        Assert.isTrue((foundField == null) == (mapping == null));
        if (foundField == null) {
            foundField = group.getFields().get(0);
        }
        // Only generate a component for the USERS/CONTACTS/USERS_CONTACTS field of this group; for other fields, don't generate anything
        if (field != foundField) {
            return;
        }

        Map<QName, String> transformClassificatorsValues = new HashMap<QName, String>();
        List<String> props = new ArrayList<String>();
        List<QName> propNames = new ArrayList<QName>();
        for (Field child : group.getFields()) {
            QName fieldId = child.getQName();

            // TODO Alar: refactor, so that components would be generated by generators
            String componentGeneratorAndProps;
            if (child.getFieldTypeEnum() == FieldType.TEXT_FIELD || usersContactsFieldTypes.contains(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = "TextAreaGenerator¤styleClass=expand19-200";
            } else if (child.getFieldTypeEnum() == FieldType.DATE) {
                componentGeneratorAndProps = "DatePickerGenerator";
            } else if (child.getFieldTypeEnum() == FieldType.LONG) {
                // TODO do calculated fields need to be read-only?
                componentGeneratorAndProps = RepoConstants.GENERATOR_TEXT_FIELD + "¤styleClass=tiny center¤converter=" + LongConverter.CONVERTER_ID;
            } else if (child.getFieldTypeEnum() == FieldType.COMBOBOX) {
                componentGeneratorAndProps = "ClassificatorSelectorGenerator¤classificatorName=" + child.getClassificator();
            } else {
                throw new RuntimeException("FieldType " + field.getFieldTypeEnum() + " is not supported inside a table");
            }
            if (child.getOriginalFieldId().equals(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName())) {
                componentGeneratorAndProps += "¤read-only=true";
            }
            props.add(fieldId.toPrefixString(namespaceService) + "¤" + componentGeneratorAndProps);
            propNames.add(fieldId);
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        // pickerCallback, showFilter, filters, dialogTitleId are set by UserContactGenerator

        // And we overwrite some other attributes set by UserContactGenerator
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setStyleClass("");
        item.setDisplayLabel(group.getReadonlyFieldsName());
        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName())) {
            item.setAddLabelId("document_add_recipient");
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName())) {
            item.setAddLabelId("document_add_additional_recipient");
        } else {
            item.setAddLabelId("add");
        }

        // And we set our own attributes
        item.setShowInViewMode(false);
        item.setPropsGeneration(StringUtils.join(props, ","));
        String stateHolderKey = field.getFieldId();

        QName substituteIdsPropName = null;
        if (mapping != null) {
            item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}");
            item.setSetterCallback(getBindingName("getContactData", stateHolderKey));
            item.setSetterCallbackReturnsMap(true);

            List<String> hiddenPropNames = new ArrayList<String>();
            for (String fieldId : documentConfigService.getHiddenPropFieldIds(group.getFields())) {
                QName propName = Field.getQName(fieldId);
                String propNameString = propName.toPrefixString(namespaceService);
                hiddenPropNames.add(propNameString);
                if (mapping.get(propName) == UserContactMappingCode.CODE) {
                    substituteIdsPropName = propName;
                }
            }
            item.setHiddenPropNames(StringUtils.join(hiddenPropNames, ','));
        }

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(field.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new UserContactTableState(propNames, mapping, viewModePropName, substituteIdsPropName, transformClassificatorsValues));
    }

    // ===============================================================================================================================

    public static class UserContactTableState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final List<QName> propNames;
        private final Map<QName, UserContactMappingCode> mapping;
        private final String viewModePropName;
        private final QName substituteIdsPropName;
        private final Map<QName /* propName */, String /* classificatorName */> transformClassificatorsValues;
        private final FastDateFormat dateFormat;

        public UserContactTableState(List<QName> propNames, Map<QName, UserContactMappingCode> mapping, String viewModePropName, QName substituteIdsPropName,
                                     Map<QName, String> transformClassificatorsValues) {
            this.propNames = propNames;
            this.mapping = mapping;
            this.viewModePropName = viewModePropName;
            this.substituteIdsPropName = substituteIdsPropName;
            this.transformClassificatorsValues = transformClassificatorsValues;
            dateFormat = FastDateFormat.getInstance(MessageUtil.getMessage("date_pattern"));
        }

        public Map<QName, Serializable> getContactData(String result) {
            Map<QName, Serializable> values = BeanHelper.getUserContactMappingService().getMappedValues(mapping, new NodeRef(result));
            Assert.notNull(values, "Preprocess must eliminate non-existent results, so this result here must exist: " + result);
            return values;
        }

        // XXX Only supports String and Date values at the moment
        @Override
        protected void reset(boolean inEditMode) {
            // TODO add substitution info based on substituteId
            final Node document = dialogDataProvider.getNode();
            if (!inEditMode) {
                int size = 0;
                List<List<Serializable>> all = new ArrayList<List<Serializable>>();
                for (QName propName : propNames) {
                    @SuppressWarnings("unchecked")
                    List<Serializable> columnValues = (List<Serializable>) document.getProperties().get(propName);
                    if (columnValues == null) {
                        columnValues = new ArrayList<Serializable>();
                    }
                    size = Math.max(columnValues.size(), size);

                    String classificatorName = transformClassificatorsValues.get(propName);
                    if (StringUtils.isNotBlank(classificatorName)) {
                        List<ClassificatorValue> classificatorValues = getClassificatorService().getAllClassificatorValues(classificatorName);
                        List<Serializable> transformedValues = new ArrayList<Serializable>(columnValues.size());
                        for (Serializable columnValue : columnValues) {
                            if (columnValue instanceof String) {
                                for (ClassificatorValue classificatorValue : classificatorValues) {
                                    if (classificatorValue.getValueName().equals(columnValue) && StringUtils.isNotBlank(classificatorValue.getClassificatorDescription())) {
                                        transformedValues.add(classificatorValue.getClassificatorDescription());
                                        continue;
                                    }
                                }
                            }
                            transformedValues.add(columnValue);
                        }
                        all.add(transformedValues);
                    } else {
                        all.add(columnValues);
                    }
                }

                List<String> substituteIds = null;
                if (substituteIdsPropName != null) {
                    substituteIds = (List<String>) document.getProperties().get(substituteIdsPropName);
                }

                List<String> rows = new ArrayList<String>(size);
                for (int i = 0; i < size; i++) {
                    List<String> rowValues = new ArrayList<String>();
                    for (List<Serializable> columnValues : all) {
                        if (i < columnValues.size()) {
                            Serializable value = columnValues.get(i);
                            if (value instanceof Date) {
                                value = dateFormat.format((Date) value);
                            } else if (value instanceof Long) {
                                value = ((Long) value).toString();
                            }
                            String stringValue = StringUtils.trim((String) value);
                            if (StringUtils.isNotBlank(stringValue)) {
                                rowValues.add(encode(stringValue));
                            }
                            // TODO email link??
                        }
                    }
                    if (!rowValues.isEmpty()) {
                        String row = StringUtils.join(rowValues, ", ");
                        if (substituteIds != null && i < substituteIds.size()) {
                            String substInfo = UserUtil.getSubstitute(substituteIds.get(i));
                            if (!StringUtils.isBlank(substInfo)) {
                                row += "<span class=\"fieldExtraInfo\">" + encode(substInfo) + "</span>";
                            }
                        }
                        rows.add(row);
                    }
                }
                document.getProperties().put(viewModePropName, StringUtils.join(rows, "<br/>"));
            }
        }
    }

    // ===============================================================================================================================

    // START: setters
    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setUserContactMappingService(UserContactMappingService userContactMappingService) {
        this.userContactMappingService = userContactMappingService;
    }
    // END: setters

}
