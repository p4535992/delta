package ee.webmedia.alfresco.docconfig.generator.systematic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
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
public class UserContactRelatedGroupGenerator extends BaseSystematicFieldGenerator implements FieldGroupGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserContactRelatedGroupGenerator.class);

    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;

    private String[] originalFieldIds;

    @Override
    public void afterPropertiesSet() {
        Set<Map<String, UserContactMappingCode>> mappings = new HashSet<Map<String, UserContactMappingCode>>();

        // We may, but don't have to specify fields that have no mapping, because no behaviour is associated with them

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

        Map<String, UserContactMappingCode> senderMapping = new HashMap<String, UserContactMappingCode>();
        senderMapping.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.getLocalName(), UserContactMappingCode.NAME);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_PERSON_NAME.getLocalName(), null);
        senderMapping.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.getLocalName(), UserContactMappingCode.EMAIL);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_STREET_HOUSE.getLocalName(), UserContactMappingCode.STREET_HOUSE);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_POSTAL_CITY.getLocalName(), UserContactMappingCode.POSTAL_CITY);
        senderMapping.put(DocumentDynamicModel.Props.SENDER_FAX.getLocalName(), UserContactMappingCode.FAX);
        mappings.add(senderMapping);

        Map<String, UserContactMappingCode> userMapping = new HashMap<String, UserContactMappingCode>();
        userMapping.put("userName", UserContactMappingCode.NAME);
        userMapping.put("userServiceRank", UserContactMappingCode.SERVICE_RANK);
        userMapping.put("userJobTitle", UserContactMappingCode.JOB_TITLE);
        userMapping.put("userOrgStructUnit", UserContactMappingCode.ORG_STRUCT_UNIT);
        userMapping.put("userWorkAddress", UserContactMappingCode.ADDRESS);
        userMapping.put("userEmail", UserContactMappingCode.EMAIL);
        userMapping.put("userPhone", UserContactMappingCode.PHONE);
        userMapping.put("userId", UserContactMappingCode.CODE);
        mappings.add(userMapping);

        Map<String, UserContactMappingCode> contactMapping = new HashMap<String, UserContactMappingCode>();
        contactMapping.put("contactName", UserContactMappingCode.NAME);
        contactMapping.put("contactAddress", UserContactMappingCode.ADDRESS);
        contactMapping.put("contactEmail", UserContactMappingCode.EMAIL);
        contactMapping.put("contactPhone", UserContactMappingCode.PHONE);
        contactMapping.put("contactFaxNumber", UserContactMappingCode.FAX);
        contactMapping.put("contactWebPage", UserContactMappingCode.WEBSITE);
        contactMapping.put("contactRegNumber", UserContactMappingCode.CODE);
        mappings.add(contactMapping);

        Set<String> fields = new HashSet<String>();
        for (Map<String, UserContactMappingCode> mapping : mappings) {
            userContactMappingService.registerOriginalFieldIdsMapping(mapping);
            fields.addAll(mapping.keySet());
        }
        //originalFieldIds = new String[] { "ownerName", "signerName", "senderName", "userName", "contactName" };
        originalFieldIds = fields.toArray(new String[fields.size()]);

        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return originalFieldIds;
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMapping(field);

        // Ensure that exactly one USER/CONTACT/USER_CONTACT field is in this group;
        // and all other fields that have mapping are TEXT_FIELD
        FieldGroup group = (FieldGroup) field.getParent();
        Field foundField = null;
        for (Field child : group.getFields()) {
            if (Arrays.asList(FieldType.USER, FieldType.CONTACT, FieldType.USER_CONTACT).contains(child.getFieldTypeEnum())) {
                Assert.isNull(foundField);
                foundField = child;
            } else {
                if (mapping.get(child.getQName()) != null) {
                    Assert.isTrue(child.getFieldTypeEnum() == FieldType.TEXT_FIELD);
                }
            }
        }
        Assert.notNull(foundField);
        // Only generate a component for the USER/CONTACT/USERS_CONTACT field of this group; for other fields generate regular component
        if (field != foundField) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        // All attributes are set by UserContactGenerator

        // And we overwrite some attributes
        item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}");

        // And we set our own attributes
        String stateHolderKey = field.getFieldId();
        item.setSetterCallback(getBindingName("setData", stateHolderKey));
        item.setAjaxParentLevel(1);

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(field.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new UserContactRelatedGroupState(mapping));

        if (field.getFieldId().equals(DocumentCommonModel.Props.OWNER_NAME.getLocalName())) {
            item.setComponentGenerator("UserSearchGenerator");
            item.setUsernameProp(DocumentCommonModel.Props.OWNER_ID.toPrefixString(BeanHelper.getNamespaceService()));
            item.setEditable(false);
        } else if (field.getFieldId().equals(DocumentCommonModel.Props.SIGNER_NAME.getLocalName())) {
            item.setComponentGenerator("UserSearchGenerator");
            item.setUsernameProp(DocumentDynamicModel.Props.SIGNER_ID.toPrefixString(BeanHelper.getNamespaceService()));
            item.setEditable(false);
        }
        // TODO ^^^ SUBSTITUTE_ID

        // TODO in view mode, use code from MetadataBlockBean ("owner")
    }

    @Override
    public void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults) {
        // Do nothing
    }

    // ===============================================================================================================================

    public static class UserContactRelatedGroupState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final Map<QName, UserContactMappingCode> mapping;

        public UserContactRelatedGroupState(Map<QName, UserContactMappingCode> mapping) {
            this.mapping = mapping;
        }

        public void setData(String result) {
            Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
            BeanHelper.getUserContactMappingService().setMappedValues(docProps, mapping, new NodeRef(result), false);
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
