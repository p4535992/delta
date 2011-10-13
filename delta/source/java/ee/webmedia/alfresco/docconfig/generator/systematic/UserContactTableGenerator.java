package ee.webmedia.alfresco.docconfig.generator.systematic;

import static org.alfresco.web.ui.common.StringUtils.encode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
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
import ee.webmedia.alfresco.utils.RepoUtil;

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

        // We must also specify fields that have no mapping, because we register all fields to get correct multiValuedOverride behaviour

        Map<String, UserContactMappingCode> recipientsMapping = new HashMap<String, UserContactMappingCode>();
        recipientsMapping.put(DocumentCommonModel.Props.RECIPIENT_NAME.getLocalName(), UserContactMappingCode.NAME);
        recipientsMapping.put(DocumentCommonModel.Props.RECIPIENT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME.getLocalName(), null);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        recipientsMapping.put(DocumentDynamicModel.Props.RECIPIENT_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        mappings.add(recipientsMapping);

        Map<String, UserContactMappingCode> additionalRecipientsMapping = new HashMap<String, UserContactMappingCode>();
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME.getLocalName(), UserContactMappingCode.NAME);
        additionalRecipientsMapping.put(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME.getLocalName(), null);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        additionalRecipientsMapping.put(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        mappings.add(additionalRecipientsMapping);

        // TODO register only the significant field for multiValuedOverride, if other fields (e.g userJobTitle) are also used in other systematic groups

        /*
         * Map<String, UserContactMappingCode> usersMapping = new HashMap<String, UserContactMappingCode>();
         * usersMapping.put(DocumentDynamicModel.Props.USER_NAMES.getLocalName(), UserContactMappingCode.NAME);
         * usersMapping.put(DocumentDynamicModel.Props.USER_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
         * usersMapping.put(DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
         * mappings.add(usersMapping);
         */

        userContactMappingService.registerMappingDependency(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), DocumentDynamicModel.Props.SUBSTITUTE_NAME.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), DocumentDynamicModel.Props.SUBSTITUTE_NAME.getLocalName());

        Set<String> fields = new HashSet<String>();
        for (Map<String, UserContactMappingCode> mapping : mappings) {
            userContactMappingService.registerOriginalFieldIdsMapping(mapping);
            fields.addAll(mapping.keySet());
        }
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(fields);
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

        Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMapping(field);

        // Ensure that exactly one USERS/CONTACTS/USERS_CONTACTS field is in this group;
        // and all other fields that have mapping, are TEXT_FIELD;
        // and all other fields that don't have mapping, are single-valued;
        FieldGroup group = (FieldGroup) field.getParent();
        Field foundField = null;
        for (Field child : group.getFields()) {
            if (Arrays.asList(FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS).contains(child.getFieldTypeEnum())) {
                Assert.isNull(foundField);
                foundField = child;
            } else {
                if (mapping.get(child.getQName()) != null) {
                    Assert.isTrue(child.getFieldTypeEnum() == FieldType.TEXT_FIELD);
                } else {
                    Assert.isTrue(!Arrays.asList(
                            FieldType.LISTBOX,
                            FieldType.HIERARCHICAL_KEYWORD_LEVEL1,
                            FieldType.HIERARCHICAL_KEYWORD_LEVEL2
                            ).contains(child.getFieldTypeEnum()));
                }
            }
        }
        Assert.notNull(foundField);
        // Only generate a component for the USERS/CONTACTS/USERS_CONTACTS field of this group; for other fields, don't generate anything
        if (field != foundField) {
            return;
        }

        List<UserContactMappingCode> mappingCodes = new ArrayList<UserContactMappingCode>();
        List<String> props = new ArrayList<String>();
        List<QName> propNames = new ArrayList<QName>();
        for (Field child : group.getFields()) {
            QName fieldId = child.getQName();
            UserContactMappingCode mappingCode = mapping.get(fieldId);
            // mappingCode may be null too
            mappingCodes.add(mappingCode);
            props.add(fieldId.toPrefixString(namespaceService) + "¤TextAreaGenerator¤styleClass=expand19-200");
            propNames.add(fieldId);
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        // pickerCallback, showFilter, filters, addLabelId, dialogTitleId are set by UserContactGenerator

        // And we overwrite some other attributes set by UserContactGenerator
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setStyleClass("");
        item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}");
        item.setDisplayLabel(group.getReadonlyFieldsName());

        // And we set our own attributes
        item.setShowInViewMode(false);
        item.setPropsGeneration(StringUtils.join(props, ","));
        String stateHolderKey = field.getFieldId();
        item.setSetterCallback(getBindingName("getContactData", stateHolderKey));

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(field.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new UserContactTableState(propNames, mappingCodes, viewModePropName));
    }

    // ===============================================================================================================================

    public static class UserContactTableState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final List<QName> propNames;
        private final List<UserContactMappingCode> mappingCodes;
        private final String viewModePropName;

        public UserContactTableState(List<QName> propNames, List<UserContactMappingCode> mappingCodes, String viewModePropName) {
            this.propNames = propNames;
            this.mappingCodes = mappingCodes;
            this.viewModePropName = viewModePropName;
        }

        public List<String> getContactData(String result) {
            List<String> values = BeanHelper.getUserContactMappingService().getMappedValues(mappingCodes, new NodeRef(result));
            Assert.notNull(values, "Preprocess must eliminate non-existent results, so this result here must exist: " + result);
            return values;
        }

        @Override
        protected void reset(boolean inEditMode) {
            final Node document = dialogDataProvider.getNode();
            if (!inEditMode) {
                int size = 0;
                List<List<String>> all = new ArrayList<List<String>>();
                for (QName propName : propNames) {
                    @SuppressWarnings("unchecked")
                    List<String> columnValues = (List<String>) document.getProperties().get(propName);
                    if (columnValues == null) {
                        columnValues = new ArrayList<String>();
                    }
                    size = Math.max(columnValues.size(), size);
                    all.add(columnValues);
                }

                List<String> rows = new ArrayList<String>(size);
                for (int i = 0; i < size; i++) {
                    List<String> rowValues = new ArrayList<String>();
                    for (List<String> columnValues : all) {
                        if (i < columnValues.size()) {
                            String value = StringUtils.trim(columnValues.get(i));
                            if (StringUtils.isNotBlank(value)) {
                                rowValues.add(encode(value));
                            }
                            // TODO email link??
                        }
                    }
                    if (!rowValues.isEmpty()) {
                        rows.add(StringUtils.join(rowValues, ", "));
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
