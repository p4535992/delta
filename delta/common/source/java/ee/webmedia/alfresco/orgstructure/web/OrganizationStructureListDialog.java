package ee.webmedia.alfresco.orgstructure.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;

public class OrganizationStructureListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient OrganizationStructureService organizationStructureService;
    private List<OrganizationStructure> orgstructs;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        orgstructs = getOrganizationStructureService().getAllOrganizationStructures();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // save button not used or shown
        return null;
    }

    @Override
    public String cancel() {
        orgstructs = null;
        return super.cancel();
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchOrgstructs(int filterIndex, String contains) {
        List<OrganizationStructure> structs = getOrganizationStructureService().searchOrganizationStructures(contains);
        SelectItem[] results = new SelectItem[structs.size()];
        int i = 0;
        for (OrganizationStructure struct : structs) {
            results[i++] = new SelectItem(Integer.toString(struct.getUnitId()), struct.getName());
        }
        return results;
    }

    // START: getters / setters

    /**
     * Used in JSP pages.
     */
    public List<OrganizationStructure> getOrgstructs() {
        return orgstructs;
    }

    protected OrganizationStructureService getOrganizationStructureService() {
        if (organizationStructureService == null) {
            organizationStructureService = (OrganizationStructureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(OrganizationStructureService.BEAN_NAME);
        }
        return organizationStructureService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }
    // END: getters / setters
}
