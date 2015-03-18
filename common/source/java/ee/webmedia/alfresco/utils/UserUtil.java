package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.utils.TextUtil.joinStringAndStringWithComma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.ibm.icu.util.StringTokenizer;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserService;

public class UserUtil {

    public static String getInitials(String fullName) {
        if (StringUtils.isBlank(fullName)) {
            return "";
        }
        List<String> initials = new ArrayList<String>();
        for (String part : fullName.split("\\s")) {
            if (part.length() > 0) {
                initials.add(part.substring(0, 1));
            }
        }

        String result = StringUtils.join(initials, ". ");
        if (StringUtils.isNotBlank(result)) {
            result += ".";
        }
        return result;
    }

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
        return StringUtils.strip(firstName) + " " + StringUtils.strip(lastName);
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
            fullName += " (" + StringUtils.strip(unitName) + ")";
        }
        return fullName;
    }

    public static String getPersonFullNameWithUnitNameAndJobTitle(Map<String, Object> props) {
        String fullName = getPersonFullName2(props);
        String bracketContent = joinStringAndStringWithComma(
                StringUtils.strip((String) props.get(ContentModel.PROP_JOBTITLE)),
                getUserDisplayUnit(props)
                );
        if (StringUtils.isNotBlank(bracketContent)) {
            fullName += " (" + bracketContent + ")";
        }
        return fullName;
    }

    public static String getUserFullNameAndId(Map<QName, Serializable> props) {
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        return getPersonFullName1(props) + " (" + userName + ")";
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

    public static List<Map<String, String>> getGroupsFromAuthorities(AuthorityService authorityService, UserService userService, Collection<String> authorities) {
        Assert.notNull(authorityService, "AuhtorityService cannot be null!");
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }

        List<Map<String, String>> groups = new ArrayList<>(authorities.size());
        for (String authority : authorities) {
            groups.add(getGroupProperties(authorityService, userService, authority));
        }

        return groups;
    }

    public static Map<String, Map<String, String>> getGroupsAsMapFromAuthorities(AuthorityService authorityService, UserService userService, Collection<String> authorities) {
        Assert.notNull(authorityService, "AuhtorityService cannot be null!");
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, String>> groupsByAuthority = new HashMap<>();
        for (String authority : authorities) {
            groupsByAuthority.put(authority, getGroupProperties(authorityService, userService, authority));
        }

        return groupsByAuthority;
    }

    private static Map<String, String> getGroupProperties(AuthorityService authorityService, UserService userService, String authority) {
        Map<String, String> authMap = new HashMap<>(7, 1.0f);

        String name = authorityService.getShortName(authority);
        authMap.put("name", name);
        authMap.put("id", authority);
        authMap.put("group", authority);
        authMap.put("groupName", name);
        authMap.put("displayName", authorityService.getAuthorityDisplayName(authority));
        Set<String> authorityZones = authorityService.getAuthorityZones(authority);
        authMap.put("structUnitBased", (authorityZones != null && authorityZones.contains(OrganizationStructureService.STRUCT_UNIT_BASED)) ? "true" : "false");
        authMap.put("deleteEnabled",  Boolean.toString(userService.isGroupDeleteAllowed(authority)));
        return authMap;
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

    public static String getDisplayUnitText(Serializable prop) {
        boolean isMultiValued = false;
        if (!((List) prop).isEmpty()) {
            for (Serializable listElement : ((List<Serializable>) prop)) {
                if (listElement != null && listElement instanceof List) {
                    isMultiValued = true;
                    break;
                }
            }
        }
        if (!isMultiValued) {
            @SuppressWarnings("unchecked")
            List<String> orgStructs = (List<String>) prop;
            return UserUtil.getDisplayUnit(orgStructs);
        }
        List<String> structUnitStr = new ArrayList<String>();
        for (List<String> orgStructs : (List<List<String>>) prop) {
            structUnitStr.add(UserUtil.getDisplayUnit(orgStructs));
        }
        return TextUtil.joinNonBlankStringsWithComma(structUnitStr);
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

    public static Set<String> getUsersInGroup(String group
            , NodeService nodeService
            , UserService userService
            , ParametersService parametersService
            , DocumentSearchService documentSearchService
            ) {
        Set<String> children = userService.getUserNamesInGroup(group);
        String structUnit = parametersService.getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT);
        if (StringUtils.isNotBlank(structUnit)) {
            List<NodeRef> res = documentSearchService.filterUsersInUserGroup(structUnit, children);
            children.clear();
            for (NodeRef nodeRef : res) {
                children.add((String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            }
        }
        return children;
    }

    /**
     * Used only for generating test data. If ever used somewhere else then move to service class and optimize performance.
     */
    @Deprecated
    public static List<Map<QName, Serializable>> getFilteredTaskOwnerStructUnitUsersProps(Set<String> usernames, NodeService nodeService, ParametersService parametersService,
            DocumentSearchService documentSearchService, UserService userService) {
        List<Map<QName, Serializable>> userProps = new ArrayList<Map<QName, Serializable>>();
        String structUnit = parametersService.getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT);
        if (StringUtils.isNotBlank(structUnit)) {
            List<NodeRef> res = documentSearchService.filterUsersInUserGroup(structUnit, usernames);
            for (NodeRef nodeRef : res) {
                userProps.add(nodeService.getProperties(nodeRef));
            }
        } else {
            for (String username : usernames) {
                userProps.add(userService.getUserProperties(username));
            }
        }
        return userProps;
    }

    public static String getUsernameAndSession(String userName, FacesContext context) {
        if (userName == null || userName.indexOf('_') > -1) {
            return userName;
        }

        if (context == null) {
            return userName;
        }

        String sessionIdentifier = "";

        String ticket = (String) ((ServletRequest) context.getExternalContext().getRequest()).getAttribute(WebDAVMethod.PARAM_TICKET);
        if (ticket != null) {
            sessionIdentifier = ticket;
        } else {
            final HttpSession httpSession = (HttpSession) context.getExternalContext().getSession(false);
            sessionIdentifier = (httpSession == null ? "" : httpSession.getId());
        }

        String userNameWithSessionId = userName + "_" + sessionIdentifier;
        return userNameWithSessionId;
    }

}
