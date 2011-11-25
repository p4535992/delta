package ee.webmedia.alfresco.orgstructure.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.PickerSearchParams;

import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;

public class OrganizationStructureListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

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
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchOrgstructs(PickerSearchParams params) {
        List<OrganizationStructure> structs = getOrganizationStructureService().searchOrganizationStructures(params.getSearchString(), params.getLimit());
        SelectItem[] results = new SelectItem[structs.size() > params.getLimit() ? params.getLimit() : structs.size()];
        int i = 0;
        for (OrganizationStructure struct : structs) {
            results[i++] = new SelectItem(Integer.toString(struct.getUnitId()), struct.getName());
            if (i == params.getLimit()) {
                break;
            }
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

    // END: getters / setters
}
