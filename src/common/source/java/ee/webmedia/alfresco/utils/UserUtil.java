package ee.webmedia.alfresco.utils;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
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
}
