package ee.webmedia.alfresco.dvk.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer;

public class DecContainerHandler {

    private static final String LIST_METHOD_SUFFIX = "List";
    private static final List<List<String>> ALLOWED_KEYS = new ArrayList<List<String>>();
    private static final Map<String, List<String>> USER_ALLOWED_KEYS = new CaseInsensitiveMap<String, List<String>>();
    private static final List<List<String>> OUTGOING_USER_DISALLOWED_KEYS = new ArrayList<List<String>>();
    private static final Map<String, List<String>> OUTGOING_FORBIDDEN_KEYS = new CaseInsensitiveMap<String, List<String>>();
    private static final List<FieldType> STRING_COMPATIBLE = new ArrayList<FieldType>();
    private static final List<FieldType> STRING_FIELDS = new ArrayList<FieldType>();
    private static final List<String> BOOLEAN_KEYS = new ArrayList<String>(Arrays.asList("Adit"));
    private static final List<String> INTEGER_KEYS = new ArrayList<String>(Arrays.asList("PostalCode"));
    private static final List<String> DATE_TIME_KEYS = new ArrayList<String>(Arrays.asList("InitiatorRecordDate", "RecordDateRegistered", "ReplyDueDate", "RestrictionBeginDate",
            "RestrictionEndDate", "RestrictionInvalidSince", "SignatureVerificationDate"));
    private static final String LIST_PREFIX = "#list#";
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String NEW_OBJECT_PREFIX = "addNew";
    private static final Pattern POSTAL_CODE_TYPE_PATTERN = Pattern.compile("[0-9]{5}");

