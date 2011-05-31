package ee.webmedia.alfresco.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
                    firstName = names[1];
                    lastName = names[0];
                }
            } else {
                int splitIndex = StringUtils.lastIndexOf(personName, " ");
                if (splitIndex < 0) {
                    lastName = personName;
                } else {
                    firstName = personName.substring(0, splitIndex - 1).trim();
                    lastName = personName.substring(splitIndex).trim();
                }
                firstNameLastName = new Pair<String, String>(firstName, lastName);
            }
        }
        return firstNameLastName;
    }

    public static List<Map<String, String>> getGroupsFromAuthorities(AuthorityService authorityService, Set<String> authorities) {
        Assert.notNull(authorityService, "AuhtorityService cannot be null!");
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }

        List<Map<String, String>> groups = new ArrayList<Map<String, String>>(authorities.size());
        for (String authority : authorities) {
            Map<String, String> authMap = new HashMap<String, String>(5, 1.0f);

            String name = authorityService.getShortName(authority);
            authMap.put("name", name);
            authMap.put("id", authority);
            authMap.put("group", authority);
            authMap.put("groupName", name);
            authMap.put("displayName", authorityService.getAuthorityDisplayName(authority));

            groups.add(authMap);
        }

        return groups;
    }

}
