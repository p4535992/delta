package ee.webmedia.alfresco.workflow.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.search.service.CompoundWorkflowSearchFilterService;

public class CompoundWorkflowSearchDialog extends AbstractSearchFilterBlockBean<CompoundWorkflowSearchFilterService> {

    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowSearchDialog.class);

    private SelectItem[] ownerSearchFilters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        if (ownerSearchFilters == null) {
            ownerSearchFilters = new SelectItem[] { new SelectItem(UserContactGroupSearchBean.USERS_FILTER, MessageUtil.getMessage("task_owner_users")),
                    new SelectItem(UserContactGroupSearchBean.CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts")), };
        }

        loadAllFilters();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getCompoundWorkflowSearchResultsDialog().setup(filter);
        super.isFinished = false;
        return AlfrescoNavigationHandler.DIALOG_PREFIX + "compoundWorkflowSearchResultsDialog";
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("task_search_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("task_search_saved");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("task_search");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("search");
    }

    @Override
    protected String getBlankFilterNameMessageKey() {
        return "task_search_save_error_nameIsBlank";
    }

    @Override
    protected String getFilterModifyDeniedMessageKey() {
        return "document_search_filter_modify_accessDenied";
    }

    @Override
    protected String getFilterDeleteDeniedMessageKey() {
        return "task_search_filter_delete_accessDenied";
    }

    @Override
    protected String getNewFilterSelectItemMessageKey() {
        return "task_search_new";
    }

    @Override
    protected Node getNewFilter() {
        // New empty filter
        Node node = new TransientNode(getFilterType(), null, null);
        Map<String, Object> props = node.getProperties();
        List<String> ownerNames = new ArrayList<String>();
        ownerNames.add("");
        props.put(CompoundWorkflowSearchModel.Props.OWNER_NAME.toString(), ownerNames);
        return node;
    }

    @Override
    protected QName getFilterType() {
        return CompoundWorkflowSearchModel.Types.FILTER;
    }

    /**
     * Getter for the task owner search picker filter.
     */
    public SelectItem[] getOwnerSearchFilters() {
        return ownerSearchFilters;
    }

    /**
     * Action listener for JSP.
     */
    public SelectItem[] executeOwnerSearch(PickerSearchParams params) {
        log.debug("executeOwnerSearch: " + params.getFilterIndex() + ", " + params.getSearchString());
        if (params.isFilterIndex(0)) { // users
            return BeanHelper.getUserListDialog().searchUsers(params);
        }
        throw new RuntimeException("Unknown filter index value: " + params.getFilterIndex());
    }

    /**
     * Action listener for JSP.
     * TODO this method might be put into an util or a central class
     */
    public List<String> processOwnerSearchResults(String searchResult) {
        log.debug("processOwnerSearchResults: " + searchResult);
        if (StringUtils.isBlank(searchResult)) {
            return null;
        }
        String name = null;
        if (searchResult.indexOf('/') > -1) { // contact
            NodeRef contact = new NodeRef(searchResult);
            Map<QName, Serializable> resultProps = getNodeService().getProperties(contact);
            QName resultType = getNodeService().getType(contact);
            if (resultType.equals(Types.ORGANIZATION)) {
                name = (String) resultProps.get(AddressbookModel.Props.ORGANIZATION_NAME);
            } else {
                name = UserUtil.getPersonFullName((String) resultProps.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) resultProps
                        .get(AddressbookModel.Props.PERSON_LAST_NAME));
            }
        } else { // user
            Map<QName, Serializable> personProps = getUserService().getUserProperties(searchResult);
            name = UserUtil.getPersonFullName1(personProps);
        }
        return Collections.singletonList(name);
    }

    /**
     * Action listener for JSP.
     */
    public void processCreatorSearchResults(String username) {
        log.debug("processCreatorSearchResults: " + username);
        Map<QName, Serializable> personProps = getUserService().getUserProperties(username);
        filter.getProperties().put(TaskSearchModel.Props.CREATOR_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    @Override
    public void clean() {
        super.clean();
        ownerSearchFilters = null;
    }

    // START: getters / setters

    @Override
    protected CompoundWorkflowSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getCompoundWorkflowSearchFilterService();
        }
        return filterService;
    }

    // END: getters / setters

}
