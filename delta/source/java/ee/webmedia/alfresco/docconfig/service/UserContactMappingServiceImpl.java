package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;

public class UserContactMappingServiceImpl implements UserContactMappingService {

    private final Map<String, Map<String, UserContactMappingCode>> mappings = new HashMap<String, Map<String, UserContactMappingCode>>();

    private static final Map<String, String> mappingDependencies = new HashMap<String, String>();
    static {
        // TODO add by registering

        // hidden fields can only be used in "RelatedGroup" generator, not in "Table" generator
        mappingDependencies.put(DocumentCommonModel.Props.OWNER_NAME.getLocalName(), DocumentCommonModel.Props.OWNER_ID.getLocalName());
        mappingDependencies.put(DocumentCommonModel.Props.SIGNER_NAME.getLocalName(), DocumentDynamicModel.Props.SIGNER_ID.getLocalName());
        mappingDependencies.put(DocumentDynamicModel.Props.SUBSTITUTE_NAME.getLocalName(), DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName());
    }

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;

    @Override
    public void registerOriginalFieldIdsMapping(Map<String, UserContactMappingCode> mapping) {
        mapping = Collections.unmodifiableMap(mapping);
        for (Entry<String, UserContactMappingCode> entry : mapping.entrySet()) {
            Assert.notNull(entry.getKey());
            Assert.isTrue(!mappings.containsKey(entry.getKey()));
            mappings.put(entry.getKey(), mapping);
        }
    }

    @Override
    public Map<QName, UserContactMappingCode> getFieldIdsMapping(Field field) {
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            return Collections.singletonMap(field.getQName(), UserContactMappingCode.NAME);
        }
        Map<String, UserContactMappingCode> originalFieldIdsMapping = mappings.get(field.getOriginalFieldId());
        if (originalFieldIdsMapping == null) {
            return Collections.singletonMap(field.getQName(), UserContactMappingCode.NAME);
        }
        Map<QName, UserContactMappingCode> fieldIdsMapping = new HashMap<QName, UserContactMappingCode>();
        FieldGroup group = (FieldGroup) field.getParent();
        for (Field child : group.getFields()) {
            UserContactMappingCode mappingCode = originalFieldIdsMapping.get(child.getOriginalFieldId());
            if (mappingCode == null) {
                continue;
            }
            fieldIdsMapping.put(child.getQName(), mappingCode);
        }

        if (group.isSystematic()) {
            for (Entry<String, String> entry : mappingDependencies.entrySet()) {
                Field foundField = group.getFieldById(entry.getKey());
                if (foundField != null && foundField.getFieldId().equals(foundField.getOriginalFieldId())) {
                    UserContactMappingCode mappingCode = originalFieldIdsMapping.get(entry.getValue());
                    Assert.notNull(mappingCode);
                    fieldIdsMapping.put(Field.getQName(entry.getValue()), mappingCode);
                }
            }
        }

