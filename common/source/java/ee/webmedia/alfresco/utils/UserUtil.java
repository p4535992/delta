package ee.webmedia.alfresco.utils;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

public class UserUtil {

    public static String getPersonFullName1(Map<QName, Serializable> props) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        String firstName = (String) props.get(ContentModel.PROP_FIRSTNAME);
        String lastName = (String) props.get(ContentModel.PROP_LASTNAME);
        return getPersonFullName(userName, firstName, lastName);
    }

    public static String getPersonFullName2(Map<String, Object> props) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        String firstName = (String) props.get(ContentModel.PROP_FIRSTNAME);
        String lastName = (String) props.get(ContentModel.PROP_LASTNAME);
        return getPersonFullName(userName, firstName, lastName);
    }

    public static String getPersonFullName(String firstName, String lastName) {
        if (StringUtils.isBlank(firstName)) {
            return lastName;
        } else if (StringUtils.isBlank(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    public static String getPersonFullName(String userName, String firstName, String lastName) {
        String fullName = getPersonFullName(firstName, lastName);
        if (StringUtils.isBlank(fullName)) {
            fullName = userName;
        }
        return fullName;
    }

    public static String getPersonFullNameWithUnitName(Map<QName, Serializable> props, String unitName) {
        String fullName = UserUtil.getPersonFullName1(props);
        if (StringUtils.isNotBlank(unitName)) {
            return fullName + " (" + unitName + ")";
        }
        return fullName;
    }

    public static String getPersonFullNameWithUnitName(Map<String, Object> props) {
        String fullName = getPersonFullName2(props);
        String unitName = (String) props.get(OrganizationStructureService.UNIT_NAME_PROP);
        if (StringUtils.isNotBlank(unitName)) {
            return fullName + " (" + unitName + ")";
        }
        return fullName;
    }

    /**
     * @param personName - name in format "LastName" or "LastName1 {LastName2}*, FirstName1 {FirstName2}*"
     *            or "FirstName1 {FirstName2}* LastName1"
     * @return pair <FirstName1 {FirstName2}*, LastName1 {LastName2}*>; where FirstName may be null
     *         or null if personName is blank
     */
    public static Pair<String, String> splitFirstNameLastName(String personName) {
        Pair<String, String> firstNameLastName = null;
        if (StringUtils.isNotBlank(personName)) {
            String firstName = null;
            String lastName = null;
            personName = personName.trim();
            if (StringUtils.contains(personName, ",")) {
                if (StringUtils.countMatches(personName, ",") == 1) {
                    String[] names = StringUtils.split(personName, ",");
                    firstName = StringUtils.trim(names[1]);
                    lastName = StringUtils.trim(names[0]);
                }
            } else {
                int splitIndex = StringUtils.lastIndexOf(personName, " ");
                if (splitIndex < 0) {
                    lastName = personName;
                } else {
                    firstName = StringUtils.trim(personName.substring(0, splitIndex));
                    lastName = StringUtils.trim(personName.substring(splitIndex));
                }
            }
            firstNameLastName = new Pair<String, String>(firstName, lastName);
        }
        return firstNameLastName;
    }

    public static boolean hasSameName(Pair<String, String> firstNameLastName, Map<QName, Serializable> userProps) {
        if (userProps == null || firstNameLastName == null || StringUtils.isBlank(firstNameLastName.getFirst()) || StringUtils.isBlank(firstNameLastName.getSecond())) {
            return false;
        }
        if (StringUtils.equalsIgnoreCase(firstNameLastName.getFirst(), (String) userProps.get(ContentModel.PROP_FIRSTNAME))
                && StringUtils.equalsIgnoreCase(firstNameLastName.getSecond(), (String) userProps.get(ContentModel.PROP_LASTNAME))) {
            return true;
        }
        return false;
    }
}
