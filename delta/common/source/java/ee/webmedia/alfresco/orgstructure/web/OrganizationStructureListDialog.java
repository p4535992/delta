package ee.webmedia.alfresco.orgstructure.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.utils.UserUtil;

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
            String organizationDisplayPath = UserUtil.getDisplayUnit(struct.getOrganizationPath());
            results[i++] = new SelectItem(Integer.toString(struct.getUnitId()), StringUtils.isNotBlank(organizationDisplayPath) ? organizationDisplayPath : struct.getName());
            if (i == params.getLimit()) {
                break;
            }
        }
        List<SelectItem> resultList = Arrays.asList(results);
        Collections.sort(resultList, new Comparator<SelectItem>() {

            @Override
            public int compare(SelectItem s1, SelectItem s2) {
                return AppConstants.DEFAULT_COLLATOR.compare(s1.getLabel(), s1.getLabel());
            }
        });
        return resultList.toArray(new SelectItem[resultList.size()]);
    }

    public String[] preprocessResultsToPaths(int filterIndex, String[] results) {
        List<String> organizationPaths = new ArrayList<String>();
        if (results != null && results.length > 0) {
            addPaths(results, organizationPaths, 0, false);

        }
        return organizationPaths.toArray(new String[organizationPaths.size()]);
    }

    private void addPaths(String[] results, List<String> organizationPaths, int index, boolean longestOnly) {
        try {
            Integer unitId = Integer.parseInt(results[index]);
            OrganizationStructure orgStruct = getOrganizationStructureService().getOrganizationStructure(unitId);
            if (longestOnly) {
                organizationPaths.add(orgStruct.getOrganizationDisplayPath());
            } else {
                organizationPaths.addAll(orgStruct.getOrganizationPath());
            }
            if (organizationPaths.isEmpty()) {
                organizationPaths.add(orgStruct.getName());
            }
        } catch (NumberFormatException e) {

        }
    }

    public String[] preprocessResultsToLongestNames(int filterIndex, String[] results) {
        List<String> organizationPaths = new ArrayList<String>();
        if (results != null) {
            for (int i = 0; i < results.length; i++) {
                addPaths(results, organizationPaths, i, true);
            }

        }
        return organizationPaths.toArray(new String[organizationPaths.size()]);
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