    static {

        ALLOWED_KEYS.add(Arrays.asList("Initiator", "InitiatorRecordOriginalIdentifier"));
        ALLOWED_KEYS.add(Arrays.asList("Initiator", "InitiatorRecordDate"));
        addOrganizationHandlers(Arrays.asList("Initiator"));
        addPersonHandlers(Arrays.asList("Initiator"));
        addContactDataHandlers(Arrays.asList("Initiator"));
        addPostalAddressHandlers(Arrays.asList("Initiator", "ContactData"));

        addOrganizationHandlers(Arrays.asList("RecordCreator"));
        addPersonHandlers(Arrays.asList("RecordCreator"));
        addContactDataHandlers(Arrays.asList("RecordCreator"));
        addPostalAddressHandlers(Arrays.asList("RecordCreator", "ContactData"));

        addOrganizationHandlers(Arrays.asList("RecordSenderToDec"));
        addPersonHandlers(Arrays.asList("RecordSenderToDec"));
        addContactDataHandlers(Arrays.asList("RecordSenderToDec"));
        addPostalAddressHandlers(Arrays.asList("RecordSenderToDec", "ContactData"));

        ALLOWED_KEYS.add(Arrays.asList(LIST_PREFIX + "Recipient", "RecipientRecordGuid"));
        ALLOWED_KEYS.add(Arrays.asList(LIST_PREFIX + "Recipient", "RecipientRecordOriginalIdentifier"));
        ALLOWED_KEYS.add(Arrays.asList(LIST_PREFIX + "Recipient", "MessageForRecipient"));
        addOrganizationHandlers(Arrays.asList(LIST_PREFIX + "Recipient"));
        addPersonHandlers(Arrays.asList(LIST_PREFIX + "Recipient"));
        addContactDataHandlers(Arrays.asList(LIST_PREFIX + "Recipient"));
        addPostalAddressHandlers(Arrays.asList(LIST_PREFIX + "Recipient", "ContactData"));

        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordGuid"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordType"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordOriginalIdentifier"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordDateRegistered"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordTitle"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordLanguage"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "RecordAbstract"));
        ALLOWED_KEYS.add(Arrays.asList("RecordMetadata", "ReplyDueDate"));

        addDecElementHandlers(Arrays.asList("Transport"), "DecSender");
        addDecElementHandlers(Arrays.asList("Transport"), "DecRecipient");
        addHandlers(Arrays.asList("SignatureMetaData"), Arrays.asList("SignatureType", "Signer", "Verified", "SignatureVerificationDate"));
        addHandlers(Arrays.asList("Access", "AccessRestriction"), Arrays.asList("InformationOwner"));
        addHandlers(Arrays.asList("DecMetaData"), Arrays.asList("DecId", "DecFolder", "DecReceiptDate"));

        STRING_COMPATIBLE.addAll(Arrays.asList(FieldType.TEXT_FIELD, FieldType.COMBOBOX, FieldType.LONG, FieldType.DOUBLE, FieldType.DATE, FieldType.COMBOBOX_EDITABLE, FieldType.USER, FieldType.USERS,
                FieldType.CONTACT, FieldType.USER_CONTACT, FieldType.CONTACTS, FieldType.USERS_CONTACTS, FieldType.COMBOBOX_AND_TEXT, FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE, FieldType.LISTBOX, FieldType.STRUCT_UNIT));
        STRING_FIELDS.addAll(Arrays.asList(FieldType.TEXT_FIELD, FieldType.COMBOBOX, FieldType.COMBOBOX_EDITABLE, FieldType.COMBOBOX_AND_TEXT,
                FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE, FieldType.LISTBOX, FieldType.USER_CONTACT, FieldType.USERS_CONTACTS, FieldType.CONTACT, FieldType.CONTACTS));

        initUserAllowedKeys();

        OUTGOING_USER_DISALLOWED_KEYS.add(Arrays.asList("RecordCreator", "ContactData", "Email"));
        OUTGOING_USER_DISALLOWED_KEYS.add(Arrays.asList("RecordCreator", "Organisation", "Name"));
        OUTGOING_USER_DISALLOWED_KEYS.add(Arrays.asList("RecordSenderToDec", "Organisation", "Name"));
        addAllSubelementsToDisallowedOutgoingKeyList("SignatureMetaData", "Transport", "Access", "DecMetaData", "Recipient");

        initOutgoingUserForbiddenKeys();

    }

    private static void addAllSubelementsToDisallowedOutgoingKeyList(String... keys) {
        for (String key : keys) {
            OUTGOING_USER_DISALLOWED_KEYS.add(Arrays.asList(key));
        }
    }

    private static void addDecElementHandlers(List<String> prefixList, String subElement) {
        List<String> baseList = new ArrayList<String>(prefixList);
        baseList.add(subElement);
        addHandlers(prefixList, Arrays.asList("OrganisationCode", "StructuralUnit", "PersonalIdCode"));
    }

    private static void addOrganizationHandlers(List<String> prefixList) {
        List<String> baseList = new ArrayList<String>(prefixList);
        baseList.add("Organisation");
        addHandlers(baseList, Arrays.asList("Name", "OrganisationCode", "StructuralUnit", "PositionTitle", "Residency"));
    }

    private static void addHandlers(List<String> prefixList, List<String> suffixes) {
        for (String suffix : suffixes) {
            List<String> list = new ArrayList<String>(prefixList);
            list.add(suffix);
            ALLOWED_KEYS.add(list);
        }
    }

    private static void addPersonHandlers(List<String> prefixList) {
        List<String> baseList = new ArrayList<String>(prefixList);
        baseList.add("Person");
        addHandlers(baseList, Arrays.asList("Name", "GivenName", "Surname", "PersonalIdCode", "Residency"));
    }

    private static void addContactDataHandlers(List<String> prefixList) {
        List<String> baseList = new ArrayList<String>(prefixList);
        baseList.add("ContactData");
        addHandlers(baseList, Arrays.asList("Adit", "Phone", "Email", "WebPage", "MessagingAddress"));
    }

    private static void addPostalAddressHandlers(List<String> prefixList) {
        List<String> baseList = new ArrayList<String>(prefixList);
        baseList.add("PostalAddress");
        addHandlers(baseList,
                Arrays.asList("Country", "County", "LocalGovernment", "AdministrativeUnit", "SmallPlace", "LandUnit", "Street", "HouseNumber", "BuildingPartNumber", "PostalCode"));
    }

    private static void initUserAllowedKeys() {
        int listKeyEnd = LIST_PREFIX.length();
        for (List<String> key : ALLOWED_KEYS) {
            USER_ALLOWED_KEYS.put(getKeyAsString(listKeyEnd, key), key);
        }
    }

    private static void initOutgoingUserForbiddenKeys() {
        NavigableMap<String, List<String>> userAllowed = new TreeMap<String, List<String>>(USER_ALLOWED_KEYS);
        for (List<String> keyList : OUTGOING_USER_DISALLOWED_KEYS) {
            if (keyList.size() == 1) {
                String stringKey = StringUtils.lowerCase(getKeyAsString(0, keyList));
                Map<String, List<String>> subMap = userAllowed.subMap(stringKey, stringKey + Character.MAX_VALUE);
                for (List<String> key : subMap.values()) {
                    OUTGOING_FORBIDDEN_KEYS.put(getKeyAsString(LIST_PREFIX.length(), key), key);
                }
            } else {
                OUTGOING_FORBIDDEN_KEYS.put(getKeyAsString(0, keyList), keyList);
            }
        }
    }

    private static String getKeyAsString(int listKeyElementLength, List<String> key) {
        StringBuffer sb = new StringBuffer();
        for (String field : key) {
            field = removeListPrefix(field, listKeyElementLength);
            sb.append("<" + field + ">");
        }
        return sb.toString();
    }

    private static String removeListPrefix(String field, int listKeyStart) {
        if (field.startsWith(LIST_PREFIX)) {
            field = field.substring(listKeyStart);
        }
        return field;
    }

    public static void setValue(String fieldName, String key, DecContainer decContainer, Object value) {
        List<String> methodBaseNames = USER_ALLOWED_KEYS.get(key);

        // Current implementation supports only single entries. Document properties that have multiple values are handled as lists.
        if (value instanceof List) {
            List list = (List) value;
            if (!list.isEmpty()) {
                value = list.get(0);
            } else {
                value = null; // If there are no values, we should avoid possible casting errors.
            }
        }

        validateValue(fieldName, methodBaseNames, value);
        XmlObject parent = getComplexParent(decContainer, methodBaseNames);
        String simpleTypeMethodBaseName = methodBaseNames.get(methodBaseNames.size() - 1);
        Method simpleTypeGetter = getSimpleTypeGetterMethod(parent, simpleTypeMethodBaseName);
        String setterName = SETTER_PREFIX + simpleTypeMethodBaseName;
        try {
            Class<?> getterReturnType = simpleTypeGetter.getReturnType();
            value = checkType(getterReturnType, value);
            Method simpleTypeSetter = parent.getClass().getMethod(setterName, getterReturnType);
            simpleTypeSetter.invoke(parent, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot call method " + setterName + " on object of class " + (parent != null ? parent.getClass() : "null"), e);
        }
    }

    private static void validateValue(String fieldName, List<String> decElements, Object value) {
        String lastElement = decElements.get(decElements.size() - 1);
        if ("PostalCode".equals(lastElement) && !POSTAL_CODE_TYPE_PATTERN.matcher((String) value).matches()) {
            throw new UnableToPerformException("dvk_send_error_invalid_postal_code_type", fieldName);
        }
    }

    public static Object getValue(String key, DecContainer decContainer) {
        return getValue(key, decContainer, 0);
    }

    public static Object getValue(String key, XmlObject decContainerOrSubElement, int depthFromRoot) {
        List<String> methodBaseNames = USER_ALLOWED_KEYS.get(key);

        // If we have already narrowed down on DecContainer structure, then reflect this constraint on method base names also.
        if (depthFromRoot > 0) {
            methodBaseNames = methodBaseNames.subList(depthFromRoot, methodBaseNames.size());
        }

        XmlObject parent = getComplexParent(decContainerOrSubElement, methodBaseNames);
        String simpleTypeMethodBaseName = methodBaseNames.get(methodBaseNames.size() - 1);
        Method simpleTypeGetter = getSimpleTypeGetterMethod(parent, simpleTypeMethodBaseName);
        try {
            return simpleTypeGetter.invoke(parent);
        } catch (Exception e) {
            throw new RuntimeException("Cannot call method " + simpleTypeMethodBaseName + " on object of class " + (parent != null ? parent.getClass() : "null"), e);
        }
    }

    public static Object checkType(Class<?> getterReturnType, Object value) {
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (value != null && !getterReturnType.isAssignableFrom(valueClass)) {
                if (isConvertable(valueClass, getterReturnType)) {
                    return convertObjectType(value, getterReturnType);
                }
                throw new RuntimeException("Cannot convert " + valueClass + " to " + getterReturnType);
            }
        }
        return value;
    }

    public static boolean isOfStringType(FieldType fieldType) {
        Assert.notNull(fieldType, "Field type is needed to check compatibility!");
        return STRING_FIELDS.contains(fieldType);
    }

    public static boolean isValidFieldType(String decElementPath, FieldType fieldTypeEnum) {
        boolean valid = false;
        if (!hasUserKey(decElementPath)) {
            return valid;
        }

        List<String> keys = USER_ALLOWED_KEYS.get(decElementPath);
        String lastElement = keys.get(keys.size() - 1);

        // By default, all field types besides FieldType.COMBOBOX are also supported as string mappings.
        switch (fieldTypeEnum) {
        case CHECKBOX:
            valid = BOOLEAN_KEYS.contains(lastElement);
            break;
        case DATE:
            valid = DATE_TIME_KEYS.contains(lastElement);
            break;
        case TEXT_FIELD:
            valid = !BOOLEAN_KEYS.contains(lastElement) && !DATE_TIME_KEYS.contains(lastElement);
            break;
        case LONG:
            valid = INTEGER_KEYS.contains(lastElement);
            break;
        default:
            valid = !BOOLEAN_KEYS.contains(lastElement) && STRING_COMPATIBLE.contains(fieldTypeEnum); // Incoming element restrictions are applied in FieldDetailsDialog
        }

        return valid;
    }

    public static boolean validateMandatoryKeysPresent(DecContainer container, Map<String, String> usedFieldNameByKey) {
        boolean valid = true;
        // Since there are only three user configurable fields that may be mandatory, perform these checks manually. :)
        DecContainer.Initiator initiator = container.getInitiator();
        if (initiator != null && initiator.getContactData() == null) {
            addFieldError(usedFieldNameByKey, Arrays.asList("Initiator", "ContactData"));
            valid = false;
        }
        if (container.getRecordMetadata().getRecordDateRegistered() == null) {
            addFieldError(usedFieldNameByKey, Arrays.asList("RecordMetaData", "RecordDateRegistered"));
            valid = false;
        }
        if (StringUtils.isBlank(container.getRecordMetadata().getRecordTitle())) {
            addFieldError(usedFieldNameByKey, Arrays.asList("RecordMetaData", "RecordTitle"));
            valid = false;
        }

        return valid;
    }

    private static void addFieldError(Map<String, String> usedFieldNameByKey, List<String> key) {
        String keyAsString = getKeyAsString(LIST_PREFIX.length(), key);
        String fieldName = usedFieldNameByKey.get(keyAsString);
        if (fieldName == null) { // Field is not configured
            MessageUtil.addErrorMessage("dvk_send_error_document_field_configuration_missing", keyAsString);
        } else { // Field value is empty
            MessageUtil.addErrorMessage("dvk_send_error_document_field_value_missing", fieldName);
        }
    }

    private static Object convertObjectType(Object object, Class<?> targetClass) {
        if (object instanceof Boolean && boolean.class.equals(targetClass)) {
            return ((Boolean) object).booleanValue();
        }
        return DefaultTypeConverter.INSTANCE.convert(targetClass, object);
    }

    private static boolean isConvertable(Class<?> fromClass, Class<?> toClass) {
        boolean booleanPrimitiveConverter = (Boolean.class.equals(fromClass) && boolean.class.equals(toClass));
        return booleanPrimitiveConverter || DefaultTypeConverter.INSTANCE.getConverter(fromClass, toClass) != null;
    }

    public static boolean hasUserKey(String key) {
        return USER_ALLOWED_KEYS.containsKey(key);
    }

    public static boolean isDisallowedOutgoingUserElement(String key) {
        return OUTGOING_FORBIDDEN_KEYS.containsKey(key);
    }

    private static Method getSimpleTypeGetterMethod(XmlObject parent, String simpleTypeMethodBaseName) {
        Method simpleTypeGetter = null;
        String getterName = GETTER_PREFIX + simpleTypeMethodBaseName;
        try {
            simpleTypeGetter = parent.getClass().getMethod(getterName);
        } catch (Exception e) {
            throw new RuntimeException("Cannot call method " + getterName + " on object of class " + (parent != null ? parent.getClass() : "null"), e);
        }
        return simpleTypeGetter;
    }

    private static XmlObject getComplexParent(XmlObject decContainer, List<String> methodBaseNames) {
        int complexMethodCount = methodBaseNames.size() - 1;
        int i = 0;
        XmlObject parent = decContainer;
        while (i < complexMethodCount) {
            parent = getComplexChild(parent, methodBaseNames.get(i), true);
            i++;
        }
        return parent;
    }

    private static XmlObject getComplexChild(XmlObject parent, String methodBaseName, boolean create) {
        XmlObject child = null;
        if (parent != null) {
            String newObjectMethodName = NEW_OBJECT_PREFIX + removeListPrefix(methodBaseName, LIST_PREFIX.length());
            if (methodBaseName.startsWith(LIST_PREFIX)) {
                List childList = null;
                String listMethodName = GETTER_PREFIX + removeListPrefix(methodBaseName, LIST_PREFIX.length()) + LIST_METHOD_SUFFIX;
                try {
                    Method listMethod = parent.getClass().getMethod(listMethodName);
                    childList = (List) listMethod.invoke(parent);
                } catch (Exception e) {
                    throw new RuntimeException("Error invoking method " + listMethodName + " on object " + parent);
                }
                if (childList == null || childList.isEmpty()) {
                    if (create) {
                        try {
                            Method newObjectMethod = parent.getClass().getMethod(newObjectMethodName);
                            child = (XmlObject) newObjectMethod.invoke(parent);
                        } catch (Exception e) {
                            throw new RuntimeException("Error invoking method " + newObjectMethodName + " on object " + parent);
                        }
                    }
                } else {
                    child = (XmlObject) childList.get(0);
                }
            } else {
                String getterName = GETTER_PREFIX + methodBaseName;
                try {
                    Method getter = parent.getClass().getMethod(getterName);
                    child = (XmlObject) getter.invoke(parent);
                } catch (Exception e) {
                    throw new RuntimeException("Error invoking method " + getterName + " on object " + parent);
                }
                if (child == null && create) {
                    try {
                        Method newObjectMethod = parent.getClass().getMethod(newObjectMethodName);
                        child = (XmlObject) newObjectMethod.invoke(parent);
                    } catch (Exception e) {
                        throw new RuntimeException("Error invoking method " + newObjectMethodName + " on object " + parent);
                    }
                }
            }
        }
        return child;
    }
}
