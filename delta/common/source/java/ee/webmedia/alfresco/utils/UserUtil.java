package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithSpace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.ibm.icu.util.StringTokenizer;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.substitute.model.Substitute;

public class UserUtil {

    public static String getInitials(String fullName) {
        if (StringUtils.isBlank(fullName)) {
            return "";
        }
        List<String> initials = new ArrayList<String>();
        for (String part : fullName.split("\\s")) {
            if (part.length() > 0) {
                initials.add(part.substring(0, 1) + ".");
            }
        }

        return StringUtils.join(initials, " ");
    }

    public static String getPersonFullName1(Map<QName, Serializable> props) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        String firstName = (String) props.get(ContentModel.PROP_FIRSTNAME);
        String lastName = (String) props.get(ContentModel.PROP_LASTNAME);
        return getPersonFullName(userName, firstName, lastName, false);
    }

    public static String getPersonFullName2(Map<String, Object> props, boolean showSubstituteInfo) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        String firstName = (String) props.get(ContentModel.PROP_FIRSTNAME);
        String lastName = (String) props.get(ContentModel.PROP_LASTNAME);
        return getPersonFullName(userName, firstName, lastName, showSubstituteInfo);
    }

    public static String getPersonFullName(String firstName, String lastName) {
        if (StringUtils.isBlank(firstName)) {
            return lastName;
        } else if (StringUtils.isBlank(lastName)) {
            return firstName;
        }
        return StringUtils.strip(firstName) + " " + StringUtils.strip(lastName);
    }

    public static String getPersonFullName(String userName, String firstName, String lastName, boolean showSubstituteInfo) {
        String fullName = getPersonFullName(firstName, lastName);
        if (StringUtils.isBlank(fullName)) {
            fullName = userName;
        }
        if (showSubstituteInfo) {
            fullName = joinStringAndStringWithSpace(fullName, getSubstitute(userName));
        }
        return fullName;
    }

    public static String getPersonFullNameWithUnitName(Map<QName, Serializable> props, String unitName) {
        String fullName = UserUtil.getPersonFullName1(props);
        if (StringUtils.isNotBlank(unitName)) {
            fullName += " (" + StringUtils.strip(unitName) + ")";
        }
        return fullName;
    }

    public static String getPersonFullNameWithUnitName(Map<String, Object> props, boolean showSubstitutionInfo) {
        String fullName = getPersonFullName2(props, false);
        String unitName = (String) props.get(OrganizationStructureService.UNIT_NAME_PROP);
        if (StringUtils.isNotBlank(unitName)) {
            fullName += " (" + StringUtils.strip(unitName) + ")";
        }
        if (showSubstitutionInfo) {
            return joinStringAndStringWithSpace(fullName, getSubstitute((String) props.get(ContentModel.PROP_USERNAME)));
        }
        return fullName;
    }

    public static String getUserFullNameAndId(Map<QName, Serializable> props) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        return getPersonFullName1(props) + " (" + userName + ")";
    }

    // TODO refactor - util should not use service
    public static String getSubstitute(String username) {
        return getSubstitute(BeanHelper.getUserService().getPerson(username));
    }

    // TODO refactor - util should not use service
    private static String getSubstitute(NodeRef nodeRef) {
        if (nodeRef == null) {
            return null;
        }
        List<Substitute> substitutes = BeanHelper.getSubstituteService().getSubstitutes(nodeRef);
        for (Substitute substitute : substitutes) {
            if (substitute.isActive()) {
                return MessageUtil.getMessage("user_away_has_substitute", substitute.getSubstitutionStartDateFormatted(), substitute.getSubstitutionEndDateFormatted(),
                        substitute.getSubstituteName());
            }
        }
        return null;
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
            authMap.put("structUnitBased", authorityService.getAuthorityZones(authority).contains(OrganizationStructureService.STRUCT_UNIT_BASED) ? "true" : "false");

            groups.add(authMap);
        }

        return groups;
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

    public static String getUserDisplayUnit(Map<String, Object> properties) {
        @SuppressWarnings("unchecked")
        String organizationPath = getDisplayUnit((List<String>) properties.get(ContentModel.PROP_ORGANIZATION_PATH));
        if (StringUtils.isNotBlank(organizationPath)) {
            return organizationPath;
        }
        return (String) properties.get(OrganizationStructureService.UNIT_NAME_PROP);
    }

    public static String getDisplayUnit(Iterable<String> propValue) {
        if (propValue == null) {
            return "";
        }
        String organizationPath = "";
        for (String path : propValue) {
            if (StringUtils.isNotBlank(path)) {
                String notEmptyPath = path.trim();
                if (organizationPath.length() < notEmptyPath.length()) {
                    organizationPath = notEmptyPath;
                }
            }
        }
        return organizationPath;
    }

    public static int getLongestValueIndex(List<String> organizationPaths) {
        if (organizationPaths == null) {
            return -1;
        }
        String organizationPath = "";
        int longestIndex = 0;
        for (int index = 0; index < organizationPaths.size(); index++) {
            String path = organizationPaths.get(index);
            if (StringUtils.isNotBlank(path)) {
                String notEmptyPath = path.trim();
                if (organizationPath.length() < notEmptyPath.length()) {
                    longestIndex = index;
                }
            }
        }
        return longestIndex;
    }

    public static List<String> formatYksusRadaToOrganizationPath(String yksusRada) {
        if (StringUtils.isBlank(yksusRada)) {
            return null;
        }
        String organizationPath = yksusRada;
        String separator = "/";
        int firstIndexOfSep = organizationPath.indexOf(separator);
        if (firstIndexOfSep != -1) {
            organizationPath = organizationPath.substring(firstIndexOfSep + (firstIndexOfSep == organizationPath.length() - 1 ? 0 : 1));
        }
        List<String> organizationPaths = getPathHierarchy(organizationPath, separator);
        return organizationPaths;
    }

    public static List<String> getPathHierarchy(String organizationPath, String separator) {
        List<String> organizationPaths = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(organizationPath, separator);
        String lastPath = "";
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (StringUtils.isNotBlank(token)) {
                if (lastPath.length() > 0) {
                    lastPath += ", ";
                }
                lastPath += token;
                organizationPaths.add(lastPath);
            }
        }
        return organizationPaths;
    }
}
