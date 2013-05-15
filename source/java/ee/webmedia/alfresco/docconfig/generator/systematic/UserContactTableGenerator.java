package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.utils.RepoUtil.getListElement;

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
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.LeaveType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingCode;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingService;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class UserContactTableGenerator extends BaseSystematicFieldGenerator {

    public static final String BEAN_NAME = "userContactTableGenerator";
    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;
    private ClassificatorService classificatorService;

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
        userContactMappingService.registerMappingDependency(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName(), DocumentCommonModel.Props.RECIPIENT_GROUP.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.RECIPIENT_GROUP.getLocalName(), DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName());

        Map<String, UserContactMappingCode> additionalRecipientsMapping = new HashMap<String, UserContactMappingCode>();
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName(), UserContactMappingCode.NAME);
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME.getLocalName(), null);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        mappings.add(additionalRecipientsMapping);
        userContactMappingService.registerMappingDependency(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName(),
                DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_GROUP.getLocalName(),
                DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName());
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName());

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

        List<String> userNamesLocalNames = RepoUtil.getLocalNames(DocumentDynamicModel.Props.USER_NAME, DocumentDynamicModel.Props.USER_JOB_TITLE,
                DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT);
        // userOrgStructUnit override is actually not needed as it is already multivalued, but it is stated here to mark
        // that in this group it has "multi-multi" values, i.e. lists of lists
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.USERS_TABLE, new HashSet<String>(userNamesLocalNames));

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
            FieldType fieldTypeEnum = child.getFieldTypeEnum();
            if (usersContactsFieldTypes.contains(fieldTypeEnum)) {
                Assert.isNull(foundField);
                foundField = child;
            } else {
                if (mapping != null && mapping.get(child.getQName()) != null) {
                    Assert.isTrue(fieldTypeEnum == FieldType.TEXT_FIELD || fieldTypeEnum == FieldType.STRUCT_UNIT);
                } else {
                    Assert.isTrue(fieldTypeEnum != FieldType.LISTBOX);
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

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();

        QName leaveTypeProp = null;
        QName beginDateProp = null;
        QName endDateProp = null;
        QName calculatedDaysProp = null;
        Set<String> leaveStudyClassificatorValueNames = new HashSet<String>();
        String stateHolderKey = field.getFieldId();
        String leaveValueChanged = "¤valueChangeListener=" + getBindingName("leaveTypeOrDateValueChanged", stateHolderKey);
        List<String> props = new ArrayList<String>();
        List<QName> propNames = new ArrayList<QName>();
        boolean isUserTableGroup = SystematicFieldGroupNames.USERS_TABLE.equals(group.getName());
        for (Field child : group.getFields()) {
            QName fieldId = child.getQName();

            ComponentUtil.addRecipientGrouping(child, item, namespaceService);
            // TODO Alar: refactor, so that components would be generated by generators
            String componentGeneratorAndProps;
            if (child.getFieldTypeEnum() == FieldType.TEXT_FIELD || usersContactsFieldTypes.contains(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = "TextAreaGenerator¤styleClass=expand19-200";
            } else if (child.getFieldTypeEnum() == FieldType.STRUCT_UNIT) {
                componentGeneratorAndProps = "StructUnitSearchGenerator¤converter=ee.webmedia.alfresco.common.propertysheet.converter.ListToLongestStringConverter";
            } else if (child.getFieldTypeEnum() == FieldType.DATE) {
                componentGeneratorAndProps = "DatePickerGenerator";
            } else if (child.getFieldTypeEnum() == FieldType.LONG) {
                // TODO do calculated fields need to be read-only?
                componentGeneratorAndProps = RepoConstants.GENERATOR_TEXT_FIELD + "¤styleClass=tiny center¤converter=" + LongConverter.CONVERTER_ID;
            } else if (child.getFieldTypeEnum() == FieldType.COMBOBOX) {
                componentGeneratorAndProps = "ClassificatorSelectorGenerator¤classificatorName=" + child.getClassificator();
            } else {
                throw new RuntimeException("FieldType " + child.getFieldTypeEnum() + " is not supported inside a table");
            }
            if (RepoUtil.getLocalNames(DocumentSpecificModel.Props.SUBSTITUTE_NAME, DocumentDynamicModel.Props.USER_NAME, DocumentDynamicModel.Props.USER_JOB_TITLE,
                            DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT).contains(child.getOriginalFieldId())
                    || (isUserTableGroup && !DocumentDynamicModel.Props.USER_NAME.getLocalName().equals(child.getOriginalFieldId()))
                    || FieldChangeableIf.ALWAYS_NOT_CHANGEABLE.equals(child.getChangeableIfEnum())) {
                componentGeneratorAndProps += "¤read-only=true";
            } else if (FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC.equals(child.getChangeableIfEnum())) {
                componentGeneratorAndProps += "¤readOnlyIf=#{" + DocumentDialogHelperBean.BEAN_NAME + ".notWorkingOrNotEditable}";
            }
            if ((isUserTableGroup && DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT.getLocalName().equals(child.getOriginalFieldId()))) {
                componentGeneratorAndProps += "¤editable=true¤textarea=true¤styleClass=expand19-200";
            }

            if (DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.SUBSTITUTION_END_DATE.getLocalName().equals(child.getOriginalFieldId())) {
                Field substituteNameField = group.getFieldsByOriginalId().get(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName());
                componentGeneratorAndProps += "¤mandatoryIf=" + substituteNameField.getQName().toPrefixString(namespaceService) + "!=null";
            }
            if (DocumentSpecificModel.Props.LEAVE_TYPE.getLocalName().equals(child.getOriginalFieldId())) {
                componentGeneratorAndProps += leaveValueChanged;
                leaveTypeProp = fieldId;
                String leaveTypeClassificator = child.getClassificator();
                if (StringUtils.isNotBlank(leaveTypeClassificator)) {
                    List<ClassificatorValue> classificatorValues = classificatorService.getActiveClassificatorValues(
                            getClassificatorService().getClassificatorByName(leaveTypeClassificator));
                    for (ClassificatorValue classificatorValue : classificatorValues) {
                        if (LeaveType.LEAVE_STUDY.getValueName().equals(classificatorValue.getValueData())) {
                            leaveStudyClassificatorValueNames.add(classificatorValue.getValueName());
                        }
                    }
                }
            } else if (DocumentDynamicModel.Props.LEAVE_BEGIN_DATE.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE.getLocalName().equals(child.getOriginalFieldId())) {
                componentGeneratorAndProps += leaveValueChanged;
                beginDateProp = fieldId;
            } else if (DocumentDynamicModel.Props.LEAVE_END_DATE.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.LEAVE_NEW_END_DATE.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE.getLocalName().equals(child.getOriginalFieldId())) {
                componentGeneratorAndProps += leaveValueChanged;
                endDateProp = fieldId;
            } else if (DocumentSpecificModel.Props.LEAVE_DAYS.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentDynamicModel.Props.LEAVE_CHANGED_DAYS.getLocalName().equals(child.getOriginalFieldId())
                    || DocumentSpecificModel.Props.LEAVE_CANCELLED_DAYS.getLocalName().equals(child.getOriginalFieldId())) {
                calculatedDaysProp = fieldId;
            }
            props.add(fieldId.toPrefixString(namespaceService) + "¤" + componentGeneratorAndProps);
            propNames.add(fieldId);
        }

        item.setName(RepoUtil.createTransientProp(field.getFieldId()).toString());
        // pickerCallback, showFilter, filters, dialogTitleId are set by UserContactGenerator

        // And we overwrite some other attributes set by UserContactGenerator
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setStyleClass("");
        item.setDisplayLabel(group.getReadonlyFieldsName());
        if (isUserTableGroup) {
            item.setDisplayLabel(group.getFieldsByOriginalId().get(DocumentDynamicModel.Props.USER_NAME.getLocalName()).getName());
        }
        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName())) {
            item.setAddLabelId("document_add_recipient");
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName())) {
            item.setAddLabelId("document_add_additional_recipient");
        } else {
            item.setAddLabelId("add");
        }

        // And we set our own attributes
        item.setPropsGeneration(StringUtils.join(props, ","));

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
                    // TODO show substitute info based on substitute id
                }
            }
            item.setHiddenPropNames(StringUtils.join(hiddenPropNames, ','));
        }

        generatorResults.addStateHolder(stateHolderKey, new UserContactTableState(mapping, leaveTypeProp, beginDateProp, endDateProp, calculatedDaysProp,
                leaveStudyClassificatorValueNames));
    }

    // ===============================================================================================================================

    public static class UserContactTableState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final Map<QName, UserContactMappingCode> mapping;
        private final QName leaveTypeProp;
        private final QName beginDateProp;
        private final QName endDateProp;
        private final QName calculatedDaysProp;
        private final Set<String> leaveStudyClassificatorValueNames;

        public UserContactTableState(Map<QName, UserContactMappingCode> mapping, QName leaveTypeProp, QName beginDateProp, QName endDateProp, QName calculatedDaysProp,
                                     Set<String> leaveStudyClassificatorValueNames) {
            this.mapping = mapping;
            this.leaveTypeProp = leaveTypeProp;
            this.beginDateProp = beginDateProp;
            this.endDateProp = endDateProp;
            this.calculatedDaysProp = calculatedDaysProp;
            this.leaveStudyClassificatorValueNames = leaveStudyClassificatorValueNames;
        }

        public Map<QName, Serializable> getContactData(String result) {
            Map<QName, Serializable> values = BeanHelper.getUserContactMappingService().getMappedValues(mapping, new NodeRef(result));
            Assert.notNull(values, "Preprocess must eliminate non-existent results, so this result here must exist: " + result);
            return values;
        }

        public void leaveTypeOrDateValueChanged(ValueChangeEvent event) {
            String vb = event.getComponent().getValueBinding("value").getExpressionString();
            final Integer index = ComponentUtil.getIndexFromValueBinding(vb);

            // Execute at the end of UPDATE_MODEL_VALUES phase, because during this phase node properties are set from user submitted data.
            // Queue executeLater event on propertySheet, because it supports handling ActionEvents.
            // Find propertySheet from component's hierarchy, do NOT use dialogDataProvider#getPropertySheet,
            // because this AJAX submit is executed only on MultiValueEditor and thus PropertySheet binding to DocumentDynamicDialog has not been updated.
            UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(event.getComponent(), UIPropertySheet.class, true);
            ComponentUtil.executeLater(PhaseId.UPDATE_MODEL_VALUES, propertySheet, new Closure() {
                @Override
                public void execute(Object input) {
                    Node document = dialogDataProvider.getNode();

                    boolean subtractNationalHolidays = false;
                    if (leaveTypeProp != null) {
                        String leaveType = getListElement(document, leaveTypeProp, index);
                        if (!leaveStudyClassificatorValueNames.contains(leaveType)) {
                            subtractNationalHolidays = true;
                        }
                    }

                    Date beginDate = getListElement(document, beginDateProp, index);
                    Date endDate = getListElement(document, endDateProp, index);

                    @SuppressWarnings("unchecked")
                    List<Date> endDateList = (List<Date>) document.getProperties().get(endDateProp.toString());
                    if (endDateList != null && beginDate != null && endDate != null && endDate.before(beginDate)) {
                        endDate = beginDate;
                        endDateList.set(index, endDate);
                    }
                    Integer calculatedDays = null;
                    if (beginDate != null && endDate != null) {
                        calculatedDays = CalendarUtil.getDaysBetween(new LocalDate(beginDate.getTime()), new LocalDate(endDate.getTime()), subtractNationalHolidays,
                                getClassificatorService());
                    }
                    final Long calculatedDaysLong = calculatedDays == null ? null : new Long(calculatedDays.longValue());

                    @SuppressWarnings("unchecked")
                    List<Long> calculatedDaysList = (List<Long>) document.getProperties().get(calculatedDaysProp.toString());
                    if (calculatedDaysList == null) {
                        calculatedDaysList = new ArrayList<Long>();
                        document.getProperties().put(calculatedDaysProp.toString(), calculatedDaysList);
                    }
                    while (calculatedDaysList.size() <= index) {
                        calculatedDaysList.add(null);
                    }
                    calculatedDaysList.set(index, calculatedDaysLong);
                }
            });
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

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }
    // END: setters

}
