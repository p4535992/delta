<<<<<<< HEAD
package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactMappingService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Alar Kvell
 */
public class UserContactGroupSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserContactGroupSearchBean";
    public static final String FILTER_INDEX_SEPARATOR = "¤";

    private SelectItem[] usersGroupsFilters;
    private SelectItem[] contactsGroupsFilters;
    private SelectItem[] usersContactsFilters;
    private SelectItem[] usersGroupsContactsGroupsFilters;

    // Filters
    public static final int USERS_FILTER = 1;
    public static final int USER_GROUPS_FILTER = 2;
    public static final int CONTACTS_FILTER = 4;
    public static final int CONTACT_GROUPS_FILTER = 8;

    /*
     * filters methods
     */

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsFilters() {
        if (usersGroupsFilters == null) {
            usersGroupsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
            };
        }
        return usersGroupsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getContactsGroupsFilters() {
        if (contactsGroupsFilters == null) {
            contactsGroupsFilters = new SelectItem[] {
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups")),
            };
        }
        return contactsGroupsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersContactsFilters() {
        if (usersContactsFilters == null) {
            usersContactsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
        return usersContactsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsContactsGroupsFilters() {
        if (usersGroupsContactsGroupsFilters == null) {
            usersGroupsContactsGroupsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups")),
            };
        }
        return usersGroupsContactsGroupsFilters;
    }

    /*
     * pickerCallback methods
     */

    public SelectItem[] searchAll(PickerSearchParams params) {
        return searchAll(params, false);
    }

    public SelectItem[] searchAllWithAdminsAndDocManagers(PickerSearchParams params) {
        return searchAll(params, true);
    }

    public SelectItem[] searchGroupsWithAdminsAndDocManagers(PickerSearchParams params) {
        return searchGroups(params, true);
    }

    /*
     * Methods that can be used programmatically
     */

    // TODO merge CompoundWorkflowDefinitionDialog#executeOwnerSearch to here
    private SelectItem[] searchAll(PickerSearchParams params, boolean withAdminsAndDocManagers) {
        SelectItem[] results = new SelectItem[0];
        if (params.isFilterIndex(USERS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getUserListDialog().searchUsers(params));
        }
        if (params.isFilterIndex(USER_GROUPS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, searchGroups(params, withAdminsAndDocManagers));
        }
        if (params.isFilterIndex(CONTACTS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getAddressbookSearchBean().searchContacts(params));
        }
        if (params.isFilterIndex(CONTACT_GROUPS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getAddressbookSearchBean().searchContactGroups(params));
        }

        return results;
    }

    public SelectItem[] searchGroups(PickerSearchParams params, boolean withAdminsAndDocManagers) {
        List<Authority> results = BeanHelper.getDocumentSearchService().searchAuthorityGroups(params.getSearchString(), true, withAdminsAndDocManagers, params.getLimit());
        SelectItem[] selectItems = new SelectItem[results.size()];
        int i = 0;
        for (Authority authority : results) {
            String auth = params.isIncludeFilterIndex() ? (authority.getAuthority() + FILTER_INDEX_SEPARATOR + USER_GROUPS_FILTER) : authority.getAuthority();
            selectItems[i++] = new SelectItem(auth, authority.getName());
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    /*
     * preprocessCallback methods
     */
    public List<Pair<String, String>> preprocessResultsToNodeRefs(int filterIndex, String[] results) {
        List<Pair<String, String>> processedResult = new ArrayList<Pair<String, String>>();
        if (results != null) {
            for (String result : results) {
                if (filterIndex == USERS_FILTER) {
                    // Replace user name with reference to the person node
                    NodeRef nodeRef = getUserService().getPerson(result);
                    if (nodeRef != null) {
                        processedResult.add(new Pair<String, String>(null, nodeRef.toString()));
                    }
                } else if (filterIndex == USER_GROUPS_FILTER) {
                    // Add all users contained in user group and replace user names with reference to the person node
                    Set<String> auths = getUserService().getUserNamesInGroup(result);
                    String groupName = null;
                    Authority group = getUserService().getAuthorityOrNull(result);
                    if (group != null) {
                        groupName = group.getName();
                    }
                    for (String auth : auths) {
                        NodeRef nodeRef = getUserService().getPerson(auth);
                        if (nodeRef != null) {
                            processedResult.add(new Pair<String, String>(groupName, nodeRef.toString()));
                        }
                    }
                } else if (filterIndex == CONTACTS_FILTER) {
                    // Add contact
                    processedResult.add(new Pair<String, String>(null, result));
                } else if (filterIndex == CONTACT_GROUPS_FILTER) {
                    // Add all contacts contained in contact group
                    NodeRef contactGroupRef = new NodeRef(result);
                    List<NodeRef> contacts = getAddressbookService().getContactGroupContents(contactGroupRef);
                    String groupName = (String) BeanHelper.getNodeService().getProperty(contactGroupRef, AddressbookModel.Props.GROUP_NAME);
                    for (NodeRef contact : contacts) {
                        processedResult.add(new Pair<String, String>(groupName, contact.toString()));
                    }
                } else {
                    throw new RuntimeException("filterIndex out of range: " + filterIndex);
                }
            }
        }
        return processedResult;
    }

    public List<Pair<String, String>> preprocessResultsToNames(int filterIndex, String[] results) {
        List<Pair<String, String>> processedResult = preprocessResultsToNodeRefs(filterIndex, results);
        for (Iterator<Pair<String, String>> iterator = processedResult.iterator(); iterator.hasNext();) {
            Pair<String, String> pair = iterator.next();
            Object nameObj = getUserContactMappingService().getMappedNameValue(new NodeRef(pair.getSecond()));
            if (nameObj != null && nameObj instanceof String) {
                pair.setSecond((String) nameObj);
            } else {
                iterator.remove();
            }
        }
        return processedResult;
    }

}
=======
package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactMappingService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class UserContactGroupSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserContactGroupSearchBean";
    public static final String FILTER_INDEX_SEPARATOR = "¤";

    private SelectItem[] usersGroupsFilters;
    private SelectItem[] contactsGroupsFilters;
    private SelectItem[] usersContactsFilters;
    private SelectItem[] usersGroupsContactsGroupsFilters;

    // Filters
    public static final int USERS_FILTER = 1;
    public static final int USER_GROUPS_FILTER = 2;
    public static final int CONTACTS_FILTER = 4;
    public static final int CONTACT_GROUPS_FILTER = 8;

    /*
     * filters methods
     */

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsFilters() {
        if (usersGroupsFilters == null) {
            usersGroupsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
            };
        }
        return usersGroupsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getContactsGroupsFilters() {
        if (contactsGroupsFilters == null) {
            contactsGroupsFilters = new SelectItem[] {
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups")),
            };
        }
        return contactsGroupsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersContactsFilters() {
        if (usersContactsFilters == null) {
            usersContactsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
        return usersContactsFilters;
    }

    /**
     * Property accessed by the Generic Picker component.
     * 
     * @return the array of filter options to show in the users/groups picker
     */
    public SelectItem[] getUsersGroupsContactsGroupsFilters() {
        if (usersGroupsContactsGroupsFilters == null) {
            usersGroupsContactsGroupsFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups")),
            };
        }
        return usersGroupsContactsGroupsFilters;
    }

    /*
     * pickerCallback methods
     */

    public SelectItem[] searchAll(PickerSearchParams params) {
        return searchAll(params, false);
    }

    public SelectItem[] searchAllWithAdminsAndDocManagers(PickerSearchParams params) {
        return searchAll(params, true);
    }

    public SelectItem[] searchGroupsWithAdminsAndDocManagers(PickerSearchParams params) {
        return searchGroups(params, true);
    }

    public SelectItem[] searchAllWithoutLogOnUser(PickerSearchParams params) {
        SelectItem[] results = BeanHelper.getUserListDialog().searchUsersWithoutCurrentUser(params);
        return results;
    }

    /*
     * Methods that can be used programmatically
     */

    // TODO merge CompoundWorkflowDefinitionDialog#executeOwnerSearch to here
    private SelectItem[] searchAll(PickerSearchParams params, boolean withAdminsAndDocManagers) {
        SelectItem[] results = new SelectItem[0];
        if (params.isFilterIndex(USERS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getUserListDialog().searchUsers(params));
        }
        if (params.isFilterIndex(USER_GROUPS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, searchGroups(params, withAdminsAndDocManagers));
        }
        if (params.isFilterIndex(CONTACTS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getAddressbookSearchBean().searchContacts(params));
        }
        if (params.isFilterIndex(CONTACT_GROUPS_FILTER)) {
            results = (SelectItem[]) ArrayUtils.addAll(results, BeanHelper.getAddressbookSearchBean().searchContactGroups(params));
        }

        return results;
    }

    public SelectItem[] searchGroups(PickerSearchParams params, boolean withAdminsAndDocManagers) {
        List<Authority> results = BeanHelper.getDocumentSearchService().searchAuthorityGroups(params.getSearchString(), true, withAdminsAndDocManagers, params.getLimit());
        SelectItem[] selectItems = new SelectItem[results.size()];
        int i = 0;
        for (Authority authority : results) {
            String auth = params.isIncludeFilterIndex() ? (authority.getAuthority() + FILTER_INDEX_SEPARATOR + USER_GROUPS_FILTER) : authority.getAuthority();
            selectItems[i++] = new SelectItem(auth, authority.getName());
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    /*
     * preprocessCallback methods
     */
    public List<Pair<String, String>> preprocessResultsToNodeRefs(int filterIndex, String[] results) {
        List<Pair<String, String>> processedResult = new ArrayList<Pair<String, String>>();
        if (results != null) {
            for (String result : results) {
                if (filterIndex == USERS_FILTER) {
                    // Replace user name with reference to the person node
                    NodeRef nodeRef = getUserService().getPerson(result);
                    if (nodeRef != null) {
                        processedResult.add(new Pair<String, String>(null, nodeRef.toString()));
                    }
                } else if (filterIndex == USER_GROUPS_FILTER) {
                    // Add all users contained in user group and replace user names with reference to the person node
                    Set<String> auths = getUserService().getUserNamesInGroup(result);
                    String groupName = null;
                    Authority group = getUserService().getAuthorityOrNull(result);
                    if (group != null) {
                        groupName = group.getName();
                    }
                    for (String auth : auths) {
                        NodeRef nodeRef = getUserService().getPerson(auth);
                        if (nodeRef != null) {
                            processedResult.add(new Pair<String, String>(groupName, nodeRef.toString()));
                        }
                    }
                } else if (filterIndex == CONTACTS_FILTER) {
                    // Add contact
                    processedResult.add(new Pair<String, String>(null, result));
                } else if (filterIndex == CONTACT_GROUPS_FILTER) {
                    // Add all contacts contained in contact group
                    NodeRef contactGroupRef = new NodeRef(result);
                    List<NodeRef> contacts = getAddressbookService().getContactGroupContents(contactGroupRef);
                    String groupName = (String) BeanHelper.getNodeService().getProperty(contactGroupRef, AddressbookModel.Props.GROUP_NAME);
                    for (NodeRef contact : contacts) {
                        processedResult.add(new Pair<String, String>(groupName, contact.toString()));
                    }
                } else {
                    throw new RuntimeException("filterIndex out of range: " + filterIndex);
                }
            }
        }
        return processedResult;
    }

    public List<Pair<String, String>> preprocessResultsToNames(int filterIndex, String[] results) {
        List<Pair<String, String>> processedResult = preprocessResultsToNodeRefs(filterIndex, results);
        for (Iterator<Pair<String, String>> iterator = processedResult.iterator(); iterator.hasNext();) {
            Pair<String, String> pair = iterator.next();
            Object nameObj = getUserContactMappingService().getMappedNameValue(new NodeRef(pair.getSecond()));
            if (nameObj != null && nameObj instanceof String) {
                pair.setSecond((String) nameObj);
            } else {
                iterator.remove();
            }
        }
        return processedResult;
    }

}
>>>>>>> develop-5.1