        return fieldIdsMapping;
    }

    @Override
    public String getMappedNameValue(NodeRef userOrContactRef) {
        List<String> values = getMappedValues(Collections.singletonList(UserContactMappingCode.NAME), userOrContactRef);
        return values == null ? null : values.get(0);
    }

    @Override
    public void setMappedValues(Map<String, Object> props, Map<QName, UserContactMappingCode> fieldIdsMapping, NodeRef userOrContactRef, boolean multiValued) {
        List<QName> propNames = new ArrayList<QName>(fieldIdsMapping.size());
        List<UserContactMappingCode> mappingCodes = new ArrayList<UserContactMappingCode>(fieldIdsMapping.size());
        for (Entry<QName, UserContactMappingCode> entry : fieldIdsMapping.entrySet()) {
            propNames.add(entry.getKey());
            mappingCodes.add(entry.getValue());
        }
        List<String> values = getMappedValues(mappingCodes, userOrContactRef);
        for (int i = 0; i < fieldIdsMapping.size(); i++) {
            QName propName = propNames.get(i);
            Serializable value;
            if (values == null) {
                value = null;
            } else {
                value = values.get(i);
                if (multiValued) {
                    ArrayList<Serializable> list = new ArrayList<Serializable>();
                    list.add(value);
                    value = list;
                }
            }
            props.put(propName.toString(), value);
        }
    }

    @Override
    public List<String> getMappedValues(List<UserContactMappingCode> mappingCodes, NodeRef userOrContactRef) {
        if (!nodeService.exists(userOrContactRef)) {
            return null;
        }
        QName type = nodeService.getType(userOrContactRef);
        Map<QName, Serializable> props = nodeService.getProperties(userOrContactRef);
        ArrayList<String> values = new ArrayList<String>(mappingCodes.size());
        for (UserContactMappingCode mappingCode : mappingCodes) {
            values.add(getMappedValue(type, props, mappingCode));
        }
        return values;
    }

    private String getMappedValue(QName type, Map<QName, Serializable> props, UserContactMappingCode mappingCode) {
        if (mappingCode == null) {
            return null;
        }
        boolean isPerson = ContentModel.TYPE_PERSON.equals(type);
        boolean isOrganization = AddressbookModel.Types.ORGANIZATION.equals(type);
        switch (mappingCode) {
        case NAME:
            if (isPerson) {
                return UserUtil.getPersonFullName1(props);
            } else if (isOrganization) {
                return getProp(props, AddressbookModel.Props.ORGANIZATION_NAME);
            } else {
                String firstName = (String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME);
                String lastName = (String) props.get(AddressbookModel.Props.PERSON_LAST_NAME);
                return UserUtil.getPersonFullName(firstName, lastName);
            }
        case CODE:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_USERNAME);
            } else if (isOrganization) {
                return getProp(props, AddressbookModel.Props.ORGANIZATION_CODE);
            } else {
                return null;
            }
        case SERVICE_RANK:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_SERVICE_RANK);
            }
            return null;
        case JOB_TITLE:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_JOBTITLE);
            }
            return null;
        case ORG_STRUCT_UNIT:
            if (isPerson) {
                String orgPath = getProp(props, ContentModel.PROP_ORGANIZATION_PATH);
                if (StringUtils.isNotBlank(orgPath)) {
                    return orgPath;
                }
                String orgId = getProp(props, ContentModel.PROP_ORGID);
                return BeanHelper.getOrganizationStructureService().getOrganizationStructure(orgId);
            }
            return null;
        case ADDRESS:
            if (isPerson) {
                return getPropsJoinedWithComma(props,
                        ContentModel.PROP_STREET_HOUSE,
                        ContentModel.PROP_VILLAGE,
                        ContentModel.PROP_MUNICIPALITY,
                        ContentModel.PROP_POSTAL_CODE,
                        ContentModel.PROP_COUNTY);
            } else {
                boolean isPrivPerson = AddressbookModel.Types.PRIV_PERSON.equals(type);
                if (isOrganization || isPrivPerson) {
                    return getPropsJoinedWithComma(props,
                            AddressbookModel.Props.ADDRESS1,
                            AddressbookModel.Props.ADDRESS2,
                            AddressbookModel.Props.POSTAL,
                            AddressbookModel.Props.CITY);
                } else {
                    return null;
                }
            }
        case EMAIL:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_EMAIL);
            }
            return getProp(props, AddressbookModel.Props.EMAIL);
        case PHONE:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_TELEPHONE);
            } else if (isOrganization) {
                return getProp(props, AddressbookModel.Props.PHONE);
            } else {
                return getPropsJoinedWithComma(props,
                        AddressbookModel.Props.PHONE,
                        AddressbookModel.Props.MOBILE_PHONE);
            }
        case FAX:
            return getProp(props, AddressbookModel.Props.FAX);
        case WEBSITE:
            return getProp(props, AddressbookModel.Props.WEBSITE);
        case STREET_HOUSE:
            if (isPerson) {
                return getProp(props, ContentModel.PROP_STREET_HOUSE);
            }
            return getProp(props, AddressbookModel.Props.ADDRESS1);
        case POSTAL_CITY:
            if (isPerson) {
                return getPropsJoinedWithComma(props,
                        ContentModel.PROP_VILLAGE,
                        ContentModel.PROP_MUNICIPALITY,
                        ContentModel.PROP_POSTAL_CODE,
                        ContentModel.PROP_COUNTY);
            }
            return getPropsJoinedWithComma(props,
                    AddressbookModel.Props.ADDRESS2,
                    AddressbookModel.Props.POSTAL,
                    AddressbookModel.Props.CITY);
        case SAP_ACCOUNT:
            return getProp(props, AddressbookModel.Props.SAP_ACCOUNT);
        }
        throw new RuntimeException();
    }

    private String getProp(Map<QName, Serializable> props, QName propName) {
        return (String) props.get(propName);
    }

    private String getPropsJoinedWithComma(Map<QName, Serializable> props, QName... propNames) {
        List<String> values = new ArrayList<String>(propNames.length);
        for (QName propName : propNames) {
            values.add(getProp(props, propName));
        }
        return TextUtil.joinNonBlankStringsWithComma(values);
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
