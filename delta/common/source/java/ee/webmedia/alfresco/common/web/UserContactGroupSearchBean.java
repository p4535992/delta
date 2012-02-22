package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactMappingService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.common.component.PickerSearchParams;

import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Alar Kvell
 */
public class UserContactGroupSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserContactGroupSearchBean";

    private SelectItem[] usersGroupsFilters;
    private SelectItem[] contactsGroupsFilters;
    private SelectItem[] usersContactsFilters;
    private SelectItem[] usersGroupsContactsGroupsFilters;

    public static final int USERS_FILTER = 0;
    public static final int USER_GROUPS_FILTER = 1;
    public static final int CONTACTS_FILTER = 2;
    public static final int CONTACT_GROUPS_FILTER = 3;

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
        if (params.isFilterIndex(USERS_FILTER)) {
            return BeanHelper.getUserListDialog().searchUsers(params);
        } else if (params.isFilterIndex(USER_GROUPS_FILTER)) {
            return searchGroups(params, withAdminsAndDocManagers);
        } else if (params.isFilterIndex(CONTACTS_FILTER)) {
            return BeanHelper.getAddressbookSearchBean().searchContacts(params);
        } else if (params.isFilterIndex(CONTACT_GROUPS_FILTER)) {
            return BeanHelper.getAddressbookSearchBean().searchContactGroups(params);
        }
        throw new RuntimeException("filterIndex out of range: " + params.getFilterIndex());
    }

    public SelectItem[] searchGroups(PickerSearchParams params, boolean withAdminsAndDocManagers) {
        List<Authority> results = BeanHelper.getDocumentSearchService().searchAuthorityGroups(params.getSearchString(), true, withAdminsAndDocManagers, params.getLimit());
        SelectItem[] selectItems = new SelectItem[results.size()];
        int i = 0;
        for (Authority authority : results) {
            selectItems[i++] = new SelectItem(authority.getAuthority(), authority.getName());
        }
        WebUtil.sort(selectItems);
        return selectItems;
    }

    /*
     * preprocessCallback methods
     */
    public String[] preprocessResultsToNodeRefs(int filterIndex, String[] results) {
        List<String> processedResult = new ArrayList<String>();
        if (results != null) {
            for (String result : results) {
                if (filterIndex == USERS_FILTER) {
                    // Replace user name with reference to the person node
                    NodeRef nodeRef = getUserService().getPerson(result);
                    if (nodeRef != null) {
                        processedResult.add(nodeRef.toString());
                    }
                } else if (filterIndex == USER_GROUPS_FILTER) {
                    // Add all users contained in user group and replace user names with reference to the person node
                    Set<String> auths = getUserService().getUserNamesInGroup(result);
                    for (String auth : auths) {
                        NodeRef nodeRef = getUserService().getPerson(auth);
                        if (nodeRef != null) {
                            processedResult.add(nodeRef.toString());
                        }
                    }
                } else if (filterIndex == CONTACTS_FILTER) {
                    // Add contact
                    processedResult.add(result);
                } else if (filterIndex == CONTACT_GROUPS_FILTER) {
                    // Add all contacts contained in contact group
                    List<NodeRef> contacts = getAddressbookService().getContactGroupContents(new NodeRef(result));
                    for (NodeRef contact : contacts) {
                        processedResult.add(contact.toString());
                    }
                } else {
                    throw new RuntimeException("filterIndex out of range: " + filterIndex);
                }
            }
        }
        return processedResult.toArray(new String[processedResult.size()]);
    }

    public String[] preprocessResultsToNames(int filterIndex, String[] results) {
        results = preprocessResultsToNodeRefs(filterIndex, results);
        List<String> processedResult = new ArrayList<String>();
        for (String result : results) {
            Object nameObj = getUserContactMappingService().getMappedNameValue(new NodeRef(result));
            if (nameObj != null && nameObj instanceof String) {
                processedResult.add((String) nameObj);
            }
        }
        return processedResult.toArray(new String[processedResult.size()]);
    }

}
