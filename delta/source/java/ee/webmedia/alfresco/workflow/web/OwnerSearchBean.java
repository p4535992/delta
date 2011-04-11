package ee.webmedia.alfresco.workflow.web;

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

    public void init() {
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getResponsibleOwnerSearchFilters() {
        if (responsibleOwnerSearchFilters == null) {
            responsibleOwnerSearchFilters = new SelectItem[] {
                    new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(1, MessageUtil.getMessage("task_owner_contacts")),
            };
        }
        return responsibleOwnerSearchFilters;
    }

    public SelectItem[] getOwnerSearchFilters() {
        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] {
                    new SelectItem(0, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(1, MessageUtil.getMessage("task_owner_usergroups")),
                    new SelectItem(2, MessageUtil.getMessage("task_owner_contacts")),
                    new SelectItem(3, MessageUtil.getMessage("task_owner_contactgroups"))
            };
        }
        return ownerSearchFilters;
    }

}
