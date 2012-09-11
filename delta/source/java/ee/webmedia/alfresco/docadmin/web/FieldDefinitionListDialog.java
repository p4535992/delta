package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for the list of field definitions
 * 
 * @author Ats Uiboupin
 */
public class FieldDefinitionListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private List<FieldDefinition> fieldDefinitions;
    private String searchCriteria;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initFields();
    }

    private void initFields() {
        cancel();
        fieldDefinitions = getDocumentAdminService().getFieldDefinitions();
    }

    @Override
    public void restored() {
        initFields();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        fieldDefinitions = getDocumentAdminService().saveOrUpdateFieldDefinitions(fieldDefinitions);
        MessageUtil.addInfoMessage(context, "save_success");
        return null;
    }

    public void search() {
        if (StringUtils.isBlank(searchCriteria)) {
            MessageUtil.addInfoMessage("fieldDefinitions_list_search_empty_search");
            return;
        }
        fieldDefinitions = getDocumentAdminService().searchFieldDefinitions(searchCriteria);
    }

    public void showAll() {
        restored();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String cancel() {
        fieldDefinitions = null;
        searchCriteria = null;
        return super.cancel();
    }

    public void deleteField(ActionEvent event) {
        NodeRef fieldDefRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentAdminService().deleteFieldDefinition(fieldDefRef);
        MessageUtil.addInfoMessage("fieldDefinitions_list_action_delete_success");
        initFields();
    }

    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<FieldDefinition> getFieldDefinitions() {
        return fieldDefinitions;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    // END: getters / setters

}
