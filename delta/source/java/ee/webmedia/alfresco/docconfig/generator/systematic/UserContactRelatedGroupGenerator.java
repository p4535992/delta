package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactMappingService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingCode;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingService;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class UserContactRelatedGroupGenerator extends BaseSystematicFieldGenerator {

    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;
    private DocumentAdminService documentAdminService;

    private String[] originalFieldIds;

    @Override
    public void afterPropertiesSet() {
        Set<Map<String, UserContactMappingCode>> mappings = new HashSet<Map<String, UserContactMappingCode>>();

        // We may, but don't have to specify fields that have no mapping, because no behaviour is associated with them
        // If we specify them with null mapping, then they are not overwritten when a user/contact/group is selected from search results

        Map<String, UserContactMappingCode> ownerMapping = new HashMap<String, UserContactMappingCode>();
        ownerMapping.put(DocumentCommonModel.Props.OWNER_ID.getLocalName(), UserContactMappingCode.CODE);
        ownerMapping.put(DocumentCommonModel.Props.OWNER_NAME.getLocalName(), UserContactMappingCode.NAME);
        ownerMapping.put(DocumentDynamicModel.Props.OWNER_SERVICE_RANK.getLocalName(), UserContactMappingCode.SERVICE_RANK);
        ownerMapping.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
        ownerMapping.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
        ownerMapping.put(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS.getLocalName(), UserContactMappingCode.ADDRESS);
        ownerMapping.put(DocumentCommonModel.Props.OWNER_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        ownerMapping.put(DocumentCommonModel.Props.OWNER_PHONE.getLocalName(), UserContactMappingCode.PHONE);
        mappings.add(ownerMapping);
        userContactMappingService.registerMappingDependency(DocumentCommonModel.Props.OWNER_ID.getLocalName(), DocumentCommonModel.Props.OWNER_NAME.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.OWNER_ID.getLocalName(), DocumentCommonModel.Props.OWNER_NAME.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.PREVIOUS_OWNER_ID.getLocalName(), DocumentCommonModel.Props.OWNER_NAME.getLocalName());

        Map<String, UserContactMappingCode> signerMapping = new HashMap<String, UserContactMappingCode>();
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), UserContactMappingCode.CODE);
        signerMapping.put(DocumentCommonModel.Props.SIGNER_NAME.getLocalName(), UserContactMappingCode.NAME);
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_SERVICE_RANK.getLocalName(), UserContactMappingCode.SERVICE_RANK);
        signerMapping.put(DocumentCommonModel.Props.SIGNER_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_WORK_ADDRESS.getLocalName(), UserContactMappingCode.ADDRESS);
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        signerMapping.put(DocumentDynamicModel.Props.SIGNER_PHONE.getLocalName(), UserContactMappingCode.PHONE);
        mappings.add(signerMapping);
        userContactMappingService.registerMappingDependency(DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), DocumentCommonModel.Props.SIGNER_NAME.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), DocumentCommonModel.Props.SIGNER_NAME.getLocalName());

        Map<String, UserContactMappingCode> senderMapping = new HashMap<String, UserContactMappingCode>();
        senderMapping.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.getLocalName(), UserContactMappingCode.NAME);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_PERSON_NAME.getLocalName(), null);
        senderMapping.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_FAX.getLocalName(), UserContactMappingCode.FAX);
        mappings.add(senderMapping);
        documentAdminService.registerGroupShowShowInTwoColumns(senderMapping.keySet());

        Map<String, UserContactMappingCode> userMapping = new HashMap<String, UserContactMappingCode>();
        userMapping.put(DocumentDynamicModel.Props.USER_NAME.getLocalName(), UserContactMappingCode.NAME);
        userMapping.put(DocumentDynamicModel.Props.USER_SERVICE_RANK.getLocalName(), UserContactMappingCode.SERVICE_RANK);
        userMapping.put(DocumentDynamicModel.Props.USER_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
        userMapping.put(DocumentDynamicModel.Props.USER_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
        userMapping.put(DocumentDynamicModel.Props.USER_WORK_ADDRESS.getLocalName(), UserContactMappingCode.ADDRESS);
        userMapping.put(DocumentDynamicModel.Props.USER_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        userMapping.put(DocumentDynamicModel.Props.USER_PHONE.getLocalName(), UserContactMappingCode.PHONE);
        userMapping.put(DocumentDynamicModel.Props.USER_ID.getLocalName(), UserContactMappingCode.CODE);
        mappings.add(userMapping);
        documentAdminService.registerGroupShowShowInTwoColumns(userMapping.keySet());

        Map<String, UserContactMappingCode> firstPartyContactPersonMapping = new HashMap<String, UserContactMappingCode>();
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_NAME.getLocalName(), UserContactMappingCode.NAME);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_SERVICE_RANK.getLocalName(), UserContactMappingCode.SERVICE_RANK);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_JOB_TITLE.getLocalName(), UserContactMappingCode.JOB_TITLE);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_ORG_STRUCT_UNIT.getLocalName(), UserContactMappingCode.ORG_STRUCT_UNIT);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_WORK_ADDRESS.getLocalName(), UserContactMappingCode.ADDRESS);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_PHONE.getLocalName(), UserContactMappingCode.PHONE);
        firstPartyContactPersonMapping.put(DocumentDynamicModel.Props.FIRST_PARTY_CONTACT_PERSON_ID.getLocalName(), UserContactMappingCode.CODE);
        mappings.add(firstPartyContactPersonMapping);
        documentAdminService.registerGroupShowShowInTwoColumns(firstPartyContactPersonMapping.keySet());

        Map<String, UserContactMappingCode> contactMapping = new HashMap<String, UserContactMappingCode>();
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_NAME.getLocalName(), UserContactMappingCode.NAME);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_ADDRESS.getLocalName(), UserContactMappingCode.ADDRESS);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_PHONE.getLocalName(), UserContactMappingCode.PHONE);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_FAX_NUMBER.getLocalName(), UserContactMappingCode.FAX);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_WEB_PAGE.getLocalName(), UserContactMappingCode.WEBSITE);
        contactMapping.put(DocumentDynamicModel.Props.CONTACT_REG_NUMBER.getLocalName(), UserContactMappingCode.CODE);
        mappings.add(contactMapping);
        documentAdminService.registerGroupShowShowInTwoColumns(contactMapping.keySet());

        Map<String, UserContactMappingCode> partyMapping = new HashMap<String, UserContactMappingCode>();
        partyMapping.put(DocumentSpecificModel.Props.PARTY_NAME.getLocalName(), UserContactMappingCode.NAME);
        partyMapping.put(DocumentSpecificModel.Props.PARTY_SIGNER.getLocalName(), null);
        partyMapping.put(DocumentSpecificModel.Props.PARTY_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        partyMapping.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON.getLocalName(), null);
        mappings.add(partyMapping);
        documentAdminService.registerGroupShowShowInTwoColumns(partyMapping.keySet());

        Set<String> fields = new HashSet<String>();
        for (Map<String, UserContactMappingCode> mapping : mappings) {
            userContactMappingService.registerOriginalFieldIdsMapping(mapping);
            fields.addAll(mapping.keySet());
        }
        // originalFieldIds = new String[] { "ownerName", "signerName", "senderName", "userName", "contactName" };
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

        FieldGroup group = (FieldGroup) field.getParent();
        Pair<Field, Integer> primaryFieldAndIndex = getPrimaryFieldAndIndex(group);
        int fieldIndex = -1;
        int i = 0;
        for (Field child : group.getFields()) {
            if (child == field) {
                fieldIndex = i;
                break;
            }
            i++;
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();

        // TODO Alar: investigate with Kaarel, why vertical-align: middle doesn't work in some cases
        // TODO Alar: investigate with Kaarel, should editable=false fields have width: 241px ? and display: inline-block ?
        // TODO Alar: add to styleClass value, not overwrite?
        Field primaryField = primaryFieldAndIndex.getFirst();
        Integer primaryFieldIndex = primaryFieldAndIndex.getSecond();
        if (group.isShowInTwoColumns()) {
            item.setStyleClass("expand19-200 medium");
            if (fieldIndex % 2 == 0 && group.getFields().size() >= fieldIndex + 2) {
                // If it is the first column and not on the last row without the second column
                item.setDisplayInline();
                if (field != primaryField && primaryFieldIndex % 2 == 0) {
                    // If it is a regular text field (without search icon) in the first column
                    // And the primary field (with search icon) is in the first column
                    // Then set margin-right
                    item.setStyleClass("expand19-200 medium icon-space");
                }
            }
        }

        // TODO Alar: forbid adding multiple contractParties groups to document in DocTypeDetailsDialog search!!
        // OR
        // TODO Alar: change SubPropsheetItem, so that assocName wouldn't be assocTypeQName, but assocQName - that way multiple different subpropsheetitems can be used
        if (group.getName().equals(SystematicFieldGroupNames.CONTRACT_PARTIES)) {
            item.setBelongsToSubPropertySheetId(primaryField.getFieldId());
            if (field == primaryField) {
                ItemConfigVO subPropSheetItem = new ItemConfigVO(RepoUtil.createTransientProp(primaryField.getFieldId()).toPrefixString(namespaceService));
                subPropSheetItem.setConfigItemType(ConfigItemType.SUB_PROPERTY_SHEET);
                subPropSheetItem.setSubPropertySheetId(primaryField.getFieldId());
                subPropSheetItem.setAssocBrand("children");
                subPropSheetItem.setAssocName(DocumentCommonModel.Types.METADATA_CONTAINER.toPrefixString(namespaceService));
                subPropSheetItem.setActionsGroupId("document_contractMvParty");
                subPropSheetItem.setTitleLabelId("document_contractMv_partyBlock_title");
                // TODO
                generatorResults.addItem(subPropSheetItem);
            }
        }

        // Only generate a component for the USER/CONTACT/USERS_CONTACT field of this group; for other fields generate regular component
        if (field != primaryField) {
            return;
        }

        // All attributes are set by UserContactGenerator
        // And we overwrite some attributes
        item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}");

        // And we set our own attributes
        String stateHolderKey = field.getFieldId();
        item.setSetterCallback(getBindingName("setData", stateHolderKey));
        item.setSetterCallbackTakesNode(true);
        item.setAjaxParentLevel(1);

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(field.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMappingOrNull(field);
        Assert.notNull(mapping);
        generatorResults.addStateHolder(stateHolderKey, new UserContactRelatedGroupState(mapping));

        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.OWNER_NAME.getLocalName())) {
            item.setEditable(false);
            if (field.getFieldId().equals(field.getOriginalFieldId())) {
                item.setComponentGenerator("UserSearchGenerator");
                item.setUsernameProp(DocumentCommonModel.Props.OWNER_ID.toPrefixString(namespaceService));
            }
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.SIGNER_NAME.getLocalName())) {
            item.setEditable(false);
            if (field.getFieldId().equals(field.getOriginalFieldId())) {
                item.setComponentGenerator("UserSearchGenerator");
                item.setUsernameProp(DocumentDynamicModel.Props.SIGNER_ID.toPrefixString(namespaceService));
            }
        }
        // TODO ^^^ SUBSTITUTE_ID

        // TODO in view mode, use code from MetadataBlockBean ("owner")
    }

    public static Pair<Field, Integer> getPrimaryFieldAndIndex(FieldGroup group) {
        // Ensure that exactly one USER/CONTACT/USER_CONTACT field is in this group;
        // and all other fields /*that have mapping*/ are TEXT_FIELD
        Field primaryField = null;
        int primaryFieldIndex = -1;
        int i = 0;
        for (Field child : group.getFields()) {
            if (Arrays.asList(FieldType.USER, FieldType.CONTACT, FieldType.USER_CONTACT).contains(child.getFieldTypeEnum())) {
                Assert.isNull(primaryField);
                primaryField = child;
                primaryFieldIndex = i;
            } else {
                // if (mapping.get(child.getQName()) != null) {
                Assert.isTrue(child.getFieldTypeEnum() == FieldType.TEXT_FIELD);
                // }
            }
            i++;
        }
        Assert.notNull(primaryField);
        Pair<Field, Integer> primaryFieldAndIndex = Pair.newInstance(primaryField, primaryFieldIndex);
        return primaryFieldAndIndex;
    }

    // ===============================================================================================================================

    public static class UserContactRelatedGroupState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final Map<QName, UserContactMappingCode> mapping;

        public UserContactRelatedGroupState(Map<QName, UserContactMappingCode> mapping) {
            this.mapping = mapping;
        }

        public void setData(String result, Node node) {
            // XXX Alar inconvenient
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            getUserContactMappingService().setMappedValues(props, mapping, new NodeRef(result), false);
            for (Entry<QName, Serializable> entry : props.entrySet()) {
                node.getProperties().put(entry.getKey().toString(), entry.getValue());
            }
            return;
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

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }
    // END: setters

}
