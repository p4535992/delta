<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.CONTACTS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.CONTACT_GROUPS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.USERS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.USER_GROUPS_FILTER;

import java.io.Serializable;

import javax.faces.model.SelectItem;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean that can be used to search owners(users, -groups, contacts, -groups)
 * 
 * @author Ats Uiboupin
 */
public class OwnerSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "OwnerSearchBean";
    private SelectItem[] ownerSearchFilters;
    private SelectItem[] responsibleOwnerSearchFilters;
    private SelectItem[] reviewOwnerSearchFilters;

    public void init() {
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getResponsibleOwnerSearchFilters() {
        if (responsibleOwnerSearchFilters == null) {
            responsibleOwnerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
        return responsibleOwnerSearchFilters;
    }

    public SelectItem[] getReviewOwnerSearchFilters() {
        if (reviewOwnerSearchFilters == null) {
            reviewOwnerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups"))
            };
        }
        return reviewOwnerSearchFilters;
    }

    public SelectItem[] getOwnerSearchFilters() {
        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups"))
            };
        }
        return ownerSearchFilters;
    }

}
=======
package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.CONTACTS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.CONTACT_GROUPS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.USERS_FILTER;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.USER_GROUPS_FILTER;

import java.io.Serializable;

import javax.faces.model.SelectItem;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean that can be used to search owners(users, -groups, contacts, -groups)
 */
public class OwnerSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "OwnerSearchBean";
    private SelectItem[] ownerSearchFilters;
    private SelectItem[] responsibleOwnerSearchFilters;
    private SelectItem[] reviewOwnerSearchFilters;

    public void init() {
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getResponsibleOwnerSearchFilters() {
        if (responsibleOwnerSearchFilters == null) {
            responsibleOwnerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
        return responsibleOwnerSearchFilters;
    }

    public SelectItem[] getReviewOwnerSearchFilters() {
        if (reviewOwnerSearchFilters == null) {
            reviewOwnerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups"))
            };
        }
        return reviewOwnerSearchFilters;
    }

    public SelectItem[] getOwnerSearchFilters() {
        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] {
                    new SelectItem(USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups")),
                    new SelectItem(CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups"))
            };
        }
        return ownerSearchFilters;
    }

}
>>>>>>> develop-5.1
